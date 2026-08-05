# Four-jar rename and the apiculture/arboriculture merge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Collapse the six-jar split into four jars named `forestry`, `forestryfarms`, `forestrymail` and `forestrybutterflies`, folding apiculture and arboriculture into core.

**Architecture:** Six Gradle source sets become four. `src/apiculture` and `src/arboriculture` move wholesale into `src/main`; `src/lepidopterology` and `src/agriculture` are renamed to `src/butterflies` and `src/farms` while keeping their Java package names, which forces a new `contentPackages` map in `build.gradle`. Mod ids lose their underscores. The ownership machinery is edited by value only, never extended, because a successor spec deletes it.

**Tech Stack:** Gradle 8 (Groovy DSL), NeoForge 21.1.230, Minecraft 1.21.1, Java 21.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-05-mod-id-rename-merge-design.md`. Read it before Task 1.
- Branch: `1.21.1-restructure`. Do not create a new branch.
- Java packages never change. `forestry.apiculture`, `forestry.arboriculture`, `forestry.lepidopterology` and `forestry.agriculture` keep their names in every task.
- All source moves use `git mv`, never `cp` + `rm`.
- **Touch the ownership machinery as lightly as possible.** Edit map *values*. Do not add rules, do not refactor `generateResourceOwners`, do not improve the promotion pass. It is slated for deletion.
- Comment style is binding: see `CLAUDE.md`. ASCII only, no em-dash, no terminal period on single-sentence inline comments.
- Regeneration order is fixed and non-negotiable: `runData` writes `src/generated/ownership.json`, then `generateResourceOwners` derives `src/generated/resource-owners.json`. Never the reverse.
- Every `gradlew` invocation runs from the repo root: `/home/thedarkcolour/IdeaProjects/ForestryCE`.

## File Structure

| file | responsibility | tasks |
| --- | --- | --- |
| `build.gradle` | source sets, ownership maps, mod ids, jar tasks, boot configs | 1, 2, 4, 5, 6 |
| `src/datagen/java/forestry/core/data/OwnershipManifest.java` | `MODULE_TO_JAR`, the only hand-written ownership input | 1, 2, 4 |
| `src/main/resources/META-INF/services/forestry.api.plugin.IForestryPlugin` | plugin discovery for the merged core | 1, 2 |
| `src/{butterflies,farms,mail}/templates/META-INF/neoforge.mods.toml` | per-jar mod metadata | 2, 4, 5 |
| `src/test/templates/META-INF/neoforge.mods.toml` | gametest mod id | 5 |
| `src/generated/ownership.json` | generated, committed | 3, 4 |
| `src/generated/resource-owners.json` | generated, committed | 3, 4 |

---

### Task 1: Fold apiculture into core

**Files:**
- Move: `src/apiculture/java/forestry/apiculture/` -> `src/main/java/forestry/apiculture/`
- Move: `src/apiculture/resources/data/forestry/neoforge/biome_modifier/hive.json` -> `src/main/resources/data/forestry/neoforge/biome_modifier/hive.json`
- Modify: `src/main/resources/META-INF/services/forestry.api.plugin.IForestryPlugin`
- Delete: `src/apiculture/`
- Modify: `build.gradle` (source sets, ownership maps, boot configs, two comments)
- Modify: `src/datagen/java/forestry/core/data/OwnershipManifest.java:49`

**Interfaces:**
- Consumes: nothing.
- Produces: `contentModules` = `['arboriculture', 'lepidopterology', 'agriculture', 'mail']`. `MODULE_TO_JAR` maps `"apiculture"` to `"core"`.

- [ ] **Step 1: Move the Java sources and the biome modifier**

```bash
cd /home/thedarkcolour/IdeaProjects/ForestryCE
git mv src/apiculture/java/forestry/apiculture src/main/java/forestry/apiculture
mkdir -p src/main/resources/data/forestry/neoforge/biome_modifier
git mv src/apiculture/resources/data/forestry/neoforge/biome_modifier/hive.json \
       src/main/resources/data/forestry/neoforge/biome_modifier/hive.json
```

- [ ] **Step 2: Merge the plugin service file**

`src/main/resources/META-INF/services/forestry.api.plugin.IForestryPlugin` currently reads:

```
forestry.core.plugin.DefaultForestryPlugin
forestry.core.platform.compat.kubejs.KubeForestryPlugin
```

Rewrite it to:

```
forestry.core.plugin.DefaultForestryPlugin
forestry.apiculture.plugin.ApicultureForestryPlugin
forestry.core.platform.compat.kubejs.KubeForestryPlugin
```

`ApicultureForestryPlugin` goes immediately after `DefaultForestryPlugin`, not at the end. `KubeForestryPlugin` is a compat shim and stays last.

Then drop the old file and the rest of the source set:

```bash
git rm src/apiculture/resources/META-INF/services/forestry.api.plugin.IForestryPlugin
git rm -r src/apiculture
```

- [ ] **Step 3: Remove the apiculture source set from `build.gradle`**

Line 39, remove `'apiculture'`:

```groovy
var contentModules = ['arboriculture', 'lepidopterology', 'agriculture', 'mail']
```

In the `sourceSets { ... }` block at line 41, delete the bare `apiculture` line.

Delete line 52 entirely:

```groovy
sourceSets.apiculture.compileClasspath += sourceSets.main.output
```

- [ ] **Step 4: Reassign apiculture in the three Groovy ownership maps**

In `lootModuleJars`, change the `apiculture` value:

```groovy
		'apiculture'     : 'core',
```

In `worldgenOwners`, change `hive`:

```groovy
var worldgenOwners = [
		'hive': 'core',
		'tree': 'arboriculture',
]
```

In `folderOwners`, change the three bee folders:

```groovy
		'data/forestry/bee_species/'              : 'core',
		'data/forestry/bee_effect/'               : 'core',
		'data/forestry/recipe/bee_mutation/'      : 'core',
```

In the `visibility` map inside `generateResourceOwners`, delete the `'apiculture'` line:

```groovy
		var visibility = [
				'core'           : ['core'] as Set,
				'arboriculture'  : ['core', 'arboriculture'] as Set,
				'agriculture'    : ['core', 'agriculture'] as Set,
				'mail'           : ['core', 'mail'] as Set,
				// lepidopterology declares a required dependency on arboriculture, so a butterfly is
				// allowed to name a tree
				'lepidopterology': ['core', 'arboriculture', 'lepidopterology'] as Set,
		]
```

- [ ] **Step 5: Reassign apiculture in `OwnershipManifest`**

`src/datagen/java/forestry/core/data/OwnershipManifest.java`, in `MODULE_TO_JAR`, change:

```java
			Map.entry("apiculture", "core"),
```

- [ ] **Step 6: Delete the apiculture boot configuration and fix the two stale comments**

Delete the whole `apicultureServer { ... }` block along with its three-line leading comment ("Bees without trees...").

In `allJarsServer`, drop `mods.forestry_apiculture`:

```groovy
			loadedMods = [mods.forestry, mods.forestry_arboriculture, mods.forestry_lepidopterology, mods.forestry_agriculture, mods.forestry_mail]
```

In the `mods { ... }` block comment, replace the sentence naming `forestry_apiculture` with:

```groovy
		// service path. Registering forestry: names from a mod whose id is forestry_agriculture is
```

In the `forestry_gametest` comment, the apiculture example is no longer a content type. Replace that sentence with:

```groovy
		// scanner resolves every @GameTest method descriptor at registration, and a test naming an
		// agriculture type crashes a core-only boot outright - the D3 safety condition, exactly
```

- [ ] **Step 7: Verify every source set still compiles**

```bash
./gradlew compileJava compileArboricultureJava compileLepidopterologyJava \
          compileAgricultureJava compileMailJava compileDatagenJava compileTestJava
```

Expected: `BUILD SUCCESSFUL`.

If `compileJava` fails on an unresolved `forestry.apiculture` symbol, the move dropped a file. If it fails on a *core* class that cannot see apiculture, that is a genuine pre-existing edge now surfaced - report it rather than adding a classpath entry.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "refactor: fold apiculture into core

Bees are what Forestry is; no install that wants Forestry wants it without
them. The apiculture source set, its biome modifier and its plugin service
entry move into main, and every ownership map reassigns apiculture to core."
```

---

### Task 2: Fold arboriculture into core

**Files:**
- Move: `src/arboriculture/java/forestry/arboriculture/` -> `src/main/java/forestry/arboriculture/`
- Move: `src/arboriculture/resources/data/forestry/neoforge/biome_modifier/tree.json` -> `src/main/resources/data/forestry/neoforge/biome_modifier/tree.json`
- Move: `src/arboriculture/resources/META-INF/services/forestry.api.client.plugin.IClientHelper` -> `src/main/resources/META-INF/services/forestry.api.client.plugin.IClientHelper`
- Modify: `src/main/resources/META-INF/services/forestry.api.plugin.IForestryPlugin`
- Delete: `src/arboriculture/`
- Modify: `build.gradle` (source sets, the cross-content edge, ownership maps, boot configs)
- Modify: `src/datagen/java/forestry/core/data/OwnershipManifest.java`
- Modify: `src/lepidopterology/templates/META-INF/neoforge.mods.toml`

**Interfaces:**
- Consumes: Task 1's `contentModules`.
- Produces: `contentModules` = `['lepidopterology', 'agriculture', 'mail']`. No source set has a compile dependency on any other content source set.

- [ ] **Step 1: Move the sources, the biome modifier and the client helper service**

```bash
cd /home/thedarkcolour/IdeaProjects/ForestryCE
git mv src/arboriculture/java/forestry/arboriculture src/main/java/forestry/arboriculture
git mv src/arboriculture/resources/data/forestry/neoforge/biome_modifier/tree.json \
       src/main/resources/data/forestry/neoforge/biome_modifier/tree.json
git mv src/arboriculture/resources/META-INF/services/forestry.api.client.plugin.IClientHelper \
       src/main/resources/META-INF/services/forestry.api.client.plugin.IClientHelper
```

The `IClientHelper` file is a plain move: `src/main/resources` has no such service file today.

- [ ] **Step 2: Merge the plugin service file and delete the source set**

`src/main/resources/META-INF/services/forestry.api.plugin.IForestryPlugin` becomes:

```
forestry.core.plugin.DefaultForestryPlugin
forestry.apiculture.plugin.ApicultureForestryPlugin
forestry.arboriculture.plugin.ArboricultureForestryPlugin
forestry.core.platform.compat.kubejs.KubeForestryPlugin
```

```bash
git rm src/arboriculture/resources/META-INF/services/forestry.api.plugin.IForestryPlugin
git rm -r src/arboriculture
```

- [ ] **Step 3: Remove the arboriculture source set and dissolve the cross-content edge**

Line 39:

```groovy
var contentModules = ['lepidopterology', 'agriculture', 'mail']
```

In `sourceSets { ... }`, delete the bare `arboriculture` line.

Delete `sourceSets.arboriculture.compileClasspath += sourceSets.main.output`.

Lepidopterology's compile classpath loses its arboriculture term:

```groovy
sourceSets.lepidopterology.compileClasspath += sourceSets.main.output
```

Delete this line entirely:

```groovy
sourceSets.lepidopterology.runtimeClasspath += sourceSets.arboriculture.output
```

Rewrite the comment above the compile classpath block. It currently describes the cross edge; that edge is gone:

```groovy
// Everything requires core, and nothing else. Arboriculture moving into core dissolved the only
// cross-content edge the split ever had - three TreeUtil imports for butterfly pollination
```

- [ ] **Step 4: Reassign arboriculture in the three Groovy ownership maps**

`lootModuleJars`:

```groovy
		'arboriculture'  : 'core',
```

`worldgenOwners`:

```groovy
var worldgenOwners = [
		'hive': 'core',
		'tree': 'core',
]
```

`folderOwners`, the two tree folders:

```groovy
		'data/forestry/tree_species/'             : 'core',
		'data/forestry/recipe/tree_mutation/'     : 'core',
```

`visibility` inside `generateResourceOwners` - delete the `arboriculture` entry and the two-line comment above `lepidopterology`, which described the dependency that no longer exists:

```groovy
		var visibility = [
				'core'           : ['core'] as Set,
				'agriculture'    : ['core', 'agriculture'] as Set,
				'mail'           : ['core', 'mail'] as Set,
				'lepidopterology': ['core', 'lepidopterology'] as Set,
		]
```

- [ ] **Step 5: Reassign arboriculture in `OwnershipManifest`**

```java
			Map.entry("arboriculture", "core"),
```

- [ ] **Step 6: Drop the arboriculture dependency from lepidopterology's mod metadata**

In `src/lepidopterology/templates/META-INF/neoforge.mods.toml`, delete the trailing block and its two-line comment:

```toml
# Butterflies pollinate leaves, so the lepidopterology jar needs the arboriculture jar.
# This is the only cross-content edge D1 allows, and the only one the code has.
[[dependencies.forestry_lepidopterology]]
modId = "forestry_arboriculture"
type = "REQUIRED"
versionRange = "[${version},]"
ordering = "AFTER"
side = "BOTH"
```

The file now ends with the `forestry` dependency block.

- [ ] **Step 7: Rework the boot configuration that named arboriculture**

Replace the `lepidopterologyNoBeesServer { ... }` block and its leading comment with:

```groovy
		// Butterflies alone. Bees and trees are core now, so this proves only what it should: that the
		// butterfly jar needs nothing but base
		lepidopterologyServer {
			server()
			gameDirectory.set(project.layout.projectDirectory.dir('run/boot-lepi'))
			programArguments.addAll('--nogui', '--port', '25568')
			loadedMods = [mods.forestry, mods.forestry_lepidopterology]
		}
```

In `allJarsServer`:

```groovy
			loadedMods = [mods.forestry, mods.forestry_lepidopterology, mods.forestry_agriculture, mods.forestry_mail]
```

Its leading comment said "All six" when this plan was written. **Superseded:** Task 1's fix pass
recast every hardcoded jar count in `build.gradle` to be count-free, because 6 -> 5 -> 4 goes stale
twice more. That comment now reads "Every jar". Leave it. Do not reintroduce a number here, or
anywhere else, except `checkJarPartition`'s log line, which computes `${jarFiles.size()}`.

- [ ] **Step 8: Verify every source set still compiles**

```bash
./gradlew compileJava compileLepidopterologyJava compileAgricultureJava \
          compileMailJava compileDatagenJava compileTestJava
```

Expected: `BUILD SUCCESSFUL`.

The three `TreeUtil` imports in `forestry.lepidopterology` now resolve through core rather than through a sibling source set. If they fail, `TreeUtil` did not move with the rest of arboriculture.

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "refactor: fold arboriculture into core

Trees join bees in the base jar. This dissolves the only cross-content edge
the split had: lepidopterology's three TreeUtil imports now resolve through
core, so every remaining jar depends on core and nothing else."
```

---

### Task 3: Regenerate ownership and prove the merge

**Files:**
- Modify (generated): `src/generated/ownership.json`
- Modify (generated): `src/generated/resource-owners.json`

**Interfaces:**
- Consumes: Task 1 and Task 2's `MODULE_TO_JAR` and Groovy map edits.
- Produces: an `ownership.json` in which no value is `apiculture` or `arboriculture`.

This task writes no hand-authored code. It is the gate that proves the two merges actually hold at runtime.

- [ ] **Step 1: Regenerate the ownership manifest**

```bash
./gradlew runData
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Confirm the manifest moved exactly the ids it should**

```bash
python3 -c "
import json, collections
o = json.load(open('src/generated/ownership.json'))
print(collections.Counter(o.values()))
"
```

Expected: `core` around 5,037, `agriculture` 237, `lepidopterology` 19, `mail` 68. No `apiculture` key and no `arboriculture` key at all.

If either still appears, a `MODULE_TO_JAR` entry was missed. If the build threw `Module(s) with no jar in OwnershipManifest`, an entry was deleted rather than repointed to `core`.

- [ ] **Step 3: Confirm the generated resources themselves did not move**

```bash
git status --short src/generated/resources | head -20
```

Expected: no output. The registries did not change, so the generated files should be byte-identical.

If files *did* change, inspect the diff before continuing. Merging the plugin service files changed `ServiceLoader` iteration order, and a pure reordering inside a generated file is acceptable - but a changed *value* is not, and means a plugin's registration now runs at a different point. Report it rather than committing it.

- [ ] **Step 4: Derive the resource owner map**

```bash
./gradlew generateResourceOwners
```

Expected: `BUILD SUCCESSFUL`. The task logs promotions; a promotion count in the low tens is normal.

If it throws `File(s) name content no single jar can see`, a file references ids across two content jars. That is a real defect the merge exposed, not a mapping problem - report it.

- [ ] **Step 5: Confirm the resource owner map collapsed into core**

```bash
python3 -c "
import json, collections
o = json.load(open('src/generated/resource-owners.json'))
print(collections.Counter(o.values()))
"
```

Expected: `core` around 11,488, `agriculture` 465, `lepidopterology` 107, `mail` 96, `split` 10. No `apiculture`, no `arboriculture`.

- [ ] **Step 6: Prove the jars partition correctly**

```bash
./gradlew checkJarPartition
```

Expected: `BUILD SUCCESSFUL`, with a line reading `checkJarPartition: 4 jars, no foreign classes, N split variant(s) all present`.

The message still hardcodes `6 jars` at this point; that is fixed in Task 4. Read the rest of the line, not the count.

- [ ] **Step 7: Boot core alone**

```bash
./gradlew runCoreOnlyServer
```

Expected: the log reaches a `Done (Ns)! For help, type "help"` line. Read for that line specifically. Zero errors is **not** evidence: a server that failed to bind its port stops before loading any datapack and logs no errors at all.

Stop the server with Ctrl-C once `Done` appears.

- [ ] **Step 8: Boot butterflies, then all four**

```bash
./gradlew runLepidopterologyServer
```

Expected: reaches `Done`. Stop it.

```bash
./gradlew runAllJarsServer
```

Expected: reaches `Done`. Stop it.

If a run fails to bind its port, a previous forked JVM survived. Each configuration has its own port, so a survivor only ever blocks a rerun of itself: kill it and rerun.

- [ ] **Step 9: Run the GameTest suite**

```bash
./gradlew runGameTestServer
```

Expected: `BUILD SUCCESSFUL`. The suite was green as of 2026-07-28 and nothing in Tasks 1-2 touches genome or creative tab membership, so a `GenomeBaselineTest` or `CreativeTabBaselineTest` failure means a registration order changed and should be investigated, not regenerated away.

- [ ] **Step 10: Commit**

```bash
git add src/generated/ownership.json src/generated/resource-owners.json
git commit -m "build: regenerate ownership after the bees and trees merge

4,381 ids move to core, leaving 324 across the three content jars. Verified
by checkJarPartition, three boot configurations and the GameTest suite."
```

---

### Task 4: Rename the surviving modules to butterflies and farms

**Files:**
- Move: `src/lepidopterology/` -> `src/butterflies/`
- Move: `src/agriculture/` -> `src/farms/`
- Modify: `build.gradle` (`contentModules`, new `contentPackages` map and guard, ownership map values, `checkJarPartition` package references, boot config names)
- Modify: `src/datagen/java/forestry/core/data/OwnershipManifest.java`
- Modify: `src/butterflies/templates/META-INF/neoforge.mods.toml`
- Modify: `src/farms/templates/META-INF/neoforge.mods.toml`
- Modify (generated): `src/generated/ownership.json`, `src/generated/resource-owners.json`

**Interfaces:**
- Consumes: Task 3's regenerated manifests.
- Produces: `contentModules` = `['butterflies', 'farms', 'mail']` and `contentPackages` = `[butterflies: 'lepidopterology', farms: 'agriculture', mail: 'mail']`. Mod ids at the end of this task are still underscored (`forestry_butterflies`, `forestry_farms`, `forestry_mail`); Task 5 drops the underscore.

- [ ] **Step 1: Move the two source sets**

```bash
cd /home/thedarkcolour/IdeaProjects/ForestryCE
git mv src/lepidopterology src/butterflies
git mv src/agriculture src/farms
```

The Java packages inside are untouched: `src/butterflies/java/forestry/lepidopterology/` and `src/farms/java/forestry/agriculture/` are the correct resulting paths.

- [ ] **Step 2: Rename the source sets in `build.gradle`**

Line 39 and the `sourceSets { ... }` block:

```groovy
var contentModules = ['butterflies', 'farms', 'mail']

sourceSets {
	butterflies
	farms
	mail
	datagen
}
```

Update the two explicit compile-classpath lines to the new names:

```groovy
sourceSets.butterflies.compileClasspath += sourceSets.main.output
sourceSets.farms.compileClasspath += sourceSets.main.output
sourceSets.mail.compileClasspath += sourceSets.main.output
```

- [ ] **Step 3: Repoint the ownership maps at the new jar names**

`lootModuleJars`:

```groovy
		'lepidopterology': 'butterflies',
		'farming'        : 'farms',
		'cultivation'    : 'farms',
```

`folderOwners`, the two butterfly folders:

```groovy
		'data/forestry/butterfly_species/'        : 'butterflies',
		'data/forestry/recipe/butterfly_mutation/': 'butterflies',
```

`visibility` inside `generateResourceOwners`:

```groovy
		var visibility = [
				'core'       : ['core'] as Set,
				'butterflies': ['core', 'butterflies'] as Set,
				'farms'      : ['core', 'farms'] as Set,
				'mail'       : ['core', 'mail'] as Set,
		]
```

`OwnershipManifest.MODULE_TO_JAR`, the three content entries:

```java
			Map.entry("lepidopterology", "butterflies"),
			Map.entry("farming", "farms"),
			Map.entry("cultivation", "farms"),
```

- [ ] **Step 4: Rename the two boot configurations that carry a module name**

`lepidopterologyServer` becomes `butterfliesServer`, and its game directory follows:

```groovy
		butterfliesServer {
			server()
			gameDirectory.set(project.layout.projectDirectory.dir('run/boot-butterflies'))
			programArguments.addAll('--nogui', '--port', '25567')
			loadedMods = [mods.forestry, mods.forestry_butterflies]
		}
```

In `allJarsServer`:

```groovy
			loadedMods = [mods.forestry, mods.forestry_butterflies, mods.forestry_farms, mods.forestry_mail]
```

The `mods { ... }` block still reads `create("forestry_${m}")`, so these ids follow the source set names automatically. Task 6 restructures the boot set properly; this step only keeps it building.

- [ ] **Step 5: Rename the mod ids in the two moved templates**

In `src/butterflies/templates/META-INF/neoforge.mods.toml`, replace every occurrence of `forestry_lepidopterology` with `forestry_butterflies` - the `modId` line and the two `[[dependencies.forestry_lepidopterology]]` table headers. Leave `displayName` and `description` alone; Task 5 handles those.

In `src/farms/templates/META-INF/neoforge.mods.toml`, replace every occurrence of `forestry_agriculture` with `forestry_farms` - the `modId` line and both `[[dependencies.forestry_agriculture]]` headers.

- [ ] **Step 6: Regenerate both manifests**

```bash
./gradlew runData && ./gradlew generateResourceOwners
```

Expected: `BUILD SUCCESSFUL` for both, in that order.

```bash
python3 -c "
import json, collections
print(collections.Counter(json.load(open('src/generated/ownership.json')).values()))
print(collections.Counter(json.load(open('src/generated/resource-owners.json')).values()))
"
```

Expected: keys are now `core`, `butterflies`, `farms`, `mail` (and `split` in the second). Counts are unchanged from Task 3, only the names differ.

- [ ] **Step 7: Run `checkJarPartition` and watch it fail**

```bash
./gradlew checkJarPartition
```

Expected: **FAIL**, with a message of the form:

```
Jar partition is wrong:
  forestry_butterflies carries N class(es) it does not own, first: forestry/lepidopterology/...
```

This failure is the point of the step. `checkJarPartition` assumes a source set's name is its Java package name, and that assumption just became false. Do not proceed until you have seen this failure - it is the evidence that the check is looking at real package names.

Note the asymmetry: the *content-jar* half of the check fails loudly like this, but the *core-jar* half fails silently. It scans for `forestry/butterflies/`, a package that does not exist, finds nothing, and reports a clean base jar however much leaked into it. That silent half is what Step 8's guard exists for.

- [ ] **Step 8: Add the `contentPackages` map and its configuration-time guard**

Immediately after the `contentModules` declaration at line 39, insert:

```groovy
// Source set name to Java package name. The two diverge because the jars were renamed and the
// packages were not, and build.gradle assumed they were the same string in four places
var contentPackages = [butterflies: 'lepidopterology', farms: 'agriculture', mail: 'mail']

// A wrong entry here disables rather than fails the base-jar leak check in checkJarPartition: it
// scans for a package that does not exist, finds nothing and reports a clean jar. Assert the
// packages are real at configuration time, where the failure is still loud
contentModules.each { m ->
	var pkg = contentPackages[m]
	if (pkg == null || !project.file("src/${m}/java/forestry/${pkg}").isDirectory()) {
		throw new GradleException("contentPackages[${m}] must name a directory under src/${m}/java/forestry, got: ${pkg}")
	}
}
```

- [ ] **Step 9: Route both halves of `checkJarPartition` through `contentPackages`**

The foreign-class scan becomes:

```groovy
		contentModules.each { m ->
			var foreign = entries[m].findAll { it.endsWith('.class') && !it.startsWith("forestry/${contentPackages[m]}/") }
			if (!foreign.isEmpty()) {
				problems.add("forestry_${m} carries ${foreign.size()} class(es) it does not own, first: ${foreign.first()}")
			}
		}
		var contentInCore = entries['core'].findAll { name ->
			name.endsWith('.class') && contentModules.any { name.startsWith("forestry/${contentPackages[it]}/") }
		}
```

**Already done in Task 1's fix pass** - the completion message no longer hardcodes a count, it
reads `${jarFiles.size()}`. Nothing to change here; verify it still does.

- [ ] **Step 10: Verify the guard rejects a wrong package**

Temporarily break the map to prove the guard is live:

```bash
sed -i "s/farms: 'agriculture'/farms: 'agirculture'/" build.gradle
./gradlew checkJarPartition
```

Expected: **FAIL** with `contentPackages[farms] must name a directory under src/farms/java/forestry, got: agirculture`.

Restore it:

```bash
sed -i "s/farms: 'agirculture'/farms: 'agriculture'/" build.gradle
git diff --stat build.gradle
```

Expected: the diff no longer contains `agirculture`.

- [ ] **Step 11: Verify the partition now passes**

```bash
./gradlew compileJava compileButterfliesJava compileFarmsJava compileMailJava \
          compileDatagenJava compileTestJava checkJarPartition
```

Expected: `BUILD SUCCESSFUL` and `checkJarPartition: 4 jars, no foreign classes, N split variant(s) all present`.

- [ ] **Step 12: Commit**

```bash
git add -A
git commit -m "refactor: rename lepidopterology and agriculture to butterflies and farms

Source sets and directories follow the jar names; Java packages do not. That
divergence breaks build.gradle's assumption that the two are the same string,
and the base-jar half of checkJarPartition fails silently under it, so
contentPackages carries the mapping and a configuration-time guard asserts
every entry names a real package."
```

---

### Task 5: Drop the underscore from every mod id

**Files:**
- Modify: `build.gradle` (mods block, jar task naming, boot config `loadedMods`)
- Modify: `src/butterflies/templates/META-INF/neoforge.mods.toml`
- Modify: `src/farms/templates/META-INF/neoforge.mods.toml`
- Modify: `src/mail/templates/META-INF/neoforge.mods.toml`
- Modify: `src/test/templates/META-INF/neoforge.mods.toml`

**Interfaces:**
- Consumes: Task 4's `contentModules` and `contentPackages`.
- Produces: mod ids `forestry`, `forestrybutterflies`, `forestryfarms`, `forestrymail`, `forestrygametest`. Jar artifacts named `forestrybutterflies-1.21.1-<version>.jar` and siblings.

- [ ] **Step 1: Rename the ids in the mods block**

```groovy
		contentModules.each { m ->
			create("forestry${m}") { sourceSet sourceSets[m] }
		}
```

and:

```groovy
		create('forestrygametest') { sourceSet sourceSets.test }
```

The block's leading comment was recast count-free in Task 1's fix pass, so it no longer says "Six
mod ids". It does still name an underscored id as its example. Update only that example, to
`forestryfarms`, and leave the rest of the comment alone. Do not reintroduce a count.

- [ ] **Step 2: Rename the ids in the boot configurations**

```groovy
			loadedMods = [mods.forestry, mods.forestrybutterflies]
```

and in `allJarsServer`:

```groovy
			loadedMods = [mods.forestry, mods.forestrybutterflies, mods.forestryfarms, mods.forestrymail]
```

- [ ] **Step 3: Rename the jar artifacts and manifest attributes**

In the `contentJars` block:

```groovy
var contentJars = contentModules.collectEntries { m ->
	[(m): tasks.register("${m}Jar", Jar) {
		group = 'build'
		description = "Assembles the forestry${m} jar"
		archiveBaseName.set("forestry${m}-${minecraftVersion}")

		// the module's classes plus its already-partitioned resources
		from sourceSets[m].output

		manifest {
			attributes([
					'Specification-Title'     : "Forestry: ${m.capitalize()}",
					'Specification-Vendor'    : 'SirSengir',
					'Specification-Version'   : "${project.version}",
					'Implementation-Title'    : "forestry${m}",
					'Implementation-Version'  : "${project.version}",
					'Implementation-Vendor'   : 'SirSengir',
					'Implementation-Timestamp': new Date().format("yyyy-MM-dd'T'HH:mm:ssZ")
			])
		}
	}]
}
```

`Specification-Title` is unchanged: `m.capitalize()` over the new source set names already yields Butterflies, Farms and Mail.

In `checkJarPartition`, the problem message also carries the id:

```groovy
				problems.add("forestry${m} carries ${foreign.size()} class(es) it does not own, first: ${foreign.first()}")
```

- [ ] **Step 4: Rename the ids and display names in the three content templates**

`src/butterflies/templates/META-INF/neoforge.mods.toml` - replace `forestry_butterflies` with `forestrybutterflies` in the `modId` line and both `[[dependencies.forestry_butterflies]]` headers, and set:

```toml
displayName = "Forestry: Butterflies"
```

`src/farms/templates/META-INF/neoforge.mods.toml` - replace `forestry_farms` with `forestryfarms` throughout, and set:

```toml
displayName = "Forestry: Farms"
```

`src/mail/templates/META-INF/neoforge.mods.toml` - replace `forestry_mail` with `forestrymail` in the `modId` line and both `[[dependencies.forestry_mail]]` headers. `displayName` is already `Forestry: Mail`.

Do not touch `src/main/templates/META-INF/neoforge.mods.toml`. Its id is already `forestry` and its description, "Trees, bees and more", is now literally accurate.

- [ ] **Step 5: Rename the gametest mod id**

In `src/test/templates/META-INF/neoforge.mods.toml`, set `modId = "forestrygametest"` and change the `[[dependencies.forestry_gametest]]` header to `[[dependencies.forestrygametest]]`.

Its `description` names an apiculture type as the crash example, which stopped being a content type in Task 1. Replace that sentence:

```toml
Dev-only GameTest suite. Never published; it spans every Forestry jar, so it is its own mod id and
the partial-install boot configurations leave it out. NeoForge's gametest scanner resolves every
@GameTest method descriptor at registration, so with this inside the core mod a core-only boot
crashes on the first test method that names a farms type.
```

- [ ] **Step 6: Confirm no underscored id survives anywhere**

```bash
grep -rn "forestry_apiculture\|forestry_arboriculture\|forestry_lepidopterology\|forestry_agriculture\|forestry_mail\|forestry_gametest\|forestry_butterflies\|forestry_farms" \
  build.gradle src/ --include="*.gradle" --include="*.toml" --include="*.java" 2>/dev/null
```

Expected: no output.

Note this deliberately does not scan `docs/`. The two historical specs describe the six-jar state as built and are left as written.

- [ ] **Step 7: Verify the build and the artifact names**

```bash
./gradlew build -x test
ls build/libs/
```

Expected: `BUILD SUCCESSFUL`, and `build/libs/` contains `forestrybutterflies-1.21.1-*.jar`, `forestryfarms-1.21.1-*.jar`, `forestrymail-1.21.1-*.jar` alongside the core `forestry-1.21.1-*.jar`.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "refactor: drop the underscore from every mod id

forestrybutterflies, forestryfarms, forestrymail and forestrygametest. The
underscore read as a namespace separator rather than part of a name, and no
mod id a player types or a pack manifest names is split that way."
```

---

### Task 6: Rework the boot configurations

**Files:**
- Modify: `build.gradle` (the boot configuration block)

**Interfaces:**
- Consumes: Task 5's mod ids.
- Produces: five boot configurations - `runCoreOnlyServer`, `runButterfliesServer`, `runFarmsServer`, `runMailServer`, `runAllJarsServer` - on ports 25566 through 25570.

Farms and mail have never been booted alone, and `folderOwners` names no data folder for either. This task is where a latent ownership bug in those two would surface.

- [ ] **Step 1: Replace the whole boot configuration block**

Keep the existing leading comment about D4 and per-configuration ports. Replace the four configurations that follow it with:

```groovy
		coreOnlyServer {
			server()
			gameDirectory.set(project.layout.projectDirectory.dir('run/boot-core'))
			programArguments.addAll('--nogui', '--port', '25566')
			loadedMods = [mods.forestry]
		}

		butterfliesServer {
			server()
			gameDirectory.set(project.layout.projectDirectory.dir('run/boot-butterflies'))
			programArguments.addAll('--nogui', '--port', '25567')
			loadedMods = [mods.forestry, mods.forestrybutterflies]
		}

		// Farms and mail had no boot of their own under the six-jar layout, and folderOwners names no
		// data folder for either. These two are where an ownership bug is most likely to still be
		farmsServer {
			server()
			gameDirectory.set(project.layout.projectDirectory.dir('run/boot-farms'))
			programArguments.addAll('--nogui', '--port', '25568')
			loadedMods = [mods.forestry, mods.forestryfarms]
		}

		mailServer {
			server()
			gameDirectory.set(project.layout.projectDirectory.dir('run/boot-mail'))
			programArguments.addAll('--nogui', '--port', '25569')
			loadedMods = [mods.forestry, mods.forestrymail]
		}

		// All four. The `server` run above loads whatever is on the dev classpath; this one names the
		// four published jars, so it is the configuration a player actually installs
		allJarsServer {
			server()
			gameDirectory.set(project.layout.projectDirectory.dir('run/boot-all'))
			programArguments.addAll('--nogui', '--port', '25570')
			loadedMods = [mods.forestry, mods.forestrybutterflies, mods.forestryfarms, mods.forestrymail]
		}
```

- [ ] **Step 2: Drop the stale boot directory**

```bash
rm -rf run/boot-lepi run/boot-api
```

These are working directories from the old configuration names and are not tracked.

- [ ] **Step 3: Confirm Gradle registers all five**

```bash
./gradlew tasks --all | grep -i "Server"
```

Expected: `runCoreOnlyServer`, `runButterfliesServer`, `runFarmsServer`, `runMailServer`, `runAllJarsServer`, plus the pre-existing `runServer` and `runGameTestServer`.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "build: one boot configuration per jar

Every jar now depends on core and nothing else, so the meaningful coverage is
each jar alone plus both endpoints. Farms and mail get their first dedicated
boot; neither has ever been loaded without its siblings."
```

---

### Task 7: Full verification and documentation

**Files:**
- Modify: `docs/superpowers/specs/2026-08-05-mod-id-rename-merge-design.md` (status line only)

**Interfaces:**
- Consumes: everything.
- Produces: a verified four-jar build.

- [ ] **Step 1: Clean build from scratch**

```bash
./gradlew clean build -x test
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Confirm the partition on freshly built jars**

```bash
./gradlew checkJarPartition checkResourceFqcn
```

Expected: `BUILD SUCCESSFUL` and `checkJarPartition: 4 jars, no foreign classes, N split variant(s) all present`.

`checkResourceFqcn` derives its source and resource roots from `contentModules`, so it needs no edit - but it is the only check that reads the Patchouli templates and `kubejs.plugins.txt`, which name Java classes by fully qualified name. Those classes did not change package in this work, so a failure here means a file was lost in a move.

- [ ] **Step 3: Boot all five configurations, each on a fresh world**

First delete every boot world directory:

```bash
rm -rf run/boot-core run/boot-butterflies run/boot-farms run/boot-mail run/boot-all
```

This is required, not tidiness. A boot world's `level.dat` records the mods that created it, so a
directory left over from an earlier layout makes NeoForge log `forestry_apiculture (version
3.0.0-alpha1 -> MISSING)` and load against remembered registry data. The run then tests an upgrade
path rather than the clean install it is supposed to prove.

Run each in turn, waiting for the `Done (Ns)! For help, type "help"` line before stopping it with Ctrl-C:

```bash
./gradlew runCoreOnlyServer
./gradlew runButterfliesServer
./gradlew runFarmsServer
./gradlew runMailServer
./gradlew runAllJarsServer
```

Read for `Done` in each. An absence of errors proves nothing: a server that failed to bind stops before loading a single datapack and logs nothing.

Watch `runFarmsServer` and `runMailServer` most closely. A missing tag entry logs at WARN and a missing loot modifier logs nothing at all, so scan the log for `forestry:` ids reported as unbound rather than waiting for a crash.

- [ ] **Step 4: Run the GameTest suite**

```bash
./gradlew runGameTestServer
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Confirm the working tree holds no leftovers**

```bash
git status --short
ls src/
```

Expected: `git status` clean. `src/` contains exactly `butterflies`, `datagen`, `farms`, `generated`, `mail`, `main`, `test`.

- [ ] **Step 6: Mark the spec implemented**

In `docs/superpowers/specs/2026-08-05-mod-id-rename-merge-design.md`, change the status line to:

```markdown
Status: implemented 2026-08-05
```

Leave the "Successor work" section exactly as written. It describes work that has not started.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "docs: mark the four-jar rename implemented

Verified by a clean build, checkJarPartition and checkResourceFqcn on the
built artifacts, five boot configurations each reaching Done, and the
GameTest suite."
```

---

## Self-Review

**Spec coverage.** Every section of the spec maps to a task: the target table to Tasks 1, 2, 4 and 5; the two simplifications to Task 2 Steps 3 and 6; the `contentPackages` indirection to Task 4 Steps 8 and 9; source moves to Tasks 1, 2 and 4 Step 1; mod metadata to Task 5; the four ownership maps to Tasks 1, 2 and 4 Step 3; naming derivations to Task 5 Step 3; boot configurations to Task 6; regeneration order to Task 3 Steps 1 and 4 and Task 4 Step 6; the verification ladder to Task 7. The spec's three "out of scope" items stay out: no task touches publishing, Java package names, or the historical specs, and Task 5 Step 6 scopes its grep away from `docs/` deliberately.

**Risks.** Both spec risks have a step that exercises them. The silent half of `checkJarPartition` gets Task 4 Step 7 (observe the red) and Step 10 (prove the guard fires). Ownership regenerated from a stale source is prevented by the `&&` ordering in Task 4 Step 6 and the separated steps in Task 3. Farms and mail booting alone for the first time is Task 6 and Task 7 Step 3, which says what to read for rather than assuming a crash.

**One deviation from strict TDD.** This is a build-configuration refactor; there is no unit under test. The verification cycle is the build's own checks, and each task ends with a command and its expected output. Task 4 is the one place a genuine red-green exists, and the plan requires seeing the failure before the fix.

**Type consistency.** `contentModules` and `contentPackages` keep the same names and shapes across Tasks 4, 5, 6 and 7. `contentPackages` is introduced in Task 4 Step 8 and consumed in Step 9 and in Task 5 Step 3. Mod ids are underscored through Task 4 and unqualified from Task 5 onward; every task that names one states which form it expects in its Interfaces block.
