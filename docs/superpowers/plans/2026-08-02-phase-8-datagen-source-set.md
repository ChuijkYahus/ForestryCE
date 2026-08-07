# Phase 8: datagen becomes a source set

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move `forestry.core.data` out of `src/main/java` into its own `datagen` source set, delete the
`exclude 'forestry/core/data/**'` jar hack and the `base-boundary-baseline.txt` ratchet along with it,
and leave `checkBaseBoundary` as a hard gate with nothing grandfathered.

**Architecture:** The exclude hack exists because datagen providers are the only code in the repo that
touches ModKit, a dev-only dependency, and FML's boot-time annotation scanner resolves method
descriptors from the shipped jar. Separating the source set removes the *cause*: `main` stops compiling
against ModKit entirely, and the jar is built from `main`'s output, so datagen cannot reach it by
construction rather than by a filename filter.

**Tech Stack:** Java 21, NeoForge 21.1.230, Minecraft 1.21.1, ModDevGradle 2.0.140, Gradle 9.2.1,
Groovy DSL. GameTests only, no JUnit.

## Global Constraints

- Comment and Javadoc style is binding; see `CLAUDE.md`. ASCII only. Lowercase `todo`.
- `./gradlew runData` must produce **no diff** in `src/generated/resources`. This is the load-bearing
  oracle for the whole phase: it proves all 55 providers still run, in the same order, with the same
  inputs.
- `./gradlew runGameTestServer` must report **all 108 tests passed**.
- `checkApiBoundary`, `checkBaseBoundary`, `checkBaseBytecode` and `checkResourceFqcn` all stay green.
- All source files are LF. Do not write `$`-anchored `sed` patterns.

## Scope: one source set, not six

The spec's Build structure section names six datagen source sets (`coreData`, `apicultureData`, ...).
**Phase 8 creates one.** The reasoning, and it should be recorded rather than rediscovered:

- Phase 8's stated deliverables are "deletes the exclude hack" and, from the Sequencing table's gate
  note, "the 20 remaining files are datagen ... they dissolve in phase 8". One source set achieves both
  completely.
- Six source sets require the *providers* to be partitioned by owning jar, and they are not
  partitionable today: `ForestryRecipeProvider` alone emits recipes for every jar, and
  `ForestryBlockLootTables` and `ForestryDataMapProvider` are the same shape. Splitting them is the
  resource-partition work the spec assigns to **phase 9** ("the resource partition makes each recipe's
  owning jar explicit").
- `main` is still one source set until phase 9. Six datagen source sets over one main source set would
  be five empty directories and a rename, then a full redo once main splits.

So the per-jar datagen split moves to phase 9, where it belongs next to the main source-set split. This
plan updates the spec to say so.

## Starting state, measured 2026-08-02

| Fact | Value |
| --- | --- |
| Files in `src/main/java/forestry/core/data` | 55 |
| Files outside `core/data` that reference it, in `src/main/java` | **0** |
| Files outside `core/data` that reference it, in `src/test/java` | 2 (`TreeSpeciesFallbackTest`, `ButterflySpeciesFallbackTest`, each calling a provider's `buildDefinitions()`) |
| Packages using ModKit | `forestry/core/data` (11 files) and `forestry/core/data/recipe` (1). **Nothing else** |
| `base-boundary-baseline.txt` entries | 20, every one under `core/data/` |
| External deps of `core/data` beyond Minecraft/NeoForge | ModKit, Curios, and guava/gson/fastutil (transitive from Minecraft) |

The zero in row two is what makes this phase a pure build change: no Java in `main` has to move or
change.

## Prototyped first

The spec says of this work: "This is the least-supported part of the build story and should be
prototyped early." That advice was followed - the build wiring below was proven working before this
plan was written, because a plan for an unproven Gradle change is fiction. Three things failed on the
way and the working configuration is the one that survived them:

1. **`datagenImplementation 'ModKit'` alone is not enough.** Every dev run loads `sourceSets.datagen`
   as part of the `forestry` mod, and FML's `AutomaticEventSubscriber` calls `getDeclaredMethods0` on
   `Data`, so ModKit must be on the *run's* classpath too. Without it `runData` dies with exactly the
   `NoClassDefFoundError: thedarkcolour/modkit/data/MKTagsProvider` the jar exclude was documented to
   prevent - the same crash, relocated from production into the data run.
2. **`additionalRuntimeClasspath` does not exist** on MDG 2.0.140's `RunModel`. The property is
   `additionalRuntimeClasspathConfiguration`, a `Configuration`.
3. **`additionalRuntimeClasspathConfiguration` is the wrong layer anyway.** With ModKit added there,
   the run got past the first error and died on
   `NoClassDefFoundError: net/minecraft/data/tags/TagsProvider` - MDG loads that configuration outside
   the game layer, and `MKTagsProvider` extends a Minecraft class. ModKit has to ride in on `main`'s
   `runtimeClasspath`, which `runtimeOnly` does while still keeping it off `main`'s *compile*
   classpath. That is the whole objective, and the published POM strips dependency nodes
   (`build.gradle:659`), so `runtimeOnly` has no publication consequence.

MDG 2.0.140 offers no way to scope a mod's source set to a single run - `ModModel.sourceSet` is
additive and global, and `RunModel.getSourceSet()` is a different concept. So datagen loads in the
client, server and gameTest runs as well. That is precisely the status quo (today `core/data` is in
`main` and ModKit is an `implementation` dependency), so nothing regresses.

---

### Task 1: The datagen source set

**Files:**
- Move: `src/main/java/forestry/core/data/**` (55 files) -> `src/datagen/java/forestry/core/data/**`
- Modify: `build.gradle` - source set, configurations, dependencies, mods block, jar block

**Interfaces:**
- Produces: source set `datagen` at `src/datagen/java`, compiled by `compileDatagenJava`, whose output
  is on `sourceSets.test`'s compile classpath and in the `forestry` mod for every dev run.
- Produces: configuration `modkit`, extended by `runtimeOnly` and `datagenCompileOnly`.
- Removes: `exclude 'forestry/core/data/**'` from `jar`, and `implementation 'com.github.thedarkcolour:ModKit'`.

- [ ] **Step 1: Move the package**

```bash
mkdir -p src/datagen/java/forestry/core
git mv src/main/java/forestry/core/data src/datagen/java/forestry/core/data
find src/datagen -name '*.java' | wc -l
```

Expected: `55`. No import rewriting is needed - the package name `forestry.core.data` is unchanged;
only the source root moves.

- [ ] **Step 2: Declare the source set and the ModKit configuration**

Insert after the `java.toolchain` lines near the top of `build.gradle`:

```groovy
// Datagen is its own source set, not a package inside main. The providers are the only thing in the
// repo that touches ModKit, which is a dev-only dependency: with them in main, every datagen lambda
// put a ModKit type into a method descriptor that FML's boot-time annotation scanner then tried to
// resolve from the shipped jar. That is what the 'forestry/core/data/**' jar exclude was working
// around. Separating the source set removes the cause rather than the symptom - main no longer
// compiles against ModKit at all, and the jar is built from main's output, so datagen cannot reach
// it by construction.
sourceSets {
	datagen {
		compileClasspath += sourceSets.main.output
		runtimeClasspath += sourceSets.main.output
	}
}

configurations {
	// ModKit, isolated so it can reach exactly two places and no others: datagen's compile classpath,
	// and the dev runs' runtime classpath. It must go on main's *runtime* classpath rather than an
	// MDG additionalRuntimeClasspath, because the latter loads outside the game layer and ModKit
	// extends net.minecraft.data.tags.TagsProvider. runtimeOnly keeps it off main's compile
	// classpath, which is the whole point, and the published POM strips dependencies anyway
	modkit
	runtimeOnly.extendsFrom modkit
	datagenCompileOnly.extendsFrom modkit
	// Curios, KubeJS and the rest of the optional-mod surface
	datagenImplementation.extendsFrom implementation
	datagenCompileOnly.extendsFrom compileOnly
}
```

- [ ] **Step 3: Register the source set with the mod and give it Minecraft**

In the `neoForge { mods { forestry { ... } } }` block, add a third source set, and a second
`addModdingDependenciesTo` call:

```groovy
	mods {
		forestry {
			sourceSet sourceSets.main
			// GameTest suite lives in src/test (forestry.gametest). It is part of the forestry mod so the
			// gameTestServer run loads it, but stays out of the published jar (jar/apiJar use main only).
			sourceSet sourceSets.test
			// Datagen lives in src/datagen (forestry.core.data). Part of the mod so the data run's
			// GatherDataEvent subscriber is discovered, and out of the jar for the same reason as test.
			sourceSet sourceSets.datagen
		}
	}

	// src/test and src/datagen have no Minecraft/NeoForge on their classpath by default — moddev only wires
	// that into source sets it's told about. Without this those sources won't compile.
	addModdingDependenciesTo sourceSets.test
	addModdingDependenciesTo sourceSets.datagen
```

- [ ] **Step 4: Note why ModKit is on every run, in `runs { configureEach }`**

```groovy
		configureEach {
			logLevel = org.slf4j.event.Level.DEBUG
			jvmArgument '-XX:+AllowEnhancedClassRedefinition'
			jvmArgument '-XX:+IgnoreUnrecognizedVMOptions'
			// Every dev run loads sourceSets.datagen as part of the forestry mod, and FML's
			// AutomaticEventSubscriber resolves Data's method signatures at construction, so ModKit
			// has to be on the runtime classpath of all of them - not just runData. See the modkit
			// configuration above; it rides in on main's runtimeClasspath
		}
```

- [ ] **Step 5: Rewire the ModKit dependency and wire the two GameTests**

In `dependencies`, replace the ModKit line:

```groovy
	// ModKit DEV ONLY - datagen and nothing else. Keeping it off main's classpath is the point of
	// the datagen source set; see the sourceSets block above
	modkit 'com.github.thedarkcolour:ModKit:51b8760'

	// two GameTests build their expected species maps from the datagen providers
	testImplementation sourceSets.datagen.output
```

- [ ] **Step 6: Delete the jar exclude**

Remove these nine lines from the `jar { }` block, leaving `from ...` followed directly by `manifest {`:

```groovy
	// Datagen providers reference ModKit types (com.github.thedarkcolour:ModKit) that
	// aren't shipped to end users. FML's annotation scanner resolves method signatures
	// at boot via getDeclaredMethods0; the lambda method-reference synthetics in
	// forestry.core.data.Data carry MKTagsProvider parameters, which crashes the JVM
	// link step in production with NoClassDefFoundError. Strip the datagen package
	// from the production jar — runData uses build/classes directly, not the jar, so
	// datagen still works in dev. Nothing outside this package references it.
	exclude 'forestry/core/data/**'
```

- [ ] **Step 7: Compile all three source sets**

```bash
./gradlew compileJava compileDatagenJava compileTestJava --console=plain
```

Expected: `BUILD SUCCESSFUL`. If `compileJava` fails on a missing ModKit symbol, something in `main`
does use ModKit after all and the measurement in Starting state is wrong - stop and re-measure rather
than putting ModKit back on `implementation`.

- [ ] **Step 8: Prove ModKit is gone from main's compile classpath**

This is the phase's real invariant, so assert it rather than inferring it from a green build:

```bash
./gradlew dependencies --configuration compileClasspath --console=plain -q | grep -ci modkit
./gradlew dependencies --configuration datagenCompileClasspath --console=plain -q | grep -ci modkit
./gradlew dependencies --configuration runtimeClasspath --console=plain -q | grep -ci modkit
```

Expected: `0`, then a non-zero, then a non-zero.

- [ ] **Step 9: Run the datagen oracle**

```bash
./gradlew runData --console=plain 2>&1 | tail -3
git diff --stat src/generated/resources
```

Expected: `BUILD SUCCESSFUL`, and an empty diff. An empty diff here proves all 55 providers were
discovered and ran identically from the new source root.

- [ ] **Step 10: Prove the jar no longer carries datagen, structurally**

```bash
./gradlew jar --console=plain
J=build/libs/forestry-1.21.1-3.0.0-alpha1.jar
echo "core/data entries: $(unzip -l $J | grep -c 'forestry/core/data')"
unzip -l $J | tail -2
```

Expected: `core/data entries: 0`, and roughly 14,600 files total. Zero is now a property of what
`main` contains, not of a filename filter.

- [ ] **Step 11: GameTests**

```bash
./gradlew runGameTestServer --console=plain 2>&1 | grep -E 'required tests|BUILD|FAILED' | tail -5
```

Expected: `All 108 required tests passed`. This is also the check that the two fallback tests can still
reach `TreeSpeciesProvider` and `ButterflySpeciesProvider` at runtime, not just at compile time.

- [ ] **Step 12: Commit**

```bash
git add -A
git commit -m "build: datagen becomes its own source set"
```

---

### Task 2: Retire the baseline and the gates' datagen carve-outs

With `core/data` out of `src/main/java`, `checkBaseBoundary` no longer scans any of the 20 baselined
files, so its stale-baseline arm fails the build - by design, that is the ratchet reporting that it has
reached zero. The right response is to delete the baseline mechanism outright and leave a hard gate:
**no base file may reference a split module, ever, with nothing grandfathered.**

Three carve-outs go with it: the `core/data/` skip in `checkBaseBytecode`, that task's now-inaccurate
"skips the datagen package" comment, and `checkResourceFqcn`'s single-source-root assumption.

**Files:**
- Delete: `gradle/base-boundary-baseline.txt`
- Modify: `build.gradle` - `checkBaseBoundary`, `checkBaseBytecode`, `checkResourceFqcn`

- [ ] **Step 1: Confirm the ratchet reports zero before removing it**

```bash
./gradlew checkBaseBoundary --console=plain 2>&1 | head -26
```

Expected: `BUILD FAILED` with "gradle/base-boundary-baseline.txt is stale" listing all 20 entries and
nothing else. If any entry is missing from that list, a base file still leaks and the baseline is not
yet dischargeable - stop.

- [ ] **Step 2: Replace `checkBaseBoundary` with a hard gate**

Replace the whole task, and its preceding comment block, with:

```groovy
// The base artifact - everything that ships in the forestry jar - must not reference the five split
// content modules. This began as a ratchet against a checked-in baseline of 68 leaking files; phases
// 1 through 6 drove that to 20, all of them datagen, and phase 8 moved datagen out of src/main/java
// entirely. There is nothing left to grandfather, so this is now a hard gate.
var checkBaseBoundary = tasks.register('checkBaseBoundary') {
	group = 'verification'
	description = 'Fails if a base-artifact package references a split content module'

	var javaDir = project.file('src/main/java/forestry')
	var basePackages = ['core', 'apiimpl', 'modules']
	var splitModules = ['apiculture', 'arboriculture', 'lepidopterology', 'agriculture', 'mail']

	inputs.dir(javaDir)
	outputs.upToDateWhen { false }

	doLast {
		var pattern = ~/^import (static )?forestry\.(${splitModules.join('|')})\..*/
		var offenders = new TreeSet<String>()

		var scan = { File file ->
			if (file.name.endsWith('.java') && file.readLines('UTF-8').any { it ==~ pattern }) {
				offenders.add(javaDir.toPath().relativize(file.toPath()).toString())
			}
		}
		// loose files directly under forestry/ are base too, ex. Forestry.java
		javaDir.listFiles({ File f -> f.isFile() } as FileFilter).each(scan)
		basePackages.each { pkg ->
			var dir = new File(javaDir, pkg)
			if (dir.exists()) {
				dir.eachFileRecurse(groovy.io.FileType.FILES, scan)
			}
		}

		if (!offenders.isEmpty()) {
			throw new GradleException(
					"Base-artifact reference(s) to a split content module:\n  "
					+ offenders.join('\n  ')
					+ "\nBase cannot name a type from an optional jar. Move the reference, or invert it "
					+ "through an api interface.")
		}
		logger.lifecycle("checkBaseBoundary: no base file references a split content module")
	}
}
```

- [ ] **Step 3: Delete the baseline file**

```bash
git rm gradle/base-boundary-baseline.txt
ls gradle/
```

- [ ] **Step 4: Drop the datagen carve-out from `checkBaseBytecode`**

In that task, delete the `rel.startsWith('core/data/')` early return and its comment, and correct the
task's leading comment, which currently ends "and skips the datagen package because the jar task
strips it". Replace that clause with "Datagen is a separate source set as of phase 8, so there is
nothing to skip."

- [ ] **Step 5: Point `checkResourceFqcn` at both source roots**

The task resolves a class name by testing for `<javaDir>/<path>.java` against a single root. Datagen
classes are not named by any resource today, but the check must not silently start reporting a valid
name as missing if one ever is. Change the single `javaDir` to a list and test both:

```groovy
	var javaDirs = [project.file('src/main/java'), project.file('src/datagen/java')]
```

with `inputs.files(javaDirs)` and the existence test becoming:

```groovy
			var relative = name.replace('.', '/') + '.java'
			if (javaDirs.every { !new File(it, relative).exists() }) {
				missing.add("${name} (${where})")
			}
```

- [ ] **Step 6: Run all four gates**

```bash
./gradlew checkApiBoundary checkBaseBoundary checkBaseBytecode checkResourceFqcn --console=plain
```

Expected:

```
checkApiBoundary: forestry.api is clean
checkBaseBoundary: no base file references a split content module
checkBaseBytecode: no packaged base class references a split module
checkResourceFqcn: 17 resource-borne class name(s) all resolve
```

- [ ] **Step 7: Prove the hard gate still bites**

The ratchet's failure mode is gone, so re-verify the remaining one:

```bash
sed -i '0,/^import /s//import forestry.apiculture.bees.BeeSpecies;\nimport /' src/main/java/forestry/core/ModuleCore.java
./gradlew checkBaseBoundary --console=plain 2>&1 | grep -E 'Base-artifact|ModuleCore|BUILD'
git checkout -- src/main/java/forestry/core/ModuleCore.java
./gradlew checkBaseBoundary --console=plain 2>&1 | grep -E 'checkBaseBoundary|BUILD'
```

Expected: `BUILD FAILED` naming `core/ModuleCore.java`, then green again.

- [ ] **Step 8: Full build and both oracles**

```bash
./gradlew build --console=plain 2>&1 | grep -E 'check[ABR]|BUILD|error:'
./gradlew runData --console=plain 2>&1 | tail -2; git diff --stat src/generated/resources
./gradlew runGameTestServer --console=plain 2>&1 | grep -E 'required tests|BUILD' | tail -3
```

Expected: all four gates green, `BUILD SUCCESSFUL`, empty datagen diff, 108 tests passed.

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "build: the base boundary is a hard gate, not a ratchet"
```

---

### Task 3: Record the phase

**Files:**
- Modify: `docs/superpowers/specs/2026-07-30-feature-package-reorg-design.md`

- [ ] **Step 1: Update the Sequencing table**

Mark row `8` DONE with its date, and amend row `9` to say it also carries the per-jar datagen split
that this phase deliberately deferred.

- [ ] **Step 2: Add a phase-8 paragraph to Sequencing**

Record, with the reasoning rather than only the outcome:

- One source set, not six, and why the per-jar split belongs with phase 9's provider partition.
- ModKit is off `main`'s compile classpath entirely; that, not the jar filter, is what makes the
  FML annotation-scanner crash impossible.
- The three prototype failures - `datagenImplementation` alone is insufficient because every dev run
  loads datagen; `additionalRuntimeClasspath` does not exist on MDG 2.0.140; and
  `additionalRuntimeClasspathConfiguration` loads outside the game layer, so `runtimeOnly` is the only
  placement that works.
- MDG 2.0.140 cannot scope a mod source set to one run, so datagen loads in every dev run. This is the
  status quo, not a regression.
- `base-boundary-baseline.txt` is deleted and `checkBaseBoundary` is now a hard gate. Record that the
  ratchet went 68 -> 20 -> 0 across phases 2 through 8.
- The sources jar no longer contains datagen, since it is built from `sourceSets.main.allJava`. That is
  a deliberate consequence: datagen is dev-only tooling.

- [ ] **Step 3: Amend the Build structure section**

Its `src/core/generated`, `src/apiculture/generated` sketch and its "**Datagen.**" paragraph both
describe the six-way end state. Add what phase 8 actually built and note that the "six data run
configurations with chained `--existing`, or a post-hoc split" question is **still open** and now
belongs to phase 9. Do not delete the paragraph - the open question is the useful part.

- [ ] **Step 4: Amend the phase-6 gate note**

The Sequencing table's gate block says the 20 remaining files "dissolve in phase 8". Mark that
discharged.

- [ ] **Step 5: Update the Status line**

`Status: phases 1-8 complete (2026-08-02); phase 9 next`

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "docs: record phase 8 completion, the exclude hack is gone"
```

---

## What phase 8 does not do

- **No per-jar datagen source sets.** Deferred to phase 9 with the provider partition. See Scope above.
- **No provider partitioning.** `ForestryRecipeProvider`, `ForestryBlockLootTables` and
  `ForestryDataMapProvider` still emit content for every jar from one class.
- **No resource partitioning.** The 1,959 hand-authored files under `src/main/resources` are untouched.
- **No mods.toml or service-file split.** Phase 9.
- **The cross-jar loot modifier is still cross-jar.** `data/forestry/loot_modifiers/chests/abandoned_mineshaft.json`
  spans `["apiculture", "factory", "storage"]` and needs redesign, not partition. Phase 9.
- **`src/generated/resources` stays a `main` resources source directory.** Generated resources ship in
  the jar; only the *providers* moved.
