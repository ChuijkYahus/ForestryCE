# Phase 9b: six jars

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn 9a's six source sets into six installable artifacts: six mod ids, per-jar resources,
per-jar datagen, and a base install that boots with any subset of the content jars present.

**Architecture:** 9a made the *code* boundary real. 9b makes the *artifact* boundary real, and the
gap between those two is where every remaining problem lives - the couplings that are carried as data
rather than as types, which no compiler and no gate in this project has ever been able to see.

**Tech Stack:** Java 21, NeoForge 21.1.230, Minecraft 1.21.1, ModDevGradle 2.0.140, Gradle 9.2.1,
Groovy DSL. GameTests only, no JUnit.

## Global Constraints

- Comment and Javadoc style is binding; see `CLAUDE.md`. ASCII only. Lowercase `todo`.
- `./gradlew runGameTestServer` reports **all 108 tests passed** at the end of every task.
- `checkApiBoundary`, `checkCoreLayers` and `checkResourceFqcn` stay green.
- `./gradlew runData` produces no diff *except* where a task enumerates one. Tasks 1 and 6 enumerate
  theirs; everything else is byte-identical.
- All source files are LF. Do not write `$`-anchored `sed` patterns.
- **The api ships whole in core** (D3) and never splits. `forestry.api.apiculture` living in the core
  jar is not a bug to fix; it is the mechanism that makes a missing content jar safe.

## The ordering constraint that shapes this phase

Task 1 is a prerequisite for the entire rest of the phase, and it is not build work. Until it lands,
splitting the jars produces an artifact set that is *provably broken* for the configuration the whole
project exists to support - lepidopterology installed without apiculture. Do it first, verify it, and
only then touch packaging.

## Starting state, measured 2026-08-02

| Fact | Value |
| --- | --- |
| Source sets | 6 + `datagen` + `test`; the compiler enforces D1 |
| Mod ids | 1 (`forestry`) |
| Hand-authored resources | 1,960, of which 1,590 textures and 211 models |
| Generated resources | 10,232 |
| Service file entries | 6 plugins in one `META-INF/services/forestry.api.plugin.IForestryPlugin` |
| Data runs | 1, one `--output`, `--mod forestry` |
| Locales | 11; `en_us` generated+merged, the other 10 hand-authored monoliths |

### The blocking finding, restated precisely

`ButterflyChromosomes.FLOWER_TYPE` **is** `BeeChromosomes.FLOWER_TYPE`, whose resolver is:

```java
ChromosomeFactory.referenceChromosome(forestry("flower_type"), id -> BEE_TYPE.get().getFlowerType(id), IFlowerType::isDominant)
```

So resolving a butterfly's flower type goes through `SpeciesUtil.BEE_TYPE`, which resolves the *bee
species type* out of the genetic manager. `getFlowerType` is declared on `IBeeSpeciesType`. Without
apiculture there is no bee species type, so every butterfly fails to project.

This is not an ordering bug - 9a already fixed the ordering half by making module load order depend on
ids rather than on classpath scan order. This is a structural one: **flower types are modelled as a
bee concept and are not one.** Bees and butterflies both pollinate.

Audited at the same time, so it is not rediscovered: `ButterflyChromosomes` borrows six chromosomes
from `BeeChromosomes`, and `FLOWER_TYPE` is the **only** one that is `ResourceLocation`-valued. The
other five - `SPEED`, `FERTILITY`, `TEMPERATURE_TOLERANCE`, `HUMIDITY_TOLERANCE`, `TOLERATES_RAIN` -
are plain values with no registry lookup and cross no boundary. `BeeChromosomes.FIREPROOF` borrows
from `ButterflyChromosomes` and is a `Boolean`. Every other `ResourceLocation` chromosome
(`SPECIES`, `ACTIVITY`, `EFFECT`, `COCOON`, `FRUIT`) resolves within its own jar. **`FLOWER_TYPE` is
the only one.**

---

### Task 1: Flower types become a core concept

**Files:**
- Create: `src/main/java/forestry/api/core/genetics/IFlowerTypeManager.java`
- Modify: `src/main/java/forestry/api/IForestryApi.java` (add `getFlowerTypeManager()`)
- Modify: `src/main/java/forestry/api/apiculture/genetics/IBeeSpeciesType.java` (remove the two getters)
- Modify: `src/main/java/forestry/api/core/genetics/alleles/BeeChromosomes.java` (retarget the resolver)
- Move: `FlowerTypeTypes`, `FlowerTypeManager` -> `forestry.core.engine.genetics`
- Move: `TagFlowerType`, `WaterTagFlowerType`, `PhotosynthesisFlowerType` -> `forestry.core.engine.genetics.flowers`
- Move: `FlowerTypeSyncPacket` -> `forestry.core.platform.network.packets`
- Modify: `ApicultureForestryPlugin`, `ModuleApiculture`, `ApicultureReloadHandler`, `BeeSpeciesType`
- Modify: `src/datagen/java/forestry/core/data/FlowerTypeProvider.java`

**Interfaces:**
- Produces: `IFlowerTypeManager` with `getFlowerType(ResourceLocation)`,
  `getFlowerTypeSafe(ResourceLocation)`, `getAllFlowerTypes()`, and an impl-side `setFlowerTypes(Map)`.
  Reached via `IForestryApi.INSTANCE.getFlowerTypeManager()`.
- Consumes: `IFlowerType` and `FlowerTypeType`, which stay in `forestry.api.apiculture` - moving them
  is a larger api break for no benefit, since api ships whole in core.

**Enumerated datagen diff:** none expected. `FlowerTypeProvider` writes
`data/forestry/flower_type/*.json` from the same definitions; only the class that owns the registry
changes. If the tree diffs, the definitions were altered - that is a defect.

- [ ] **Step 1: Write a failing GameTest**

Add to `src/test/java/forestry/gametest/FlowerTypeTest.java` a test that resolves a flower type
without touching the bee species type:

```java
	@GameTest(template = "forestry:empty")
	public static void flowerTypesResolveWithoutBeeSpeciesType(GameTestHelper helper) {
		IFlowerType vanilla = IForestryApi.INSTANCE.getFlowerTypeManager()
				.getFlowerType(ForestryConstants.forestry("flower_type_vanilla"));
		helper.assertTrue(vanilla != null, "flower_type_vanilla must resolve from the core manager");
		helper.succeed();
	}
```

- [ ] **Step 2: Run it and watch it fail to compile**

```bash
./gradlew compileTestJava --console=plain 2>&1 | grep -E 'error:|BUILD' | head -5
```

Expected: `cannot find symbol: method getFlowerTypeManager()`. That is the test failing in the only
way it can before the interface exists.

- [ ] **Step 3: Add the api interface**

`src/main/java/forestry/api/core/genetics/IFlowerTypeManager.java`:

```java
package forestry.api.core.genetics;

import java.util.Map;

import net.minecraft.resources.ResourceLocation;

import forestry.api.apiculture.IFlowerType;

/**
 * Used to look up the flower types that pollinating species search for. Bees and butterflies both
 * carry a flower type chromosome, so this is owned by base rather than by either module.
 *
 * @since 2.10.0
 */
public interface IFlowerTypeManager {
	/**
	 * Used to get a flower type by id.
	 *
	 * @param id The id of the flower type
	 * @return The flower type, or null if no flower type was registered with that id
	 */
	@Nullable
	IFlowerType getFlowerType(ResourceLocation id);

	/**
	 * Used to get a flower type by id, falling back to the vanilla flower type.
	 *
	 * @param id The id of the flower type
	 * @return The flower type, or the vanilla flower type if none was registered with that id
	 */
	IFlowerType getFlowerTypeSafe(ResourceLocation id);

	/**
	 * Used to get every registered flower type.
	 *
	 * @return The flower types, keyed by id
	 */
	Map<ResourceLocation, IFlowerType> getAllFlowerTypes();
}
```

Add the getter to `IForestryApi`, matching the style of its siblings:

```java
	/**
	 * Used to get the manager for flower types.
	 *
	 * @return The flower type manager
	 * @since 2.10.0
	 */
	IFlowerTypeManager getFlowerTypeManager();
```

- [ ] **Step 4: Move the five implementation classes**

```bash
S=<scratchpad>
$S/move-class.sh forestry.apiculture.bees.genetics.FlowerTypeTypes   forestry.core.engine.genetics.FlowerTypeTypes
$S/move-class.sh forestry.apiculture.bees.genetics.FlowerTypeManager forestry.core.engine.genetics.FlowerTypeManager
for c in TagFlowerType WaterTagFlowerType PhotosynthesisFlowerType; do
  $S/move-class.sh forestry.apiculture.bees.$c forestry.core.engine.genetics.flowers.$c
done
$S/move-class.sh forestry.apiculture.network.packets.FlowerTypeSyncPacket \
                 forestry.core.platform.network.packets.FlowerTypeSyncPacket
```

`move-class.sh` and `rewrite-refs.sh` are the phase-7 helpers; recreate them in the scratchpad if the
session is new. Create `core/engine/genetics/flowers/package-info.java` from the standard template.

The packet moves because a packet whose payload is the flower-type map must be registered by whichever
jar owns the map. Its id must not change - check `PacketIdServer`/`PacketIdClient` after the move.

- [ ] **Step 5: Give the manager a home in `ForestryApiImpl`**

Add a `FlowerTypeManager` field alongside the other managers. It is **not** a `Fake*`/`isLoaded()`
case per D7: base always owns flower types now, so there is always a real implementation.

- [ ] **Step 6: Retarget the chromosome resolver**

In `BeeChromosomes`:

```java
	public static final IChromosome<ResourceLocation> FLOWER_TYPE = ChromosomeFactory.referenceChromosome(forestry("flower_type"), id -> IForestryApi.INSTANCE.getFlowerTypeManager().getFlowerType(id), IFlowerType::isDominant);
```

Then delete `getFlowerType` and `getFlowerTypeSafe` from `IBeeSpeciesType` and their implementations
in `BeeSpeciesType`, and repoint every caller at the manager.

- [ ] **Step 7: Move registration out of the apiculture plugin**

`ApicultureForestryPlugin:70` calls `FlowerTypeTypes.registerBuiltins()`; `ApicultureReloadHandler`
calls it again as an idempotent safety net, and `ModuleApiculture` registers the reload listener. All
three move to core - `DefaultForestryPlugin` and `ModuleCore` respectively. **This is what actually
fixes the bug**: after this, no butterfly resolution path passes through apiculture.

- [ ] **Step 8: Compile, and confirm the boundary the compiler can now see**

```bash
./gradlew build --console=plain 2>&1 | grep -E 'check[ACR]|BUILD|error:'
grep -rn 'FlowerType' src/lepidopterology/java --include='*.java' | grep -i apiculture \
  || echo "lepidopterology names no apiculture flower type"
```

- [ ] **Step 9: Oracles, including the new test**

```bash
./gradlew runData --console=plain 2>&1 | tail -2; git diff --stat src/generated/resources
./gradlew runGameTestServer --console=plain 2>&1 | grep -E 'required tests|BUILD' | tail -3
```

Expected: **empty** datagen diff, and `All 109 required tests passed` - 108 plus the new one.

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "core: flower types are a pollination concept, not a bee one"
```

---

### Task 2: Choose the per-jar resource strategy

The oldest open question in the build story, flagged twice in the spec and never answered: a data run
takes one `--output`, and six jars need six resource trees.

**Prototype before planning further.** Two candidate strategies, and the choice must be made on
evidence, not preference:

- **A. Six data runs.** Each content jar gets its own `@EventBusSubscriber`-carrying `Data` class under
  its own mod id, its own `runData<Name>`, its own `--output` and `--existing` chain. Faithful to how
  NeoForge intends datagen to work; costs six run configurations and requires the providers to be
  partitioned by owning jar first, which is the expensive part.
- **B. One data run, post-hoc split.** Keep one `runData` writing to `src/generated/resources`, then a
  Gradle task partitions that tree into six per-jar resource directories using a path-to-jar mapping.
  Cheap to build, but the mapping is the whole risk and it has no compile-time oracle.

**The mapping is derivable and must not be hand-written.** Every feature registers under a module id
already (`ModFeatureRegistry.get(ForestryModuleIds.APICULTURE)`), so the owning jar of every registry
id is known *in code*. The honest way to get the mapping is to have datagen emit it: a provider that
writes `registry id -> module id` for every registered block, item, tile and menu, from the same
registry the features were created from.

- [ ] **Step 1: Measure how much of the generated tree is addressable by registry id**

```bash
find src/generated/resources -type f | wc -l
find src/generated/resources -path '*/models/*' -o -path '*/blockstates/*' -o -path '*/recipe/*' \
  -o -path '*/loot_table/*' -o -path '*/advancement/*' | wc -l
```

Record the fraction. Files not addressable by a registry id (tags, taxa, species definitions, lang)
need their own rule and must be enumerated, not guessed.

- [ ] **Step 2: Write the ownership provider and inspect its output**

Add a datagen provider that writes `forestry_ownership.json` (never shipped) mapping every registered
id to its module id. Read it. Confirm the ids partition cleanly and that nothing is claimed twice.

- [ ] **Step 3: Decide, and record the decision with its evidence**

Write the choice and the measurement that drove it into the spec's Build structure section before
building anything on top of it. If strategy B is chosen, the ownership manifest **is** the design;
say so explicitly, because a later reader will otherwise assume the mapping was hand-maintained.

- [ ] **Step 4: Commit the prototype and the decision**

```bash
git add -A
git commit -m "build: decide the per-jar resource strategy"
```

---

### Task 3: Partition the resources

Blocked on Task 2's decision. The shape of this task depends on it, so it is deliberately not
written in detail here - writing it now would be inventing steps for a strategy that has not been
chosen. **Re-plan this task once Task 2 lands.**

What is known regardless of strategy:

- **Textures are the bulk and are derivable, not hand-sortable.** 1,590 of the 1,960 hand-authored
  files are textures with no owning-jar metadata. Generated models reference them; models partition by
  registry id; so texture ownership follows from model ownership. Derive it. Anything left unreferenced
  after that pass must be listed and placed by hand, and the list must appear in the commit message.
- **`pack.mcmeta`, the logo, the access transformer and `kubejs.plugins.txt` stay in core.**
- **The Patchouli book stays in core** until its ownership is decided; the spec has that as an open
  Deferred item and 9b does not settle it.
- **`config/forestry/*` stays in core.**

---

### Task 4: Six mod ids

**Files:**
- Create: `src/<name>/templates/META-INF/neoforge.mods.toml` x5
- Modify: `build.gradle` - a `generateModMetadata` per jar, a `jar` task per jar, `mods { }` x6
- Split: `META-INF/services/forestry.api.plugin.IForestryPlugin`

- [ ] **Step 1: One mods.toml per content jar**

Each declares its own `modId` (`forestry_apiculture`, ...), a `REQUIRED` dependency on `forestry` at a
pinned range, and for `forestry_lepidopterology` a second `REQUIRED` dependency on
`forestry_arboriculture`. Patchouli and JEI dependencies stay on core only.

- [ ] **Step 2: Split the service file**

Core keeps `forestry.core.plugin.DefaultForestryPlugin` and
`forestry.core.platform.compat.kubejs.KubeForestryPlugin`. Each content jar ships a
`META-INF/services/forestry.api.plugin.IForestryPlugin` naming only its own plugin. **This is the
mechanism that makes optional installs work**: a missing jar means the plugin is absent from the
service path, so `registerApiculture` is never called, rather than a class failing to load.

- [ ] **Step 3: Six jar tasks**

`jar` keeps `main` only. Five new `Jar` tasks, one per content source set, each with its own
`archivesName`. Remove the `contentModules.each { from sourceSets[it].output }` block added in 9a.

- [ ] **Step 4: Verify each jar's contents**

For each jar, assert it contains only its own package tree and its own resources, and that core
contains no `forestry/{apiculture,arboriculture,lepidopterology,agriculture,mail}` entries at all.
This is the artifact-level restatement of the gate that phases 2 through 8 maintained by other means.

- [ ] **Step 5: Commit**

---

### Task 5: Lang

`generateEnUsLang` merges the hand-written `en_us.json` over the generated one, manual winning. It
becomes a task per jar over that jar's two lang files.

The 10 non-English locales are hand-authored monoliths with no generated counterpart and no per-key
ownership metadata. Per the spec's Deferred section, splitting them needs tooling that does not exist.
**Ship all 10 in core unsplit**, and record that a player with only core installed sees translated
strings for content they do not have - which is harmless, since a missing translation key falls back
to the key and an unused one is simply never looked up.

---

### Task 6: The cross-jar loot modifier

`data/forestry/loot_modifiers/chests/abandoned_mineshaft.json` has an `extensions` list spanning
`["apiculture", "factory", "storage"]`. Two of those are now core (`factory` -> `core.content.machines`,
`storage` -> `core.content.backpacks`), so the real cross-jar span is core plus apiculture.

This needs **redesign, not partition**: one file cannot be owned by two jars. Split into per-module
modifiers and let `global_loot_modifiers.json` merge them, which it already does across packs. The
chest sub-tables are already per-module (`chests/<chest>/<module>.json`), so the pattern exists.

**Enumerated datagen diff:** this task changes generated loot modifier files. Enumerate exactly which,
and confirm by inspection that the merged effect is unchanged.

---

### Task 7: Boot configurations

The first end-to-end proof that any of this works. Per the spec's Verification section:

- [ ] Core alone. Expect: no bees, trees, butterflies, farms or mail; no crash; no missing-class
      errors in the log; the Portable Analyzer present but inert (an accepted cost, recorded in the
      spec).
- [ ] Core + apiculture.
- [ ] Core + arboriculture + lepidopterology.
- [ ] **Core + lepidopterology without apiculture** - the configuration Task 1 exists to make work.
      Butterflies must project. If this fails, Task 1 was incomplete.
- [ ] All six.

Each is a `runServer` with a subset on the classpath. Record the log excerpt proving each one, because
this is the only evidence in the project that the split actually delivers what D4 promised.

---

### Task 8: Record the phase

Update the Sequencing table, the Build structure section with the chosen resource strategy, the
Deferred section (locale split, Patchouli ownership), and the Status line. Record the flower-type
finding as resolved and note the audit that found it was the only one of its kind.

---

## Known risks

- **Registration order shifts.** Six mods register in mod-sort order instead of one mod's module order,
  and plugins sort by id, so six ids replace one. The spec flags this as mostly benign but
  churn-inducing for datagen diffs and order-sensitive display. `CreativeTabBaselineTest` and the
  datagen diff are the detectors.
- **`forestry:` names from a mod whose id is `forestry_apiculture`.** Verified unrestricted in
  NeoForge 21.1.230, which has no alternative-prefix check.
- **The GameTest suite lives in one source set** and references all six jars. It stays whole and stays
  in the core dev runtime; it is not published.
