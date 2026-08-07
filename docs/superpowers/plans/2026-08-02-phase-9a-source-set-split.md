# Phase 9a: the compiler takes over the boundary

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split `src/main/java` into six source sets with explicit compile-classpath edges matching D1,
so that the jar dependency graph becomes a compile error instead of a Gradle task's opinion. Still one
jar and one mod id - splitting the artifacts is 9b.

**Architecture:** `sourceSets.main` *is* the core jar; the five content modules move to
`src/<name>/java`. Each content source set gets `main.output` on its compile classpath and nothing
else, except `lepidopterology`, which also gets `arboriculture.output`. That single wiring decision
replaces `checkBaseBoundary` and `checkBaseBytecode`, both of which were standing in for a compiler
that could not see the boundary yet.

**Tech Stack:** Java 21, NeoForge 21.1.230, Minecraft 1.21.1, ModDevGradle 2.0.140, Gradle 9.2.1,
Groovy DSL. GameTests only, no JUnit.

## Global Constraints

- Comment and Javadoc style is binding; see `CLAUDE.md`. ASCII only. Lowercase `todo`.
- `./gradlew runGameTestServer` must report **all 108 tests passed** at the end of every task.
- `./gradlew runData` must produce **no diff** in `src/generated/resources` - with exactly one
  enumerated exception, in Task 1, which adds one new tag file. Every other task is byte-identical.
- `checkApiBoundary` and `checkResourceFqcn` stay green throughout.
- All source files are LF. Do not write `$`-anchored `sed` patterns.
- **Do not rename `main` to `core`.** The spec's Build structure sketch shows `src/core/java`, but
  Gradle's `jar`, `processResources`, `components.java` and MDG's `accessTransformers` all attach to
  `main` by convention, and core is precisely the artifact those conventions should describe. `main`
  is core. Record it; do not fight it.

## Scope: one jar, six source sets

9a delivers the compiler-enforced graph and nothing else. Deferred to 9b, in full:

- Six `neoforge.mods.toml` files and six mod ids.
- Splitting `META-INF/services/forestry.api.plugin.IForestryPlugin` per jar.
- Partitioning 1,960 hand-authored and 10,232 generated resources.
- Per-jar datagen and the still-unprototyped six-`--output` question.
- The parameterized lang merge and the cross-jar loot modifier redesign.
- Publishing six artifacts.

The value of stopping here is that every one of those is a *runtime* or *packaging* change with weak
oracles, whereas 9a is a compile-time change with the strongest oracle in the project: it either
compiles or it does not.

## Starting state, measured 2026-08-02

Cross-package imports out of each content module, by target root:

| From | api | core | own | apiimpl | modules | Forestry | **illegal** |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `apiculture` | 607 | 295 | 297 | 8 | 1 | 5 | 0 |
| `arboriculture` | 498 | 171 | 286 | 6 | 1 | 5 | 0 |
| `lepidopterology` | 193 | 92 | 59 | 3 | 1 | 5 | 0 (+3 to `arboriculture`, allowed) |
| `agriculture` | 216 | 186 | 115 | 3 | 2 | 0 | **1** |
| `mail` | 99 | 131 | 170 | 0 | 1 | 1 | 0 |

**One illegal edge in 1,220 cross-package imports**, and it is the one the spec's Graph decisions table
already resolved: `agriculture/farmlogic/farmables/FarmableGE.java:14` imports
`arboriculture.features.ArboricultureBlocks`. Everything else already obeys D1.

File counts after the move: `apiculture` 204, `arboriculture` 201, `agriculture` 117, `mail` 77,
`lepidopterology` 62; `main` keeps `api`, `apiimpl`, `core`, `modules`, `Forestry.java`.

## Prototyped first

Same discipline as phase 8. The move and the source-set wiring were run before this plan was written,
and the compiler's verdict is the reason the plan is only four tasks: with all six source sets wired
and the classpath edges in place, **the only compile errors in the entire repository were the two lines
of `FarmableGE`.** No hidden cycle, no split package, no surprise from `apiimpl` or `modules`.

That is worth stating plainly because it is the payoff for phases 1 through 8: the graph was already
correct, and 9a only asks the build system to start believing it.

---

### Task 1: Sever the last illegal edge

`FarmableGE.isSaplingAt` asks `ArboricultureBlocks.SAPLING_GE.blockEqual(state)`. The spec's decision:
"Sever. One call site; resolve the sapling by block tag or registry id instead. Without arboriculture
the Arboretum still farms vanilla saplings and simply never matches Forestry ones - degrades rather
than breaks."

Use a block tag, the spec's first option and the better design: an addon that adds a genetic tree wants
its sapling farmed too, and an undefined tag resolves empty, which is exactly the required degradation.

**Do not use `BlockTags.SAPLINGS`.** `ForestryBlockTagsProvider:154` already puts `SAPLING_GE` in it,
which makes it tempting, but that tag also holds every vanilla sapling, and `FarmableGE` is the
*genetic* farmable. Widening it would silently change what the Arboretum harvests.

**Files:**
- Modify: `src/main/java/forestry/api/ForestryTags.java`
- Modify: `src/datagen/java/forestry/core/data/ForestryBlockTagsProvider.java`
- Modify: `src/main/java/forestry/agriculture/farmlogic/farmables/FarmableGE.java` (pre-move path)

**Interfaces:**
- Produces: `ForestryTags.Blocks.TREE_SAPLINGS`, a `TagKey<Block>` for `forestry:tree_saplings`.

**This is the one task in phase 9a with a non-empty datagen diff:** exactly one added file,
`src/generated/resources/data/forestry/tags/block/tree_saplings.json`, containing
`forestry:sapling_ge`. Anything else in the diff is a defect.

- [ ] **Step 1: Declare the tag**

In `ForestryTags.Blocks`, next to `VALID_FARM_BASE`:

```java
		// Saplings that grow into a genetic tree. Read by the Arboretum's farm logic, which lives in
		// the agriculture jar and cannot name an arboriculture block. Undefined without the
		// arboriculture jar, which vanilla resolves as empty, so the Arboretum then farms only
		// vanilla saplings
		public static final TagKey<Block> TREE_SAPLINGS = blockTag("tree_saplings");
```

- [ ] **Step 2: Populate it from datagen**

In `ForestryBlockTagsProvider`, beside the existing `BlockTags.SAPLINGS` line at 154:

```java
		tags.tag(ForestryTags.Blocks.TREE_SAPLINGS).add(ArboricultureBlocks.SAPLING_GE.block());
```

- [ ] **Step 3: Read the tag instead of the block**

In `FarmableGE`, delete `import forestry.arboriculture.features.ArboricultureBlocks;`, add
`import forestry.api.ForestryTags;`, and replace the body of `isSaplingAt`:

```java
	@Override
	public boolean isSaplingAt(Level level, BlockPos pos, BlockState state) {
		return state.is(ForestryTags.Blocks.TREE_SAPLINGS);
	}
```

- [ ] **Step 4: Prove the edge is gone**

```bash
grep -rn '^import \(static \)\?forestry\.arboriculture' src/main/java/forestry/agriculture \
  || echo "agriculture no longer imports arboriculture"
```

- [ ] **Step 5: Compile and regenerate**

```bash
./gradlew compileJava compileDatagenJava compileTestJava --console=plain
./gradlew runData --console=plain 2>&1 | tail -2
git status --porcelain src/generated/resources
```

Expected: `BUILD SUCCESSFUL`, and exactly one line:
`?? src/generated/resources/data/forestry/tags/block/tree_saplings.json`. Read the file and confirm
it lists `forestry:sapling_ge` and nothing else.

- [ ] **Step 6: GameTests**

```bash
./gradlew runGameTestServer --console=plain 2>&1 | grep -E 'required tests|BUILD' | tail -3
```

Expected: `All 108 required tests passed`.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "agriculture: farm genetic saplings by tag, not by block"
```

---

### Task 2: Six source sets

**Files:**
- Move: `src/main/java/forestry/{apiculture,arboriculture,lepidopterology,agriculture,mail}` ->
  `src/<name>/java/forestry/<name>`
- Modify: `build.gradle` - source sets, classpath edges, mods block, dependencies, jar

**Interfaces:**
- Produces: source sets `apiculture`, `arboriculture`, `lepidopterology`, `agriculture`, `mail`, each
  compiled by `compile<Name>Java`, each registered with the `forestry` mod, each contributing to `jar`.
- Produces: the Groovy list `contentModules`, used by every subsequent loop in the build.

- [ ] **Step 1: Move the five packages**

```bash
for m in apiculture arboriculture lepidopterology agriculture mail; do
  mkdir -p src/$m/java/forestry
  git mv src/main/java/forestry/$m src/$m/java/forestry/$m
done
ls src/main/java/forestry
```

Expected: `api apiimpl core Forestry.java modules package-info.java`. No import rewriting: package
names are unchanged, only the source roots move.

- [ ] **Step 2: Declare the source sets and the classpath edges**

Replace the `sourceSets { datagen { ... } }` block added in phase 8 with:

```groovy
// The six jars of D1, as six source sets. sourceSets.main IS the core jar: Gradle's jar task,
// processResources, components.java and the access transformers all attach to main by convention,
// and core is the artifact those conventions should describe. The other five sit at src/<name>/java.
//
// This is the point at which the dependency graph stops being a convention checked by a gradle task
// and becomes a compile error. checkBaseBoundary and checkBaseBytecode were standing in for the
// compiler since phase 2; from here the compiler does it, and it sees descriptors, generics and
// inline fully-qualified references that an import scan never could.
//
// 9a still produces one jar and one mod id. Splitting the artifacts is 9b.
var contentModules = ['apiculture', 'arboriculture', 'lepidopterology', 'agriculture', 'mail']

sourceSets {
	apiculture
	arboriculture
	lepidopterology
	agriculture
	mail
	datagen
}

// Everything requires core. lepidopterology also requires arboriculture - the only cross-content
// edge D1 allows, and the only one the code has: three TreeUtil imports for butterfly pollination
sourceSets.apiculture.compileClasspath += sourceSets.main.output
sourceSets.arboriculture.compileClasspath += sourceSets.main.output
sourceSets.agriculture.compileClasspath += sourceSets.main.output
sourceSets.mail.compileClasspath += sourceSets.main.output
sourceSets.lepidopterology.compileClasspath += sourceSets.main.output + sourceSets.arboriculture.output

contentModules.each { m ->
	sourceSets[m].runtimeClasspath += sourceSets.main.output
	configurations["${m}Implementation"].extendsFrom configurations.implementation
	configurations["${m}CompileOnly"].extendsFrom configurations.compileOnly
}
sourceSets.lepidopterology.runtimeClasspath += sourceSets.arboriculture.output

// Datagen and the GameTests both read every jar, so they get all six
sourceSets.datagen.compileClasspath += sourceSets.main.output
sourceSets.datagen.runtimeClasspath += sourceSets.main.output
contentModules.each { m ->
	sourceSets.datagen.compileClasspath += sourceSets[m].output
	sourceSets.datagen.runtimeClasspath += sourceSets[m].output
}
```

- [ ] **Step 3: Register them with the mod**

In the `mods { forestry { ... } }` block, after the datagen line:

```groovy
			// The five content source sets. Still one mod id in 9a: module discovery is annotation-scan
			// based over the mod's paths, so all six are scanned exactly as one source set was.
			contentModules.each { sourceSet sourceSets[it] }
```

and extend the `addModdingDependenciesTo` calls:

```groovy
	// src/test, src/datagen and the five content roots have no Minecraft/NeoForge on their classpath by
	// default — moddev only wires that into source sets it's told about. Without this they won't compile.
	addModdingDependenciesTo sourceSets.test
	addModdingDependenciesTo sourceSets.datagen
	contentModules.each { addModdingDependenciesTo sourceSets[it] }
```

- [ ] **Step 4: Give the GameTests every jar**

In `dependencies`, after the datagen line:

```groovy
	// GameTests exercise every jar
	contentModules.each { testImplementation sourceSets[it].output }
```

- [ ] **Step 5: Keep the jar whole**

In the `jar { }` block, after the two `from sourceSets.main.output` lines:

```groovy
	// 9a keeps one artifact: the five content source sets compile separately but ship together, so
	// the jar's contents are unchanged. 9b gives each its own jar task and mod id.
	contentModules.each {
		from sourceSets[it].output.classesDirs
		from sourceSets[it].output.resourcesDir
	}
```

- [ ] **Step 6: Compile everything**

```bash
./gradlew classes compileApicultureJava compileArboricultureJava compileLepidopterologyJava \
    compileAgricultureJava compileMailJava compileDatagenJava compileTestJava --console=plain 2>&1 \
  | grep -E 'error:|BUILD' | head -30
```

Expected: `BUILD SUCCESSFUL`. If any `error: package forestry.<module> does not exist` appears, that is
a real illegal edge the import measurement missed - **sever it, do not widen the classpath.** Widening
`compileClasspath` to make an error go away silently reintroduces the coupling this phase exists to
prevent.

- [ ] **Step 7: Prove the edges are actually restrictive**

A classpath that compiles proves nothing unless it also rejects. Plant a violation in each direction
that D1 forbids:

```bash
sed -i '0,/^import /s//import forestry.apiculture.bees.BeeSpecies;\nimport /' \
  src/mail/java/forestry/mail/ModuleMail.java
./gradlew compileMailJava --console=plain 2>&1 | grep -E 'does not exist|BUILD'
git checkout -- src/mail/java/forestry/mail/ModuleMail.java

sed -i '0,/^import /s//import forestry.lepidopterology.butterflies.ButterflySpecies;\nimport /' \
  src/arboriculture/java/forestry/arboriculture/ModuleArboriculture.java
./gradlew compileArboricultureJava --console=plain 2>&1 | grep -E 'does not exist|BUILD'
git checkout -- src/arboriculture/java/forestry/arboriculture/ModuleArboriculture.java
```

Expected: both `BUILD FAILED` with `package forestry.apiculture.bees does not exist` and
`package forestry.lepidopterology.butterflies does not exist` respectively. The second is the more
important of the two - it proves the *allowed* lepidopterology -> arboriculture edge is one-way.

- [ ] **Step 8: Verify the jar is unchanged**

The whole claim of 9a is that the artifact does not move. Check it against the phase-8 jar:

```bash
J=build/libs/forestry-1.21.1-3.0.0-alpha1.jar
unzip -l $J | tail -2 > /tmp/claude-1000/-home-thedarkcolour-IdeaProjects-ForestryCE/*/scratchpad/jar-before.txt
./gradlew jar --console=plain
unzip -l $J | tail -2
```

Expected: the same file count as before the split, around 14,666. A drop means a source set is not in
the `jar` task; a rise means resources were duplicated.

- [ ] **Step 9: Oracles**

```bash
./gradlew runData --console=plain 2>&1 | tail -2; git diff --stat src/generated/resources
./gradlew runGameTestServer --console=plain 2>&1 | grep -E 'required tests|BUILD' | tail -3
```

Expected: empty datagen diff (Task 1's new tag file is already committed), `All 108 required tests
passed`. The GameTest run is what proves module discovery still works across six source sets - if
`@ForestryModule` scanning had broken, the species counts would collapse and a dozen tests would fail.

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "build: six source sets, the compiler owns the graph"
```

---

### Task 3: Retire the stand-in gates, gate what the compiler still cannot see

`checkBaseBoundary` and `checkBaseBytecode` both existed to answer "does base reference a content
module?". After Task 2 that question is answered by `main`'s compile classpath, which contains no
content output at all. They are now unfalsifiable - they cannot fail, so they measure nothing. Delete
them.

Two things the compiler still does **not** see, and one measurement that has to be recorded rather
than gated:

- `forestry.api` must not import outside `forestry.api`. `api` lives inside `main` alongside `core`,
  so this is not a classpath edge. `checkApiBoundary` stays.
- Resource-borne FQCNs. `checkResourceFqcn` stays, and **must now scan all six source roots** - the
  service file names `forestry.apiculture.plugin.ApicultureForestryPlugin` and three siblings, which
  are no longer under `src/main/java`. Without this change the gate reports four false missing classes.
- D2's internal layer rule. See Step 4.

**Files:**
- Modify: `build.gradle`

- [ ] **Step 1: Delete `checkBaseBoundary` and `checkBaseBytecode`**

Remove both task registrations, their comment blocks, and their two `tasks.named('check')` blocks.

- [ ] **Step 2: Point `checkResourceFqcn` at all six roots**

```groovy
	// Every source root. The service file names one plugin class per content jar, and those left
	// src/main/java in 9a; a single-root check would report all four as missing
	var javaDirs = [project.file('src/main/java'), project.file('src/datagen/java')] +
			contentModules.collect { project.file("src/${it}/java") }
```

- [ ] **Step 3: Confirm it still finds everything**

```bash
./gradlew checkResourceFqcn --console=plain 2>&1 | grep -E 'checkResourceFqcn|BUILD'
```

Expected: `checkResourceFqcn: 17 resource-borne class name(s) all resolve`. If it reports fewer than
17 or fails, the root list is wrong - a false "missing" here is the gate working correctly on a broken
config, not a reason to relax it.

- [ ] **Step 4: Add `checkCoreLayers`, hard on `engine`, reporting on `platform`**

D2 says "nothing in `platform` or `engine` may import `content`". Measured 2026-08-02: `engine` obeys
it at **0** imports; `platform` violates it **64** times across **26** files, and always has - no phase
ever gated it. Gate the half that holds, and print the half that does not so it cannot be forgotten.
Do not add a second checked-in baseline; the ratchet mechanism was just retired and one number in the
build log is enough to keep this visible.

```groovy
// D2 gives core one internal edge to enforce: platform and engine are below content and must not
// import it. Measured at the start of phase 9a: engine obeys this at 0, platform does not, at 64
// imports across 26 files. The engine half is gated; the platform half is reported every build so it
// stays visible. Severing those 64 is its own phase - it is not packaging work and nothing about the
// jar split depends on it.
var checkCoreLayers = tasks.register('checkCoreLayers') {
	group = 'verification'
	description = 'Fails if forestry.core.engine imports forestry.core.content'

	var coreDir = project.file('src/main/java/forestry/core')

	inputs.dir(coreDir)
	outputs.upToDateWhen { false }

	doLast {
		var pattern = ~/^import (static )?forestry\.core\.content\..*/
		var count = { String layer ->
			var dir = new File(coreDir, layer)
			var hits = new TreeSet<String>()
			if (dir.exists()) {
				dir.eachFileRecurse(groovy.io.FileType.FILES) { File file ->
					if (file.name.endsWith('.java') && file.readLines('UTF-8').any { it ==~ pattern }) {
						hits.add(coreDir.toPath().relativize(file.toPath()).toString())
					}
				}
			}
			return hits
		}

		var engine = count('engine')
		if (!engine.isEmpty()) {
			throw new GradleException(
					"forestry.core.engine must not import forestry.core.content:\n  "
					+ engine.join('\n  ')
					+ "\nThe engine layer is below content. Invert the reference through an api interface.")
		}
		logger.lifecycle("checkCoreLayers: engine is clean; platform has ${count('platform').size()} "
				+ "file(s) importing content (known, ungated - see D2 in the reorg spec)")
	}
}

tasks.named('check') {
	dependsOn checkCoreLayers
}
```

- [ ] **Step 5: Run it and confirm both halves**

```bash
./gradlew checkCoreLayers --console=plain 2>&1 | grep -E 'checkCoreLayers|BUILD'
```

Expected: `checkCoreLayers: engine is clean; platform has 26 file(s) importing content (known,
ungated - see D2 in the reorg spec)`.

- [ ] **Step 6: Prove the engine half bites**

```bash
sed -i '0,/^import /s//import forestry.core.content.machines.tiles.TileCarpenter;\nimport /' \
  src/main/java/forestry/core/engine/climate/ForestryClimateManager.java
./gradlew checkCoreLayers --console=plain 2>&1 | grep -E 'engine must not|climate|BUILD'
git checkout -- src/main/java/forestry/core/engine/climate/ForestryClimateManager.java
./gradlew checkCoreLayers --console=plain 2>&1 | grep -E 'checkCoreLayers|BUILD'
```

Expected: `BUILD FAILED` naming the climate file, then green again. If `TileCarpenter` is not the right
class name, use any real type under `forestry.core.content`.

- [ ] **Step 7: Full build and oracles**

```bash
./gradlew build --console=plain 2>&1 | grep -E 'check[ACR]|BUILD|error:'
./gradlew runData --console=plain 2>&1 | tail -2; git diff --stat src/generated/resources
./gradlew runGameTestServer --console=plain 2>&1 | grep -E 'required tests|BUILD' | tail -3
```

Expected: `checkApiBoundary`, `checkCoreLayers`, `checkResourceFqcn` all green; no
`checkBaseBoundary`/`checkBaseBytecode` lines at all; empty datagen diff; 108 tests passed.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "build: retire the stand-in gates, gate the core layers"
```

---

### Task 4: Record the phase

**Files:**
- Modify: `docs/superpowers/specs/2026-07-30-feature-package-reorg-design.md`

- [ ] **Step 1: Split the Sequencing row**

Replace row `9` with `9a DONE 2026-08-02` and `9b`, listing for 9b everything under "Scope" above.

- [ ] **Step 2: Add a phase-9a paragraph**

Record:

- The measurement that made this cheap: 1 illegal edge in 1,220 cross-package imports, and the
  prototype's confirmation that `FarmableGE`'s two lines were the *only* compile errors in the repo.
- `main` is core, and why the spec's `src/core/java` sketch was not followed.
- `checkBaseBoundary` and `checkBaseBytecode` are deleted, and the reason is not that the boundary
  stopped mattering but that it became unfalsifiable: they cannot fail, so they measure nothing.
- The classpath edges were verified to *reject*, not merely to compile, including that the allowed
  lepidopterology -> arboriculture edge is one-way.
- `FarmableGE` now reads `forestry:tree_saplings` rather than `BlockTags.SAPLINGS`, and why the wider
  vanilla tag would have been a silent behaviour change.

- [ ] **Step 3: Correct D2**

D2's rationale claims "Layers give one enforceable edge: nothing in `platform` or `engine` may import
`content`". Measured: `engine` 0, `platform` 64 across 26 files. Amend D2 in place to say what is
actually true and gated, and note that severing platform's 64 is unscheduled work that no phase has
ever owned.

- [ ] **Step 4: Discharge the ArchUnit line**

The Verification section says an ArchUnit-style test is "the *only* proof available for the phase-6
gate" until phase 9. That is now discharged by the compiler; no ArchUnit test was ever written and none
is needed for the base/content edge. Say so, and note that `checkApiBoundary` and `checkCoreLayers`
cover the two boundaries the compiler still cannot see.

- [ ] **Step 5: Update the Status line**

`Status: phases 1-9a complete (2026-08-02); phase 9b next`

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "docs: record phase 9a completion, the compiler owns the graph"
```

---

## Handoff to 9b

- **Prototype the six-`--output` datagen question first.** It is the oldest open item in the build
  story, the spec has flagged it twice, and nothing in 9a needed to answer it.
- **The service file is the mechanism that makes optional installs work.** Splitting
  `META-INF/services/forestry.api.plugin.IForestryPlugin` into one entry per jar is what makes a
  missing jar mean "nothing registers" rather than "class not found". `checkResourceFqcn` guards it.
- **Texture ownership is derivable.** 1,590 of the 1,960 hand-authored resources are textures with no
  owning-jar metadata, but generated models reference them, and models partition per jar once datagen
  does. Derive the mapping; do not hand-sort 1,590 files.
- **The cross-jar loot modifier needs redesign, not partition.**
  `data/forestry/loot_modifiers/chests/abandoned_mineshaft.json` spans
  `["apiculture", "factory", "storage"]` in one file.
- **`forestry.api` ships whole in core** per D3 and does not split. `api.apiculture` staying in the
  core jar is correct and is what makes a missing content jar safe.
