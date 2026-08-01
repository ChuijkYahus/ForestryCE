# Phase 3: sever the species-type-aware engine

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Clear bucket C - the eleven base-artifact files that know about specific species types -
taking `checkBaseBoundary` from 60 to 49.

**Architecture:** For the third phase running, the bucket is mostly not what the spec assumed. One
leak is a dead import. Six are files that belong to a content module outright. Three are single
registrations sitting in the wrong jar. Only `GeneticsReloadHandler` needs redesign, and even there
the fix is to move its typed halves out rather than to build a mechanism. Tasks are ordered cheapest
first so the count falls early and the one risky change lands last against a suite that has already
proven itself.

**Tech Stack:** Java 21, NeoForge 21.1.230, Minecraft 1.21.1, ModDevGradle 2.0.x. GameTests only, no
JUnit.

## Global Constraints

- Comment and Javadoc style is binding; see `CLAUDE.md`. ASCII only. `@return` and `@param` are
  noun-phrase fragments with no terminal period. Lowercase `todo`.
- Every task ends with `./gradlew runData` producing no diff in `src/generated/resources` and
  `./gradlew runGameTestServer` reporting all 100 tests passed.
- Both gates stay honest: `checkApiBoundary` green, `checkBaseBoundary` trimmed in the same commit
  that clears a file. A stale baseline fails the build by design.
- **Deleting a usage is not enough.** Java permits unused imports, so the import-based gate still sees
  a file as leaking until the import line goes too. Phase 2 ended with 29 dead imports in one file
  for exactly this reason. After removing the last usage, delete the import.
- All source files are LF as of 2026-07-31. Do not write `$`-anchored `sed` patterns that assume
  otherwise.

## Starting state

`checkBaseBoundary`: 60 files. Bucket C is 11 of them, holding 27 leaking imports.

| File | Leaks | What it actually is |
| --- | --- | --- |
| `core/genetics/GeneticsReloadHandler.java` | 12 | reload hub with typed per-species-type halves |
| `core/network/packets/BeeSpeciesSyncPacket.java` | 2 | apiculture's packet |
| `core/network/packets/FlowerTypeSyncPacket.java` | 2 | apiculture's packet |
| `core/network/packets/TreeSpeciesSyncPacket.java` | 2 | arboriculture's packet |
| `core/network/packets/ButterflySpeciesSyncPacket.java` | 2 | lepidopterology's packet |
| `core/utils/TreeUtil.java` | 2 | tree logic sitting in core |
| `core/network/packets/BeeEffectSyncPacket.java` | 1 | apiculture's packet |
| `core/network/packets/TaxonSyncPacket.java` | 1 | genuinely core; `TaxonManager` is misfiled |
| `core/genetics/ProductTypes.java` | 1 | one registration in the wrong jar |
| `core/loot/CoreLootFunctions.java` | 1 | one registration in the wrong jar |
| `core/utils/GeneticsUtil.java` | 1 | **a dead import** |

## File Structure

| Action | File | Responsibility |
| --- | --- | --- |
| Modify | `core/utils/GeneticsUtil.java` | drop one dead import |
| Create | `arboriculture/loot/ArboricultureLootFunctions.java` | arboriculture's loot function registry |
| Modify | `core/loot/CoreLootFunctions.java` | drop the `COUNT` registration |
| Modify | `core/genetics/ProductTypes.java` | make `register` callable from a module |
| Modify | `apiculture/genetics/FireworkProduct.java` or its module | register the firework product type |
| Move | `apiculture/genetics/TaxonManager.java` -> `core/genetics/TaxonManager.java` | taxonomy is engine-level |
| Move | 5 sync packets -> their owning modules | per-species-type reload sync |
| Move | `core/utils/TreeUtil.java` -> `arboriculture/TreeUtil.java` | tree pollination helpers |
| Modify | `apiculture/ModuleApiculture.java` | `doSelfPollination` moves to a core config |
| Split | `core/genetics/GeneticsReloadHandler.java` | typed rebuilds move to their modules |

---

### Task 1: Delete the dead import in GeneticsUtil

`GeneticsUtil` imports `forestry.arboriculture.capabilities.SpectacleVision` and never uses it - line
90 goes through the api interface `ISpectacleVision` via `ForestryCapabilities.SPECTACLE_VISION`. The
import is the file's only leak, so deleting one line clears it.

This also settles a question left open in phase 1: the spec's bucket D lists `ItemSpectacles` as
needing a decision about `SpectacleVision`. That decision is unaffected - this is a different file
and a genuinely unused import.

**Files:**
- Modify: `src/main/java/forestry/core/utils/GeneticsUtil.java:41`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: nothing.
- Produces: nothing. No behavior change whatsoever.

- [ ] **Step 1: Confirm it really is unused**

```bash
grep -n '\bSpectacleVision\b' src/main/java/forestry/core/utils/GeneticsUtil.java
```

Expected: exactly one hit, line 41, the import itself. If a second hit appears the import is live and
this task does not apply - report it.

- [ ] **Step 2: Delete it**

Remove line 41:

```java
import forestry.arboriculture.capabilities.SpectacleVision;
```

- [ ] **Step 3: Compile and trim**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`. Remove this line from `gradle/base-boundary-baseline.txt`:

```
core/utils/GeneticsUtil.java
```

```bash
./gradlew checkBaseBoundary checkApiBoundary
```

Expected: `checkBaseBoundary: 59 known leaking file(s) remaining`.

- [ ] **Step 4: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "core: drop a dead arboriculture import from GeneticsUtil

The concrete SpectacleVision was imported and never referenced; the one use
goes through the api interface via ForestryCapabilities.SPECTACLE_VISION.

checkBaseBoundary: 60 -> 59 files."
```

---

### Task 2: Give arboriculture its own loot functions

`CoreLootFunctions` registers two loot function types. `ORGANISM` is genuinely core;
`COUNT` wraps `forestry.arboriculture.loot.CountBlockFunction`. Same shape as phase 2's letter data
component: the owner registers it.

**Files:**
- Create: `src/main/java/forestry/arboriculture/loot/ArboricultureLootFunctions.java`
- Modify: `src/main/java/forestry/core/loot/CoreLootFunctions.java`
- Modify: every reference to `CoreLootFunctions.COUNT`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: nothing.
- Produces: `ArboricultureLootFunctions.COUNT`, same
  `DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<?>>` type, registered under the same
  name `count_from_block`.

- [ ] **Step 1: Find the references**

```bash
grep -rn "CoreLootFunctions.COUNT" src/main/java --include='*.java'
```

- [ ] **Step 2: Create the arboriculture holder**

Create `src/main/java/forestry/arboriculture/loot/ArboricultureLootFunctions.java`, mirroring
`CoreLootFunctions` exactly but bound to arboriculture's registry:

```java
package forestry.arboriculture.loot;

import forestry.api.modules.ForestryModuleIds;
import forestry.modules.features.FeatureProvider;
import forestry.modules.features.IFeatureRegistry;
import forestry.modules.features.ModFeatureRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Arboriculture-side loot function registrations.
 */
@FeatureProvider
public class ArboricultureLootFunctions {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.ARBORICULTURE);
	private static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTIONS = REGISTRY.getRegistry(Registries.LOOT_FUNCTION_TYPE);

	public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<?>> COUNT =
		LOOT_FUNCTIONS.register("count_from_block", () -> new LootItemFunctionType<>(CountBlockFunction.CODEC));
}
```

Keep the registry name `count_from_block` exactly. It appears in generated loot tables; changing it
would orphan them.

- [ ] **Step 3: Remove it from core**

Delete the `COUNT` field and `import forestry.arboriculture.loot.CountBlockFunction;` from
`src/main/java/forestry/core/loot/CoreLootFunctions.java`, then repoint the references found in
Step 1 to `ArboricultureLootFunctions.COUNT`.

- [ ] **Step 4: Compile, trim, verify**

```bash
./gradlew compileJava
```

Remove `core/loot/CoreLootFunctions.java` from the baseline, then:

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `58 known leaking file(s)`, no datagen diff, all 100 tests passed. The datagen check is the
one that matters here - generated loot tables name this function type, so a changed registry name
shows up immediately.

- [ ] **Step 5: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "arboriculture: own the count_from_block loot function

CoreLootFunctions registered a function type wrapping arboriculture's
CountBlockFunction. The owner registers it now, under the same name so
generated loot tables are unaffected.

checkBaseBoundary: 59 -> 58 files."
```

---

### Task 3: Let apiculture register its own product type

`ProductTypes` is core's registry of product serializers. Line 121 registers apiculture's
`FireworkProduct.TYPE`. Unlike Tasks 2 and 4 this cannot be a straight relocation, because
`ProductTypes` owns the registry map - so `register` becomes callable from a module. That makes this
the phase's one small extension point.

**Files:**
- Modify: `src/main/java/forestry/core/genetics/ProductTypes.java`
- Modify: `src/main/java/forestry/apiculture/ModuleApiculture.java` (or apiculture's plugin)
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: nothing.
- Produces: `ProductTypes.register(ResourceLocation, IProductType)` as a public entry point. Confirm
  the exact parameter type in Step 1; the plan does not assume it.

- [ ] **Step 1: Read the registry and its register method**

```bash
grep -n "register\|private static\|Map" src/main/java/forestry/core/genetics/ProductTypes.java | head -20
sed -n '110,125p' src/main/java/forestry/core/genetics/ProductTypes.java
```

Note the exact signature of `register` and when it is called - whether at class-init or from a
lifecycle hook. That timing determines where apiculture's call has to go.

- [ ] **Step 2: Make register public**

Change `register` from private to public and give it a javadoc:

```java
	/**
	 * Registers a product type serializer. Called by the module that owns the product.
	 *
	 * @param id   The id the type is stored under in JSON
	 * @param type The serializer for the product
	 */
```

- [ ] **Step 3: Move the firework registration**

Delete line 121 and the `import forestry.apiculture.genetics.FireworkProduct;` from `ProductTypes`.
Also fix the javadoc at line 31, which names `{@link FireworkProduct}` - change it to
`{@code forestry.apiculture.genetics.FireworkProduct}`, matching the convention phase 1a established
for naming impl classes from a package that must not import them.

Add the call on the apiculture side at whatever point Step 1 showed the core registrations happen -
the same lifecycle phase, so ordering does not change.

- [ ] **Step 4: Compile, trim, verify**

```bash
./gradlew compileJava
```

Remove `core/genetics/ProductTypes.java` from the baseline, then:

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `57 known leaking file(s)`, no datagen diff, all 100 tests passed. If a bee product fails to
serialize, the registration is happening too late - revisit the timing from Step 1.

- [ ] **Step 5: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "apiculture: register its own firework product type

ProductTypes owns the serializer registry so the registration cannot simply
relocate; register is public now and apiculture calls it. The javadoc
reference to FireworkProduct becomes {@code} prose.

checkBaseBoundary: 58 -> 57 files."
```

---

### Task 4: Move TaxonManager into core

`TaxonSyncPacket` is the one sync packet that is genuinely core - taxonomy spans every species type,
not one of them. Its single leak is `forestry.apiculture.genetics.TaxonManager`, and that class is
simply misfiled: its users are `ModuleCore`, `TaxonDefinition`, `TaxonSyncPacket`,
`DefaultForestryPlugin` and `ForestryTaxonomy`, only one of which is apiculture.

**Files:**
- Move: `src/main/java/forestry/apiculture/genetics/TaxonManager.java` -> `src/main/java/forestry/core/genetics/TaxonManager.java`
- Modify: every referencing file
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: nothing.
- Produces: `forestry.core.genetics.TaxonManager`, unchanged in shape.

- [ ] **Step 1: Confirm it does not drag apiculture with it**

```bash
grep -n "^import forestry" src/main/java/forestry/apiculture/genetics/TaxonManager.java
```

Expected: no `forestry.apiculture.*` imports beyond same-package ones. **If any appear, stop.** Moving
it would add a new leaking file to the base artifact rather than removing one, and `checkBaseBoundary`
would correctly fail with a new regression.

- [ ] **Step 2: Move it**

```bash
git mv src/main/java/forestry/apiculture/genetics/TaxonManager.java src/main/java/forestry/core/genetics/TaxonManager.java
sed -i '1s|^package forestry\.apiculture\.genetics;|package forestry.core.genetics;|' src/main/java/forestry/core/genetics/TaxonManager.java
```

- [ ] **Step 3: Fix imports both ways**

```bash
grep -rl "import forestry.apiculture.genetics.TaxonManager;" src/main/java src/test/java \
  | xargs -r sed -i 's|import forestry\.apiculture\.genetics\.TaxonManager;|import forestry.core.genetics.TaxonManager;|'
```

Then handle same-package effects in both directions, which bit twice in phases 1b and 2:

```bash
grep -rlw TaxonManager src/main/java/forestry/apiculture/genetics/
grep -rlw TaxonManager src/main/java/forestry/core/genetics/
```

Files in the first list used it without an import and now need
`import forestry.core.genetics.TaxonManager;`. Files in the second list other than `TaxonManager`
itself need any new import deleted. `TaxonManager` itself may now have redundant
`forestry.core.genetics.*` imports - delete those too.

- [ ] **Step 4: Compile, trim, verify**

```bash
./gradlew compileJava
```

Remove `core/network/packets/TaxonSyncPacket.java` from the baseline. `TaxonManager` was not in the
baseline (it lived in apiculture), and must not appear as a new entry - if `checkBaseBoundary` reports
it as a new leak, Step 1 was wrong.

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `56 known leaking file(s)`, no datagen diff, all 100 tests passed.

- [ ] **Step 5: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "core: move TaxonManager out of apiculture

Taxonomy spans every species type. TaxonManager's users are ModuleCore,
TaxonDefinition, TaxonSyncPacket, DefaultForestryPlugin and ForestryTaxonomy;
only one is apiculture. Moving it clears TaxonSyncPacket, the one sync packet
that is genuinely core.

checkBaseBoundary: 57 -> 56 files."
```

---

### Task 5: Move the five species sync packets to their modules

`BeeSpeciesSyncPacket`, `BeeEffectSyncPacket`, `FlowerTypeSyncPacket`, `TreeSpeciesSyncPacket` and
`ButterflySpeciesSyncPacket` each sync one species type's reloaded definitions. They are their
module's packets sitting in core, exactly like the packet id constants in phase 2.

Do this before Task 7, not after. These packets call `GeneticsReloadHandler.rebuildX`, and moving
them first means Task 7's split lands on callers that are already in the right module.

**Files:**
- Move: `core/network/packets/BeeSpeciesSyncPacket.java`, `BeeEffectSyncPacket.java`, `FlowerTypeSyncPacket.java` -> `src/main/java/forestry/apiculture/network/packets/`
- Move: `core/network/packets/TreeSpeciesSyncPacket.java` -> `src/main/java/forestry/arboriculture/network/`
- Move: `core/network/packets/ButterflySpeciesSyncPacket.java` -> `src/main/java/forestry/lepidopterology/network/packets/`
- Modify: the packet id holders and registration sites
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: `ApiculturePacketIds`, `ArboriculturePacketIds` from phase 2.
- Produces: the same `CustomPacketPayload.Type` constants under the same path strings, relocated to
  their module's id holder.

- [ ] **Step 1: Record the wire paths before touching anything**

```bash
grep -rhoE 'type\("[^"]+"\)' src/main/java/forestry --include='*.java' | sort > /tmp/paths-before.txt
wc -l /tmp/paths-before.txt
```

Expected: 41. These strings are the packets' wire identity; no GameTest exercises a round trip, so
this file is the only guard.

- [ ] **Step 2: Find each packet's id constant and registration**

```bash
grep -rn "BeeSpeciesSyncPacket\|BeeEffectSyncPacket\|FlowerTypeSyncPacket\|TreeSpeciesSyncPacket\|ButterflySpeciesSyncPacket" src/main/java --include='*.java' | grep -v "packets/.*SyncPacket.java:"
```

The ids currently live in `PacketIdClient` at lines 53-60 and move to their module's holder. The
path strings are the wire identity and must be carried over exactly:

| Constant | Path | Moves to |
| --- | --- | --- |
| `BEE_SPECIES_SYNC` | `bee_species_sync` | `ApiculturePacketIds` |
| `FLOWER_TYPE_SYNC` | `flower_type_sync` | `ApiculturePacketIds` |
| `BEE_EFFECT_SYNC` | `bee_effect_sync` | `ApiculturePacketIds` |
| `TREE_SPECIES_SYNC` | `tree_species_sync` | `ArboriculturePacketIds` |
| `BUTTERFLY_SPECIES_SYNC` | `butterfly_species_sync` | `LepidopterologyPacketIds` (new) |
| `TAXON_SYNC` | `taxon_sync` | stays in `PacketIdClient` |

- [ ] **Step 3: Create the lepidopterology id holder**

Create `src/main/java/forestry/lepidopterology/network/LepidopterologyPacketIds.java`, mirroring
`ApiculturePacketIds` from phase 2:

```java
package forestry.lepidopterology.network;

import forestry.core.network.PacketIdServer;
import forestry.lepidopterology.network.packets.ButterflySpeciesSyncPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Packet ids owned by the lepidopterology module. The path strings are the packets' wire identity and
 * must never change.
 */
public class LepidopterologyPacketIds {
	public static final CustomPacketPayload.Type<ButterflySpeciesSyncPacket> BUTTERFLY_SPECIES_SYNC = PacketIdServer.type("butterfly_species_sync");
}
```

- [ ] **Step 4: Move the five packets**

For each, `git mv` to the destination, change the `package` line, then rewrite imports at every call
site. Move each id constant into its module's holder and delete it from `PacketIdClient`.

- [ ] **Step 5: Compile and verify wire identity**

```bash
./gradlew compileJava
grep -rhoE 'type\("[^"]+"\)' src/main/java/forestry --include='*.java' | sort > /tmp/paths-after.txt
diff /tmp/paths-before.txt /tmp/paths-after.txt
```

Expected: `BUILD SUCCESSFUL` and no diff. Every path string must survive.

- [ ] **Step 6: Trim and verify**

Remove these five from the baseline:

```
core/network/packets/BeeEffectSyncPacket.java
core/network/packets/BeeSpeciesSyncPacket.java
core/network/packets/ButterflySpeciesSyncPacket.java
core/network/packets/FlowerTypeSyncPacket.java
core/network/packets/TreeSpeciesSyncPacket.java
```

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `51 known leaking file(s)`, no datagen diff, all 100 tests passed. The reload tests
(`ButterflySpeciesReloadTest`, `ButterflyEntityReloadTest`, `TreeReloadSideEffectsTest`) exercise the
paths these packets serve.

- [ ] **Step 7: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "network: move the species sync packets to their modules

Each syncs one species type's reloaded definitions, so each belongs to that
module rather than core - the same relocation the packet id holders got in
phase 2. TaxonSyncPacket stays; taxonomy is engine-level.

All 41 type(\"...\") path strings verified identical before and after.

checkBaseBoundary: 56 -> 51 files."
```

---

### Task 6: Move TreeUtil to arboriculture

`TreeUtil` is tree pollination logic living in `core/utils`. Its users are three arboriculture and
lepidopterology files, and lepidopterology may depend on arboriculture under D1, so arboriculture is a
valid home for all of them.

One line blocks it. `TreeUtil:90` reads `ModuleApiculture.doSelfPollination`, and arboriculture may
not depend on apiculture. The flag is misfiled rather than misused: it governs whether an organism can
pollinate itself, which applies to trees as much as bees.

**Files:**
- Move: `src/main/java/forestry/core/utils/TreeUtil.java` -> `src/main/java/forestry/arboriculture/TreeUtil.java`
- Modify: `src/main/java/forestry/apiculture/ModuleApiculture.java`
- Modify: a core config to hold `doSelfPollination`
- Modify: every referencing file
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: nothing.
- Produces: `forestry.arboriculture.TreeUtil`, unchanged in shape. `doSelfPollination` readable from
  base.

- [ ] **Step 1: Find where the flag lives and who reads it**

```bash
grep -rn "doSelfPollination" src/main/java --include='*.java'
grep -n "doSelfPollination" src/main/java/forestry/apiculture/ModuleApiculture.java
```

`ModuleApiculture` holds it as a public static field alongside other tunables. Note whether anything
writes it (a config load) as well as reads it.

- [ ] **Step 2: Move the flag to a core config**

`doSelfPollination` is a plain `public static boolean` on `ModuleApiculture:59`, alongside
`ticksPerBeeWorkCycle`, `hivesDamageOnPeaceful` and friends. It is **not** config-backed -
`ForestryConfig` uses `ModConfigSpec.BooleanValue`, a different mechanism, and this field does not
appear there.

So do not invent a config entry. Move the field as-is to a core class in the same plain-static style,
and leave the other `ModuleApiculture` tunables alone - they are genuinely bee settings. Matching the
surrounding code matters more than improving it here; converting it to a real config entry is a
separate change with its own migration question.

- [ ] **Step 3: Move TreeUtil**

```bash
git mv src/main/java/forestry/core/utils/TreeUtil.java src/main/java/forestry/arboriculture/TreeUtil.java
sed -i '1s|^package forestry\.core\.utils;|package forestry.arboriculture;|' src/main/java/forestry/arboriculture/TreeUtil.java
grep -rl "import forestry.core.utils.TreeUtil;" src/main/java src/test/java \
  | xargs -r sed -i 's|import forestry\.core\.utils\.TreeUtil;|import forestry.arboriculture.TreeUtil;|'
```

`TileLeaves` is now reachable without the `forestry.arboriculture.tiles` import staying a leak, since
the file is no longer in the base artifact. Delete any import that same-package placement makes
redundant.

- [ ] **Step 4: Compile, trim, verify**

```bash
./gradlew compileJava
```

Remove `core/utils/TreeUtil.java` from the baseline.

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `50 known leaking file(s)`, no datagen diff, all 100 tests passed. `TreeReloadSideEffectsTest`
and the butterfly mating tests exercise `TreeUtil`.

- [ ] **Step 5: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "arboriculture: take ownership of TreeUtil

Tree pollination helpers living in core/utils, used only by arboriculture
and lepidopterology - and lepidopterology may depend on arboriculture.

doSelfPollination moves out of ModuleApiculture to a core config: it governs
whether an organism can pollinate itself, which applies to trees as much as
bees, and arboriculture may not depend on apiculture.

checkBaseBoundary: 51 -> 50 files."
```

---

### Task 7: Split GeneticsReloadHandler

The last and only real redesign. `GeneticsReloadHandler` has seven public `rebuildX` methods. Three
are engine-level and stay; four are typed to a single species type and carry all 12 leaks.

| Method | Fate |
| --- | --- |
| `rebuildTaxa(Collection<TaxonDefinition>)` | stays in core |
| `rebuildMutations(RecipeManager)` | stays in core - already iterates registered species types |
| `rebuildOne(ISpeciesType, RecipeManager)` | stays in core, private |
| `rebuildSpecies(Map<..., BeeSpeciesDefinition>)` | to apiculture |
| `rebuildFlowerTypes(Map<..., IFlowerType>)` | to apiculture |
| `rebuildBeeEffects(Map<..., IBeeEffect>)` | to apiculture |
| `rebuildTreeSpecies(Map<..., TreeSpeciesDefinition>)` | to arboriculture |
| `rebuildButterflySpecies(Map<..., ButterflySpeciesDefinition>)` | to lepidopterology |

The spec's bucket C description said to make these "iterate registered species types". That is wrong
and would waste the implementer's time: they take per-type typed definition maps and call per-type
projectors, so there is nothing to iterate. They move.

**Files:**
- Modify: `src/main/java/forestry/core/genetics/GeneticsReloadHandler.java`
- Create: `src/main/java/forestry/apiculture/genetics/ApicultureReloadHandler.java`
- Create: `src/main/java/forestry/arboriculture/genetics/ArboricultureReloadHandler.java`
- Create: `src/main/java/forestry/lepidopterology/genetics/LepidopterologyReloadHandler.java`
- Modify: every caller
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: the relocated sync packets from Task 5.
- Produces: `ApicultureReloadHandler.rebuildSpecies`, `.rebuildFlowerTypes`, `.rebuildBeeEffects`;
  `ArboricultureReloadHandler.rebuildTreeSpecies`; `LepidopterologyReloadHandler.rebuildButterflySpecies`
  - each with the identical signature it has today. `GeneticsReloadHandler.rebuildTaxa` and
  `.rebuildMutations` keep their names and signatures.

- [ ] **Step 1: Read the whole file**

```bash
cat src/main/java/forestry/core/genetics/GeneticsReloadHandler.java
```

Note especially anything the typed methods share with the engine-level ones - a private helper, a lock,
a static field. Shared state that stays in core must become accessible to the new handlers, and that
is the part most likely to bite.

- [ ] **Step 2: List every caller before moving anything**

```bash
grep -rn "GeneticsReloadHandler\." src/main/java --include='*.java' | grep -v "GeneticsReloadHandler.java"
```

Expected: 17 call sites across managers, datagen providers and sync packets. Note that the datagen
providers under `core/data` will end up calling into module handlers - that is fine, they are bucket A
and already in the baseline, so no new file starts leaking. Confirm that in Step 5 rather than
assuming.

- [ ] **Step 3: Create the three module handlers**

Each is a final class with a private constructor holding its methods moved verbatim, with `@FeatureProvider`
omitted - these are not registries. Example:

```java
package forestry.apiculture.genetics;

/**
 * Reload entry points for apiculture's datapack-driven definitions. Split out of
 * {@code forestry.core.genetics.GeneticsReloadHandler} so the base artifact does not name bee types.
 */
public final class ApicultureReloadHandler {
	private ApicultureReloadHandler() {
	}

	// rebuildSpecies, rebuildFlowerTypes, rebuildBeeEffects moved verbatim
}
```

Move the method bodies unchanged. If a body calls a private helper that stays in core, make that helper
public on `GeneticsReloadHandler` rather than duplicating it.

- [ ] **Step 4: Delete the moved methods and their imports from core**

Remove the four typed methods from `GeneticsReloadHandler` and every import they alone needed. Verify
the file has zero content imports left:

```bash
grep -cE "^import forestry\.(apiculture|arboriculture|lepidopterology|farming|cultivation|mail)\." src/main/java/forestry/core/genetics/GeneticsReloadHandler.java
```

Expected: `0`. Remember unused imports still count as leaks.

- [ ] **Step 5: Repoint all callers and compile**

```bash
./gradlew compileJava
./gradlew checkBaseBoundary
```

Remove `core/genetics/GeneticsReloadHandler.java` from the baseline. Expected:
`49 known leaking file(s) remaining`, and no new entries reported.

- [ ] **Step 6: Verify reload behavior**

```bash
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: no datagen diff, all 100 tests passed. This is the best-covered change in the phase:
`ButterflySpeciesReloadTest`, `ButterflyEntityReloadTest`, `ButterflySpeciesFallbackTest`,
`TreeReloadSideEffectsTest`, `BeeSpeciesDefinitionTest` and `MutationRecipeTest` all exercise the
reload path, and six datagen providers call these methods directly, so a datagen diff would also
catch a mistake.

- [ ] **Step 7: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "genetics: split the typed reload handlers out of core

GeneticsReloadHandler had seven rebuild methods. rebuildTaxa, rebuildMutations
and rebuildOne are engine-level and stay; the four typed ones took per-species
definition maps and carried all 12 of the file's leaks, so they move to their
modules.

The spec described this as making them iterate registered species types. They
cannot - they take typed maps and call per-type projectors. rebuildMutations,
which does iterate, already did.

checkBaseBoundary: 50 -> 49 files."
```

---

### Task 8: Record phase 3 completion

**Files:**
- Modify: `docs/superpowers/specs/2026-07-30-feature-package-reorg-design.md`

- [ ] **Step 1: Confirm from a clean build**

```bash
./gradlew clean build
```

Expected: `BUILD SUCCESSFUL`, `checkApiBoundary: forestry.api is clean`,
`checkBaseBoundary: 49 known leaking file(s) remaining`.

- [ ] **Step 2: Update the spec**

Mark phase 3 `DONE` in the sequencing block and add:

```markdown
Phase 3 landed 2026-08-01. Bucket C is closed; `checkBaseBoundary` is at 49 of the original 68.
For the third phase running the bucket was mostly relocation rather than redesign: one leak was a
dead import, six files belonged to a content module outright, three were single registrations in the
wrong jar, and only GeneticsReloadHandler needed splitting. The spec's prescription for bucket C -
make the typed rebuild methods iterate registered species types - was wrong; they take typed
definition maps and had to move instead.
```

- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/specs/2026-07-30-feature-package-reorg-design.md
git commit -m "docs: record phase 3 completion, base boundary at 49 of 68"
```

---

## Notes for phase 4

- Buckets D, E and H are next, 11 files. Given three phases of evidence, check first whether each leak
  is a misfiled type, a dead import or a registration in the wrong jar before planning a mechanism.
  Run the dead-import sweep from Task 1 across all of them first; it is nearly free and phase 3 found
  one that way.
- Phase 4 already has two decisions banked from the spec's Graph decisions: `ItemSpectacles` versus
  the `SpectacleVision` capability, and `NaturalistChestBlockType`, which phase 2 already executed.
- After phase 4 the remaining base leaks should be almost entirely bucket A - the 20 datagen providers,
  led by `ForestryRecipeProvider` at 26 imports - plus buckets I and G. Bucket A dissolves in phase 8
  rather than being fixed, so the count will stall around 30 until then. That is expected, not a
  regression.
