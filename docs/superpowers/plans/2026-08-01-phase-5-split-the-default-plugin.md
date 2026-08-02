# Phase 5: split the default plugin

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Clear bucket G - the seven `plugin/` files that register base Forestry's content - taking
`checkBaseBoundary` from 39 to 32.

**Architecture:** Four of the seven are content definition classes that belong to a module outright
and move whole. The other three are one plugin and its client half, and both already split along
seams that exist in the source: `DefaultForestryPlugin` implements nine `IForestryPlugin` hooks whose
contents are already grouped by module, and `DefaultForestryClientRegistration` already has three
per-module private methods. The split is into **five** plugins, not the six the spec assumed - mail
registers nothing. Tasks 1 through 5 are the whole-file moves, cheapest first; tasks 6 through 9 take
the plugin apart one module at a time so the system is registrable and testable after every commit.

**Tech Stack:** Java 21, NeoForge 21.1.230, Minecraft 1.21.1, ModDevGradle 2.0.x. GameTests only, no
JUnit.

## Global Constraints

- Comment and Javadoc style is binding; see `CLAUDE.md`. ASCII only. `@return` and `@param` are
  noun-phrase fragments with no terminal period. Lowercase `todo`.
- Every task ends with `./gradlew runData` producing no diff in `src/generated/resources` and
  `./gradlew runGameTestServer` reporting all 100 tests passed.
- Both gates stay honest: `checkApiBoundary` green, `checkBaseBoundary` trimmed in the same commit
  that clears a file. A stale baseline fails the build by design.
- **Run `./gradlew compileTestJava`, not just `compileJava`.** `build` covers both; `compileJava` does
  not, and phase 3 lost time to exactly that.
- **Deleting a usage is not enough.** Java permits unused imports, so the gate still sees a file as
  leaking until the import line goes too.
- When a class moves package, check for callers that used it **without an import** because they were
  in the same package, and for imports inside the moved file that same-package placement now makes
  redundant. This has bitten in phases 1b, 2, 3 and 4, in both directions.
- **Do not rewrite fully-qualified references with a bare class-name `sed`.** After any such sweep,
  grep for the old package paired with the new class name.
- All source files are LF as of 2026-07-31. Do not write `$`-anchored `sed` patterns.

## Starting state

`checkBaseBoundary`: 39 files. Bucket G is 7 of them, holding 44 leaking imports.

| File | Lines | Leaks | What it actually is |
| --- | --- | --- | --- |
| `plugin/DefaultForestryPlugin.java` | 406 | 24 | nine hooks, already grouped by module |
| `plugin/DefaultBeeSpecies.java` | 848 | 7 | apiculture's species table, datagen input |
| `plugin/DefaultTreeSpecies.java` | 771 | 5 | arboriculture's species table |
| `plugin/DefaultWoods.java` | 45 | 3 | arboriculture's refractory waxables |
| `plugin/DefaultFarms.java` | 154 | 2 | farming's farm types |
| `plugin/client/DefaultForestryClientRegistration.java` | 143 | 2 | three per-module methods already |
| `plugin/client/BeeAnalyzerPlugin.java` | 167 | 1 | apiculture's analyzer plugin |

`forestry/plugin/` holds twelve files; the five that do not leak (`BeeTaxonomy`, `TreeTaxonomy`,
`ButterflyTaxonomy`, `ForestryTaxonomy`, `DefaultButterflySpecies`) and two client ones
(`TreeAnalyzerPlugin`, `ButterflyAnalyzerPlugin`) are **out of scope**: moving them changes no count
and phase 7 relocates packages wholesale. Leave them.

### A measurement bug worth knowing about

The ad-hoc listing used while surveying this bucket missed **wildcard imports**:

```
plugin/DefaultForestryPlugin.java:25   import forestry.apiculture.*;
plugin/DefaultForestryPlugin.java:31   import forestry.apiculture.genetics.effects.*;
plugin/DefaultFarms.java:16            import forestry.farming.logic.*;
plugin/DefaultFarms.java:17            import forestry.farming.logic.farmables.*;
```

`checkBaseBoundary` catches these correctly - its pattern stops at the package segment, so
`import forestry.farming.*;` matches. Only the survey script was wrong, and 87 wildcard forestry
imports exist repo-wide. **Trust the gate's count, not a hand-written regex.** When listing a file's
leaks, use:

```bash
grep -nE "^import (static )?forestry\.(apiculture|arboriculture|lepidopterology|farming|cultivation|mail)\." <file>
```

A wildcard import cannot be resolved to a class list without a compiler, so expanding
`forestry.apiculture.*` into explicit imports is part of the work in Task 6, not something to
shortcut. Let the compiler tell you what was needed: delete the wildcard, compile, add what it asks
for.

### What is and is not order-sensitive

Two registries assign **numeric ids by insertion order**, and getting this wrong is silent:

- **Errors.** `PluginManager:81-86` assigns `(short) i` over the registered list, and that short is
  the wire form. Only `DefaultForestryPlugin.registerErrors` registers any, and it registers
  `ForestryError.values()` wholesale. **Errors stay in the core plugin, in one hook, unsplit.** Then
  the order is exactly what it is today.
- **Filter rules.** `FilterManager:21-22` calls `registeredRuleTypes.sort(FILTER_COMPARATOR)` -
  alphabetically - *before* assigning `filterIdByName`. So splitting `registerFilterRuleTypes` across
  four plugins is safe: the ids come out identical regardless of which plugin registered which set.
  Verified by reading the constructor; do not take it on trust if that file changes.

Everything else keys by `ResourceLocation` or string id: species types, circuits, hives, fruits,
cocoons, farm types, pollen. Plugin order does not affect them.

### How the hooks reach plugins

`PluginManager` makes six passes over `LOADED_PLUGINS` (`registerErrors`, `registerCircuits`,
`registerGenetics`, `registerFarming`, `registerPollen`, `registerClient`). The three content hooks
are called by the species types instead - `BeeSpeciesType.handleSpeciesRegistration(plugins)` at
`:186-191` loops the plugin list calling `plugin.registerApiculture(registration)`, and
`TreeSpeciesType`/`ButterflySpeciesType` do the same for theirs.

Every pass visits every plugin, and every hook on `IForestryPlugin` has a no-op default. So a plugin
that implements only its own hooks behaves identically to one big plugin implementing all of them.
That is what makes this split mechanical rather than a redesign.

## File Structure

| Action | File | Responsibility |
| --- | --- | --- |
| Move | `plugin/DefaultFarms.java` -> `farming/plugin/` | farm type definitions |
| Move | `plugin/DefaultWoods.java` -> `arboriculture/plugin/` | refractory waxables |
| Move | `plugin/DefaultTreeSpecies.java` -> `arboriculture/plugin/` | tree species table |
| Move | `plugin/DefaultBeeSpecies.java` -> `apiculture/plugin/` | bee species table |
| Move | `plugin/client/BeeAnalyzerPlugin.java` -> `apiculture/client/plugin/` | bee analyzer panel |
| Split | `plugin/client/DefaultForestryClientRegistration.java` | three per-module registrations |
| Create | `apiculture/plugin/ApicultureForestryPlugin.java` | bee genetics, hives, effects |
| Create | `arboriculture/plugin/ArboricultureForestryPlugin.java` | tree genetics, fruits, woods, pollen |
| Create | `lepidopterology/plugin/LepidopterologyForestryPlugin.java` | butterfly genetics, cocoons |
| Create | `farming/plugin/AgricultureForestryPlugin.java` | farm types and farm circuits |
| Modify | `plugin/DefaultForestryPlugin.java` | reduced to core: errors, machine circuits, core filter rules |
| Modify | `apiimpl/plugin/PluginManager.java:49-66` | order Forestry's plugins first without naming one |
| Modify | `src/main/resources/META-INF/services/forestry.api.plugin.IForestryPlugin` | five entries |

---

### Task 1: Move DefaultFarms to farming

154 lines of farm type definitions, called from exactly one place - `DefaultForestryPlugin:387`. Two
leaking imports, both wildcards into `forestry.farming.logic`.

**Files:**
- Move: `src/main/java/forestry/plugin/DefaultFarms.java` -> `src/main/java/forestry/farming/plugin/DefaultFarms.java`
- Modify: `src/main/java/forestry/plugin/DefaultForestryPlugin.java`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: nothing.
- Produces: `forestry.farming.plugin.DefaultFarms.registerFarmTypes(IFarmingRegistration)`, unchanged.
  It is **already** `public class DefaultFarms` with `public static void registerFarmTypes`, so no
  widening is needed here. Of the four content classes only `DefaultWoods` is package-private.

- [ ] **Step 1: Confirm the caller**

```bash
grep -rn "DefaultFarms" src/main/java src/test/java --include='*.java' | grep -v "plugin/DefaultFarms.java:"
```

Expected: one caller, `DefaultForestryPlugin:387`.

- [ ] **Step 2: Move it**

```bash
mkdir -p src/main/java/forestry/farming/plugin
git mv src/main/java/forestry/plugin/DefaultFarms.java src/main/java/forestry/farming/plugin/DefaultFarms.java
sed -i '1s@^package forestry\.plugin;@package forestry.farming.plugin;@' src/main/java/forestry/farming/plugin/DefaultFarms.java
```

Widen the class and `registerFarmTypes` to `public` if Step 1 showed them package-private.

- [ ] **Step 3: Repoint the caller**

`DefaultForestryPlugin` referenced it with no import (same package). Add:

```java
import forestry.farming.plugin.DefaultFarms;
```

`DefaultForestryPlugin` is still a leaking file - it is cleared in Task 9 - so this import adds
nothing new to the gate.

- [ ] **Step 4: Compile, trim, verify**

```bash
./gradlew compileJava compileTestJava
```

Remove `plugin/DefaultFarms.java` from `gradle/base-boundary-baseline.txt`, then:

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `38 known leaking file(s)`, no datagen diff, all 100 tests passed.

- [ ] **Step 5: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "farming: take ownership of the default farm types

154 lines of farm type definitions with one caller. Both leaking imports were
wildcards into forestry.farming.logic.

checkBaseBoundary: 39 -> 38 files."
```

---

### Task 2: Move DefaultWoods to arboriculture

45 lines registering refractory waxable block pairs for every wood type. One caller,
`DefaultForestryPlugin:311`. Declared `class DefaultWoods` with `static void register` - both
package-private, both must widen to `public`.

**Files:**
- Move: `src/main/java/forestry/plugin/DefaultWoods.java` -> `src/main/java/forestry/arboriculture/plugin/DefaultWoods.java`
- Modify: `src/main/java/forestry/plugin/DefaultForestryPlugin.java`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: nothing.
- Produces: `public final class forestry.arboriculture.plugin.DefaultWoods` with
  `public static void register(IArboricultureRegistration)`.

- [ ] **Step 1: Move it**

```bash
mkdir -p src/main/java/forestry/arboriculture/plugin
git mv src/main/java/forestry/plugin/DefaultWoods.java src/main/java/forestry/arboriculture/plugin/DefaultWoods.java
sed -i '1s@^package forestry\.plugin;@package forestry.arboriculture.plugin;@' src/main/java/forestry/arboriculture/plugin/DefaultWoods.java
sed -i 's@^class DefaultWoods {@public class DefaultWoods {@; s@^\tstatic void register(@\tpublic static void register(@' src/main/java/forestry/arboriculture/plugin/DefaultWoods.java
```

Check for a private helper that also needs no change - `registerVanillaRefractory` is private static
and stays private.

- [ ] **Step 2: Repoint the caller**

Add `import forestry.arboriculture.plugin.DefaultWoods;` to `DefaultForestryPlugin`.

- [ ] **Step 3: Compile, trim, verify**

```bash
./gradlew compileJava compileTestJava
```

Remove `plugin/DefaultWoods.java` from the baseline, then:

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `37 known leaking file(s)`, no datagen diff, all 100 tests passed.

Refractory waxable pairs are runtime registrations with no datagen output and no GameTest. The
compiler is the oracle: this task moves no code, only a package line and two modifiers.

- [ ] **Step 4: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "arboriculture: take ownership of the default woods

45 lines pairing every wood block with its fireproof form, one caller. The
class and its register method widen to public because the caller is now in a
different package.

checkBaseBoundary: 38 -> 37 files."
```

---

### Task 3: Move DefaultTreeSpecies to arboriculture

771 lines defining every base tree species. Two callers: `DefaultForestryPlugin:282` at runtime and
`core/data/TreeSpeciesProvider:76` at datagen.

**Files:**
- Move: `src/main/java/forestry/plugin/DefaultTreeSpecies.java` -> `src/main/java/forestry/arboriculture/plugin/DefaultTreeSpecies.java`
- Modify: `src/main/java/forestry/plugin/DefaultForestryPlugin.java`, `src/main/java/forestry/core/data/TreeSpeciesProvider.java`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: nothing.
- Produces: `forestry.arboriculture.plugin.DefaultTreeSpecies.register(IArboricultureRegistration)`.
  Already `public class` with `public static void register` - no widening needed.

- [ ] **Step 1: Move it**

```bash
git mv src/main/java/forestry/plugin/DefaultTreeSpecies.java src/main/java/forestry/arboriculture/plugin/DefaultTreeSpecies.java
sed -i '1s@^package forestry\.plugin;@package forestry.arboriculture.plugin;@' src/main/java/forestry/arboriculture/plugin/DefaultTreeSpecies.java
```

- [ ] **Step 2: Repoint both callers**

`DefaultForestryPlugin` and `TreeSpeciesProvider` both need
`import forestry.arboriculture.plugin.DefaultTreeSpecies;`. `TreeSpeciesProvider` is in `core/data`
and already baselined under bucket A, so this adds no new leaking file - confirm that in Step 3
rather than assuming.

Check whether the moved file now has redundant imports: it imports
`forestry.arboriculture.ForestryWoodType`, `VanillaWoodType`, `blocks.ForestryLeafType` and
`features.ArboricultureBlocks`. All are sibling packages of `forestry.arboriculture.plugin`, not the
same package, so **all four imports stay**. Verify rather than delete on reflex.

- [ ] **Step 3: Compile, trim, verify**

```bash
./gradlew compileJava compileTestJava
```

Remove `plugin/DefaultTreeSpecies.java` from the baseline, then:

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
./gradlew runGameTestServer 2>&1 | grep -E "Loaded [0-9]+ tree species"
```

Expected: `36 known leaking file(s)`, no datagen diff, all 100 tests passed, and `Loaded 50 tree
species`. **This task has a real oracle**: `TreeSpeciesProvider` generates
`src/generated/resources/data/forestry/tree_species/*.json` from this exact class, so a dropped
species shows up as a datagen diff immediately.

- [ ] **Step 4: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "arboriculture: take ownership of the default tree species

771 lines defining every base tree, used by the plugin at runtime and by
TreeSpeciesProvider at datagen. Both callers repointed; the generated
tree_species JSON is byte-identical.

checkBaseBoundary: 37 -> 36 files."
```

---

### Task 4: Move DefaultBeeSpecies to apiculture

848 lines, the largest file in the bucket. **Only caller is `core/data/BeeSpeciesProvider:79`** -
`DefaultForestryPlugin` no longer calls it, because bee species became datapack JSON and this class
is datagen input only. Its own class comment says so.

**Files:**
- Move: `src/main/java/forestry/plugin/DefaultBeeSpecies.java` -> `src/main/java/forestry/apiculture/plugin/DefaultBeeSpecies.java`
- Modify: `src/main/java/forestry/core/data/BeeSpeciesProvider.java`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: nothing.
- Produces: `forestry.apiculture.plugin.DefaultBeeSpecies.register(IApicultureRegistration)`. Already
  `public class` with `public static void register` - no widening needed.

- [ ] **Step 1: Confirm the single caller**

```bash
grep -rn "DefaultBeeSpecies" src/main/java src/test/java --include='*.java' | grep -v "plugin/DefaultBeeSpecies.java:"
```

Expected: `BeeSpeciesProvider:79` (the call) and `:90` (a comment). If `DefaultForestryPlugin`
appears, the class is also a runtime registration and the commit message below is wrong - fix it.

- [ ] **Step 2: Move it**

```bash
mkdir -p src/main/java/forestry/apiculture/plugin
git mv src/main/java/forestry/plugin/DefaultBeeSpecies.java src/main/java/forestry/apiculture/plugin/DefaultBeeSpecies.java
sed -i '1s@^package forestry\.plugin;@package forestry.apiculture.plugin;@' src/main/java/forestry/apiculture/plugin/DefaultBeeSpecies.java
```

Note it carries two **static** imports
(`ApicultureItems.BEE_COMBS`, `ApicultureItems.POLLEN_CLUSTER`) alongside the plain one - all three
stay, they are a sibling package.

- [ ] **Step 3: Repoint the caller and compile**

Add `import forestry.apiculture.plugin.DefaultBeeSpecies;` to `BeeSpeciesProvider`.

```bash
./gradlew compileJava compileTestJava
```

- [ ] **Step 4: Trim and verify**

Remove `plugin/DefaultBeeSpecies.java` from the baseline, then:

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
./gradlew runGameTestServer 2>&1 | grep -E "Loaded [0-9]+ bee species"
```

Expected: `35 known leaking file(s)`, no datagen diff, all 100 tests passed, `Loaded 69 bee species`.
The 69 generated `bee_species/*.json` files come from this class, so the datagen check is exact.

- [ ] **Step 5: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "apiculture: take ownership of the default bee species

848 lines, datagen input only - bee species became datapack JSON, so the
plugin stopped calling this and BeeSpeciesProvider is the sole caller. The 69
generated bee_species files are byte-identical.

checkBaseBoundary: 36 -> 35 files."
```

---

### Task 5: Split the client registration

`DefaultForestryClientRegistration` implements `Consumer<IClientRegistration>` and its `accept`
already delegates to `registerApiculture`, `registerArboriculture` and `registerLepidopterology`. Its
two leaks (`arboriculture.client.BiomeLeafTint`, `FixedLeafTint`) are both inside the arboriculture
method. `BeeAnalyzerPlugin` is one leak (`apiculture.TagFlowerType`) and is instantiated by the
apiculture method.

Split the registration three ways and move `BeeAnalyzerPlugin` with its half. `TreeAnalyzerPlugin`
and `ButterflyAnalyzerPlugin` **stay put**: all three analyzer plugins are `public`, so the new
module classes can import them from `forestry.plugin.client`, and content importing base is the
allowed direction. Neither leaks, so moving them would change no count. Only `BeeAnalyzerPlugin`
moves, because it is the one that leaks (`apiculture.TagFlowerType`).

**Files:**
- Move: `src/main/java/forestry/plugin/client/BeeAnalyzerPlugin.java` -> `src/main/java/forestry/apiculture/client/plugin/BeeAnalyzerPlugin.java`
- Create: `src/main/java/forestry/apiculture/client/plugin/ApicultureClientRegistration.java`
- Create: `src/main/java/forestry/arboriculture/client/plugin/ArboricultureClientRegistration.java`
- Create: `src/main/java/forestry/lepidopterology/client/plugin/LepidopterologyClientRegistration.java`
- Modify: `src/main/java/forestry/plugin/client/DefaultForestryClientRegistration.java`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: `IClientRegistration` from api.
- Produces: three `Consumer<IClientRegistration>` implementations. `DefaultForestryClientRegistration`
  keeps its own identity and delegates to all three, so `DefaultForestryPlugin.registerClient` does
  not change in this task - Task 9 splits that.

- [ ] **Step 1: Read the whole file**

```bash
cat src/main/java/forestry/plugin/client/DefaultForestryClientRegistration.java
```

Note which analyzer plugin each method instantiates. All three are `public`, verified: the
arboriculture and lepidopterology registrations import theirs from `forestry.plugin.client`.

- [ ] **Step 2: Move BeeAnalyzerPlugin and create the three registrations**

```bash
mkdir -p src/main/java/forestry/apiculture/client/plugin src/main/java/forestry/arboriculture/client/plugin src/main/java/forestry/lepidopterology/client/plugin
git mv src/main/java/forestry/plugin/client/BeeAnalyzerPlugin.java src/main/java/forestry/apiculture/client/plugin/BeeAnalyzerPlugin.java
sed -i '1s@^package forestry\.plugin\.client;@package forestry.apiculture.client.plugin;@' src/main/java/forestry/apiculture/client/plugin/BeeAnalyzerPlugin.java
```

Each new registration class takes its method body **verbatim** as its `accept`:

```java
package forestry.apiculture.client.plugin;

import java.util.function.Consumer;

import forestry.api.client.plugin.IClientRegistration;

/**
 * Apiculture's client-side registrations. Split out of
 * {@code forestry.plugin.client.DefaultForestryClientRegistration} so the base artifact does not name
 * bee client types.
 */
public class ApicultureClientRegistration implements Consumer<IClientRegistration> {
	@Override
	public void accept(IClientRegistration client) {
		// body of DefaultForestryClientRegistration.registerApiculture, moved verbatim
	}
}
```

- [ ] **Step 3: Reduce the base class to delegation**

```java
public class DefaultForestryClientRegistration implements Consumer<IClientRegistration> {
	@Override
	public void accept(IClientRegistration client) {
		new ApicultureClientRegistration().accept(client);
		new ArboricultureClientRegistration().accept(client);
		new LepidopterologyClientRegistration().accept(client);
	}
}
```

That keeps registration order identical to today's `accept`. Delete every import the moved bodies
alone needed and confirm:

```bash
grep -cE "^import (static )?forestry\.(apiculture|arboriculture|lepidopterology|farming|cultivation|mail)\." src/main/java/forestry/plugin/client/DefaultForestryClientRegistration.java
```

Expected: `0`.

- [ ] **Step 4: Compile, trim, verify**

```bash
./gradlew compileJava compileTestJava
```

Remove both from the baseline:

```
plugin/client/BeeAnalyzerPlugin.java
plugin/client/DefaultForestryClientRegistration.java
```

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `33 known leaking file(s)`, no datagen diff, all 100 tests passed.

**No GameTest runs a client**, so leaf tints, sapling models, bee models and analyzer panels are
entirely uncovered here. Count the registrations instead: every `client.set*` call in the original
file must appear exactly once across the three new classes.

```bash
git show HEAD:src/main/java/forestry/plugin/client/DefaultForestryClientRegistration.java | grep -c "client\.set"
grep -hc "client\.set" src/main/java/forestry/*/client/plugin/*ClientRegistration.java | paste -sd+ | bc
```

Both numbers must match.

- [ ] **Step 5: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "modules: own their client registrations

DefaultForestryClientRegistration already delegated to three per-module
methods; each becomes its module's own Consumer<IClientRegistration> and the
base class delegates to all three in the same order. BeeAnalyzerPlugin moves
with apiculture's half.

Registration count verified identical: no client.set call was lost.

checkBaseBoundary: 35 -> 33 files."
```

---

### Task 6: Give apiculture its own plugin

The first of four module plugins. Each takes its hooks out of `DefaultForestryPlugin` verbatim, gets
a service-file entry in the same commit, and leaves the system fully registrable - so every one of
these four tasks is independently testable.

Apiculture's share:
- from `registerGenetics`: the bee species type block (`:77-99`), plus
  `genetics.registerFilterRuleTypes(ApicultureFilterRuleType.values())` and
  `ApicultureFilterRule.init()`
- the whole of `registerApiculture` (`:158-274`) and its private helper `getHoneyComb` (`:276-278`)

**Files:**
- Create: `src/main/java/forestry/apiculture/plugin/ApicultureForestryPlugin.java`
- Modify: `src/main/java/forestry/plugin/DefaultForestryPlugin.java`
- Modify: `src/main/resources/META-INF/services/forestry.api.plugin.IForestryPlugin`

**Interfaces:**
- Consumes: `IForestryPlugin`, `IGeneticRegistration`, `IApicultureRegistration` from api.
- Produces: a plugin whose `id()` returns `ForestryModuleIds.APICULTURE`. Reusing the module id
  avoids inventing a second naming scheme; the ids only need to be unique and stable.

- [ ] **Step 1: Write the plugin**

```java
package forestry.apiculture.plugin;

import forestry.api.modules.ForestryModuleIds;
import forestry.api.plugin.IApicultureRegistration;
import forestry.api.plugin.IForestryPlugin;
import forestry.api.plugin.IGeneticRegistration;
import net.minecraft.resources.ResourceLocation;

/**
 * Base Forestry's apiculture registrations. Split out of {@code forestry.plugin.DefaultForestryPlugin}
 * so the base artifact does not register bee content.
 */
public class ApicultureForestryPlugin implements IForestryPlugin {
	@Override
	public void registerGenetics(IGeneticRegistration genetics) {
		// bee species type block, moved verbatim from DefaultForestryPlugin
		// then the apiculture filter rules
	}

	@Override
	public void registerApiculture(IApicultureRegistration apiculture) {
		// moved verbatim
	}

	@Override
	public ResourceLocation id() {
		return ForestryModuleIds.APICULTURE;
	}
}
```

Move `getHoneyComb` across as a private static helper. Do not retype the hive and effect tables -
cut and paste them.

- [ ] **Step 2: Delete the moved hooks from DefaultForestryPlugin**

Remove the bee species type block and the two apiculture filter-rule lines from `registerGenetics`,
and delete `registerApiculture` and `getHoneyComb` entirely. Then strip every import they alone
needed - including expanding or deleting `import forestry.apiculture.*;` and
`import forestry.apiculture.genetics.effects.*;`. Delete both wildcards, compile, and add back only
what the compiler asks for.

- [ ] **Step 3: Register the plugin**

Add to `src/main/resources/META-INF/services/forestry.api.plugin.IForestryPlugin`, keeping the
existing two lines:

```
forestry.plugin.DefaultForestryPlugin
forestry.apiculture.plugin.ApicultureForestryPlugin
forestry.compat.kubejs.KubeForestryPlugin
```

**This must land in the same commit as Step 2.** A moved hook with no service entry is a silently
unregistered one: no crash, no failing compile, just missing bees.

- [ ] **Step 4: Compile and verify**

```bash
./gradlew compileJava compileTestJava
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
./gradlew runGameTestServer 2>&1 | grep -E "Loaded [0-9]+ bee species|Registered [0-9]+ species types"
```

`DefaultForestryPlugin` still leaks (arboriculture, lepidopterology, farming remain), so the count
stays at `33` and it must **not** be removed from the baseline yet.

Expected: `Registered 3 species types` and `Loaded 69 bee species`, both unchanged. Species types are
the sharpest signal here: if the bee type failed to register, the count drops to 2 and a great many
tests fail loudly.

Also confirm the hive and effect registrations survived - they have no datagen output:

```bash
./gradlew runGameTestServer 2>&1 | grep -iE "hive|bee effect" | head
```

- [ ] **Step 5: Commit**

```bash
git add -A src/main/java/forestry src/main/resources/META-INF/services
git commit -m "apiculture: register its own content through its own plugin

The bee species type, the apiculture filter rules and the whole of
registerApiculture - hives, village bees, effects, jubilances, activity types
and the swarmer material - move to ApicultureForestryPlugin, listed in the same
service file. Every PluginManager pass visits every plugin and every hook has a
no-op default, so behavior is unchanged.

DefaultForestryPlugin still leaks; it clears in the last of these four."
```

---

### Task 7: Give arboriculture its own plugin

Arboriculture's share:
- from `registerGenetics`: the tree species type block (`:102-116`) and
  `genetics.registerFilterRuleTypes(ArboricultureFilterRuleType.values())`
- the whole of `registerArboriculture` (`:281-319`), which calls the already-moved
  `DefaultTreeSpecies.register` and `DefaultWoods.register`
- the whole of `registerPollen` (`:393-395`) - one line registering `TreePollenType`

**Files:**
- Create: `src/main/java/forestry/arboriculture/plugin/ArboricultureForestryPlugin.java`
- Modify: `src/main/java/forestry/plugin/DefaultForestryPlugin.java`
- Modify: `src/main/resources/META-INF/services/forestry.api.plugin.IForestryPlugin`

**Interfaces:**
- Consumes: `DefaultTreeSpecies` and `DefaultWoods` from Tasks 2 and 3, both now in
  `forestry.arboriculture.plugin` - the plugin's own package, so **neither needs an import**.
- Produces: a plugin with `id()` returning `ForestryModuleIds.ARBORICULTURE`.

- [ ] **Step 1: Write the plugin**

Same shape as Task 6, implementing `registerGenetics`, `registerArboriculture` and `registerPollen`.
The fruit table (`:290-306`) references `CoreItems.FRUITS` and `ItemFruit.EnumFruit` - both core, so
those imports come across unchanged. `PodFruit`, `RipeningFruit`, `DummyFruit` and `ForestryPodType`
are arboriculture but in sibling packages, so they keep their imports.

- [ ] **Step 2: Delete the moved hooks and add the service entry**

Remove the tree species type block, the arboriculture filter-rule line, `registerArboriculture` and
`registerPollen` from `DefaultForestryPlugin`, strip the imports they alone needed, and add
`forestry.arboriculture.plugin.ArboricultureForestryPlugin` to the service file in the same commit.

- [ ] **Step 3: Compile and verify**

```bash
./gradlew compileJava compileTestJava
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
./gradlew runGameTestServer 2>&1 | grep -E "Loaded [0-9]+ tree species|Registered [0-9]+ species types"
```

Expected: count still `33`, no datagen diff, all 100 tests passed, `Registered 3 species types`,
`Loaded 50 tree species`.

`registerPollen` is worth an extra look: it is the only hook moving that has a uniqueness check
(`PluginManager:217-224` throws on a duplicate pollen type id). If `TreePollenType` ended up
registered twice - left in core *and* added to arboriculture - the server fails to start with
"A pollen type was already registered". That is a loud failure, not a silent one, which is why this
hook is safe to move.

- [ ] **Step 4: Commit**

```bash
git add -A src/main/java/forestry src/main/resources/META-INF/services
git commit -m "arboriculture: register its own content through its own plugin

The tree species type, the arboriculture filter rules, registerArboriculture
(fruits, tree effects, woods, charcoal pit walls) and registerPollen move to
ArboricultureForestryPlugin. DefaultTreeSpecies and DefaultWoods are already in
that package, so the calls need no imports."
```

---

### Task 8: Give lepidopterology its own plugin

Lepidopterology's share:
- from `registerGenetics`: the butterfly species type block (`:119-142`),
  `genetics.registerFilterRuleTypes(LepidopterologyFilterRuleType.values())` and
  `LepidopterologyFilterRule.init()`
- the whole of `registerLepidopterology` (`:322-337`)

**Files:**
- Create: `src/main/java/forestry/lepidopterology/plugin/LepidopterologyForestryPlugin.java`
- Modify: `src/main/java/forestry/plugin/DefaultForestryPlugin.java`
- Modify: `src/main/resources/META-INF/services/forestry.api.plugin.IForestryPlugin`

**Interfaces:**
- Consumes: `DefaultButterflySpecies`, which stays in `forestry.plugin` - it does not leak, so it is
  out of scope. The new plugin therefore needs `import forestry.plugin.DefaultButterflySpecies;`.
  Content importing base is the allowed direction.
- Produces: a plugin with `id()` returning `ForestryModuleIds.LEPIDOPTEROLOGY`.

- [ ] **Step 1: Write the plugin and move the hooks**

Same shape as Tasks 6 and 7. Note the butterfly species type block ends with
`.addResearchMaterials(map -> map.put(Items.GLASS_BOTTLE, 0.9f))` - carry that chained call across
with the rest.

The cocoon registrations reference `CoreItems.CRAFTING_MATERIALS` and `EnumCraftingMaterial`, both
core, so those imports come across unchanged.

- [ ] **Step 2: Add the service entry and verify**

```bash
./gradlew compileJava compileTestJava
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
./gradlew runGameTestServer 2>&1 | grep -E "Loaded [0-9]+ butterfly species|Registered [0-9]+ species types"
```

Expected: count still `33`, no datagen diff, all 100 tests passed, `Registered 3 species types`,
`Loaded 35 butterfly species`. Butterflies are the best-tested species type in the suite
(`ButterflySpeciesReloadTest`, `ButterflySpeciesFallbackTest`, `ButterflyEntityReloadTest`,
`ButterflySpawnerReloadTest`), so a mistake here surfaces immediately.

- [ ] **Step 3: Commit**

```bash
git add -A src/main/java/forestry src/main/resources/META-INF/services
git commit -m "lepidopterology: register its own content through its own plugin

The butterfly species type, the lepidopterology filter rules and
registerLepidopterology (butterflies, cocoons, the null effect) move to
LepidopterologyForestryPlugin. DefaultButterflySpecies stays in forestry.plugin
- it does not leak and phase 7 relocates it with the package."
```

---

### Task 9: Give agriculture its own plugin and reduce the default to core

The last one. Agriculture takes farming's share, and what remains in `DefaultForestryPlugin` is
genuinely core: errors, the machine-upgrade circuits, and the core filter rules.

Agriculture's share:
- the whole of `registerFarming` (`:386-390`) - `DefaultFarms.registerFarmTypes` plus the fertilizer
- from `registerCircuits`: the `MANAGED_FARM` and `MANUAL_FARM` layouts, all sixteen
  `registerFarmCircuit` calls and the private `registerFarmCircuit` helper (`:373-376`), which
  constructs `CircuitFarmLogic`

Core keeps: `registerErrors`, the `MACHINE_UPGRADE` layout and its four `CircuitMachineUpgrade`
registrations, `registerGenetics`'s `DefaultFilterRuleType` line, and `registerClient`.

**Files:**
- Create: `src/main/java/forestry/farming/plugin/AgricultureForestryPlugin.java`
- Modify: `src/main/java/forestry/plugin/DefaultForestryPlugin.java`
- Modify: `src/main/java/forestry/apiimpl/plugin/PluginManager.java:49-66`
- Modify: `src/main/resources/META-INF/services/forestry.api.plugin.IForestryPlugin`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: `DefaultFarms` from Task 1, already in `forestry.farming.plugin` - the plugin's own
  package, so no import.
- Produces: a plugin with `id()` returning `ForestryModuleIds.FARMING`. `DefaultForestryPlugin` keeps
  `ID = forestry:default` and its `id()`, so nothing referencing that constant breaks.

- [ ] **Step 1: Move farming's share**

Write `AgricultureForestryPlugin` implementing `registerFarming` and `registerCircuits`. Its
`registerCircuits` registers only the two farm layouts and the farm circuits; core's keeps only
`MACHINE_UPGRADE`. Both plugins' `registerCircuits` run in the same `PluginManager` pass, and
circuits key by string id, so splitting the method across two plugins is safe.

Carry `registerFarmCircuit` across as a private static helper.

- [ ] **Step 2: Fix the plugin ordering**

`PluginManager.loadPlugins:52-59` currently puts Forestry's plugin first by naming the class:

```java
if (plugin.getClass() == DefaultForestryPlugin.class) {
    LOADED_PLUGINS.add(0, plugin);
} else {
    LOADED_PLUGINS.add(plugin);
}
```

With five Forestry plugins that is wrong twice over: it only matches one of them, and repeated
`add(0, ...)` would reverse their order. Partition instead, which also stops `PluginManager` naming a
plugin class at all:

```java
	public static void loadPlugins() {
		ServiceLoader<IForestryPlugin> serviceLoader = ServiceLoader.load(IForestryPlugin.class);

		// Forestry's own plugins register before any addon's, so an addon can build on the base content.
		// Within each group the id sort is preserved.
		List<IForestryPlugin> forestryPlugins = new ArrayList<>();
		List<IForestryPlugin> addonPlugins = new ArrayList<>();

		serviceLoader.stream().map(ServiceLoader.Provider::get).sorted(Comparator.comparing(IForestryPlugin::id)).forEachOrdered(plugin -> {
			if (plugin.shouldLoad()) {
				(plugin.id().getNamespace().equals(ForestryConstants.MOD_ID) ? forestryPlugins : addonPlugins).add(plugin);
				Forestry.LOGGER.debug("Registered IForestryPlugin {} with class {}", plugin.id(), plugin.getClass().getName());
			} else {
				Forestry.LOGGER.warn("Detected IForestryPlugin {} with class {} but did not load it because IForestryPlugin.shouldLoad returned false.", plugin.id(), plugin.getClass().getName());
			}
		});

		LOADED_PLUGINS.addAll(forestryPlugins);
		LOADED_PLUGINS.addAll(addonPlugins);
		LOADED_PLUGINS.trimToSize();
	}
```

Delete `import forestry.plugin.DefaultForestryPlugin;` and **add
`import forestry.api.ForestryConstants;`** - `PluginManager` does not import it today. `List`,
`ArrayList` and `Comparator` are already covered by its `import java.util.*;`.

`PluginManager` stays baselined - it leaks `forestry.farming.FarmingManager` and others under bucket
I - so removing this import does not clear it.

- [ ] **Step 3: Verify the resulting plugin order at runtime**

The order changes from "one plugin first" to "five plugins first, id-sorted". Prove it rather than
assume, the way phase 4 proved module load order. Add a temporary log line at the end of
`loadPlugins`:

```java
Forestry.LOGGER.info("PLUGIN ORDER: {}", LOADED_PLUGINS.stream().map(p -> p.id().toString()).toList());
```

```bash
./gradlew runGameTestServer 2>&1 | grep "PLUGIN ORDER"
```

Expected: the five `forestry:` ids first, then any addon. **Remove the log line before committing.**

- [ ] **Step 4: Finish the service file and trim**

Final content:

```
forestry.plugin.DefaultForestryPlugin
forestry.apiculture.plugin.ApicultureForestryPlugin
forestry.arboriculture.plugin.ArboricultureForestryPlugin
forestry.lepidopterology.plugin.LepidopterologyForestryPlugin
forestry.farming.plugin.AgricultureForestryPlugin
forestry.compat.kubejs.KubeForestryPlugin
```

Confirm `DefaultForestryPlugin` is clean, then remove it from the baseline:

```bash
grep -cE "^import (static )?forestry\.(apiculture|arboriculture|lepidopterology|farming|cultivation|mail)\." src/main/java/forestry/plugin/DefaultForestryPlugin.java
```

Expected: `0`.

- [ ] **Step 5: Verify**

```bash
./gradlew compileJava compileTestJava
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
./gradlew runGameTestServer 2>&1 | grep -E "Loaded [0-9]+ (bee|tree|butterfly) species|Registered [0-9]+ species types|Loaded [0-9]+ .* mutation"
```

Expected: `32 known leaking file(s) remaining`, no datagen diff, all 100 tests passed,
`Registered 3 species types`, and the full count set unchanged: 69 bees, 50 trees, 35 butterflies,
114/42/1 mutation recipes.

Farm circuits have no datagen output and no GameTest. Count them:

```bash
git show HEAD~4:src/main/java/forestry/plugin/DefaultForestryPlugin.java | grep -c "registerFarmCircuit\|circuits.registerCircuit\|circuits.registerLayout"
grep -hc "registerFarmCircuit\|circuits.registerCircuit\|circuits.registerLayout" src/main/java/forestry/plugin/DefaultForestryPlugin.java src/main/java/forestry/farming/plugin/AgricultureForestryPlugin.java | paste -sd+ | bc
```

The second number is one higher than the first, because the private `registerFarmCircuit` helper's
own declaration is counted in the module file. Adjust the grep or subtract one - do not skip the
check, circuits are otherwise invisible to every oracle in this repo.

- [ ] **Step 6: Commit**

```bash
git add -A src/main/java/forestry src/main/resources/META-INF/services gradle/base-boundary-baseline.txt
git commit -m "agriculture: register its own content, leaving the default plugin to core

registerFarming and the farm circuits move to AgricultureForestryPlugin. What
remains in DefaultForestryPlugin is core: errors, the machine-upgrade circuits
and the core filter rules.

PluginManager no longer names a plugin class to sort it first. It partitions on
the id namespace instead, which handles five Forestry plugins where the old
add(0, ...) would have reversed them.

Five plugins, not the six the spec assumed - mail registers nothing.

checkBaseBoundary: 33 -> 32 files."
```

---

### Task 10: Record phase 5 completion

**Files:**
- Modify: `docs/superpowers/specs/2026-07-30-feature-package-reorg-design.md`

- [ ] **Step 1: Confirm from a clean build**

```bash
./gradlew clean build
```

Expected: `BUILD SUCCESSFUL`, `checkApiBoundary: forestry.api is clean`,
`checkBaseBoundary: 32 known leaking file(s) remaining`.

- [ ] **Step 2: Update the spec**

Mark phase 5 `DONE` in the sequencing block and record: five plugins rather than six; that the split
was mechanical because every `PluginManager` pass visits every plugin and every hook has a no-op
default; that errors stayed unsplit because their numeric ids are insertion-ordered and
network-serialized, while filter rules were safe to split because `FilterManager` sorts
alphabetically first; and that `PluginManager` no longer names a plugin class.

- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/specs/2026-07-30-feature-package-reorg-design.md
git commit -m "docs: record phase 5 completion, base boundary at 32 of 68"
```

---

## Notes for phase 5a

- Bucket I is next: the twelve `apiimpl/` files, and the hardest remaining work. Unlike buckets B
  through G, "check whether it is really a misfiled type" will **not** apply - `ForestryApiImpl:14-19`
  constructs `HiveManager`, `TreeManager` and `FarmingManager` directly, and the eight registration
  builders implement api/plugin interfaces that D3 ships in base while constructing content classes.
- Phase 4's Task 8 and this phase's Task 9 are both rehearsals for it: an SPI core already owns, with
  per-module contribution behind it, and core keeping the sequencing. The difference is that bucket I
  needs the *construction* inverted, not just the call sites moved.
- After 5a the remaining leaks should be almost entirely bucket A - the 20 datagen providers, led by
  `ForestryRecipeProvider` at 26 imports. **They dissolve in phase 8** when datagen splits into
  per-jar source sets, not by being fixed in place, so expect the count to sit at 20 from the end of
  5a until then. That is the plan working, not a stall.
