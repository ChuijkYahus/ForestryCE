# Phase 7b: the content jars take their final shape

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Execute move-manifest steps 7.7 through 7.12 - the five content fan-outs and the dissolution of
`forestry.compat` - plus the two packages the manifest never assigned (`forestry.plugin`, and the `core`
residue 7a left behind), leaving every package in the spec's target tree and both boundary gates green.

**Architecture:** Same as 7a. A package move is a textual rewrite of one fully-qualified prefix across the
whole repository, which reaches package declarations, `package-info` annotations, javadoc `{@link}`s,
inline fully-qualified references and the hand-authored resources IntelliJ cannot touch. 7b is mostly
*fan-out* rather than *package move*, and 7a proved that a per-class rewrite has three structural gaps a
package rewrite does not. Task 1 closes all three in tooling rather than leaving them to the compiler.

**Tech Stack:** Java 21, NeoForge 21.1.230, Minecraft 1.21.1, ModDevGradle 2.0.x, Gradle Groovy DSL.
GameTests only, no JUnit.

## Global Constraints

- Comment and Javadoc style is binding; see `CLAUDE.md`. ASCII only. Lowercase `todo`.
- Every task ends with `./gradlew runData` producing **no diff** in `src/generated/resources` and
  `./gradlew runGameTestServer` reporting **all 108 tests passed**. For a pure move the datagen tree must
  be byte-identical; per the spec, "any diff is a real defect", not a rebase artifact.
- Both gates stay green after every task: `checkApiBoundary` clean, `checkBaseBoundary` at
  `0 packaged leaking file(s), 20 datagen-only`, `checkBaseBytecode` clean. Run `checkBaseBytecode`
  after every task, not only at the end.
- **Run `./gradlew compileTestJava`, not just `compileJava`.** `src/test/java` imports moved classes.
- `gradle/base-boundary-baseline.txt` is **not edited in 7b**. Its 20 entries are all under `core/data/`,
  which does not move in this phase. See "Why the baseline does not move" below.
- All source files are LF. Do not write `$`-anchored `sed` patterns.
- **Shell state does not persist between tool calls.** Several steps below open with
  `M=<scratchpad>/move-class.sh` or `P=<scratchpad>/move-package.sh`. Re-set them in every block that
  uses them; do not assume a definition from an earlier step survives.
- **Every new package needs a `package-info.java`.** The template is the fully-qualified form:

  ```java
  @javax.annotation.ParametersAreNonnullByDefault
  @forestry.core.platform.util.FieldsAreNonnullByDefault
  @net.minecraft.MethodsReturnNonnullByDefault
  package forestry.apiculture.bees;
  ```

  22 existing packages have no `package-info.java` (`apiculture/plugin`, `*/tab`, `mail/postalstates`,
  ...). That is pre-existing and out of scope; do not add them. Do not remove them either.
- When a fan-out empties a package, `git rm` its leftover `package-info.java` and let the directory go.

## Starting state, measured 2026-08-02

`checkBaseBoundary`: 0 packaged, 20 datagen-only. `checkBaseBytecode`: clean. 108 GameTests.

| Package | Files | Kind |
| --- | --- | --- |
| `core` residue (Task 2) | 4 | move inside base |
| `forestry.plugin` (Task 3) | 10 | fan-out, 3 destinations |
| `apiculture` (Task 4) | 204 | 3 package moves + 6 fan-outs |
| `arboriculture` (Task 5) | 198 | 1 package move + 4 fan-outs |
| `lepidopterology` (Task 6) | 61 | 2 package moves + 3 fan-outs |
| `farming` + `cultivation` (Task 7) | 120 | package moves only |
| `mail` (Task 8) | 76 | 4 fan-outs |
| `compat` (Task 9) | 30 | 1 package move |

### The collision surface, re-measured for 7b's prefixes

Zero lang keys and zero `src/generated/resources` entries begin with any prefix this phase rewrites.
`grep -rF` counts per rewritten prefix, across `src/main/resources` and `src/generated/resources`:

| Prefix | Hits | What they are |
| --- | --- | --- |
| `forestry.apiculture.{genetics,multiblock,villagers}` | 0 | - |
| `forestry.arboriculture.genetics` | 0 | - |
| `forestry.lepidopterology.genetics` | 0 | - |
| `forestry.cultivation` | 0 | - |
| `forestry.mail.{items,tiles,carriers,postalstates}` | 0 | - |
| `forestry.farming` | 1 | `META-INF/services/forestry.api.plugin.IForestryPlugin` line 5 |
| `forestry.plugin` | 1 | same service file, line 1 |
| `forestry.compat` | 6 | `kubejs.plugins.txt`, the service file, and 4 Patchouli FQCNs |

Two near-misses that are safe because the rewrite is anchored `\bforestry\.x\.y\b`:

- `block.forestry.mailbox` in `en_us.json`. `\bforestry\.mail\b` does not match it - `l` and `b` are
  both word characters, so there is no boundary after `mail`. 7b never rewrites the bare prefix
  `forestry.mail` in any case.
- `for.chat.command.forestry.plugins.*` in `pt_br.json` and `fr_fr.json`, 12 keys. Same reasoning:
  `\bforestry\.plugin\b` does not match `forestry.plugins`.

### Why the baseline does not move

Only three things move *into* a split module in this phase: `DefaultButterflySpecies`,
`TreeAnalyzerPlugin` and `ButterflyAnalyzerPlugin`, all in Task 3. Their base-side consumers were
measured:

- `DefaultButterflySpecies`: `core/data/ButterflySpeciesProvider.java`, **already baselined**;
  `core/data/Data.java` names it only in a comment on line 111, with no import; plus one GameTest, which
  the gate does not scan.
- `TreeAnalyzerPlugin`: one consumer, `arboriculture/client/plugin/ArboricultureClientRegistration.java`.
- `ButterflyAnalyzerPlugin`: one consumer, `lepidopterology/client/plugin/LepidopterologyClientRegistration.java`.

So no base file gains a leak, and no baseline entry stops leaking. If `checkBaseBoundary` reports
anything other than `0 packaged leaking file(s), 20 datagen-only`, stop and diagnose - do not edit the
baseline to make it pass.

## Corrections to the manifest, measured before writing this plan

Steps 7.7 through 7.12 were written 2026-07-31 and six phases have landed since. Every drift below is
folded into the task bodies; this table is the audit trail.

| Manifest claim | Correction |
| --- | --- |
| 7.5 `(?)` `ItemFruit` "goes with arboriculture in 7b" | **Wrong.** `core/features/CoreItems.java:121` instantiates it: `REGISTRY.itemGroup(ItemFruit::new, ...)`. Moving it to arboriculture creates a new *packaged* base leak. It stays base, in `core.platform.item` - the manifest's own alternative |
| Corrections table: the three naturalist chests "belong to content jars, so 7b moves them" | **Wrong.** Each is a 13-line subclass importing only `CoreTiles`, `SpeciesUtil` and `TileNaturalistChest` - all base. Everything else about the chest is base by prior decision: the enum (moved to `core.platform.block` in 7a *to remove five leaks*), `CoreBlocks.NATURALIST_CHEST`, the item, the recipes, the block tags, the blockstate models, `ForestryBewlr`, `ForestryModelLayers`, `RenderNaturalistChest` and the base creative tab. Moving only the tile classes would put a base-registered block's BlockEntity in an optional jar and add two packaged leaks (`CoreTiles`, `ForestryBewlr`) for no gain. They stay base, in `core.platform.tile` next to `TileNaturalistChest` |
| 7.7 apiculture loose files (25) | `FakeBeekeepingLogic` left in phase 6 (`apiimpl.fake`); `ItemScoop`, `ItemBeesWax`, `ItemRefractoryWax` left in phase 2; `VillageHive` no longer exists. `EventHandlerApiculture` and `IApiary` are present and unassigned |
| 7.7 `(?)` `NaturalistChestBlockType` | Resolved and moved in 7a. Not in `apiculture/blocks` any more |
| 7.8 `forestry.arboriculture.capabilities` | Does not exist. Phase 4 moved `SpectacleVision` to base |
| 7.8 arboriculture loose files (13) | `TreeUtil` is present and unassigned |
| 7.10 "`farming.IFarmHousingInternal` and `cultivation.IFarmHousingInternal`" | There is only one, in `cultivation`. No name collision to resolve |
| 7.10 loose `farming` files | Unassigned: `FarmHelper`, `FarmingManager`, `FarmingStage`, `FarmManager`, `FarmTarget`, `FarmWorkStatus`. Also unassigned: `farming/plugin` (5) and `farming/tab` (1) |
| 7.11 `postoffice` gets `IWatchable` | `IWatchable` is in `forestry/api/mail`, not `forestry/mail` |
| 7.12 "`forestry.compat.jei` split per jar" | Nothing per-jar is left in it. Each content jar already has its own `compat/`; `forestry.compat.jei` holds only `JeiUtil` and `IndividualSubtypeInterpreter`, both shared plumbing. All of `forestry.compat` goes to `forestry.core.platform.compat` |
| 7.12 "three Patchouli FQCNs" | There are **four** occurrences of three distinct classes - `FluidComponent` appears in both `carpenter/base.json` and `fabricator/base.json` |

One spec-versus-manifest conflict, resolved in favour of the manifest because it is the more specific
document: the spec's content tree lists `postalstates/` as a surviving mail directory, while manifest
step 7.11 assigns both its files to `letters`. 7b follows the manifest and `mail/postalstates` dies.

---

### Task 1: Tooling - a per-class mover that closes 7a's three gaps, and the FQCN gate

7a moved 81 files individually and hit four failure modes. Three were structural to per-class moves and
are fixed here in tooling; the fourth (`protected` is package-scoped) has no mechanical fix and is caught
by the compiler.

| 7a failure mode | Fix |
| --- | --- |
| Package declarations not rewritten - a class move has no prefix to rewrite | `move-class.sh` rewrites the moved file's own `package` line from its new directory |
| Wildcard imports of the fanning package silently keep resolving to the old package | `expand-wildcard.sh`, run on the 8 measured sites before any fan-out |
| Same-package effect - siblings that stop being siblings lose their implicit import (one missing import cascaded into 754 compile errors in 7a) | `explode-package-imports.sh`, run on the fanning package before the fan-out, makes every intra-package reference an explicit import |

The spec's Verification section also asks for "a grep-based check that every FQCN appearing in a resource
file resolves to an existing class". It has never been written, and 7b is the phase that breaks
hand-authored JSON. Task 1 adds it so that Task 9's Patchouli edit has an oracle.

**Files:**
- Create: `<scratchpad>/move-class.sh`
- Create: `<scratchpad>/explode-package-imports.sh`
- Create: `<scratchpad>/expand-wildcard.sh`
- Modify: `<scratchpad>/rewrite-refs.sh` (guard rewrite)
- Modify: `build.gradle` (add `checkResourceFqcn`, wire into `check`)

The three scripts stay in the scratchpad and are **not** committed, matching 7a.

**Interfaces:**
- Produces: `move-class.sh <from-fqcn> <to-fqcn>`; `explode-package-imports.sh <package>`;
  `expand-wildcard.sh <file> <package>`; Gradle task `checkResourceFqcn`.
- Consumes: `rewrite-refs.sh <from> <to>` from 7a, unchanged in behavior.

- [ ] **Step 1: Harden `rewrite-refs.sh`'s guards**

7a's `|| true` fix stopped `grep`'s no-match exit from aborting the script, but the two
`[ -n "$files" ] && ...` lines are still `&&`-lists: when the variable is empty the whole list returns
1, and under `set -e` that aborts. It has not fired yet only because no move so far has had zero
matches. A fan-out has many single-reference classes, so it will. Rewrite both as `if` blocks.

Replace the two guard lines in `<scratchpad>/rewrite-refs.sh`:

```bash
files=$(grep -rlF "$from" src/main/java src/test/java --include='*.java' 2>/dev/null || true)
if [ -n "$files" ]; then printf '%s\n' "$files" | xargs -r sed -i "s@\\b${esc_from}\\b@${to}@g"; fi

res=$(grep -rlF "$from" src/main/resources 2>/dev/null || true)
if [ -n "$res" ]; then printf '%s\n' "$res" | xargs -r sed -i "s@\\b${esc_from}\\b@${to}@g"; fi
```

- [ ] **Step 2: Write `move-class.sh`**

```bash
#!/usr/bin/env bash
# Move one class to another package and rewrite every reference to it, repo-wide.
# Unlike move-package.sh there is no directory prefix to rewrite, so this fixes the moved
# file's own package declaration explicitly. That gap cost 81 hand repairs in phase 7a.
set -euo pipefail
from="$1"; to="$2"
fromfile="src/main/java/${from//.//}.java"
tofile="src/main/java/${to//.//}.java"
topkg="${to%.*}"

if [ ! -f "$fromfile" ]; then echo "no such class: $fromfile" >&2; exit 1; fi
if [ -e "$tofile" ]; then echo "destination exists: $tofile" >&2; exit 1; fi

mkdir -p "$(dirname "$tofile")"
git mv "$fromfile" "$tofile"
"$(dirname "$0")/rewrite-refs.sh" "$from" "$to"
sed -i "0,/^package .*;/s@^package .*;@package ${topkg};@" "$tofile"
echo "moved $from -> $to"
```

- [ ] **Step 3: Write `explode-package-imports.sh`**

```bash
#!/usr/bin/env bash
# Make every intra-package reference in <package> an explicit import.
# A class in package P referring to a sibling needs no import. When the two land in different
# packages the reference stops compiling, and the failure cascades: in phase 7a one such case
# (ItemFruit losing ItemForestryFood) produced 754 errors. Running this first turns the fan-out
# into a pure per-class prefix rewrite with no same-package effect at all.
# Redundant imports are the cost: a sibling named only in a comment gets an unused import. That
# is harmless, and this only ever edits files inside <package>, never a base file.
set -euo pipefail
pkg="$1"
dir="src/main/java/${pkg//.//}"
esc="${pkg//./\\.}"
if [ ! -d "$dir" ]; then echo "no such package: $dir" >&2; exit 1; fi

mapfile -t classes < <(find "$dir" -maxdepth 1 -name '*.java' ! -name 'package-info.java' \
    -printf '%f\n' | sed 's/\.java$//' | sort)

for f in "$dir"/*.java; do
  self="$(basename "$f" .java)"
  if [ "$self" = "package-info" ]; then continue; fi
  for c in "${classes[@]}"; do
    if [ "$c" = "$self" ]; then continue; fi
    if grep -q "^import ${esc}\.${c};" "$f"; then continue; fi
    if ! grep -v '^import \|^package ' "$f" | grep -qE "\b${c}\b"; then continue; fi
    last=$(grep -n '^import ' "$f" | tail -1 | cut -d: -f1 || true)
    if [ -z "$last" ]; then last=$(grep -n '^package ' "$f" | head -1 | cut -d: -f1); fi
    sed -i "${last}a import ${pkg}.${c};" "$f"
  done
done
echo "exploded $pkg"
```

- [ ] **Step 4: Write `expand-wildcard.sh`**

```bash
#!/usr/bin/env bash
# Replace "import <package>.*;" in one file with an explicit import per class actually used.
# A wildcard import survives a fan-out silently: it keeps resolving to the now-emptied package
# instead of following the classes, so the compiler reports missing symbols far from the cause.
set -euo pipefail
file="$1"; pkg="$2"
dir="src/main/java/${pkg//.//}"
esc="${pkg//./\\.}"
if ! grep -q "^import ${esc}\.\*;" "$file"; then echo "no wildcard import of $pkg in $file" >&2; exit 1; fi

added=""
for c in $(find "$dir" -maxdepth 1 -name '*.java' ! -name 'package-info.java' \
    -printf '%f\n' | sed 's/\.java$//' | sort); do
  if grep -v '^import ' "$file" | grep -qE "\b${c}\b"; then added="${added}import ${pkg}.${c};"$'\n'; fi
done

ln=$(grep -n "^import ${esc}\.\*;" "$file" | head -1 | cut -d: -f1)
tmp=$(mktemp)
awk -v ln="$ln" -v repl="$added" 'NR==ln{printf "%s", repl; next} {print}' "$file" > "$tmp"
mv "$tmp" "$file"
echo "expanded $pkg in $file"
```

- [ ] **Step 5: Make the three scripts executable and smoke-test them**

```bash
chmod +x <scratchpad>/move-class.sh <scratchpad>/explode-package-imports.sh <scratchpad>/expand-wildcard.sh
git stash list  # must be empty; these tests are done on a clean tree and reverted
<scratchpad>/explode-package-imports.sh forestry.mail
git diff --stat
```

Expected: a handful of `import forestry.mail.X;` lines added inside `src/main/java/forestry/mail/`,
nothing outside it. Then:

```bash
./gradlew compileJava --console=plain -q && echo COMPILES
git checkout -- src/main/java/forestry/mail
```

Expected: `COMPILES`, then a clean tree. The point of the smoke test is that exploding imports is a
no-op semantically.

- [ ] **Step 6: Add `checkResourceFqcn` to `build.gradle`**

Insert after the `checkBaseBytecode` block (currently ending at `build.gradle:262`):

```groovy
// The spec's Verification section asks for this and phase 7b is what makes it necessary: the
// Patchouli book templates, kubejs.plugins.txt and META-INF/services all name Java classes by
// fully qualified name, in hand-authored files that neither the datagen diff nor the GameTests
// can see. A move that forgets one of them fails silently at runtime.
var checkResourceFqcn = tasks.register('checkResourceFqcn') {
	group = 'verification'
	description = 'Fails if a resource file names a forestry class that does not exist'

	var javaDir = project.file('src/main/java')
	var resourceDirs = [project.file('src/main/resources'), project.file('src/generated/resources')]

	inputs.dir(javaDir)
	outputs.upToDateWhen { false }

	doLast {
		// A dotted run starting at "forestry"; the class is the first segment that starts with an
		// uppercase letter, so lang keys such as "block.forestry.mailbox" never match
		var token = ~/forestry(?:\.[A-Za-z_$][A-Za-z0-9_$]*)+/
		var missing = new TreeSet<String>()
		var checked = 0

		var check = { String fqcn, String where ->
			var parts = fqcn.split('\\.')
			var end = -1
			for (int i = 1; i < parts.length; i++) {
				if (Character.isUpperCase(parts[i].charAt(0) as char)) { end = i; break }
			}
			if (end < 0) {
				return
			}
			checked++
			var path = parts[0..end].join('/') + '.java'
			if (!new File(javaDir, path).exists()) {
				missing.add("${fqcn} (${where})")
			}
		}

		resourceDirs.each { dir ->
			if (!dir.exists()) {
				return
			}
			dir.eachFileRecurse(groovy.io.FileType.FILES) { File file ->
				// service files name the service in the file name itself
				if (file.parentFile.name == 'services') {
					check(file.name, file.name)
				}
				if (file.length() > 4_000_000) {
					return
				}
				var text
				try {
					text = file.getText('UTF-8')
				} catch (ignored) {
					return
				}
				var m = token.matcher(text)
				while (m.find()) {
					check(m.group(), dir.toPath().relativize(file.toPath()).toString())
				}
			}
		}

		if (!missing.isEmpty()) {
			throw new GradleException(
					"Resource file(s) name a forestry class that does not exist:\n  "
					+ missing.join('\n  ')
					+ "\nA package move must update hand-authored JSON, service files and kubejs.plugins.txt.")
		}
		logger.lifecycle("checkResourceFqcn: ${checked} resource-borne class name(s) all resolve")
	}
}
```

Then add it to the `check` wiring next to the existing `dependsOn checkBaseBytecode` at
`build.gradle:266`:

```groovy
	dependsOn checkResourceFqcn
```

- [ ] **Step 7: Run the new gate and confirm it sees the classes it must protect**

```bash
./gradlew checkResourceFqcn --console=plain
```

Expected: `checkResourceFqcn: 17 resource-borne class name(s) all resolve`, and `BUILD SUCCESSFUL`.
The 17 are 4 service-file names, 9 service-file contents (`ForestryApiImpl`, `ForestryClientApiImpl`,
`DefaultForestryPlugin`, the four content plugins, `KubeForestryPlugin`, `ClientHelper`), 1 kubejs
plugin and 3 distinct Patchouli class names. If the count is lower than 17 the regex is not reaching the
Patchouli templates - fix that before proceeding, because Task 9 depends on it.

- [ ] **Step 8: Prove the gate actually fails**

```bash
sed -i 's/forestry\.compat\.patchouli\.component\.FluidComponent/forestry.compat.patchouli.component.NoSuchComponent/' \
  src/main/resources/assets/forestry/patchouli_books/foresters_manual/en_us/templates/carpenter/base.json
./gradlew checkResourceFqcn --console=plain
```

Expected: `BUILD FAILED` naming `forestry.compat.patchouli.component.NoSuchComponent`. Then revert:

```bash
git checkout -- src/main/resources/assets/forestry/patchouli_books/foresters_manual/en_us/templates/carpenter/base.json
```

A gate that has never been seen to fail is not a gate.

- [ ] **Step 9: Commit**

```bash
git add build.gradle
git commit -m "build: check that resource-borne class names resolve"
```

---

### Task 2: The `core` residue 7a left behind

7a emptied `core/blocks` and `core/items/definitions` down to their `package-info.java`, and left four
real classes behind because their destination was still open. Both are now settled (see the corrections
table). This is a move inside base: nothing crosses a jar boundary, both gates must stay at their
current numbers.

**Files:**
- Move: `core/tiles/Tile{Apiarist,Arborist,Lepidopterist}Chest.java` -> `core/platform/tile/`
- Move: `core/items/ItemFruit.java` -> `core/platform/item/`
- Delete: `core/blocks/package-info.java`, `core/items/definitions/package-info.java`,
  `core/items/package-info.java`, `core/tiles/package-info.java`

**Interfaces:**
- Produces: `forestry.core.platform.tile.Tile{Apiarist,Arborist,Lepidopterist}Chest`,
  `forestry.core.platform.item.ItemFruit` (and its nested `ItemFruit.EnumFruit`).

- [ ] **Step 1: Explode intra-package imports in the two fanning packages**

`core/tiles` has four files and three leave for the same destination, so there is no same-package
effect - but `TileNaturalistChest` already lives in `core.platform.tile`, which is where they are going,
so their `import forestry.core.platform.tile.TileNaturalistChest;` becomes a same-package import. That
is legal Java and is left alone.

```bash
<scratchpad>/explode-package-imports.sh forestry.core.tiles
<scratchpad>/explode-package-imports.sh forestry.core.items
```

- [ ] **Step 2: Expand the two wildcard imports of the fanning packages**

Measured sites: `core/features/CoreTiles.java:6` (`import forestry.core.tiles.*;`),
`core/platform/render/ForestryBewlr.java:8` (same), `core/features/CoreItems.java:10`
(`import forestry.core.items.*;`).

```bash
<scratchpad>/expand-wildcard.sh src/main/java/forestry/core/features/CoreTiles.java forestry.core.tiles
<scratchpad>/expand-wildcard.sh src/main/java/forestry/core/platform/render/ForestryBewlr.java forestry.core.tiles
<scratchpad>/expand-wildcard.sh src/main/java/forestry/core/features/CoreItems.java forestry.core.items
```

- [ ] **Step 3: Move the four classes**

```bash
for c in TileApiaristChest TileArboristChest TileLepidopteristChest; do
  <scratchpad>/move-class.sh forestry.core.tiles.$c forestry.core.platform.tile.$c
done
<scratchpad>/move-class.sh forestry.core.items.ItemFruit forestry.core.platform.item.ItemFruit
```

- [ ] **Step 4: Remove the four emptied packages**

```bash
git rm src/main/java/forestry/core/tiles/package-info.java \
       src/main/java/forestry/core/items/package-info.java \
       src/main/java/forestry/core/items/definitions/package-info.java \
       src/main/java/forestry/core/blocks/package-info.java
ls src/main/java/forestry/core
```

Expected listing: `content data engine features ModuleCore.java ModuleFluids.java package-info.java
platform`. Nothing else.

- [ ] **Step 5: Compile, gate, verify**

```bash
./gradlew compileJava compileTestJava checkApiBoundary checkBaseBoundary checkBaseBytecode checkResourceFqcn --console=plain
```

Expected: `BUILD SUCCESSFUL`, `checkBaseBoundary: 0 packaged leaking file(s), 20 datagen-only`,
`checkBaseBytecode: no packaged base class references a split module`.

- [ ] **Step 6: Oracles**

```bash
./gradlew runData --console=plain && git diff --stat src/generated/resources
./gradlew runGameTestServer --console=plain 2>&1 | tail -20
```

Expected: empty diff; `All 108 required tests passed`.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "core: the last four classes find their layer"
```

---

### Task 3: Dissolve `forestry.plugin`

The manifest never assigned this package; the spec says it "does not survive as a base package" and
expected phase 5 to empty it. Phase 5 moved seven of its classes and left ten.

Destinations, with the measurement each rests on:

| File | To | Why |
| --- | --- | --- |
| `BeeTaxonomy`, `TreeTaxonomy`, `ButterflyTaxonomy` | `core.data.taxonomy` | Sole consumer is `ForestryTaxonomy`, whose sole consumer is `core/data/TaxonProvider`. They are datagen input, not runtime content. Each imports only `forestry.api.*`, so none leaks |
| `ForestryTaxonomy` | `core.data.taxonomy` | Consumed by `TaxonProvider` and named once in a `DefaultForestryPlugin` comment. Its own javadoc says the runtime loads the taxonomy from generated JSON, not from this class |
| `DefaultButterflySpecies` | `lepidopterology.plugin` | Runtime consumer `LepidopterologyForestryPlugin`, so it is content, not datagen |
| `DefaultForestryPlugin` | `core.plugin` | Base's own `IForestryPlugin`, symmetric with `apiculture.plugin`, `arboriculture.plugin`, `lepidopterology.plugin` and `farming.plugin` |
| `client/TreeAnalyzerPlugin` | `arboriculture.client.plugin` | Sole consumer `ArboricultureClientRegistration` |
| `client/ButterflyAnalyzerPlugin` | `lepidopterology.client.plugin` | Sole consumer `LepidopterologyClientRegistration` |

`core.data.taxonomy` rather than the three content jars is deliberate. Sending `BeeTaxonomy` to
apiculture would make `ForestryTaxonomy` import all three split modules, adding a new `core/data` entry
to a baseline whose header says it never grows - for no benefit, since phase 8 partitions the whole of
`core/data` per jar anyway and would carry these four along with it. Record this in the spec so phase 8
does not have to rediscover it.

**Files:**
- Create: `core/data/taxonomy/package-info.java`, `core/plugin/package-info.java`
- Move: 8 classes as tabled above
- Delete: `plugin/package-info.java`, `plugin/client/package-info.java`
- Modify (mechanically, by `rewrite-refs.sh`): `META-INF/services/forestry.api.plugin.IForestryPlugin`

**Interfaces:**
- Produces: `forestry.core.plugin.DefaultForestryPlugin` (its `ID` constant is read by all four content
  plugins, `PluginManager`, and two `core/data` providers),
  `forestry.core.data.taxonomy.ForestryTaxonomy#buildDefaultTaxa`,
  `forestry.lepidopterology.plugin.DefaultButterflySpecies`.

- [ ] **Step 1: Create the two new packages**

`src/main/java/forestry/core/data/taxonomy/package-info.java`:

```java
@javax.annotation.ParametersAreNonnullByDefault
@forestry.core.platform.util.FieldsAreNonnullByDefault
@net.minecraft.MethodsReturnNonnullByDefault
package forestry.core.data.taxonomy;
```

`src/main/java/forestry/core/plugin/package-info.java`:

```java
@javax.annotation.ParametersAreNonnullByDefault
@forestry.core.platform.util.FieldsAreNonnullByDefault
@net.minecraft.MethodsReturnNonnullByDefault
package forestry.core.plugin;
```

- [ ] **Step 2: Explode intra-package imports**

`forestry.plugin` has four classes that reference each other (`ForestryTaxonomy` names all three
taxonomies) and they do not all land in the same place.

```bash
<scratchpad>/explode-package-imports.sh forestry.plugin
git diff --stat src/main/java/forestry/plugin
```

Expected: imports added to `ForestryTaxonomy.java` only. There are no wildcard imports of
`forestry.plugin` anywhere in the tree, so no `expand-wildcard.sh` call is needed here.

- [ ] **Step 3: Move the eight classes**

```bash
for c in BeeTaxonomy TreeTaxonomy ButterflyTaxonomy ForestryTaxonomy; do
  <scratchpad>/move-class.sh forestry.plugin.$c forestry.core.data.taxonomy.$c
done
<scratchpad>/move-class.sh forestry.plugin.DefaultForestryPlugin forestry.core.plugin.DefaultForestryPlugin
<scratchpad>/move-class.sh forestry.plugin.DefaultButterflySpecies forestry.lepidopterology.plugin.DefaultButterflySpecies
<scratchpad>/move-class.sh forestry.plugin.client.TreeAnalyzerPlugin forestry.arboriculture.client.plugin.TreeAnalyzerPlugin
<scratchpad>/move-class.sh forestry.plugin.client.ButterflyAnalyzerPlugin forestry.lepidopterology.client.plugin.ButterflyAnalyzerPlugin
```

- [ ] **Step 4: Remove the emptied package**

```bash
git rm src/main/java/forestry/plugin/package-info.java src/main/java/forestry/plugin/client/package-info.java
test ! -d src/main/java/forestry/plugin && echo "forestry.plugin is gone"
```

- [ ] **Step 5: Verify the service file was rewritten**

```bash
cat src/main/resources/META-INF/services/forestry.api.plugin.IForestryPlugin
```

Expected line 1: `forestry.core.plugin.DefaultForestryPlugin`. The other five lines are unchanged.
This is the check that would have caught the `pipefail` bug 7a shipped with.

- [ ] **Step 6: Compile, gate, verify**

```bash
./gradlew compileJava compileTestJava checkApiBoundary checkBaseBoundary checkBaseBytecode checkResourceFqcn --console=plain
```

Expected: `BUILD SUCCESSFUL`; `checkBaseBoundary: 0 packaged leaking file(s), 20 datagen-only`;
`checkResourceFqcn: 13 resource-borne class name(s) all resolve`.

- [ ] **Step 7: Oracles**

```bash
./gradlew runData --console=plain && git diff --stat src/generated/resources
./gradlew runGameTestServer --console=plain 2>&1 | tail -20
```

Expected: empty diff; `All 108 required tests passed`. The datagen diff is the load-bearing oracle
here: `TaxonProvider` writes 40+ `data/forestry/taxon/*.json` files from `ForestryTaxonomy`, so any
damage to the taxonomy classes shows up immediately.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "plugin: dissolve the package, taxonomy to datagen"
```

---

### Task 4: Step 7.7 - apiculture fan-out

The largest task in the phase: three package moves and six fan-outs across 204 files, producing five new
feature packages (`bees`, `apiary`, `beehouse`, `alveary`, `apiarist`) alongside the existing `hives`.

**Files:** everything under `src/main/java/forestry/apiculture/`.

**Interfaces:**
- Produces: `forestry.apiculture.{bees,apiary,beehouse,alveary,apiarist,hives}.*`,
  `forestry.apiculture.bees.genetics.*`, `forestry.apiculture.alveary.multiblock.*`,
  `forestry.apiculture.apiarist.villagers.ApicultureVillagers`.
- `apiculture/{features,network,particles,render,models,entities,commands,compat,recipes,proxy,worldgen,plugin,client,tab}`
  stay at jar level per D6 and are untouched except for rewritten imports.

- [ ] **Step 1: Three package moves**

```bash
<scratchpad>/move-package.sh forestry.apiculture.genetics    forestry.apiculture.bees.genetics
<scratchpad>/move-package.sh forestry.apiculture.multiblock  forestry.apiculture.alveary.multiblock
<scratchpad>/move-package.sh forestry.apiculture.villagers   forestry.apiculture.apiarist.villagers
./gradlew compileJava --console=plain -q && echo COMPILES
```

These are pure prefix rewrites and must compile with no follow-up. `apiculture.genetics.effects` moves
with its parent. `ApicultureBeeEffectTypes`' wildcard `import forestry.apiculture.genetics.effects.*;`
and `ApicultureTiles`' `import forestry.apiculture.multiblock.*;` are rewritten in place by the prefix
rule and stay wildcards, which is correct - those packages moved whole.

- [ ] **Step 2: Create the five new feature packages**

Write a `package-info.java` for each of `bees`, `apiary`, `beehouse`, `alveary`, `apiarist` using the
Global Constraints template, substituting the package name. `hives` already has one. `bees` and
`alveary` and `apiarist` directories now exist from step 1, but only as parents - they still need their
own `package-info.java`.

```bash
ls src/main/java/forestry/apiculture/{bees,alveary,apiarist,apiary,beehouse}/package-info.java
```

Expected: all five present.

- [ ] **Step 3: Explode intra-package imports in the six fanning packages**

```bash
for p in forestry.apiculture forestry.apiculture.blocks forestry.apiculture.tiles \
         forestry.apiculture.items forestry.apiculture.gui forestry.apiculture.inventory; do
  <scratchpad>/explode-package-imports.sh $p
done
./gradlew compileJava --console=plain -q && echo COMPILES
```

`COMPILES` before any class has moved is the whole point: exploding imports is semantically a no-op.

- [ ] **Step 4: Expand the four wildcard imports of fanning packages**

Measured sites (`apiculture.multiblock` and `apiculture.genetics.effects` are excluded - they moved
whole in step 1, so their wildcards are already correct):

```bash
<scratchpad>/expand-wildcard.sh src/main/java/forestry/apiculture/features/ApicultureBlocks.java    forestry.apiculture.blocks
<scratchpad>/expand-wildcard.sh src/main/java/forestry/apiculture/features/ApicultureItems.java     forestry.apiculture.items
<scratchpad>/expand-wildcard.sh src/main/java/forestry/apiculture/features/ApicultureMenuTypes.java forestry.apiculture.gui
<scratchpad>/expand-wildcard.sh src/main/java/forestry/apiculture/proxy/ApicultureClientHandler.java forestry.apiculture.gui
./gradlew compileJava --console=plain -q && echo COMPILES
```

- [ ] **Step 5: Fan out the 24 loose files**

```bash
M=<scratchpad>/move-class.sh
for c in BeeSpecies BeekeepingLogic BeeHousingListener BeeHousingModifier InventoryBeeHousing \
         HasFlowersCache SingleActivityType CathemeralActivityType CrepuscularActivityType \
         TagFlowerType WaterTagFlowerType PhotosynthesisFlowerType \
         ApicultureFilterRule ApicultureFilterRuleType; do
  $M forestry.apiculture.$c forestry.apiculture.bees.$c
done
for c in IApiary ApiaryBeeListener ApiaryBeeModifier; do
  $M forestry.apiculture.$c forestry.apiculture.apiary.$c
done
$M forestry.apiculture.BeehouseBeeModifier    forestry.apiculture.beehouse.BeehouseBeeModifier
$M forestry.apiculture.AlvearyBeeModifier     forestry.apiculture.alveary.AlvearyBeeModifier
$M forestry.apiculture.WorldgenBeekeepingLogic forestry.apiculture.hives.WorldgenBeekeepingLogic
$M forestry.apiculture.ApiaristAI             forestry.apiculture.apiarist.ApiaristAI
$M forestry.apiculture.ArmorApiaristHelper    forestry.apiculture.apiarist.ArmorApiaristHelper
```

`ModuleApiculture` and `EventHandlerApiculture` stay at `forestry.apiculture`. `EventHandlerApiculture`
is not in the manifest; it stays at the root by symmetry with `core.platform.EventHandlerCore`.

- [ ] **Step 6: Fan out `blocks`, `tiles`, `items`, `gui`, `inventory`**

```bash
$M forestry.apiculture.blocks.BlockAlveary        forestry.apiculture.alveary.BlockAlveary
$M forestry.apiculture.blocks.BlockApiculture     forestry.apiculture.apiary.BlockApiculture
$M forestry.apiculture.blocks.BlockTypeApiculture forestry.apiculture.apiary.BlockTypeApiculture
$M forestry.apiculture.blocks.BlockBeeHive        forestry.apiculture.hives.BlockBeeHive
$M forestry.apiculture.blocks.BlockHiveType       forestry.apiculture.hives.BlockHiveType
$M forestry.apiculture.blocks.BlockHoneyComb      forestry.apiculture.bees.BlockHoneyComb

$M forestry.apiculture.tiles.TileBeeHousingBase      forestry.apiculture.bees.TileBeeHousingBase
$M forestry.apiculture.tiles.FakeBeeHousingInventory forestry.apiculture.bees.FakeBeeHousingInventory
$M forestry.apiculture.tiles.TileApiary              forestry.apiculture.apiary.TileApiary
$M forestry.apiculture.tiles.TileBeeHouse            forestry.apiculture.beehouse.TileBeeHouse
$M forestry.apiculture.tiles.TileHive                forestry.apiculture.hives.TileHive
$M forestry.apiculture.tiles.HiveBeeHousingInventory forestry.apiculture.hives.HiveBeeHousingInventory

for c in ItemBeeGE EnumHoneyComb ItemHoneyComb ItemBlockHoneyComb EnumPollenCluster \
         ItemPollenCluster EnumPropolis ItemPropolis ItemAmbrosia; do
  $M forestry.apiculture.items.$c forestry.apiculture.bees.$c
done
$M forestry.apiculture.items.ItemHiveFrame         forestry.apiculture.apiary.ItemHiveFrame
$M forestry.apiculture.items.ItemCreativeHiveFrame forestry.apiculture.apiary.ItemCreativeHiveFrame
$M forestry.apiculture.items.ItemArmorApiarist     forestry.apiculture.apiarist.ItemArmorApiarist
$M forestry.apiculture.items.ItemSmoker            forestry.apiculture.apiarist.ItemSmoker

for c in ContainerAlveary ContainerAlvearyHygroregulator ContainerAlvearySieve ContainerAlvearySwarmer \
         GuiAlveary GuiAlvearyHygroregulator GuiAlvearySieve GuiAlvearySwarmer; do
  $M forestry.apiculture.gui.$c forestry.apiculture.alveary.$c
done
for c in ContainerBeeHousing GuiBeeHousing IContainerBeeHousing IGuiBeeHousingDelegate ContainerBeeHelper; do
  $M forestry.apiculture.gui.$c forestry.apiculture.bees.$c
done

for c in InventoryAlvearySieve InventoryHygroregulator InventorySwarmer; do
  $M forestry.apiculture.inventory.$c forestry.apiculture.alveary.$c
done
$M forestry.apiculture.inventory.IApiaryInventory forestry.apiculture.apiary.IApiaryInventory
$M forestry.apiculture.inventory.InventoryApiary  forestry.apiculture.apiary.InventoryApiary
```

- [ ] **Step 7: Remove the five emptied packages**

```bash
git rm src/main/java/forestry/apiculture/{blocks,tiles,items,gui,inventory}/package-info.java
ls src/main/java/forestry/apiculture
```

Expected: `alveary apiarist apiary bees beehouse client commands compat entities EventHandlerApiculture.java
features hives models ModuleApiculture.java network package-info.java particles plugin proxy recipes render
tab worldgen`.

- [ ] **Step 8: Compile and repair**

```bash
./gradlew compileJava compileTestJava --console=plain
```

If this fails, the remaining cause is 7a's fourth failure mode, which no script can prevent: `protected`
members are package-scoped, so a screen that read `container.someProtectedField` across what used to be
one package now cannot. Fix by using the existing public accessor, exactly as
`GuiAnalyzer`/`PortableAnalyzerScreen` were fixed in 7a. Do **not** widen a field's visibility, and do
**not** move a class to make the error go away.

- [ ] **Step 9: Gate and verify**

```bash
./gradlew checkApiBoundary checkBaseBoundary checkBaseBytecode checkResourceFqcn --console=plain
./gradlew runData --console=plain && git diff --stat src/generated/resources
./gradlew runGameTestServer --console=plain 2>&1 | tail -20
```

Expected: gates unchanged at `0 packaged / 20 datagen-only`; empty datagen diff;
`All 108 required tests passed`.

- [ ] **Step 10: Prove nothing was left behind**

```bash
grep -rn 'forestry\.apiculture\.\(blocks\|tiles\|items\|gui\|inventory\|genetics\|multiblock\|villagers\)\b' \
  src/main/java src/test/java src/main/resources || echo "no stale apiculture references"
```

Expected: `no stale apiculture references`.

- [ ] **Step 11: Commit**

```bash
git add -A
git commit -m "apiculture: bees, apiary, beehouse, alveary, hives, apiarist"
```

---

### Task 5: Step 7.8 - arboriculture fan-out

One package move and four fan-outs across 198 files, producing `trees`, `wood`, `leaves`, `fruit`,
`sapling` and growing the existing `charcoal`.

**Interfaces:**
- Produces: `forestry.arboriculture.{trees,wood,leaves,fruit,sapling,charcoal}.*`,
  `forestry.arboriculture.trees.genetics.*`.
- `arboriculture/worldgen` (62 files) stays at jar level per the manifest - it is tree generation
  throughout. `features`, `network`, `client`, `models`, `commands`, `loot`, `entities`, `villagers`,
  `compat`, `plugin`, `tab` likewise.
- `forestry.arboriculture.capabilities` does not exist; phase 4 moved `SpectacleVision` to base. Skip
  that manifest row.

- [ ] **Step 1: One package move**

```bash
<scratchpad>/move-package.sh forestry.arboriculture.genetics forestry.arboriculture.trees.genetics
./gradlew compileJava --console=plain -q && echo COMPILES
```

- [ ] **Step 2: Create the five new feature packages**

`package-info.java` for `trees`, `wood`, `leaves`, `fruit`, `sapling`, from the Global Constraints
template. `charcoal` already has one.

- [ ] **Step 3: Explode intra-package imports in the four fanning packages**

```bash
for p in forestry.arboriculture forestry.arboriculture.blocks forestry.arboriculture.items \
         forestry.arboriculture.tiles; do
  <scratchpad>/explode-package-imports.sh $p
done
./gradlew compileJava --console=plain -q && echo COMPILES
```

- [ ] **Step 4: Expand the four wildcard imports of fanning packages**

Two of these are **base** files carrying a wildcard import of `forestry.arboriculture.blocks`. Both are
already in `gradle/base-boundary-baseline.txt`, so expanding them cannot change the gate - but check the
gate after this step rather than assuming.

```bash
<scratchpad>/expand-wildcard.sh src/main/java/forestry/arboriculture/features/ArboricultureBlocks.java forestry.arboriculture.blocks
<scratchpad>/expand-wildcard.sh src/main/java/forestry/arboriculture/features/ArboricultureBlocks.java forestry.arboriculture.items
<scratchpad>/expand-wildcard.sh src/main/java/forestry/core/data/ForestryBlockLootTables.java           forestry.arboriculture.blocks
<scratchpad>/expand-wildcard.sh src/main/java/forestry/core/data/models/ForestryWoodModelProvider.java  forestry.arboriculture.blocks
./gradlew compileJava --console=plain -q && ./gradlew checkBaseBoundary --console=plain
```

Expected: still `0 packaged leaking file(s), 20 datagen-only`.

`ArboricultureClientHandler`'s `import forestry.arboriculture.models.*;` and `DefaultTreeSpecies`'
`import forestry.arboriculture.worldgen.*;` are **not** expanded - neither package fans out.

- [ ] **Step 5: Fan out the 14 loose files**

```bash
M=<scratchpad>/move-class.sh
for c in TreeSpecies TreeManager ArboricultureFilterRuleType TreeUtil; do
  $M forestry.arboriculture.$c forestry.arboriculture.trees.$c
done
for c in ForestryWoodType VanillaWoodType IWoodTyped WoodAccess WoodHelper; do
  $M forestry.arboriculture.$c forestry.arboriculture.wood.$c
done
for c in Fruit DummyFruit PodFruit RipeningFruit; do
  $M forestry.arboriculture.$c forestry.arboriculture.fruit.$c
done
```

`TreeUtil` is not in the manifest. It resolves leaf pollination through `SpeciesUtil` and `TileLeaves`;
`trees` is its subject, so it goes with `TreeSpecies` and `TreeManager`. `ModuleArboriculture` stays at
the root.

- [ ] **Step 6: Fan out `blocks` (29 files)**

```bash
M=<scratchpad>/move-class.sh
for c in BlockForestryLog BlockForestryPlank BlockForestrySlab BlockForestryStairs BlockForestryFence \
         BlockForestryFenceGate BlockForestryDoor BlockForestryTrapdoor BlockForestryButton \
         BlockForestryPressurePlate BlockForestryStandingSign BlockForestryWallSign \
         BlockForestryHangingSign BlockForestryWallHangingSign; do
  $M forestry.arboriculture.blocks.$c forestry.arboriculture.wood.$c
done
for c in BlockAbstractLeaves BlockForestryLeaves BlockDefaultLeaves BlockDefaultLeavesFruit \
         BlockDecorativeLeaves BlockExtendedLeaves ForestryLeafType ILeafTypeBlock; do
  $M forestry.arboriculture.blocks.$c forestry.arboriculture.leaves.$c
done
$M forestry.arboriculture.blocks.BlockFruitPod    forestry.arboriculture.fruit.BlockFruitPod
$M forestry.arboriculture.blocks.ForestryPodType  forestry.arboriculture.fruit.ForestryPodType
$M forestry.arboriculture.blocks.BlockSapling     forestry.arboriculture.sapling.BlockSapling
for c in BlockCharcoal BlockAsh LogPileBlock DecorativeLogPileBlock; do
  $M forestry.arboriculture.blocks.$c forestry.arboriculture.charcoal.$c
done
```

- [ ] **Step 7: Fan out `items` (12) and `tiles` (4)**

```bash
M=<scratchpad>/move-class.sh
for c in ItemBlockWood ItemBlockWoodDoor ItemBlockWoodSlab ItemBlockSign ItemBlockHangingSign \
         ItemForestryBoat ForestryBoatDispenserBehavior; do
  $M forestry.arboriculture.items.$c forestry.arboriculture.wood.$c
done
for c in ItemBlockLeaves ItemBlockDecorativeLeaves ItemBlockDefaultLeaves; do
  $M forestry.arboriculture.items.$c forestry.arboriculture.leaves.$c
done
$M forestry.arboriculture.items.TreeItem   forestry.arboriculture.trees.TreeItem
$M forestry.arboriculture.items.GrafterItem forestry.arboriculture.trees.GrafterItem

$M forestry.arboriculture.tiles.TileFruitPod      forestry.arboriculture.fruit.TileFruitPod
$M forestry.arboriculture.tiles.TileLeaves        forestry.arboriculture.leaves.TileLeaves
$M forestry.arboriculture.tiles.TileSapling       forestry.arboriculture.sapling.TileSapling
$M forestry.arboriculture.tiles.TileTreeContainer forestry.arboriculture.trees.TileTreeContainer
```

- [ ] **Step 8: Remove the three emptied packages**

```bash
git rm src/main/java/forestry/arboriculture/{blocks,items,tiles}/package-info.java
```

- [ ] **Step 9: Compile, repair, gate, verify**

```bash
./gradlew compileJava compileTestJava --console=plain
./gradlew checkApiBoundary checkBaseBoundary checkBaseBytecode checkResourceFqcn --console=plain
./gradlew runData --console=plain && git diff --stat src/generated/resources
./gradlew runGameTestServer --console=plain 2>&1 | tail -20
```

Expected: gates unchanged; empty datagen diff; `All 108 required tests passed`. The datagen diff is the
strongest oracle in this task - `ForestryWoodModelProvider` and `ForestryBlockStateProvider` emit
several hundred wood and leaf models.

- [ ] **Step 10: Prove nothing was left behind**

```bash
grep -rn 'forestry\.arboriculture\.\(blocks\|items\|tiles\|genetics\)\b' \
  src/main/java src/test/java src/main/resources || echo "no stale arboriculture references"
```

- [ ] **Step 11: Commit**

```bash
git add -A
git commit -m "arboriculture: trees, wood, leaves, fruit, sapling, charcoal"
```

---

### Task 6: Step 7.9 - lepidopterology fan-out

**Interfaces:**
- Produces: `forestry.lepidopterology.butterflies.*`, `forestry.lepidopterology.butterflies.genetics.*`,
  `forestry.lepidopterology.cocoons.*`.
- `entities`, `features`, `render`, `recipe`, `commands`, `compat`, `proxy`, `network`, `plugin`,
  `client`, `tab` stay at the root.

- [ ] **Step 1: Two package moves**

`lepidopterology.blocks` holds only `BlockCocoon` and `BlockSolidCocoon`, and both go to `cocoons`, so
it is a package move rather than a fan-out.

```bash
<scratchpad>/move-package.sh forestry.lepidopterology.genetics forestry.lepidopterology.butterflies.genetics
<scratchpad>/move-package.sh forestry.lepidopterology.blocks   forestry.lepidopterology.cocoons
./gradlew compileJava --console=plain -q && echo COMPILES
```

Note `BlockSolidCocoon` is one of the four open bugs in the dead-callback audit. Moving it does not fix
that; do not conflate the two.

- [ ] **Step 2: Create `butterflies/package-info.java`**

`cocoons/package-info.java` arrived with the `blocks` move in step 1 and already declares
`package forestry.lepidopterology.cocoons;`. Only `butterflies` needs a new one.

- [ ] **Step 3: Explode intra-package imports in the root package**

```bash
<scratchpad>/explode-package-imports.sh forestry.lepidopterology
./gradlew compileJava --console=plain -q && echo COMPILES
```

`lepidopterology.items` and `lepidopterology.tiles` hold one class each, so they have no siblings and
need no exploding. There are no wildcard imports of any lepidopterology package.

- [ ] **Step 4: Fan out**

```bash
M=<scratchpad>/move-class.sh
for c in ButterflySpecies ButterflySpawner DummyButterflyEffect \
         LepidopterologyFilterRule LepidopterologyFilterRuleType; do
  $M forestry.lepidopterology.$c forestry.lepidopterology.butterflies.$c
done
$M forestry.lepidopterology.items.ItemButterflyGE forestry.lepidopterology.butterflies.ItemButterflyGE
$M forestry.lepidopterology.tiles.TileCocoon      forestry.lepidopterology.cocoons.TileCocoon
git rm src/main/java/forestry/lepidopterology/{items,tiles}/package-info.java
```

`ModuleLepidopterology` stays at the root.

- [ ] **Step 5: Compile, gate, verify, commit**

```bash
./gradlew compileJava compileTestJava --console=plain
./gradlew checkApiBoundary checkBaseBoundary checkBaseBytecode checkResourceFqcn --console=plain
./gradlew runData --console=plain && git diff --stat src/generated/resources
./gradlew runGameTestServer --console=plain 2>&1 | tail -20
grep -rn 'forestry\.lepidopterology\.\(blocks\|items\|tiles\|genetics\)\b' \
  src/main/java src/test/java src/main/resources || echo "no stale lepidopterology references"
git add -A
git commit -m "lepidopterology: butterflies and cocoons"
```

Expected: gates unchanged; empty datagen diff; `All 108 required tests passed`. Nine of the 108 tests
are butterfly tests, so this task has unusually good coverage.

---

### Task 7: Step 7.10 - agriculture

`farming` and `cultivation` merge into one jar directory. This task is entirely package moves - no
fan-out - which is why it can move 120 files with no import-repair phase.

**Interfaces:**
- Produces: `forestry.agriculture.{farmlogic,multifarm,planter,features,client,compat,plugin,tab}.*`,
  `forestry.agriculture.ModuleFarming`, `forestry.agriculture.ModuleCultivation`.
- Module ids do **not** change. Per the spec's Deferred section the 13 existing ids are referenced by
  config, so `forestry:farming` and `forestry:cultivation` both stay, mapping into one jar. The two
  module classes are **not** merged.

- [ ] **Step 1: Rename the whole of `farming` in one move**

```bash
<scratchpad>/move-package.sh forestry.farming forestry.agriculture
./gradlew compileJava --console=plain -q && echo COMPILES
grep -n 'AgricultureForestryPlugin' src/main/resources/META-INF/services/forestry.api.plugin.IForestryPlugin
```

Expected: `COMPILES`, and the service file line now reads
`forestry.agriculture.plugin.AgricultureForestryPlugin`. This single move carries `blocks`, `circuits`,
`client`, `compat`, `features`, `gui`, `items`, `logic`, `multiblock`, `plugin`, `tab`, `tiles` and the
seven loose files.

`forestry.agriculture` does not collide with `forestry.api.agriculture` - they are different packages,
and the rewrite is anchored `\bforestry\.farming\b`, which cannot match inside `forestry.api.farming`
(a package that no longer exists after 7a in any case).

- [ ] **Step 2: Re-nest the multifarm and farmlogic subpackages**

```bash
P=<scratchpad>/move-package.sh
$P forestry.agriculture.logic       forestry.agriculture.farmlogic
$P forestry.agriculture.multiblock  forestry.agriculture.multifarm.multiblock
$P forestry.agriculture.tiles       forestry.agriculture.multifarm.tiles
$P forestry.agriculture.blocks      forestry.agriculture.multifarm.blocks
$P forestry.agriculture.gui         forestry.agriculture.multifarm.gui
$P forestry.agriculture.items       forestry.agriculture.multifarm.items
$P forestry.agriculture.circuits    forestry.agriculture.multifarm.circuits
./gradlew compileJava --console=plain -q && echo COMPILES
```

`agriculture.farmlogic.crops` and `agriculture.farmlogic.farmables` move with their parent.
`FarmBlock`'s `import forestry.farming.tiles.*;` and `FarmingTiles`' likewise are rewritten in place by
the prefix rule and stay correct - `tiles` moved whole.

- [ ] **Step 3: Do not create `package-info.java` for `multifarm` or `planter`**

Both are pure parent directories that will never hold a class of their own. The repo's
`FieldsAreNonnullByDefault` default does not inherit into subpackages, so a `package-info.java` in an
empty parent would annotate nothing. Confirm the precedent set in 7a:

```bash
ls src/main/java/forestry/core/content/package-info.java 2>&1
```

Expected: `No such file or directory` - `core.content` and `core.platform` are exactly this shape after
7a. Match them.

- [ ] **Step 4: Move `cultivation` in under `agriculture`**

```bash
P=<scratchpad>/move-package.sh
$P forestry.cultivation.tiles     forestry.agriculture.planter.tiles
$P forestry.cultivation.blocks    forestry.agriculture.planter.blocks
$P forestry.cultivation.gui       forestry.agriculture.planter.gui
$P forestry.cultivation.inventory forestry.agriculture.planter.inventory
$P forestry.cultivation.items     forestry.agriculture.planter.items
./gradlew compileJava --console=plain -q && echo COMPILES
```

`cultivation.gui.widgets` moves with `cultivation.gui`.

- [ ] **Step 5: Merge `cultivation`'s `features` and `proxy` into agriculture's**

These two target packages already exist, so `move-package.sh` refuses. Move the classes individually,
then delete the emptied `package-info.java`. There are no simple-name collisions: agriculture has
`Farming{Blocks,MenuTypes,Tiles}` and `FarmingClientHandler`, cultivation has
`Cultivation{Blocks,MenuTypes,Tiles}` and `CultivationClientHandler`.

```bash
M=<scratchpad>/move-class.sh
for c in CultivationBlocks CultivationMenuTypes CultivationTiles; do
  $M forestry.cultivation.features.$c forestry.agriculture.features.$c
done
$M forestry.cultivation.proxy.CultivationClientHandler forestry.agriculture.client.CultivationClientHandler
git rm src/main/java/forestry/cultivation/features/package-info.java \
       src/main/java/forestry/cultivation/proxy/package-info.java
```

- [ ] **Step 6: Move the two remaining loose `cultivation` files, then delete the package**

```bash
M=<scratchpad>/move-class.sh
$M forestry.cultivation.ModuleCultivation     forestry.agriculture.ModuleCultivation
$M forestry.cultivation.IFarmHousingInternal  forestry.agriculture.farmlogic.IFarmHousingInternal
git rm src/main/java/forestry/cultivation/package-info.java
test ! -d src/main/java/forestry/cultivation && echo "forestry.cultivation is gone"
```

There is only one `IFarmHousingInternal` in the tree; the manifest's claimed collision with a `farming`
twin does not exist.

- [ ] **Step 7: Move the six loose farm-logic files into `farmlogic`**

```bash
M=<scratchpad>/move-class.sh
for c in FarmHelper FarmingManager FarmingStage FarmManager FarmTarget FarmWorkStatus; do
  $M forestry.agriculture.$c forestry.agriculture.farmlogic.$c
done
```

The manifest assigns none of these. All six are the shared farm engine, used by both the multifarm and
the planters, which is exactly what `farmlogic` is for. `FarmingManager` is the `IFarmingManager`
implementation that `ModuleFarming.installManagers()` installs (phase 6); it is a registry of
`IFarmType`s, so it belongs with the logic rather than at the jar root.

`ModuleFarming`, `ModuleCultivation` and `package-info.java` are all that stay at `forestry.agriculture`.

- [ ] **Step 8: Compile, gate, verify**

```bash
./gradlew compileJava compileTestJava --console=plain
./gradlew checkApiBoundary checkBaseBoundary checkBaseBytecode checkResourceFqcn --console=plain
./gradlew runData --console=plain && git diff --stat src/generated/resources
./gradlew runGameTestServer --console=plain 2>&1 | tail -20
```

**`checkBaseBoundary` and `checkBaseBytecode` will now under-report**: both hardcode
`splitModules = [..., 'farming', 'cultivation', ...]`, and neither package exists any more. Nothing
fails, but the farming edge stops being measured. Task 10 fixes the lists. Until then, run this manual
equivalent and expect no output:

```bash
grep -rn '^import \(static \)\?forestry\.agriculture\.' \
  src/main/java/forestry/{core,apiimpl,modules} src/main/java/forestry/*.java \
  | grep -v '^src/main/java/forestry/core/data/' || echo "no packaged base leak to agriculture"
```

- [ ] **Step 9: Prove nothing was left behind, and commit**

```bash
grep -rn '\bforestry\.\(farming\|cultivation\)\b' src/main/java src/test/java src/main/resources \
  || echo "no stale farming or cultivation references"
ls src/main/java/forestry/agriculture
git add -A
git commit -m "agriculture: farming and cultivation become one jar"
```

Expected listing: `client compat features ModuleCultivation.java ModuleFarming.java farmlogic multifarm
package-info.java planter plugin tab`.

---

### Task 8: Step 7.11 - mail fan-out

**Interfaces:**
- Produces: `forestry.mail.{letters,postoffice,tradestation}.*`.
- `carriers` (with `players` and `trading`), `blocks`, `features`, `gui`, `inventory`, `network`,
  `client`, `commands`, `compat`, `tab` stay at the root. `mail/blocks` holds only `BlockMail` and
  `BlockTypeMail`, which cover all three mail machines, so they stay at jar level rather than picking
  one feature directory.
- `mail/postalstates` dies; both its files go to `letters`. This contradicts the spec's content tree,
  which lists `postalstates/` as surviving - the manifest is the more specific document and wins. Record
  the divergence in the spec.

- [ ] **Step 1: Create the three new packages**

`package-info.java` for `letters`, `postoffice`, `tradestation`.

- [ ] **Step 2: Explode intra-package imports in the four fanning packages**

```bash
for p in forestry.mail forestry.mail.items forestry.mail.tiles forestry.mail.postalstates; do
  <scratchpad>/explode-package-imports.sh $p
done
./gradlew compileJava --console=plain -q && echo COMPILES
```

`forestry.mail.postalstates` has no `package-info.java`. That is pre-existing; `explode-package-imports.sh`
does not need one and does not create one.

- [ ] **Step 3: Expand the one wildcard import of a fanning package**

```bash
<scratchpad>/expand-wildcard.sh src/main/java/forestry/mail/carriers/trading/TradeStation.java forestry.mail
./gradlew compileJava --console=plain -q && echo COMPILES
```

`MailClientHandler`, `MailMenuTypes` and `ModuleMail` wildcard-import `mail.gui` and
`mail.network.packets`, neither of which fans out. Leave them.

- [ ] **Step 4: Fan out**

```bash
M=<scratchpad>/move-class.sh
for c in Letter LetterProperties LetterUtils MailAddress; do
  $M forestry.mail.$c forestry.mail.letters.$c
done
$M forestry.mail.PostOffice forestry.mail.postoffice.PostOffice

for c in LetterItem ItemStamp EnumStampDefinition; do
  $M forestry.mail.items.$c forestry.mail.letters.$c
done
$M forestry.mail.items.CatalogueItem forestry.mail.tradestation.CatalogueItem

$M forestry.mail.tiles.TileMailbox       forestry.mail.postoffice.TileMailbox
$M forestry.mail.tiles.TileStampCollector forestry.mail.postoffice.TileStampCollector
$M forestry.mail.tiles.TileTrader        forestry.mail.tradestation.TileTrader

$M forestry.mail.postalstates.EnumDeliveryState    forestry.mail.letters.EnumDeliveryState
$M forestry.mail.postalstates.ResponseNotMailable  forestry.mail.letters.ResponseNotMailable

git rm src/main/java/forestry/mail/{items,tiles}/package-info.java
test ! -d src/main/java/forestry/mail/postalstates && echo "postalstates is gone"
```

`ModuleMail` stays at the root.

- [ ] **Step 5: Compile, gate, verify, commit**

```bash
./gradlew compileJava compileTestJava --console=plain
./gradlew checkApiBoundary checkBaseBoundary checkBaseBytecode checkResourceFqcn --console=plain
./gradlew runData --console=plain && git diff --stat src/generated/resources
./gradlew runGameTestServer --console=plain 2>&1 | tail -20
grep -rn 'forestry\.mail\.\(items\|tiles\|postalstates\)\b' src/main/java src/test/java src/main/resources \
  || echo "no stale mail references"
git add -A
git commit -m "mail: letters, postoffice and tradestation"
```

Expected: gates unchanged; empty datagen diff; `All 108 required tests passed`.

Mail has the weakest test coverage of any jar in this phase - no GameTest exercises it. The datagen diff
covers its items and recipes; the `grep` above covers the rest. Read the `git diff --stat` for this
commit and confirm every file listed is one of the 13 moved classes or a file that imports one.

---

### Task 9: Step 7.12 - dissolve `compat`

The manifest treats this as five separate moves with a hand-edit for the Patchouli JSON. It is one move:
every destination shares the prefix `forestry.core.platform.compat`, and `rewrite-refs.sh` already
rewrites `src/main/resources`, so the Patchouli templates, `kubejs.plugins.txt` and the service file are
all carried mechanically.

The manifest's "split per jar into `<jar>.compat.jei`" and "`<jar>.compat.patchouli`" rows are stale:
each content jar already has its own `compat/` package, and what is left in `forestry.compat` is only
shared plumbing. `JeiUtil` and `IndividualSubtypeInterpreter` are used by `core.content.worktable.compat`
and by every jar's JEI plugin; `FluidComponent`, `CarpenterProcessor` and `FabricatorProcessor` document
base machines that live in `core.content.machines` after 7a.

**Interfaces:**
- Produces: `forestry.core.platform.compat.{jei,patchouli,kubejs,curios}.*` and
  `forestry.core.platform.compat.ModuleCurios`, which keeps its `forestry:curios` module id.

- [ ] **Step 1: One package move**

```bash
<scratchpad>/move-package.sh forestry.compat forestry.core.platform.compat
./gradlew compileJava compileTestJava --console=plain
```

- [ ] **Step 2: Verify the four resource files by eye**

This is the one place in the phase with no compile-time oracle. `checkResourceFqcn` is the automated
check; read the files as well, because a gate written in the same session as the move it guards deserves
one human look.

```bash
grep -rn 'forestry\.core\.platform\.compat' src/main/resources
```

Expected, exactly six lines:

```
src/main/resources/kubejs.plugins.txt:1:forestry.core.platform.compat.kubejs.ForestryKubeJsPlugin
src/main/resources/META-INF/services/forestry.api.plugin.IForestryPlugin:6:forestry.core.platform.compat.kubejs.KubeForestryPlugin
.../templates/fabricator/base.json:2:  "processor": "forestry.core.platform.compat.patchouli.processor.FabricatorProcessor",
.../templates/fabricator/base.json:94:      "class": "forestry.core.platform.compat.patchouli.component.FluidComponent",
.../templates/carpenter/base.json:2:  "processor": "forestry.core.platform.compat.patchouli.processor.CarpenterProcessor",
.../templates/carpenter/base.json:80:      "class": "forestry.core.platform.compat.patchouli.component.FluidComponent",
```

Then confirm nothing still names the old prefix:

```bash
grep -rn '\bforestry\.compat\b' src/main/java src/test/java src/main/resources \
  || echo "no stale compat references"
```

- [ ] **Step 3: Gate and verify**

```bash
./gradlew checkApiBoundary checkBaseBoundary checkBaseBytecode checkResourceFqcn --console=plain
./gradlew runData --console=plain && git diff --stat src/generated/resources
./gradlew runGameTestServer --console=plain 2>&1 | tail -20
```

`checkResourceFqcn` must still report 17 names resolving. If it reports fewer, the Patchouli templates
were rewritten into something the regex no longer recognizes - investigate rather than accepting a lower
number.

`checkBaseBoundary` still lists `compat` in `basePackages`, which is now a directory that does not
exist. The task loops over it with an `if (dir.exists())` guard, so it silently scans nothing. That is
harmless here - the classes moved *into* `core`, which is scanned - but it leaves a dead entry. Task 10
removes it.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "compat: dissolve into core.platform.compat"
```

---

### Task 10: Retarget the gates, and record the phase

Two of the three gates still describe the pre-7b tree. `checkBaseBoundary` and `checkBaseBytecode` both
carry `splitModules = ['apiculture', 'arboriculture', 'lepidopterology', 'farming', 'cultivation',
'mail']` and `basePackages = ['core', 'apiimpl', 'plugin', 'modules', 'compat']`. After Tasks 3, 7 and 9,
`farming`, `cultivation`, `plugin` and `compat` are all gone. Left alone the gates do not fail - they
just stop measuring the agriculture edge, which is precisely the blind spot 7a found in the `factory`,
`energy`, `storage`, `sorting` and `worktable` packages.

**Files:**
- Modify: `build.gradle` (four list literals)
- Modify: `.git-blame-ignore-revs`
- Modify: `docs/superpowers/specs/2026-07-30-phase-7-move-manifest.md`
- Modify: `docs/superpowers/specs/2026-07-30-feature-package-reorg-design.md`

- [ ] **Step 1: Retarget both gates**

In `checkBaseBoundary` and `checkBaseBytecode`, replace both pairs of lists with:

```groovy
	var basePackages = ['core', 'apiimpl', 'modules']
	var splitModules = ['apiculture', 'arboriculture', 'lepidopterology', 'agriculture', 'mail']
```

`plugin` and `compat` go because 7b dissolved both into `core`. `farming` and `cultivation` collapse
into the single `agriculture` entry.

`checkBaseBytecode` searches for the literal `forestry/agriculture/`. That cannot match
`forestry/api/agriculture/`, because the constant-pool string there is `forestry/api/agriculture/...`
and the search requires `forestry/` immediately followed by `agriculture/`. Same reasoning that made
`forestry/apiculture/` safe against `forestry/api/apiculture/` in phase 6. Verify rather than assume:

```bash
./gradlew checkBaseBytecode --console=plain
```

Expected: `checkBaseBytecode: no packaged base class references a split module`. If `forestry/api/`
classes start being reported, the substring reasoning is wrong and the check needs a word boundary.

- [ ] **Step 2: Prove the retargeted gate still bites**

```bash
sed -i '0,/^import /s//import forestry.agriculture.farmlogic.FarmType;\nimport /' \
  src/main/java/forestry/core/ModuleCore.java
./gradlew checkBaseBoundary --console=plain
```

Expected: `BUILD FAILED` naming `core/ModuleCore.java` as a new base-artifact reference. Then revert:

```bash
git checkout -- src/main/java/forestry/core/ModuleCore.java
./gradlew checkBaseBoundary --console=plain
```

Expected: back to `0 packaged leaking file(s), 20 datagen-only`.

- [ ] **Step 3: Full verification pass**

```bash
./gradlew build --console=plain
./gradlew runData --console=plain && git diff --stat src/generated/resources
./gradlew runGameTestServer --console=plain 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL` with all four gates reporting clean; empty datagen diff;
`All 108 required tests passed`.

- [ ] **Step 4: Confirm the target tree**

```bash
ls src/main/java/forestry
ls src/main/java/forestry/apiculture src/main/java/forestry/arboriculture \
   src/main/java/forestry/lepidopterology src/main/java/forestry/agriculture \
   src/main/java/forestry/mail
```

Expected top level, exactly: `agriculture api apiculture apiimpl arboriculture core Forestry.java
lepidopterology mail modules package-info.java`. `compat`, `cultivation`, `farming` and `plugin` are all
gone.

- [ ] **Step 5: Append the mechanical commits to `.git-blame-ignore-revs`**

Every commit in Tasks 4 through 9 is a pure move. Tasks 1 and 10 change behavior and gates and are
**not** added. Task 2 and Task 3 are moves and are added.

```bash
git log --format='%H %s' -9 | tac
```

Add the eight move shas with their subjects as comments, matching the file's existing format.

- [ ] **Step 6: Update the move manifest**

Mark steps 7.7 through 7.12 `(DONE 2026-08-02)`. Change the Status section to say the manifest is fully
executed. Fold the "Corrections to the manifest" table from this plan into the manifest's existing
corrections table, so the manifest stands alone as the record of what actually moved.

- [ ] **Step 7: Update the spec**

Add a phase-7b paragraph to the Sequencing section recording:

- The naturalist chests and `ItemFruit` stayed base, and why - this contradicts two statements in the
  spec's own "Notes on placement", which must be corrected in place rather than left to disagree with
  the tree.
- The four taxonomy classes went to `core.data.taxonomy` rather than to the three content jars, so that
  phase 8 partitions them with the rest of datagen instead of the baseline growing now.
- `mail/postalstates` did not survive, contradicting the spec's content tree.
- `checkResourceFqcn` now exists and closes the Verification section's outstanding item.
- The gate lists were retargeted, and the blind-spot lesson from 7a: a gate that names packages by
  literal keeps passing after those packages are renamed, and stops measuring anything.

Update the Verification section to strike "Add a grep-based check that every FQCN appearing in a resource
file resolves to an existing class" and name the task that does it.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "docs: record phase 7b completion, the six jars in their target shape"
```

---

## What 7b does not do

Stated so phase 8 does not have to rediscover them:

- **`core/data` (50 files) does not move.** It is base-only today and the 20 baseline entries are all
  inside it. Phase 8 makes datagen a per-jar source set, which is when it partitions. Task 3 adds
  `core/data/taxonomy` to it.
- **`core/features` (9 files) does not move**, per D6.
- **`apiimpl` (30 files) does not move.** The spec's target tree keeps it at the top level of base.
- **`arboriculture/wood` does not become a seventh jar.** Deferred by the spec.
- **Module ids do not change.** `forestry:farming` and `forestry:cultivation` both survive inside the
  agriculture jar directory, per the spec's Deferred section.
- **No jar is actually split.** That is phase 9. Until then the gates are the only proof the boundaries
  hold, which is why Task 10 retargets them rather than leaving them nominally green.
