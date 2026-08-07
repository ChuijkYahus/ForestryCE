# Phase 5a: sever the api implementation wiring

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Clear bucket I - the twelve `apiimpl/` files that construct content classes - taking
`checkBaseBoundary` from 32 to 20.

**Architecture:** The spec calls this the hardest remaining work and warns that "check whether it is
really a misfiled type" will not apply. That is half right. Twenty-two leaks across twelve files
resolve into four kinds, and only two of them are the inversion the spec describes:

| Kind | Files | Fix |
| --- | --- | --- |
| Setter takes the concrete class while the getter returns the interface | 1 | widen the parameter |
| A registration and the builder it constructs, both belonging to one module | 8 | move each pair together |
| Base constructs a content manager | 2 | the module's own registration builds it |
| Base needs a content factory at class-init time | 1 | `ServiceLoader`, as api already does twice |

Tasks are ordered cheapest first. The last two carry the phase's only real design decisions.

**Tech Stack:** Java 21, NeoForge 21.1.230, Minecraft 1.21.1, ModDevGradle 2.0.x. GameTests only, no
JUnit.

## Global Constraints

- Comment and Javadoc style is binding; see `CLAUDE.md`. ASCII only. `@return` and `@param` are
  noun-phrase fragments with no terminal period. Lowercase `todo`.
- Every task ends with `./gradlew runData` producing no diff in `src/generated/resources` and
  `./gradlew runGameTestServer` reporting all 100 tests passed.
- Both gates stay honest: `checkApiBoundary` green, `checkBaseBoundary` trimmed in the same commit
  that clears a file. A stale baseline fails the build by design.
- **Run `./gradlew compileTestJava`, not just `compileJava`.**
- **A moved class must move with everything that constructs it, or the constructor site becomes a new
  leak.** This bit in phase 4 (`BeeClientManager`) and was designed around in phase 5. Bucket I is
  almost entirely constructor chains, so check the construction site of every class before moving it.
- **Repoint existing imports; do not add a second one.** Phase 5 hit this: a file that already
  imported the moved class ended up with two imports of the same simple name and would not compile.
- When a class moves package, check for callers that used it **without an import** because they were
  in the same package, and for imports inside the moved file that same-package placement now makes
  redundant.
- All source files are LF as of 2026-07-31. Do not write `$`-anchored `sed` patterns.

## Starting state

`checkBaseBoundary`: 32 files. Bucket I is 12 of them, holding 22 leaking imports.

| File | Leaks | What it actually is |
| --- | --- | --- |
| `apiimpl/client/plugin/ClientHelper.java` | 3 | an arboriculture factory behind an api interface |
| `apiimpl/ForestryApiImpl.java` | 3 | three setters typed to the impl |
| `apiimpl/plugin/SpeciesTypeBuilder.java` | 2 | a generic builder with one bee item in its defaults |
| `apiimpl/plugin/BeeSpeciesBuilder.java` | 2 | apiculture's builder |
| `apiimpl/plugin/ArboricultureRegistration.java` | 2 | arboriculture's registration |
| `apiimpl/plugin/HiveBuilder.java` | 2 | apiculture's builder |
| `apiimpl/plugin/PluginManager.java` | 2 | constructs two content managers |
| `apiimpl/plugin/TreeSpeciesBuilder.java` | 2 | arboriculture's builder |
| `apiimpl/client/TreeClientManager.java` | 1 | arboriculture's client manager |
| `apiimpl/plugin/FarmTypeBuilder.java` | 1 | farming's builder |
| `apiimpl/plugin/ButterflySpeciesBuilder.java` | 1 | lepidopterology's builder |
| `apiimpl/plugin/ApicultureRegistration.java` | 1 | apiculture's registration |

`forestry/apiimpl/` holds 33 files; the 21 that do not leak stay put. Phase 7 relocates packages
wholesale.

### The construction graph, measured

This is the fact the whole phase rests on. Every registration is constructed by its own module
already, or by a file that is already baselined:

| Registration | Constructed by | In scope? |
| --- | --- | --- |
| `ApicultureRegistration` | `apiculture/genetics/BeeSpeciesType:187`, `core/data/BeeSpeciesProvider:89` | module + baselined |
| `ArboricultureRegistration` | `arboriculture/genetics/TreeSpeciesType:147`, `core/data/TreeSpeciesProvider:86` | module + baselined |
| `LepidopterologyRegistration` | `lepidopterology/genetics/ButterflySpeciesType:82`, `core/data/ButterflySpeciesProvider:87` | module + baselined |
| `FarmingRegistration` | `apiimpl/plugin/PluginManager:205` | baselined |
| `GeneticRegistration` | `apiimpl/plugin/PluginManager:152`, `plugin/ForestryTaxonomy:26` | base, stays |

And each builder is constructed only by its registration:

| Builder | Constructed by |
| --- | --- |
| `BeeSpeciesBuilder`, `HiveBuilder` | `ApicultureRegistration:44,97` |
| `TreeSpeciesBuilder` | `ArboricultureRegistration:30` |
| `ButterflySpeciesBuilder` | `LepidopterologyRegistration:41` |
| `FarmTypeBuilder` | `FarmingRegistration:22` |
| `SpeciesTypeBuilder` | `GeneticRegistration:74` |

So each builder moves **with its registration**, and `LepidopterologyRegistration` and
`FarmingRegistration` move even though they do not leak - leaving them behind would make them leak.
`GeneticRegistration` and `SpeciesTypeBuilder` are genuinely generic and stay in base; Task 7 deals
with the one bee item inside `SpeciesTypeBuilder`.

### What has no oracle

Say this plainly before starting, because most of this phase is invisible to the test suite:

- **Research materials are runtime-only.** `SpeciesType:52-53` builds the map in the constructor;
  nothing writes it to `src/generated/resources` and no GameTest asserts on it. Task 8 therefore has
  to preserve behavior by construction, not by test.
- **`ClientHelper` and `TreeClientManager` are client-only**, and no GameTest runs a client.
- The builders and registrations **are** well covered: they produce every species, so
  `runData` regenerates 69 bee, 50 tree and 35 butterfly JSON files through them, and
  `Registered 3 species types` plus the species counts catch a break immediately.

## File Structure

| Action | File | Responsibility |
| --- | --- | --- |
| Modify | `apiimpl/ForestryApiImpl.java` | three setters take their interfaces |
| Move | `apiimpl/plugin/ApicultureRegistration.java`, `BeeSpeciesBuilder.java`, `HiveBuilder.java` -> `apiculture/plugin/` | apiculture's registration chain |
| Move | `apiimpl/plugin/ArboricultureRegistration.java`, `TreeSpeciesBuilder.java` -> `arboriculture/plugin/` | arboriculture's registration chain |
| Move | `apiimpl/plugin/LepidopterologyRegistration.java`, `ButterflySpeciesBuilder.java` -> `lepidopterology/plugin/` | lepidopterology's registration chain |
| Move | `apiimpl/plugin/FarmingRegistration.java`, `FarmTypeBuilder.java` -> `farming/plugin/` | farming's registration chain |
| Move | `apiimpl/client/TreeClientManager.java` -> `arboriculture/client/` | arboriculture's client manager |
| Modify | `apiimpl/plugin/PluginManager.java` | stops constructing two content managers |
| Modify | `apiimpl/plugin/SpeciesTypeBuilder.java` | resolves the comb by id |
| Move | `apiimpl/client/plugin/ClientHelper.java` -> `arboriculture/client/plugin/` | behind a `ServiceLoader` lookup |

---

### Task 1: Type the ForestryApiImpl setters to their interfaces

`ForestryApiImpl` holds `IFarmingManager`, `IHiveManager` and `ITreeManager` fields and returns the
interfaces from its getters, but its three setters take `FarmingManager`, `HiveManager` and
`TreeManager` - the impls. Those three imports are the file's only leaks.

This is the exact defect phase 4 found in `setBeeManager`, which took the concrete
`BeeClientManager` while `setTreeManager` and `setButterflyManager` took interfaces. Same fix.

**Files:**
- Modify: `src/main/java/forestry/apiimpl/ForestryApiImpl.java:148-160`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: nothing.
- Produces: `setFarmingManager(IFarmingManager)`, `setHiveManager(IHiveManager)`,
  `setTreeManager(ITreeManager)`. All three callers already pass the concrete type, which still
  compiles against the widened parameter.

- [ ] **Step 1: Check the other setters for the same shape**

```bash
grep -n "public void set" src/main/java/forestry/apiimpl/ForestryApiImpl.java
```

`setCircuitManager`, `setErrorManager`, `setGeneticManager` and `setFilterManager` also take concrete
types, but `CircuitManager`, `ErrorManager`, `GeneticManager` and `FilterManager` are all base
(`core`, `apiimpl`, `sorting`), so they are not leaks and are **out of scope**. Do not widen them -
that is unrelated churn. Only the three content managers change.

- [ ] **Step 2: Widen the three**

```java
	@ApiStatus.Internal
	public void setFarmingManager(IFarmingManager farmingManager) {
		this.farmingManager = farmingManager;
	}

	@ApiStatus.Internal
	public void setHiveManager(IHiveManager hiveManager) {
		this.hiveManager = hiveManager;
	}

	@ApiStatus.Internal
	public void setTreeManager(ITreeManager treeManager) {
		this.treeManager = treeManager;
	}
```

Keep whatever annotations the existing setters carry. Then delete the three now-unused imports:

```java
import forestry.apiculture.hives.HiveManager;
import forestry.arboriculture.TreeManager;
import forestry.farming.FarmingManager;
```

- [ ] **Step 3: Compile, trim, verify**

```bash
./gradlew compileJava compileTestJava
```

Remove `apiimpl/ForestryApiImpl.java` from `gradle/base-boundary-baseline.txt`, then:

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `31 known leaking file(s)`, no datagen diff, all 100 tests passed.

- [ ] **Step 4: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "api: type the manager setters to their interfaces

ForestryApiImpl stored and returned IFarmingManager, IHiveManager and
ITreeManager but its setters took the impls. Same defect phase 4 found in
setBeeManager; the callers already pass the concrete type.

checkBaseBoundary: 32 -> 31 files."
```

---

### Task 2: Move apiculture's registration chain

`ApicultureRegistration` implements the api `IApicultureRegistration` and constructs
`BeeSpeciesBuilder` (`:44`) and `HiveBuilder` (`:97`). Those two build `BeeSpecies`/`DefaultBeeJubilance`
and `Hive`/`HiveDrop`. All three files belong to apiculture, and apiculture already constructs the
registration at `BeeSpeciesType:187`.

**Files:**
- Move: `apiimpl/plugin/ApicultureRegistration.java`, `BeeSpeciesBuilder.java`, `HiveBuilder.java` -> `src/main/java/forestry/apiculture/plugin/`
- Modify: `apiculture/genetics/BeeSpeciesType.java`, `core/data/BeeSpeciesProvider.java`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: nothing.
- Produces: `forestry.apiculture.plugin.ApicultureRegistration`, `BeeSpeciesBuilder`, `HiveBuilder`,
  unchanged in shape. `forestry.apiculture.plugin` already exists - phase 5 put `DefaultBeeSpecies`
  and `ApicultureForestryPlugin` there.

- [ ] **Step 1: Confirm the users**

```bash
grep -rn "ApicultureRegistration\|BeeSpeciesBuilder\|HiveBuilder" src/main/java src/test/java --include='*.java' \
  | grep -vE "apiimpl/plugin/(ApicultureRegistration|BeeSpeciesBuilder|HiveBuilder)\.java:"
```

Expected: `BeeSpeciesType:187`, `BeeSpeciesProvider:89` and any javadoc mentions. **If a base file
other than `BeeSpeciesProvider` constructs one of these, it becomes a new leak** - stop and account
for it.

- [ ] **Step 2: Move all three together**

```bash
for f in ApicultureRegistration BeeSpeciesBuilder HiveBuilder; do
  git mv src/main/java/forestry/apiimpl/plugin/$f.java src/main/java/forestry/apiculture/plugin/$f.java
  sed -i '1s@^package forestry\.apiimpl\.plugin;@package forestry.apiculture.plugin;@' src/main/java/forestry/apiculture/plugin/$f.java
done
```

They referenced each other without imports (same package before, same package after), so nothing to
add between them.

- [ ] **Step 3: Repoint the two users**

Both `BeeSpeciesType` and `BeeSpeciesProvider` import `forestry.apiimpl.plugin.ApicultureRegistration`
today. **Repoint those imports, do not add new ones:**

```bash
grep -rl "import forestry\.apiimpl\.plugin\.\(ApicultureRegistration\|BeeSpeciesBuilder\|HiveBuilder\);" src/main/java \
  | xargs -r sed -i 's@import forestry\.apiimpl\.plugin\.\(ApicultureRegistration\|BeeSpeciesBuilder\|HiveBuilder\);@import forestry.apiculture.plugin.\1;@'
```

`BeeSpeciesType` is in `forestry.apiculture.genetics` - a sibling of `forestry.apiculture.plugin`, not
the same package - so it keeps an import, just a rewritten one.

Then check the moved files for imports the new package makes redundant:

```bash
grep -n "^import forestry" src/main/java/forestry/apiculture/plugin/ApicultureRegistration.java
```

Anything under `forestry.apiculture.plugin` is now same-package and must go; anything under
`forestry.apiculture.*` elsewhere stays.

- [ ] **Step 4: Compile, trim, verify**

```bash
./gradlew compileJava compileTestJava
```

Remove these three from the baseline:

```
apiimpl/plugin/ApicultureRegistration.java
apiimpl/plugin/BeeSpeciesBuilder.java
apiimpl/plugin/HiveBuilder.java
```

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
./gradlew runGameTestServer 2>&1 | grep -E "Loaded [0-9]+ bee species"
```

Expected: `28 known leaking file(s)`, no datagen diff, all 100 tests passed, `Loaded 69 bee species`.
The 69 generated `bee_species/*.json` files are produced through `BeeSpeciesBuilder`, so the datagen
check exercises the moved code directly.

- [ ] **Step 5: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "apiculture: own its registration and builders

ApicultureRegistration builds BeeSpeciesBuilder and HiveBuilder, which build
BeeSpecies, Hive and HiveDrop. All three move together: leaving the
registration behind would have made it leak instead of clearing it, which is
the trap phase 4 hit with BeeClientManager.

apiculture already constructed the registration at BeeSpeciesType:187.

checkBaseBoundary: 31 -> 28 files."
```

---

### Task 3: Move arboriculture's registration chain

`ArboricultureRegistration` leaks `TreeManager` and `CharcoalManager` and constructs
`TreeSpeciesBuilder` (`:30`), which builds `TreeSpecies` and `DefaultTreeGenerator`. Constructed by
`TreeSpeciesType:147` (arboriculture) and `TreeSpeciesProvider:86` (baselined).

**Files:**
- Move: `apiimpl/plugin/ArboricultureRegistration.java`, `TreeSpeciesBuilder.java` -> `src/main/java/forestry/arboriculture/plugin/`
- Modify: `arboriculture/genetics/TreeSpeciesType.java`, `core/data/TreeSpeciesProvider.java`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: nothing.
- Produces: `forestry.arboriculture.plugin.ArboricultureRegistration`, `TreeSpeciesBuilder`.
  `forestry.arboriculture.plugin` already exists from phase 5.

- [ ] **Step 1: Move and repoint**

```bash
for f in ArboricultureRegistration TreeSpeciesBuilder; do
  git mv src/main/java/forestry/apiimpl/plugin/$f.java src/main/java/forestry/arboriculture/plugin/$f.java
  sed -i '1s@^package forestry\.apiimpl\.plugin;@package forestry.arboriculture.plugin;@' src/main/java/forestry/arboriculture/plugin/$f.java
done
grep -rl "import forestry\.apiimpl\.plugin\.\(ArboricultureRegistration\|TreeSpeciesBuilder\);" src/main/java \
  | xargs -r sed -i 's@import forestry\.apiimpl\.plugin\.\(ArboricultureRegistration\|TreeSpeciesBuilder\);@import forestry.arboriculture.plugin.\1;@'
```

`ArboricultureRegistration` imports `forestry.arboriculture.TreeManager` and
`forestry.arboriculture.charcoal.CharcoalManager` - both sibling packages, so both imports stay.

`TreeSpeciesProvider` already imports it (phase 5 repointed `DefaultTreeSpecies` there); the sed above
handles it. Confirm there is exactly one import per file afterwards:

```bash
grep -c "import forestry.arboriculture.plugin.ArboricultureRegistration;" src/main/java/forestry/core/data/TreeSpeciesProvider.java
```

Expected: `1`.

- [ ] **Step 2: Compile, trim, verify**

```bash
./gradlew compileJava compileTestJava
```

Remove `apiimpl/plugin/ArboricultureRegistration.java` and `apiimpl/plugin/TreeSpeciesBuilder.java`
from the baseline, then:

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests|Loaded [0-9]+ tree species"
```

Expected: `26 known leaking file(s)`, no datagen diff, all 100 tests passed, `Loaded 50 tree species`.

- [ ] **Step 3: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "arboriculture: own its registration and builder

ArboricultureRegistration names TreeManager and CharcoalManager and builds
TreeSpeciesBuilder, which builds TreeSpecies and DefaultTreeGenerator. Both
move together for the same reason apiculture's did.

checkBaseBoundary: 28 -> 26 files."
```

---

### Task 4: Move lepidopterology's and farming's registration chains

Two chains, one commit, because each is a single builder plus a registration that does not itself
leak:

- `ButterflySpeciesBuilder` leaks `lepidopterology.ButterflySpecies`. Its only constructor is
  `LepidopterologyRegistration:41`, which is **clean today** - so it must move too, or it becomes a
  new leak.
- `FarmTypeBuilder` leaks `farming.logic.FarmType`. Its only constructor is `FarmingRegistration:22`,
  also clean today, also must move.

**Files:**
- Move: `apiimpl/plugin/LepidopterologyRegistration.java`, `ButterflySpeciesBuilder.java` -> `src/main/java/forestry/lepidopterology/plugin/`
- Move: `apiimpl/plugin/FarmingRegistration.java`, `FarmTypeBuilder.java` -> `src/main/java/forestry/farming/plugin/`
- Modify: `lepidopterology/genetics/ButterflySpeciesType.java`, `core/data/ButterflySpeciesProvider.java`, `apiimpl/plugin/PluginManager.java`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: nothing.
- Produces: `forestry.lepidopterology.plugin.LepidopterologyRegistration`, `ButterflySpeciesBuilder`;
  `forestry.farming.plugin.FarmingRegistration`, `FarmTypeBuilder`. Both target packages exist from
  phase 5.

- [ ] **Step 1: Move both pairs**

```bash
for f in LepidopterologyRegistration ButterflySpeciesBuilder; do
  git mv src/main/java/forestry/apiimpl/plugin/$f.java src/main/java/forestry/lepidopterology/plugin/$f.java
  sed -i '1s@^package forestry\.apiimpl\.plugin;@package forestry.lepidopterology.plugin;@' src/main/java/forestry/lepidopterology/plugin/$f.java
done
for f in FarmingRegistration FarmTypeBuilder; do
  git mv src/main/java/forestry/apiimpl/plugin/$f.java src/main/java/forestry/farming/plugin/$f.java
  sed -i '1s@^package forestry\.apiimpl\.plugin;@package forestry.farming.plugin;@' src/main/java/forestry/farming/plugin/$f.java
done
```

- [ ] **Step 2: Repoint every user**

```bash
grep -rl "import forestry\.apiimpl\.plugin\.\(LepidopterologyRegistration\|ButterflySpeciesBuilder\);" src/main/java \
  | xargs -r sed -i 's@import forestry\.apiimpl\.plugin\.\(LepidopterologyRegistration\|ButterflySpeciesBuilder\);@import forestry.lepidopterology.plugin.\1;@'
grep -rl "import forestry\.apiimpl\.plugin\.\(FarmingRegistration\|FarmTypeBuilder\);" src/main/java \
  | xargs -r sed -i 's@import forestry\.apiimpl\.plugin\.\(FarmingRegistration\|FarmTypeBuilder\);@import forestry.farming.plugin.\1;@'
```

`PluginManager:205` constructs `FarmingRegistration` **without an import** - they were the same
package. It now needs `import forestry.farming.plugin.FarmingRegistration;` added. `PluginManager` is
already baselined and Task 6 clears it, so this is not a new leaking file, but the import is required
to compile. This is the same-package effect that has bitten in phases 1b, 2, 3 and 4.

- [ ] **Step 3: Compile, trim, verify**

```bash
./gradlew compileJava compileTestJava
```

Remove `apiimpl/plugin/ButterflySpeciesBuilder.java` and `apiimpl/plugin/FarmTypeBuilder.java` from
the baseline. `LepidopterologyRegistration` and `FarmingRegistration` were never in it.

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests|Loaded [0-9]+ butterfly species"
```

Expected: `24 known leaking file(s)`, no datagen diff, all 100 tests passed, `Loaded 35 butterfly
species`. Farm types have no datagen output; the farm circuits registered in phase 5 reference them
by id, so a break shows as a missing circuit rather than a diff.

- [ ] **Step 4: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "lepidopterology, farming: own their registrations and builders

ButterflySpeciesBuilder and FarmTypeBuilder each leak one content class, and
each is constructed by a registration that is clean today. Moving the builders
alone would have moved the leak rather than removed it, so both pairs move.

PluginManager constructed FarmingRegistration without an import because they
shared a package; it needs one now.

checkBaseBoundary: 26 -> 24 files."
```

---

### Task 5: Move TreeClientManager to arboriculture

`TreeClientManager` implements the api `ITreeClientManager` and leaks
`arboriculture.client.FixedLeafTint` at `:50`, where it falls back to a fixed tint built from the
species' escritoire color. It is arboriculture's client manager sitting in `apiimpl`, exactly like
`BeeClientManager` before phase 4 moved it.

Its only constructor is `PluginManager:283`, which is baselined, and
`ForestryClientApiImpl.setTreeManager` already takes `ITreeClientManager` - phase 4 confirmed that
asymmetry existed only for the bee one.

**Files:**
- Move: `apiimpl/client/TreeClientManager.java` -> `src/main/java/forestry/arboriculture/client/TreeClientManager.java`
- Modify: `apiimpl/plugin/PluginManager.java`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: nothing.
- Produces: `forestry.arboriculture.client.TreeClientManager`, unchanged. `forestry.arboriculture.client`
  already holds `BiomeLeafTint` and `FixedLeafTint`, so the leaking import becomes same-package and is
  **deleted**, not rewritten.

- [ ] **Step 1: Confirm the setter takes the interface**

```bash
grep -n "setTreeManager" src/main/java/forestry/apiimpl/client/ForestryClientApiImpl.java
```

Expected: `public void setTreeManager(ITreeClientManager treeManager)`. If it takes the concrete type,
widen it first - that is Task 1's fix applied here, and without it this move relocates the leak.

- [ ] **Step 2: Move it**

```bash
git mv src/main/java/forestry/apiimpl/client/TreeClientManager.java src/main/java/forestry/arboriculture/client/TreeClientManager.java
sed -i '1s@^package forestry\.apiimpl\.client;@package forestry.arboriculture.client;@' src/main/java/forestry/arboriculture/client/TreeClientManager.java
sed -i '/^import forestry\.arboriculture\.client\.FixedLeafTint;$/d' src/main/java/forestry/arboriculture/client/TreeClientManager.java
grep -rl "import forestry\.apiimpl\.client\.TreeClientManager;" src/main/java \
  | xargs -r sed -i 's@import forestry\.apiimpl\.client\.TreeClientManager;@import forestry.arboriculture.client.TreeClientManager;@'
```

If `PluginManager` referenced it without an import, add one. Check the moved file for anything it used
from `forestry.apiimpl.client` without an import - that package is no longer its own.

- [ ] **Step 3: Compile, trim, verify**

```bash
./gradlew compileJava compileTestJava
```

Remove `apiimpl/client/TreeClientManager.java` from the baseline, then:

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `23 known leaking file(s)`, no datagen diff, all 100 tests passed. **Client-only, so the
suite cannot see this** - the compiler and the unchanged constructor call are the oracle.

- [ ] **Step 4: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "arboriculture: own its client manager

TreeClientManager is arboriculture's ITreeClientManager implementation, and its
leaf-tint fallback names FixedLeafTint, which lives in the package it moves
into - so the import is deleted rather than rewritten.

Same move phase 4 made for BeeClientManager, minus the setter fix: setTreeManager
already took the interface.

checkBaseBoundary: 24 -> 23 files."
```

---

### Task 6: Stop PluginManager constructing content managers

`PluginManager` has two leaks left, both constructions:

- `:216` builds `new FarmingManager(...)` from the registration's fertilizers and farm types, then
  hands it to `setFarmingManager`.
- `:257` builds `new BeeClientManager(defaultBeeModels, customBeeModels)` and hands it to
  `setBeeManager`.

Both registrations already live in a module after Tasks 2 and 4. Let the registration build its own
manager and return it as the api interface, so `PluginManager` never names either impl.

**Files:**
- Modify: `src/main/java/forestry/farming/plugin/FarmingRegistration.java`
- Modify: `src/main/java/forestry/apiimpl/client/plugin/ClientRegistration.java` (or wherever the bee
  models are gathered - confirm in Step 1)
- Modify: `src/main/java/forestry/apiimpl/plugin/PluginManager.java:210-220,250-260`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: `IFarmingManager` and `IBeeClientManager` from api.
- Produces: `FarmingRegistration.buildManager()` returning `IFarmingManager`, and an equivalent for
  the bee client manager. Confirm the exact holder of `defaultBeeModels`/`customBeeModels` in Step 1;
  this plan does not assume it.

- [ ] **Step 1: Read both construction sites**

```bash
sed -n '203,222p' src/main/java/forestry/apiimpl/plugin/PluginManager.java
sed -n '245,262p' src/main/java/forestry/apiimpl/plugin/PluginManager.java
grep -n "defaultBeeModels\|customBeeModels" src/main/java/forestry/apiimpl/plugin/*.java src/main/java/forestry/apiimpl/client/plugin/*.java
```

Note where the two model maps come from. If they are fields on the client registration object, that
object is the natural place to build the manager. If they are locals in `PluginManager`, they move
with the build call.

- [ ] **Step 2: Move the farming manager construction**

On `FarmingRegistration`, add:

```java
	/**
	 * @return The farming manager built from everything registered through this object
	 */
	public IFarmingManager buildManager() {
		return new FarmingManager(new Object2IntOpenHashMap<>(getFertilizers()), buildFarmTypes());
	}
```

`FarmingRegistration` is in `forestry.farming.plugin` and `FarmingManager` is in `forestry.farming`, a
sibling package, so it needs `import forestry.farming.FarmingManager;` - allowed, content importing
content within one jar.

`PluginManager:216-218` becomes:

```java
		((ForestryApiImpl) IForestryApi.INSTANCE).setFarmingManager(registration.buildManager());
```

Delete `import forestry.farming.FarmingManager;`. Also delete
`import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;` - it appears exactly twice in
`PluginManager`, the import and this one line, so moving the construction leaves it unused. Verify
with `grep -c Object2IntOpenHashMap` before and after rather than trusting this.

- [ ] **Step 3: Move the bee client manager construction**

The same shape, on whichever object Step 1 identified as holding the model maps. If that object is in
`apiimpl`, it will start leaking `BeeClientManager` - so the build method must go on something in
`forestry.apiculture.client` instead. **The simplest correct placement is a static factory on
`BeeClientManager` itself**, taking the two maps:

```java
	public static IBeeClientManager create(Map<ILifeStage, ResourceLocation> defaultModels, Table<ResourceLocation, ILifeStage, ResourceLocation> customModels) {
		return new BeeClientManager(defaultModels, customModels);
	}
```

That does not help - `PluginManager` still names the class to call it. Instead have the client
registration expose the two maps and let **`ApicultureForestryPlugin`** or an apiculture-side hook
install the manager. Decide this from what Step 1 shows; if the wiring makes that awkward, the honest
fallback is to leave `PluginManager` in the baseline for this one import and clear it in phase 6 with
the other `isLoaded()` work, rather than inventing a mechanism that fits badly.

**Do not guess here.** Read the code first and pick the placement that needs no new indirection.

- [ ] **Step 4: Compile, trim, verify**

```bash
./gradlew compileJava compileTestJava
grep -cE "^import (static )?forestry\.(apiculture|arboriculture|lepidopterology|farming|cultivation|mail)\." src/main/java/forestry/apiimpl/plugin/PluginManager.java
```

Expected: `0`. Remove `apiimpl/plugin/PluginManager.java` from the baseline only if that is `0`.

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `22 known leaking file(s)`, no datagen diff, all 100 tests passed. If Step 3 ended in the
fallback, expect `23` and leave `PluginManager` baselined - say so in the commit message rather than
trimming it.

- [ ] **Step 5: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "plugin: let the registrations build their own managers

PluginManager constructed FarmingManager and BeeClientManager directly. Both
registrations now live in their module after the earlier tasks, so each builds
its own manager and returns the api interface.

checkBaseBoundary: 23 -> 22 files."
```

---

### Task 7: Resolve the honey comb by id in SpeciesTypeBuilder

`SpeciesTypeBuilder` is the **generic** species type builder - `GeneticRegistration:74` constructs one
for every species type, bee, tree and butterfly alike. Its constructor seeds the default research
materials:

```java
		// The default research materials across all species in Forestry
		this.researchMaterials = map -> {
			map.put(CoreItems.HONEY_DROP.item(), 0.5f);
			map.put(CoreItems.HONEYDEW.item(), 0.7f);
			map.put(ApicultureItems.BEE_COMBS.item(EnumHoneyComb.HONEY), 0.4f);
		};
```

Honey drop and honeydew moved to core in phase 2. The comb is apiculture's, and it is the file's only
leak - both imports serve that one line.

**Do not simply delete it.** It applies to trees and butterflies too, and nothing in this repo would
notice: research materials are built at runtime by `SpeciesType:52-53`, never written to
`src/generated/resources`, and no GameTest asserts on them. Behavior has to be preserved by
construction.

Resolve the item from the registry instead, the pattern phase 1a established for `BeeLifeStage`:

**Files:**
- Modify: `src/main/java/forestry/apiimpl/plugin/SpeciesTypeBuilder.java`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: nothing.
- Produces: no signature change. The lambda is unchanged in shape and still deferred - it is only
  invoked from `SpeciesType`'s constructor, well after item registration.

- [ ] **Step 1: Confirm the registry id**

```bash
ls src/generated/resources/assets/forestry/models/item/honey_comb.json
```

The id is `forestry:honey_comb`: `ApicultureItems.BEE_COMBS` names each comb
`type.getSerializedName() + "_comb"`, and `EnumHoneyComb.HONEY` serializes to `honey`. Verified by the
generated model file above.

- [ ] **Step 2: Replace the reference**

```java
		// The default research materials across all species in Forestry. The comb is resolved by id
		// rather than named: it belongs to apiculture, and the escritoire has to work without it.
		this.researchMaterials = map -> {
			map.put(CoreItems.HONEY_DROP.item(), 0.5f);
			map.put(CoreItems.HONEYDEW.item(), 0.7f);
			Item honeyComb = BuiltInRegistries.ITEM.get(ForestryConstants.forestry("honey_comb"));
			if (honeyComb != Items.AIR) {
				map.put(honeyComb, 0.4f);
			}
		};
```

The `!= Items.AIR` guard matters: `BuiltInRegistries.ITEM` is a `DefaultedRegistry`, so `get` returns
AIR rather than null for a missing id, and seeding AIR into the research map would make an empty hand
count as research material.

Delete `import forestry.apiculture.features.ApicultureItems;` and
`import forestry.apiculture.items.EnumHoneyComb;`; add `forestry.api.ForestryConstants`,
`net.minecraft.core.registries.BuiltInRegistries`, `net.minecraft.world.item.Item` and
`net.minecraft.world.item.Items` as needed.

- [ ] **Step 3: Prove the behavior is unchanged**

There is no test for this, so add the check by hand. Confirm the comb still resolves at the point the
lambda runs:

```bash
./gradlew runGameTestServer 2>&1 | grep -iE "honey_comb|research" | head
```

That will likely print nothing, which is the point. Instead verify by temporary logging: add a
`Forestry.LOGGER.info` inside the lambda printing the resolved item, run the server, confirm it prints
`forestry:honey_comb` and not `minecraft:air`, then **remove the logging before committing**. This is
the same technique phase 4 and phase 5 used for module and plugin order.

- [ ] **Step 4: Compile, trim, verify**

```bash
./gradlew compileJava compileTestJava
```

Remove `apiimpl/plugin/SpeciesTypeBuilder.java` from the baseline, then:

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `21 known leaking file(s)`, no datagen diff, all 100 tests passed.

- [ ] **Step 5: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "genetics: resolve the default research comb by id

SpeciesTypeBuilder is the generic builder for every species type, and its
default research materials named an apiculture item. Deleting it would have
silently changed tree and butterfly research values, which nothing in this repo
tests - research materials are runtime-only, absent from datagen and from every
GameTest. Resolving forestry:honey_comb from the registry preserves the behavior
exactly while apiculture is present, and skips it when the item is absent.

Verified by temporary logging that the id resolves and does not fall back to
air.

checkBaseBoundary: 22 -> 21 files."
```

---

### Task 8: Put ClientHelper behind a ServiceLoader lookup

The last file, and the only one that needs a mechanism.

`ClientHelper` implements the api `IClientHelper` and every one of its five methods returns an
arboriculture type - `FixedLeafTint`, `BiomeLeafTint`, `LeafSprite`. It is arboriculture's factory
wearing a generic name.

It cannot simply move, because `ForestryClientApiImpl:17` holds it as a **field initialiser**:

```java
	private final IClientHelper helper = new ClientHelper();
```

Moving `ClientHelper` to arboriculture makes `ForestryClientApiImpl` - currently clean - leak. And it
cannot become a settable field like the managers, because `ForestryLeafSprites` (in **api**) resolves
the helper in a static initialiser and immediately builds twenty-odd sprites:

```java
	private static final IClientHelper HELPER = IForestryClientApi.INSTANCE.getHelper();
	public static final ILeafSprite OAK = HELPER.createLeafSprite(...);
```

So the helper must be available the first time that class is touched, which no module-installed
setter can guarantee.

`ServiceLoader` can: it is already how `IForestryApi` and `IForestryClientApi` themselves resolve
(`IForestryApi:21`, `IForestryClientApi:15`), it resolves on demand rather than on a lifecycle hook,
and it is the mechanism the jar split needs anyway.

**Files:**
- Move: `apiimpl/client/plugin/ClientHelper.java` -> `src/main/java/forestry/arboriculture/client/plugin/ClientHelper.java`
- Modify: `src/main/java/forestry/apiimpl/client/ForestryClientApiImpl.java:17`
- Create: `src/main/resources/META-INF/services/forestry.api.client.plugin.IClientHelper`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: `IClientHelper` from api, unchanged.
- Produces: `ForestryClientApiImpl.getHelper()` returning a `ServiceLoader`-resolved `IClientHelper`.
  No api signature changes.

- [ ] **Step 1: Move the class and declare the service**

```bash
mkdir -p src/main/java/forestry/arboriculture/client/plugin
git mv src/main/java/forestry/apiimpl/client/plugin/ClientHelper.java src/main/java/forestry/arboriculture/client/plugin/ClientHelper.java
sed -i '1s@^package forestry\.apiimpl\.client\.plugin;@package forestry.arboriculture.client.plugin;@' src/main/java/forestry/arboriculture/client/plugin/ClientHelper.java
```

`BiomeLeafTint` and `FixedLeafTint` are in `forestry.arboriculture.client`, a sibling of the new
package, so those two imports stay. `LeafSprite` is in `forestry.arboriculture.models` - also stays.

Create `src/main/resources/META-INF/services/forestry.api.client.plugin.IClientHelper` containing one
line:

```
forestry.arboriculture.client.plugin.ClientHelper
```

- [ ] **Step 2: Resolve it in ForestryClientApiImpl**

```java
	private final IClientHelper helper = ServiceLoader.load(IClientHelper.class).findFirst().orElseThrow();
```

Delete `import forestry.apiimpl.client.plugin.ClientHelper;` and add `java.util.ServiceLoader`.

`orElseThrow` rather than a no-op fallback is deliberate for now: with one jar the service is always
present, so the throw is unreachable and a silent no-op would hide a broken service file. **Phase 6
replaces this with the `isLoaded()` no-op** when a missing arboriculture jar becomes possible - leave
a `todo` saying so.

- [ ] **Step 3: Verify the helper resolves before ForestryLeafSprites needs it**

This is the risk in this task, and it is client-side, so no GameTest covers it. Check it directly:

```bash
grep -rn "ForestryLeafSprites" src/main/java --include='*.java' | grep -v "api/client/arboriculture/ForestryLeafSprites.java"
```

Every use is inside a client registration that runs well after service loading. Confirm by adding a
temporary `Forestry.LOGGER.info` to `ClientHelper`'s constructor and to
`ForestryLeafSprites`' static block, running the client once if practical, and checking the helper
line comes first. **If running a client is not practical, say so in the commit message rather than
claiming the ordering was verified.**

- [ ] **Step 4: Compile, trim, verify**

```bash
./gradlew compileJava compileTestJava
```

Remove `apiimpl/client/plugin/ClientHelper.java` from the baseline, then:

```bash
./gradlew clean build
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `20 known leaking file(s) remaining`, no datagen diff, all 100 tests passed.

Datagen matters here despite being server-side: `ForestryLeafSprites` is referenced from
`ArboricultureClientRegistration`, and if the service file were malformed the failure would surface as
a `NoSuchElementException` at class-init rather than a quiet wrong value.

- [ ] **Step 5: Commit**

```bash
git add -A src/main/java/forestry src/main/resources/META-INF/services gradle/base-boundary-baseline.txt
git commit -m "arboriculture: own the client helper, resolved by ServiceLoader

Every method on ClientHelper returns an arboriculture type - it is
arboriculture's factory behind a generic api name. It could not simply move:
ForestryClientApiImpl holds it in a field initialiser and would have started
leaking, and it cannot be a settable field because ForestryLeafSprites resolves
the helper in a static initialiser and builds twenty sprites immediately.

ServiceLoader resolves on demand rather than on a lifecycle hook, and api
already loads IForestryApi and IForestryClientApi the same way.

orElseThrow for now: with one jar the service is always present. Phase 6
replaces it with the isLoaded() no-op.

checkBaseBoundary: 21 -> 20 files."
```

---

### Task 9: Record phase 5a completion

**Files:**
- Modify: `docs/superpowers/specs/2026-07-30-feature-package-reorg-design.md`

- [ ] **Step 1: Confirm from a clean build**

```bash
./gradlew clean build
```

Expected: `BUILD SUCCESSFUL`, `checkApiBoundary: forestry.api is clean`,
`checkBaseBoundary: 20 known leaking file(s) remaining`.

- [ ] **Step 2: Update the spec**

Mark phase 5a `DONE` and record: that bucket I was mostly constructor chains rather than the
factory inversion the spec predicted, so eight of the twelve files moved as registration/builder
pairs; that one file was the same concrete-setter defect phase 4 found in `setBeeManager`; that
`SpeciesTypeBuilder`'s bee comb had to be resolved by id rather than deleted because research
materials have no oracle; and that `ClientHelper` needed `ServiceLoader` because `ForestryLeafSprites`
resolves the helper in a static initialiser.

Also record the remaining shape: 20 files, of which 20 are bucket A.

- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/specs/2026-07-30-feature-package-reorg-design.md
git commit -m "docs: record phase 5a completion, base boundary at 20 of 68"
```

---

## Notes for phase 6

- After this phase the baseline should be **exactly bucket A** - the 20 datagen providers, led by
  `ForestryRecipeProvider` at 26 imports. They dissolve in phase 8 when datagen splits into per-jar
  source sets, not by being fixed in place, so the count sits at 20 until then. That is the plan
  working, not a stall.
- Phase 6 is the D7 work: no-op managers carrying `isLoaded()`. Two hooks from this phase land there -
  `ClientHelper`'s `orElseThrow`, and whatever Task 6 could not invert.
- The phase-6 gate is "the base artifact references no split-jar types". `checkBaseBoundary` reaching
  an empty baseline **is** that gate, but only for imports. The spec's D3 safety condition is
  stricter: reflection-reached signatures naming absent classes crash at boot, and no import-based
  gate sees those. Phase 6 should decide whether the bytecode-level check the spec discusses is worth
  building before phase 9 wires the real jar split.
