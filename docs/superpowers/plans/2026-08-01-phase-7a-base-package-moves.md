# Phase 7a: base takes its final shape

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Execute move-manifest steps 7.1 through 7.6 - the api renames, `core/{platform,engine,content}`,
and absorbing the five base modules - leaving `forestry.core` in its target shape with both boundary
gates still green.

**Architecture:** A package move is a textual rewrite of one fully-qualified prefix across the whole
repository. Done that way it catches, in a single pass, the things an import-targeted `sed` misses:
194 `package-info` annotations, 55 javadoc `{@link}`s and 30 inline fully-qualified references - the
class of reference phase 6 caught `PluginManager` making. It also reaches the resources IntelliJ
cannot: 3 Patchouli FQCNs, 5 service files and `kubejs.plugins.txt`. Two files in step 7.6 are **not**
mechanical and are severed first, so that every later step is a pure move.

**Tech Stack:** Java 21, NeoForge 21.1.230, Minecraft 1.21.1, ModDevGradle 2.0.x, Gradle Groovy DSL.
GameTests only, no JUnit.

## Global Constraints

- Comment and Javadoc style is binding; see `CLAUDE.md`. ASCII only. Lowercase `todo`.
- Every task ends with `./gradlew runData` producing **no diff** in `src/generated/resources` and
  `./gradlew runGameTestServer` reporting **all 108 tests passed**. For a pure move the datagen tree
  must be byte-identical; per the spec, "any diff is a real defect", not a rebase artifact.
- Both gates stay green: `checkApiBoundary` clean, `checkBaseBoundary` at `0 packaged leaking
  file(s), 20 datagen-only`, `checkBaseBytecode` clean. **Run `checkBaseBytecode` after every step,
  not only at the end** - a package move is precisely the operation that can introduce a
  descriptor-level reference without an import.
- **Run `./gradlew compileTestJava`, not just `compileJava`.** `src/test/java` imports moved classes.
- The baseline file lists 20 paths under `core/data/`. **`core/data` does not move in this phase**,
  so those paths stay valid. Do not edit `gradle/base-boundary-baseline.txt` in 7a.
- All source files are LF. Do not write `$`-anchored `sed` patterns.
- **Every new package needs a `package-info.java`.** 194 of them carry
  `@forestry.core.utils.FieldsAreNonnullByDefault`, which is a per-package default. A fan-out that
  creates a package without one silently drops nonnull-by-default for those files.

## Why this is scripted

The spec (`### Who performs phase 7`) says a human must drive these moves in IntelliJ, because the
JetBrains MCP server exposes only `rename_refactoring`. That reasoning was sound when written and its
premise has since weakened on two counts, both measured on 2026-08-01:

1. **The collision surface is empty.** Zero lang keys begin with any package prefix this phase
   rewrites, zero FQCNs appear in `src/generated/resources`, and the one `Class.forName` site
   (`modules/ModuleUtil.java:35`) reads FML scan data rather than a hardcoded name.
2. **A bytecode-level verifier now exists.** `checkBaseBytecode` landed in phase 6 and reads the
   constant pool, so a botched rewrite that still compiles is caught rather than shipped.

And the IDE was never sufficient on its own: it cannot update the Patchouli JSON, the service files
or `kubejs.plugins.txt`, all of which name classes that move.

## Starting state, measured

`checkBaseBoundary`: 0 packaged, 20 datagen-only. `checkBaseBytecode`: clean. 108 GameTests.

| Step | Files | Kind |
| --- | --- | --- |
| Tasks 1-2 (prerequisite) | 2 | **not mechanical** - two real leaks |
| 7.1 api renames | 123 | package move |
| 7.2 `modules.features` | 28 | package move |
| 7.3 core -> platform | 272 | package move |
| 7.4 core -> engine | 61 | package move |
| 7.5 core fan-out | 147 | per-file |
| 7.6 absorb five modules | 220 | package move |

### The two files that are not moves

`forestry.factory`, `energy`, `storage`, `sorting` and `worktable` are base by D3 but
`checkBaseBoundary` has **never scanned them** - its `basePackages` list is
`['core', 'apiimpl', 'plugin', 'modules', 'compat']`. Step 7.6 moves all five under
`forestry.core.content`, at which point they come into scope. Two of them leak:

| File | Leak | Spec's decision |
| --- | --- | --- |
| `factory/features/FactoryRecipeTypes.java:21` | `apiculture.recipes.HygroregulatorRecipe` | "Not a graph edge - one misplaced registration line" |
| `storage/features/CrateItems.java:95-100` | 4 imports, crated bee products | "Needs a crate extension point so apiculture registers its own crates" |

Both decisions were recorded in the spec's Graph decisions table and never executed, because the gate
could not see them. They are severed in Tasks 1 and 2 so that step 7.6 is a pure move.

**Neither is a one-line move, and the reason is the phase 4 trap.** `FactoryRecipeTypes.HYGROREGULATOR`
has two *base* consumers (`core/fluids/FluidRecipeFilter.java:13` and
`core/utils/RecipeUtils.java:146`), and the crated bee products have one
(`core/client/CoreClientHandler.java:267-272`). Moving only the registration would relocate each leak
rather than remove it.

### Gaps in the move manifest

Four things the manifest does not assign. Decided here rather than discovered mid-move:

| Not in the manifest | Decision |
| --- | --- |
| `forestry/apiimpl` (30 files) | **Stays.** The spec's target tree keeps `apiimpl/` at the top level of base |
| `forestry/core/data` (50 files) | **Stays.** Phase 8 turns it into per-jar source sets; moving it now would invalidate 20 baseline paths and the jar's `exclude` rule for no gain |
| `forestry/core/features` (9 files) | **Stays** at `forestry.core.features`. D6: "per-jar registration holders keep the name `features/`" |
| `forestry/core/capabilities` (`SpectacleVision`) | To `forestry.core.platform.capabilities`. Phase 4 resolved the manifest's `(?) ItemSpectacles` by moving the capability to base; it is platform plumbing, not content |
| `forestry/plugin` (10 files) | **Deferred to 7b.** Seven of the ten (`BeeTaxonomy`, `TreeTaxonomy`, `ButterflyTaxonomy`, `DefaultButterflySpecies`, `client/TreeAnalyzerPlugin`, `client/ButterflyAnalyzerPlugin`) belong to content jars, so the package dissolves in the same phase its contents land |

## The move procedure

Every package move in Tasks 4-9 uses one helper, written once to the scratchpad in Task 3. It is
deliberately **not** checked into the repo - it is scaffolding for one phase.

The rewrite is anchored on the full dotted package starting at `forestry.`, with a word boundary at
each end. That is what makes it safe: `forestry.core.multiblock` cannot match inside
`forestry.api.core.multiblock` (the substring is absent), and `forestry.core.gui` cannot match
`forestry.core.guitar` (the trailing `\b` fails on a word character).

---

### Task 1: Sever the hygroregulator recipe type

`FactoryRecipeTypes:21` registers the hygroregulator recipe type with an apiculture serializer. The
hygroregulator is an alveary component: `IHygroregulatorRecipe` is already in api and the recipe impl
already lives in `apiculture/recipes`. The registration is simply in the wrong module.

Two base helpers hang off that constant and must move with it, or the leak relocates:

- `core/fluids/FluidRecipeFilter.java:13` - `HYGROREGULATOR_INPUT`, whose only consumer is
  `apiculture/multiblock/TileAlvearyHygroregulator.java:45`
- `core/utils/RecipeUtils.java:146` - `getHygroRegulatorRecipe`

**Files:**
- Modify: `src/main/java/forestry/factory/features/FactoryRecipeTypes.java:5,21`
- Create: `src/main/java/forestry/apiculture/features/ApicultureRecipeTypes.java`
- Modify: `src/main/java/forestry/core/fluids/FluidRecipeFilter.java:13`
- Modify: `src/main/java/forestry/core/utils/RecipeUtils.java:143-148`
- Modify: `src/main/java/forestry/apiculture/recipes/HygroregulatorRecipe.java:81,86`
- Modify: `src/main/java/forestry/apiculture/multiblock/TileAlvearyHygroregulator.java:45`

**Interfaces:**
- Consumes: nothing.
- Produces: `forestry.apiculture.features.ApicultureRecipeTypes.HYGROREGULATOR`, a
  `FeatureRecipeType<IHygroregulatorRecipe>` with the same `"hygroregulator"` identifier.

- [ ] **Step 1: Establish the oracle before touching anything**

The recipe type id appears in generated recipe JSON, so `runData` is a real oracle here. Record what
it should stay:

```bash
grep -rl '"type": *"forestry:hygroregulator"' src/generated/resources | wc -l
```

Note the number. It must be identical at the end of this task.

- [ ] **Step 2: Check whether getHygroRegulatorRecipe has any caller at all**

```bash
grep -rn "getHygroRegulatorRecipe" src/main/java src/test/java --include='*.java'
```

If the only hit is its own declaration in `RecipeUtils`, **delete it** rather than moving it - a
helper with no callers is not worth relocating. If it has callers, move it to
`apiculture/recipes/HygroregulatorRecipe.java` as a static method and repoint them. Record which
happened; the commit message says so.

- [ ] **Step 3: Create the apiculture holder**

```java
package forestry.apiculture.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.api.recipes.IHygroregulatorRecipe;
import forestry.apiculture.recipes.HygroregulatorRecipe;
import forestry.modules.features.FeatureProvider;
import forestry.modules.features.FeatureRecipeType;
import forestry.modules.features.IFeatureRegistry;
import forestry.modules.features.ModFeatureRegistry;

/**
 * Apiculture's recipe types. The hygroregulator is an alveary component, so its recipe type is
 * registered here rather than with the factory machines.
 */
@FeatureProvider
public class ApicultureRecipeTypes {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.APICULTURE);

	public static final FeatureRecipeType<IHygroregulatorRecipe> HYGROREGULATOR = REGISTRY.recipeType("hygroregulator", HygroregulatorRecipe.Serializer::new);
}
```

Check `FactoryRecipeTypes` for the exact `@FeatureProvider` annotation and `REGISTRY` idiom and match
it; if it differs, follow the existing file rather than this block.

- [ ] **Step 4: Remove the line from factory and repoint the three users**

Delete line 21 and the `import forestry.apiculture.recipes.HygroregulatorRecipe;` from
`FactoryRecipeTypes`. Then repoint:

```bash
grep -rl "FactoryRecipeTypes.HYGROREGULATOR" src/main/java --include='*.java' \
  | xargs -r sed -i 's@FactoryRecipeTypes\.HYGROREGULATOR@ApicultureRecipeTypes.HYGROREGULATOR@g'
```

Then fix each file's imports by hand: `HygroregulatorRecipe.java` and `TileAlvearyHygroregulator.java`
are in apiculture and import `ApicultureRecipeTypes` cleanly. `FluidRecipeFilter` and `RecipeUtils`
are **base** and must not - move `HYGROREGULATOR_INPUT` into
`apiculture/multiblock/TileAlvearyHygroregulator.java` as a private static field, since that is its
only consumer.

- [ ] **Step 5: Verify no leak was relocated**

```bash
./gradlew compileJava compileTestJava
for f in core/fluids/FluidRecipeFilter core/utils/RecipeUtils factory/features/FactoryRecipeTypes; do
  printf "%-40s %s\n" "$f" "$(grep -cE '^import (static )?forestry\.(apiculture|arboriculture|lepidopterology|farming|cultivation|mail)\.' src/main/java/forestry/$f.java)"
done
```

Expected: `0` for all three. A non-zero here means the leak moved instead of going away.

- [ ] **Step 6: Verify behavior**

```bash
./gradlew checkBaseBoundary checkApiBoundary checkBaseBytecode 2>&1 | grep -E "clean|leaking|split module"
./gradlew runData && git status --porcelain src/generated/resources
grep -rl '"type": *"forestry:hygroregulator"' src/generated/resources | wc -l
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: gates unchanged, **no datagen diff**, the hygroregulator recipe count identical to Step 1,
all 108 tests passed. A datagen diff means the recipe type id changed with the module - stop and
find out why before proceeding.

- [ ] **Step 7: Commit**

```bash
git add -A src/main/java/forestry
git commit -m "apiculture: own the hygroregulator recipe type

The hygroregulator is an alveary component, so registering its recipe type
alongside the factory machines put an apiculture serializer in a base class.
FactoryRecipeTypes has never been scanned by checkBaseBoundary - its
basePackages list does not include forestry.factory - so this survived six
phases. Step 7.6 moves factory under core.content.machines, which brings it
into scope.

The registration could not move alone: FluidRecipeFilter.HYGROREGULATOR_INPUT
and RecipeUtils.getHygroRegulatorRecipe are both base and both name the
constant, so moving only the line would have relocated the leak rather than
removed it. HYGROREGULATOR_INPUT follows to its sole consumer,
TileAlvearyHygroregulator.

Recipe type id is unchanged, proven by a byte-identical datagen tree."
```

---

### Task 2: A crate extension point so apiculture registers its own

`storage/features/CrateItems.java` registers six crated bee products plus a comb group, and imports
four apiculture classes to do it. The spec: "Base cannot register apiculture items and the only
alternative is abandoning the split."

The trap here is the same one Task 1 hit: `core/client/CoreClientHandler.java:267-272` registers
colour handlers for four of those crates. Phase 4 already established the fix - each module registers
its own colour handlers - so this follows that precedent rather than inventing anything.

**Files:**
- Modify: `src/main/java/forestry/storage/features/CrateItems.java`
- Create: `src/main/java/forestry/apiculture/features/ApicultureCrates.java`
- Modify: `src/main/java/forestry/core/client/CoreClientHandler.java:265-273`
- Modify: `src/main/java/forestry/apiculture/proxy/ApicultureClientHandler.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `CrateItems.registerCrate(ItemLike contained, String identifier)` returning
  `FeatureItem<ItemCrated>`, public so another module can add a crate and have it appear in
  `CrateItems.getCrates()`; and `forestry.apiculture.features.ApicultureCrates` holding the seven
  bee registrations.

- [ ] **Step 1: Record the oracle**

Crates are datagen'd - models, recipes and lang entries - and `getCrates()` drives
`ForestryItemModelProvider:51`. Record the counts:

```bash
ls src/generated/resources/assets/forestry/models/item/crated_*.json | wc -l
grep -c '"forestry.item.crated' src/main/resources/assets/forestry/lang/en_us.json || true
```

Both must be identical at the end. **This is the strongest oracle in the task** - if a crate fails to
register, its model file disappears from the datagen tree.

- [ ] **Step 2: Read how the registry derives an item id**

```bash
grep -n "item(" -A6 src/main/java/forestry/modules/features/ModFeatureRegistry.java | head -20
```

Confirm the registry name is `forestry:<identifier>` and does **not** include the module id. If it
does include the module id, the bee crates would be renamed by this move, every generated model path
would change, and this task needs redesigning - stop and report that.

- [ ] **Step 3: Open the extension point**

In `CrateItems`, make the private helper public and give it a doc comment:

```java
	/**
	 * Used by another module to register a crate for an item it owns. The crate is added to
	 * {@link #getCrates()}, so it is picked up by model datagen like every other crate.
	 *
	 * @param contained  The item the crate holds
	 * @param identifier The registry path, ex. "crated_propolis"
	 * @return The registered crate
	 */
	public static FeatureItem<ItemCrated> registerCrate(ItemLike contained, String identifier) {
		FeatureItem<ItemCrated> item = REGISTRY.item(() -> new ItemCrated(() -> new ItemStack(contained)), identifier);
		CRATES.add(item);
		return item;
	}
```

Keep the existing private `register` as a thin caller of it, or rename all internal uses - either is
fine, but do not leave two bodies that can drift.

**Note the registry stays base's.** The crates keep registering through `storage`'s
`IFeatureRegistry`, so ids and datagen output cannot change. What moves is only *which class names
the apiculture items*.

- [ ] **Step 4: Move the seven registrations to apiculture**

Create `src/main/java/forestry/apiculture/features/ApicultureCrates.java`:

```java
package forestry.apiculture.features;

import forestry.apiculture.items.EnumHoneyComb;
import forestry.apiculture.items.EnumPollenCluster;
import forestry.apiculture.items.EnumPropolis;
import forestry.modules.features.FeatureItem;
import forestry.modules.features.FeatureItemGroup;
import forestry.modules.features.FeatureProvider;
import forestry.storage.features.CrateItems;
import forestry.storage.items.ItemCrated;

/**
 * Crates for apiculture products. Registered here rather than in {@link CrateItems} so the base
 * artifact does not name a bee item; the crates themselves still go through base's registry, so
 * their ids and generated models are unchanged.
 */
@FeatureProvider
public class ApicultureCrates {
	public static final FeatureItem<ItemCrated> CRATED_POLLEN_CLUSTER_NORMAL = CrateItems.registerCrate(ApicultureItems.POLLEN_CLUSTER.get(EnumPollenCluster.NORMAL), "crated_pollen_cluster_normal");
	public static final FeatureItem<ItemCrated> CRATED_POLLEN_CLUSTER_CRYSTALLINE = CrateItems.registerCrate(ApicultureItems.POLLEN_CLUSTER.get(EnumPollenCluster.CRYSTALLINE), "crated_pollen_cluster_crystalline");
	public static final FeatureItem<ItemCrated> CRATED_PROPOLIS = CrateItems.registerCrate(ApicultureItems.PROPOLIS.get(EnumPropolis.NORMAL), "crated_propolis");
	public static final FeatureItem<ItemCrated> CRATED_ROYAL_JELLY = CrateItems.registerCrate(ApicultureItems.ROYAL_JELLY, "crated_royal_jelly");
}
```

`CRATED_BEESWAX`, `CRATED_REFRACTORY_WAX` and `CRATED_HONEYDEW` **stay in `CrateItems`** - phase 2
moved those items to `CoreItems`, so they are core items and name nothing in apiculture. Check that
before moving them by mistake.

`CRATED_BEE_COMBS` is a `FeatureItemGroup` built with `REGISTRY.itemGroup(...)` rather than the crate
helper, and it feeds `CRATES` through a `static` block. Move it to `ApicultureCrates` with its
`static` block, and add whatever accessor it needs - `REGISTRY` is private to `CrateItems`, so it
needs a second entry point:

```java
	/**
	 * @return The crate registry, for a module registering a crate group of its own
	 */
	public static IFeatureRegistry registry() {
		return REGISTRY;
	}

	/**
	 * Used to add a crate group's members to the crate list after the group is built.
	 */
	public static void addCrates(Collection<FeatureItem<ItemCrated>> crates) {
		CRATES.addAll(crates);
	}
```

- [ ] **Step 5: Ordering matters - verify the feature providers run**

`CrateItems` and `ApicultureCrates` are both `@FeatureProvider` classes whose registrations happen in
static initialisers. `ApicultureCrates` now calls into `CrateItems`, so `CrateItems` must be
initialised first, which the JVM guarantees on first static access.

What is **not** guaranteed is that `ApicultureCrates` is ever loaded. Confirm the discovery mechanism
picks it up:

```bash
grep -rn "FeatureProvider" src/main/java/forestry/modules/features/ModFeatureRegistry.java src/main/java/forestry/modules/ModuleUtil.java | head
```

If providers are discovered by annotation scan, the new class is found automatically. **If they are
discovered from a hardcoded list, add it there** - and the Step 1 model count is what will tell you
if you missed it.

- [ ] **Step 6: Move the colour handlers**

`CoreClientHandler:267-272` registers item colours for `CRATED_BEE_COMBS`,
`CRATED_POLLEN_CLUSTER_NORMAL`, `CRATED_POLLEN_CLUSTER_CRYSTALLINE` and `CRATED_PROPOLIS`. Move those
four to `apiculture/proxy/ApicultureClientHandler.java`, next to the colour registrations phase 4 put
there. `CRATED_GRASS_BLOCK` stays in `CoreClientHandler` - it is vanilla.

Count before and after, the way phases 4 and 5 did:

```bash
grep -c "FORESTRY_ITEM_COLOR\|register(" src/main/java/forestry/core/client/CoreClientHandler.java
```

- [ ] **Step 7: Verify**

```bash
./gradlew compileJava compileTestJava
grep -cE '^import (static )?forestry\.(apiculture|arboriculture|lepidopterology|farming|cultivation|mail)\.' src/main/java/forestry/storage/features/CrateItems.java src/main/java/forestry/core/client/CoreClientHandler.java
./gradlew checkBaseBoundary checkApiBoundary checkBaseBytecode 2>&1 | grep -E "clean|leaking|split module"
./gradlew runData && git status --porcelain src/generated/resources
ls src/generated/resources/assets/forestry/models/item/crated_*.json | wc -l
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `0` leaks in both files, gates unchanged, **no datagen diff**, the crate model count
identical to Step 1, all 108 tests passed.

- [ ] **Step 8: Commit**

```bash
git add -A src/main/java/forestry
git commit -m "apiculture: register its own crates

CrateItems named four apiculture classes to register seven crated bee products.
Like FactoryRecipeTypes, it was never scanned - forestry.storage is not in the
gate's basePackages - and step 7.6 brings it into scope.

The crates still register through base's registry, so their ids and generated
models are untouched; only the class that names the bee items moved. The four
colour handlers in CoreClientHandler moved to ApicultureClientHandler, which is
where phase 4 put apiculture's others.

Crated beeswax, refractory wax and honeydew stayed behind: phase 2 made those
core items.

Proven by a byte-identical datagen tree and an unchanged crate model count."
```

---

### Task 3: Write the move helper

One script, used by Tasks 4 through 9. It lives in the scratchpad, not the repo.

**Files:**
- Create: `<scratchpad>/move-package.sh`

**Interfaces:**
- Consumes: nothing.
- Produces: `move-package.sh <from.dotted.package> <to.dotted.package>`, and
  `rewrite-refs.sh <from> <to>` for the reference rewrite alone (used by the fan-out steps, where
  files move individually but references still need updating).

- [ ] **Step 1: Write it**

```bash
mkdir -p "$SCRATCH"
cat > "$SCRATCH/move-package.sh" <<'SCRIPT'
#!/usr/bin/env bash
# Move one Java package and rewrite every reference to it, repo-wide.
#
# The rewrite is anchored on the full dotted package starting at "forestry.", with a word
# boundary at each end. That is what makes it safe:
#   - forestry.core.multiblock cannot match inside forestry.api.core.multiblock (substring absent)
#   - forestry.core.gui cannot match forestry.core.guitar (trailing \b fails on a word char)
# It deliberately catches more than imports: package declarations, package-info annotations,
# javadoc {@link}, inline fully-qualified references, and FQCNs in resources.
set -euo pipefail

from="$1"; to="$2"
fromdir="src/main/java/${from//.//}"
todir="src/main/java/${to//.//}"

if [ ! -d "$fromdir" ]; then echo "no such package: $fromdir" >&2; exit 1; fi
if [ -e "$todir" ]; then echo "destination exists: $todir" >&2; exit 1; fi

mkdir -p "$(dirname "$todir")"
git mv "$fromdir" "$todir"

"$(dirname "$0")/rewrite-refs.sh" "$from" "$to"
echo "moved $from -> $to"
SCRIPT

cat > "$SCRATCH/rewrite-refs.sh" <<'SCRIPT'
#!/usr/bin/env bash
# Rewrite every reference to a dotted package, in source and in resources.
set -euo pipefail

from="$1"; to="$2"
esc_from="${from//./\\.}"

# Java sources: imports, package decls, package-info annotations, javadoc, inline FQ refs
grep -rlF "$from" src/main/java src/test/java --include='*.java' 2>/dev/null \
  | xargs -r sed -i "s@\\b${esc_from}\\b@${to}@g"

# Resources: META-INF/services contents, kubejs.plugins.txt, patchouli JSON
grep -rlF "$from" src/main/resources 2>/dev/null \
  | xargs -r sed -i "s@\\b${esc_from}\\b@${to}@g"

# Service FILENAMES are themselves FQCNs
for f in src/main/resources/META-INF/services/*; do
  [ -e "$f" ] || continue
  base="$(basename "$f")"
  case "$base" in
    "$from"|"$from".*)
      git mv "$f" "$(dirname "$f")/${base/#$from/$to}" ;;
  esac
done
SCRIPT

chmod +x "$SCRATCH/move-package.sh" "$SCRATCH/rewrite-refs.sh"
```

Set `SCRATCH` once at the start of each task's shell:

```bash
SCRATCH=/tmp/claude-1000/-home-thedarkcolour-IdeaProjects-ForestryCE/a14b6292-0978-46b8-95b1-84eb1fb04cb8/scratchpad
```

- [ ] **Step 2: Prove the boundary anchoring on a case that must NOT match**

Before trusting it on 853 files, check the one hazard by hand. `forestry.api.multiblock` is renamed
in Task 4 and `forestry.core.multiblock` in Task 6; neither may touch the other.

```bash
printf 'import forestry.api.multiblock.IMultiblockComponent;\nimport forestry.core.multiblock.MultiblockTicker;\n' > /tmp/anchor-test.txt
sed 's@\bforestry\.core\.multiblock\b@forestry.core.platform.multiblock@g' /tmp/anchor-test.txt
```

Expected: line 1 **unchanged**, line 2 rewritten. If line 1 changed, the anchoring is wrong - stop.

- [ ] **Step 3: No commit**

Nothing in the repo changed. Do not commit.

---

### Task 4: Step 7.1 - api package moves

Nine package moves inside `forestry.api`. `api.plugin`, `api.client`, the four content api packages,
`api.modules` and the loose `api.core` types stay - the concern-first grouping inside `api.plugin`
and `api.client` is deliberate.

**Files:** 123 files across nine packages, plus every referencing file repo-wide.

**Interfaces:**
- Consumes: `move-package.sh` from Task 3.
- Produces: `forestry.api.core.genetics`, `forestry.api.core.multiblock`, `forestry.api.core.circuits`,
  `forestry.api.core.climate`, `forestry.api.core.machines`, `forestry.api.core.machines.fuels`,
  `forestry.api.core.backpacks`, `forestry.api.agriculture`, `forestry.api.core.util`.

- [ ] **Step 1: Move them, deepest-nesting last**

`api.fuels` becomes a subpackage of `api.core.machines`, which `api.recipes` becomes. Move
`api.recipes` first so the parent directory exists.

```bash
SCRATCH=/tmp/claude-1000/-home-thedarkcolour-IdeaProjects-ForestryCE/a14b6292-0978-46b8-95b1-84eb1fb04cb8/scratchpad
$SCRATCH/move-package.sh forestry.api.genetics    forestry.api.core.genetics
$SCRATCH/move-package.sh forestry.api.multiblock  forestry.api.core.multiblock
$SCRATCH/move-package.sh forestry.api.circuits    forestry.api.core.circuits
$SCRATCH/move-package.sh forestry.api.climate     forestry.api.core.climate
$SCRATCH/move-package.sh forestry.api.recipes     forestry.api.core.machines
$SCRATCH/move-package.sh forestry.api.fuels       forestry.api.core.machines.fuels
$SCRATCH/move-package.sh forestry.api.storage     forestry.api.core.backpacks
$SCRATCH/move-package.sh forestry.api.farming     forestry.api.agriculture
$SCRATCH/move-package.sh forestry.api.util        forestry.api.core.util
```

- [ ] **Step 2: Check the service filenames**

Four service files are named after api interfaces. None of the four is in a moved package
(`forestry.api.IForestryApi`, `forestry.api.client.IForestryClientApi`,
`forestry.api.client.plugin.IClientHelper`, `forestry.api.plugin.IForestryPlugin`), so all four names
must be **unchanged**:

```bash
ls src/main/resources/META-INF/services/
```

Expected: exactly the same four names as before the move. If one was renamed, `rewrite-refs.sh`
over-matched - revert and fix the pattern.

- [ ] **Step 3: Compile and verify**

```bash
./gradlew compileJava compileTestJava
./gradlew checkApiBoundary checkBaseBoundary checkBaseBytecode 2>&1 | grep -E "clean|leaking|split module"
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `checkApiBoundary: forestry.api is clean`, `0 packaged leaking file(s), 20 datagen-only`,
`checkBaseBytecode` clean, no datagen diff, all 108 tests passed.

- [ ] **Step 4: Confirm nothing was left behind**

```bash
for p in genetics multiblock circuits climate recipes fuels storage farming util; do
  [ -d "src/main/java/forestry/api/$p" ] && echo "STILL PRESENT: api/$p"
done
grep -rn "forestry\.api\.\(genetics\|multiblock\|circuits\|climate\|recipes\|fuels\|storage\|farming\|util\)\." src/main/java src/test/java src/main/resources 2>/dev/null | head
```

Expected: no output from either.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "api: group the shared api under api.core

Move-manifest step 7.1. Nine package moves; api.plugin, api.client, the four
content api packages and the loose api.core types are untouched - their
concern-first grouping is deliberate.

api.farming becomes api.agriculture to match the jar name.

Pure move: byte-identical datagen tree, all 108 GameTests green, and
checkBaseBytecode confirms no reference changed shape."
```

---

### Task 5: Step 7.2 - the registration framework

One package move. `forestry.modules` itself stays - it is the module framework, not the registration
framework.

**Files:** 28 files in `forestry/modules/features`, plus references.

**Interfaces:**
- Consumes: `move-package.sh`.
- Produces: `forestry.core.platform.registration` (`FeatureBlock`, `FeatureItem`,
  `ModFeatureRegistry`, `FeatureTable`, ...).

- [ ] **Step 1: Move it**

```bash
SCRATCH=/tmp/claude-1000/-home-thedarkcolour-IdeaProjects-ForestryCE/a14b6292-0978-46b8-95b1-84eb1fb04cb8/scratchpad
$SCRATCH/move-package.sh forestry.modules.features forestry.core.platform.registration
```

- [ ] **Step 2: Confirm forestry.modules survived**

```bash
ls src/main/java/forestry/modules/
```

Expected: `BlankForestryModule.java`, `ForestryModuleManager.java`, `ModuleUtil.java`,
`package-info.java` and nothing else.

- [ ] **Step 3: Verify**

```bash
./gradlew compileJava compileTestJava
./gradlew checkApiBoundary checkBaseBoundary checkBaseBytecode 2>&1 | grep -E "clean|leaking|split module"
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: gates green, no datagen diff, all 108 tests passed.

**This step touches nearly every registration holder in the mod** - `ModFeatureRegistry` is imported
by every `*Items`/`*Blocks`/`*Tiles` class. A clean datagen diff is therefore strong evidence.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "core: registration framework to core.platform.registration

Move-manifest step 7.2. forestry.modules keeps the module framework
(BlankForestryModule, ForestryModuleManager, ModuleUtil); only the registration
framework moves.

Per-jar registration holders keep the name features/ and do not move.

Touches nearly every registration holder in the mod, so the byte-identical
datagen tree is the evidence that matters here."
```

---

### Task 6: Step 7.3 - core to platform

Twenty-two package moves, the largest mechanical step. Every one is framework with no game content.

Two passengers are deliberately **not** handled here and are left for Task 8:
- `core.gui` fans out (analyzer and escritoire screens leave). Move the package first, split later.
- `core.render` carries `RenderMill`, which leaves for `core.content.machines`.

**Files:** 272 files, plus `core/capabilities` (2 files), which the manifest omits.

**Interfaces:**
- Consumes: `move-package.sh`.
- Produces: `forestry.core.platform.{gui,network,inventory,util,fluids,multiblock,render,models,commands,owner,errors,config,client,loot,particles,entities,tab,damage,villager,worldgen,recipes,capabilities}`.

- [ ] **Step 1: Move them**

```bash
SCRATCH=/tmp/claude-1000/-home-thedarkcolour-IdeaProjects-ForestryCE/a14b6292-0978-46b8-95b1-84eb1fb04cb8/scratchpad
$SCRATCH/move-package.sh forestry.core.gui          forestry.core.platform.gui
$SCRATCH/move-package.sh forestry.core.network      forestry.core.platform.network
$SCRATCH/move-package.sh forestry.core.inventory    forestry.core.platform.inventory
$SCRATCH/move-package.sh forestry.core.utils        forestry.core.platform.util
$SCRATCH/move-package.sh forestry.core.fluids       forestry.core.platform.fluids
$SCRATCH/move-package.sh forestry.core.multiblock   forestry.core.platform.multiblock
$SCRATCH/move-package.sh forestry.core.render       forestry.core.platform.render
$SCRATCH/move-package.sh forestry.core.models       forestry.core.platform.models
$SCRATCH/move-package.sh forestry.core.commands     forestry.core.platform.commands
$SCRATCH/move-package.sh forestry.core.owner        forestry.core.platform.owner
$SCRATCH/move-package.sh forestry.core.errors       forestry.core.platform.errors
$SCRATCH/move-package.sh forestry.core.config       forestry.core.platform.config
$SCRATCH/move-package.sh forestry.core.client       forestry.core.platform.client
$SCRATCH/move-package.sh forestry.core.loot         forestry.core.platform.loot
$SCRATCH/move-package.sh forestry.core.particles    forestry.core.platform.particles
$SCRATCH/move-package.sh forestry.core.entities     forestry.core.platform.entities
$SCRATCH/move-package.sh forestry.core.tab          forestry.core.platform.tab
$SCRATCH/move-package.sh forestry.core.damage       forestry.core.platform.damage
$SCRATCH/move-package.sh forestry.core.registration forestry.core.platform.villager
$SCRATCH/move-package.sh forestry.core.worldgen     forestry.core.platform.worldgen
$SCRATCH/move-package.sh forestry.core.recipes      forestry.core.platform.recipes
$SCRATCH/move-package.sh forestry.core.capabilities forestry.core.platform.capabilities
```

`core.registration` holds only `VillagerTrade`, hence the rename - `registration` is taken by Task 5.

`core.capabilities` is **not in the manifest**; it holds `SpectacleVision`, which phase 4 moved to
base when it resolved the manifest's `(?) ItemSpectacles` marker. It is platform plumbing.

- [ ] **Step 2: Check the two names that could collide**

Task 4 created `forestry.api.core.multiblock`; this step creates
`forestry.core.platform.multiblock`. Neither may have touched the other:

```bash
grep -rc "forestry\.api\.core\.multiblock" src/main/java --include='*.java' | grep -v ':0' | head -3
grep -rn "forestry\.api\.core\.platform" src/main/java 2>/dev/null | head
```

Expected: the first prints real counts, the second prints **nothing**. Any
`forestry.api.core.platform` is proof of a double rewrite - revert the step and fix the anchoring.

- [ ] **Step 3: Verify**

```bash
./gradlew compileJava compileTestJava
./gradlew checkApiBoundary checkBaseBoundary checkBaseBytecode 2>&1 | grep -E "clean|leaking|split module"
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: gates green, no datagen diff, all 108 tests passed.

- [ ] **Step 4: Confirm what is left directly under core**

```bash
ls -d src/main/java/forestry/core/*/ | xargs -n1 basename
```

Expected exactly: `data`, `features`, `platform`, plus the three engine packages
(`genetics`, `climate`, `circuits`) that Task 7 moves, plus `blocks`, `items`, `tiles` that Task 8
fans out.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "core: platform layer

Move-manifest step 7.3, twenty-two package moves. Everything here is framework
with no game content, which is the D2 layer boundary: nothing in platform or
engine may import content.

core.registration held only VillagerTrade and becomes core.platform.villager,
since 'registration' is now the feature-registration framework.

core.capabilities is not in the manifest. It holds SpectacleVision, which phase
4 moved to base when it resolved the manifest's (?) ItemSpectacles marker; it is
platform plumbing and goes with the rest.

core.gui and core.render still carry passengers for the fan-out step.

Pure move: byte-identical datagen tree, 108 GameTests green."
```

---

### Task 7: Step 7.4 - core to engine

Three package moves.

**Files:** 61 files.

**Interfaces:**
- Consumes: `move-package.sh`.
- Produces: `forestry.core.engine.genetics`, `forestry.core.engine.climate`,
  `forestry.core.engine.circuits`.

- [ ] **Step 1: Move them**

```bash
SCRATCH=/tmp/claude-1000/-home-thedarkcolour-IdeaProjects-ForestryCE/a14b6292-0978-46b8-95b1-84eb1fb04cb8/scratchpad
$SCRATCH/move-package.sh forestry.core.genetics forestry.core.engine.genetics
$SCRATCH/move-package.sh forestry.core.climate  forestry.core.engine.climate
$SCRATCH/move-package.sh forestry.core.circuits forestry.core.engine.circuits
```

- [ ] **Step 2: Check the inline references specifically**

This step has the highest concentration of inline fully-qualified references measured anywhere in the
repo - `forestry.core.genetics.ProductTypes` appears 3 times inline, and
`forestry.core.genetics.mutations.MutationConditionTypes` once. They are exactly what an
import-targeted rewrite would have missed.

```bash
grep -rn "forestry\.core\.genetics\.\|forestry\.core\.climate\.\|forestry\.core\.circuits\." src/main/java src/test/java 2>/dev/null | head
```

Expected: **no output.** Any hit is an unrewritten reference.

- [ ] **Step 3: Verify**

```bash
./gradlew compileJava compileTestJava
./gradlew checkApiBoundary checkBaseBoundary checkBaseBytecode 2>&1 | grep -E "clean|leaking|split module"
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: gates green, no datagen diff, all 108 tests passed. The genetics engine is what every
species type is built on, so the species counts in the GameTest log are a second oracle - confirm
`Loaded 69 bee species`, `Loaded 50 tree species`, `Loaded 35 butterfly species` still appear.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "core: engine layer

Move-manifest step 7.4. Genome, Karyotype, Species, SpeciesType, alleles,
mutations, climate and circuits.

This package had the repo's highest concentration of inline fully-qualified
references - ProductTypes alone is named that way three times - which is what a
prefix rewrite catches and an import-targeted one does not.

Pure move: byte-identical datagen tree, 108 GameTests green, species counts
unchanged."
```

---

### Task 8: Step 7.5 - the core fan-out

The only step in 7a needing per-file work. Four packages split; the destinations are new, so each
needs a `package-info.java`.

**Files:** 147 files across `core/tiles`, `core/blocks`, `core/items`, `core/platform/gui`, plus 8
loose files at `forestry.core`.

**Interfaces:**
- Consumes: `rewrite-refs.sh` from Task 3 (files move individually here, so `move-package.sh` does
  not apply).
- Produces: `forestry.core.platform.{tile,block,item}`, `forestry.core.content.{machines,escritoire,analyzer,soil,resources,tools}`.

- [ ] **Step 1: Create the destination packages with their package-info**

Every one of these is new, and 194 existing `package-info.java` files carry
`@forestry.core.platform.util.FieldsAreNonnullByDefault` (renamed in Task 6). A new package without
one silently loses nonnull-by-default.

**Use `core/tiles/package-info.java` as the template, not `core/utils/package-info.java`.** They are
not interchangeable. `core/utils` is the package that *declares* the annotation, so its own
`package-info` names it unqualified:

```java
@ParametersAreNonnullByDefault
@FieldsAreNonnullByDefault
@MethodsReturnNonnullByDefault
```

Copying that into a new package produces a file that does not compile. Every other `package-info`
uses the fully-qualified form, which is what a new package needs:

```java
@javax.annotation.ParametersAreNonnullByDefault
@forestry.core.utils.FieldsAreNonnullByDefault
@net.minecraft.MethodsReturnNonnullByDefault
```

By the time this task runs, Task 6 has rewritten that middle line to
`@forestry.core.platform.util.FieldsAreNonnullByDefault`, so copying it is correct as-is.

```bash
SCRATCH=/tmp/claude-1000/-home-thedarkcolour-IdeaProjects-ForestryCE/a14b6292-0978-46b8-95b1-84eb1fb04cb8/scratchpad
TEMPLATE=src/main/java/forestry/core/tiles/package-info.java
grep -q "forestry\.core\.platform\.util\.FieldsAreNonnullByDefault" "$TEMPLATE" \
  || { echo "template is not the fully-qualified form - pick another package-info"; exit 1; }
for p in platform/tile platform/block platform/item \
         content/machines content/escritoire content/analyzer content/soil content/resources content/tools; do
  d="src/main/java/forestry/core/$p"
  mkdir -p "$d"
  sed "s@^package .*;@package forestry.core.${p//\//.};@" "$TEMPLATE" > "$d/package-info.java"
done
cat src/main/java/forestry/core/content/machines/package-info.java
```

Confirm all three annotations survived fully qualified and the package line is right.

- [ ] **Step 2: Move core/tiles**

Two corrections to the manifest's list, both verified against the tree on 2026-08-01:

- **`IFilterSlotDelegate` is not here.** It lives at `forestry/api/core/IFilterSlotDelegate.java` -
  phase 1b moved it into api. Do not look for it.
- **The three naturalist chests are still here.** The manifest's Prerequisite section claims phase 4
  moved `TileApiaristChest`, `TileArboristChest` and `TileLepidopteristChest` out of `core/tiles`.
  It did not. They belong to content jars, so like `forestry/plugin` they are **deferred to 7b**.
  They do not leak (confirmed against `checkBaseBoundary`), so leaving them in `core/tiles` for one
  more phase costs nothing.

```bash
cd src/main/java/forestry/core
for f in AdjacentTileCache IActivatable IForestryTicker IItemStackDisplay \
         ILiquidTankTile IPowerHandler IRenderableTile ITitled TemperatureState TileBase \
         TileForestry TilePowered TileUtil TileNaturalistChest; do
  git mv tiles/$f.java platform/tile/$f.java
done
git mv tiles/TileMill.java content/machines/TileMill.java
for f in EscritoireGame EscritoireGameBoard EscritoireGameToken EscritoireTextSource TileEscritoire; do
  git mv tiles/$f.java content/escritoire/$f.java
done
git mv tiles/TileAnalyzer.java content/analyzer/TileAnalyzer.java
ls tiles/
cd -
```

Expected exactly: `package-info.java`, `TileApiaristChest.java`, `TileArboristChest.java`,
`TileLepidopteristChest.java`. **If anything else remains, stop** - the manifest's counts were taken
on 2026-07-31 and it says to re-derive. Assign the leftovers before continuing; do not guess.

- [ ] **Step 3: Move core/blocks, core/items and the gui passengers**

```bash
cd src/main/java/forestry/core
for f in BlockForestry BlockBase BlockCore BlockStructure BlockTesr BlockTypeCoreTesr IBlockType \
         IColoredBlock IMachineProperties IShapeProvider ISimpleShapeProvider MachineProperties \
         TileStreamUpdateTracker; do
  git mv blocks/$f.java platform/block/$f.java
done
git mv blocks/NaturalistChestBlockType.java platform/block/NaturalistChestBlockType.java
git mv blocks/BlockBogEarth.java content/soil/BlockBogEarth.java
git mv blocks/BlockHumus.java     content/soil/BlockHumus.java
git mv blocks/BlockResourceStorage.java content/resources/BlockResourceStorage.java
git mv blocks/EnumResourceType.java     content/resources/EnumResourceType.java

for f in ItemForestry ItemBlockForestry ItemBlockTesr ItemOverlay WithScreenItem \
         ItemFluidContainerForestry ItemForestryFood HasRemnants; do
  git mv items/$f.java platform/item/$f.java
done
for f in ForestersManualItem ItemWrench ItemPipette SolderingIronItem ItemScoop ItemSpectacles; do
  git mv items/$f.java content/tools/$f.java
done
git mv items/PortableAnalyzerItem.java content/analyzer/PortableAnalyzerItem.java
for f in ItemCraftingMaterial ItemElectronTube ItemFertilizer ItemAssemblyKit ItemBeesWax ItemRefractoryWax; do
  git mv items/$f.java content/resources/$f.java
done
git mv items/definitions/EnumCraftingMaterial.java content/resources/EnumCraftingMaterial.java
git mv items/definitions/EnumElectronTube.java     content/resources/EnumElectronTube.java
git mv items/definitions/ToolTier.java             content/tools/ToolTier.java
for f in DrinkProperties EnumContainerType FluidHandlerItemForestry IColoredItem ICraftingPlan; do
  git mv items/definitions/$f.java platform/item/$f.java
done

for f in ContainerAnalyzer GuiAnalyzer PortableAnalyzerMenu PortableAnalyzerScreen; do
  git mv platform/gui/$f.java content/analyzer/$f.java
done
git mv platform/gui/ContainerEscritoire.java content/escritoire/ContainerEscritoire.java
git mv platform/gui/GuiEscritoire.java       content/escritoire/GuiEscritoire.java

git mv platform/render/RenderMill.java content/machines/RenderMill.java

ls blocks/ items/ items/definitions/ 2>/dev/null
cd -
```

Expected: `blocks/` and `items/definitions/` hold only `package-info.java`; `items/` holds
`package-info.java` and `ItemFruit.java`. **Stop and assign anything else.**

Five files above are not in the manifest's lists, all verified present on 2026-08-01:

- **`NaturalistChestBlockType`** is the manifest's `(?)` marker in step 7.7. The spec's Graph
  decisions table already resolved it - "the enum is misfiled... it moves to `core.platform.block`,
  which also removes five core -> apiculture references" - so it goes to `platform/block`, not to
  apiculture.
- **`ItemSpectacles`** is the manifest's other `(?)`. Phase 4 resolved it by moving
  `SpectacleVision` to base (it is in `core/capabilities`, which Task 6 moved), so the item is a core
  tool.
- **`ItemScoop`, `ItemBeesWax`, `ItemRefractoryWax`** arrived in `core` during phase 2, after the
  manifest was written.

**`ItemFruit` stays** and is deferred to 7b. Its destination was conditional on "whether anything
outside arboriculture instantiates it"; nothing instantiates it by name anywhere, so it goes with the
arboriculture fan-out. It does not leak, so leaving it costs nothing.

- [ ] **Step 4: Move the eight loose files**

```bash
cd src/main/java/forestry/core
git mv ClientsideCode.java   platform/client/ClientsideCode.java
git mv ForestryColors.java   platform/client/ForestryColors.java
git mv EventHandlerCore.java platform/EventHandlerCore.java
git mv PickupHandlerCore.java platform/PickupHandlerCore.java
git mv FluidProductTypes.java platform/fluids/FluidProductTypes.java
git mv TranslationKeys.java  platform/util/TranslationKeys.java
ls *.java
cd -
```

Expected: `ModuleCore.java` and `ModuleFluids.java` only.

`forestry.core.platform` now holds two loose classes, so it needs its own `package-info.java`:

```bash
sed "s@^package .*;@package forestry.core.platform;@" \
  src/main/java/forestry/core/platform/util/package-info.java \
  > src/main/java/forestry/core/platform/package-info.java
```

- [ ] **Step 5: Rewrite every reference**

The files moved individually, so nothing has been rewritten yet and the tree does not compile. One
call per old package:

```bash
SCRATCH=/tmp/claude-1000/-home-thedarkcolour-IdeaProjects-ForestryCE/a14b6292-0978-46b8-95b1-84eb1fb04cb8/scratchpad
$SCRATCH/rewrite-refs.sh forestry.core.tiles.TileMill        forestry.core.content.machines.TileMill
$SCRATCH/rewrite-refs.sh forestry.core.platform.render.RenderMill forestry.core.content.machines.RenderMill
```

For the rest, a per-class rewrite is needed because one package split several ways. Generate it from
what git recorded, which is exact and needs no hand-maintained list:

```bash
git status --porcelain | awk '/^R/ {print $2, $3}' \
  | sed 's@src/main/java/@@g; s@\.java@@g; s@/@.@g' \
  | while read -r from to; do
      [ "$from" = "$to" ] && continue
      $SCRATCH/rewrite-refs.sh "$from" "$to"
    done
```

Then confirm no old package name survives:

```bash
grep -rn "forestry\.core\.tiles\.\|forestry\.core\.blocks\.\|forestry\.core\.items\." src/main/java src/test/java 2>/dev/null | head
```

Expected: no output.

- [ ] **Step 6: Verify**

```bash
./gradlew compileJava compileTestJava
./gradlew checkApiBoundary checkBaseBoundary checkBaseBytecode 2>&1 | grep -E "clean|leaking|split module"
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: gates green, no datagen diff, all 108 tests passed.

`CreativeTabBaselineTest` and `GenomeBaselineTest` are both in that suite and both would notice a
dropped registration, so a green run is meaningful here in a way it is not for a pure package move.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "core: fan out tiles, blocks, items and the analyzer/escritoire screens

Move-manifest step 7.5, the only per-file step in 7a. core/{tiles,blocks,items}
and four gui classes split between platform and content.

TileMill reads as framework and is not - TileMillRainmaker is its only subclass -
so it goes to content.machines and takes RenderMill with it.

Every destination package is new and got a package-info.java: 194 of them carry
the nonnull-by-default annotation, and a package without one drops that default
silently.

The reference rewrite was generated from git's own rename records rather than a
hand-maintained list, so it cannot drift from what actually moved."
```

---

### Task 9: Step 7.6 - absorb the five base modules

Five package moves. Each top-level module becomes a content directory. Their internal `features/`,
`gui/`, `tiles/`, `blocks/`, `client/`, `network/` and `compat/` subpackages move unchanged, per D6.

**This is the step Tasks 1 and 2 exist for.** All five come into `checkBaseBoundary`'s scope for the
first time when they land under `core`.

**Files:** 220 files.

**Interfaces:**
- Consumes: `move-package.sh`; the severed leaks from Tasks 1 and 2.
- Produces: `forestry.core.content.{machines,energy,backpacks,sorting,worktable}`.

- [ ] **Step 1: Confirm the five are clean before moving them**

If this is not zero, Tasks 1 and 2 did not finish. **Do not move them and then baseline the
failure** - the gate reached zero in phase 6 and must not regress.

```bash
for p in factory energy storage sorting worktable; do
  printf "%-12s %s\n" "$p" "$(grep -rlE '^import (static )?forestry\.(apiculture|arboriculture|lepidopterology|farming|cultivation|mail)\.' src/main/java/forestry/$p --include='*.java' 2>/dev/null | wc -l)"
done
```

Expected: `0` for all five.

- [ ] **Step 2: Move them**

`forestry.core.content.machines` already exists from Task 8 (`TileMill`, `RenderMill`), so
`move-package.sh` will refuse it - its destination-exists guard is deliberate. Merge that one by
hand:

```bash
SCRATCH=/tmp/claude-1000/-home-thedarkcolour-IdeaProjects-ForestryCE/a14b6292-0978-46b8-95b1-84eb1fb04cb8/scratchpad
$SCRATCH/move-package.sh forestry.energy    forestry.core.content.energy
$SCRATCH/move-package.sh forestry.storage   forestry.core.content.backpacks
$SCRATCH/move-package.sh forestry.sorting   forestry.core.content.sorting
$SCRATCH/move-package.sh forestry.worktable forestry.core.content.worktable

# factory merges into the existing machines package
for f in src/main/java/forestry/factory/*; do
  git mv "$f" "src/main/java/forestry/core/content/machines/$(basename "$f")"
done
rmdir src/main/java/forestry/factory
$SCRATCH/rewrite-refs.sh forestry.factory forestry.core.content.machines
```

`factory` already has a `package-info.java`; moving it over the one Task 8 created would clobber it.
Check first and keep whichever carries the annotation:

```bash
head -3 src/main/java/forestry/core/content/machines/package-info.java
```

- [ ] **Step 3: The five module classes moved with their packages**

`ModuleFactory`, `ModuleEnergy`, `ModuleStorage`, `ModuleSorting` and `ModuleWorktable` keep their
module ids. Confirm discovery still finds all of them:

```bash
./gradlew runGameTestServer 2>&1 | grep -iE "registered .* module|module.*loaded" | head -5
```

Module discovery is annotation-scan based (`ModuleUtil.forEachAnnotated` reads FML scan data), so a
package move cannot break it - but confirm rather than assume, since a missing module is silent.

- [ ] **Step 4: Verify the gate now covers 220 more files and is still clean**

```bash
./gradlew compileJava compileTestJava
./gradlew checkApiBoundary checkBaseBoundary checkBaseBytecode 2>&1 | grep -E "clean|leaking|split module"
```

Expected: still `0 packaged leaking file(s), 20 datagen-only`, and `checkBaseBytecode` clean. **The
gate is now measuring 220 files it has never measured before** - that it stays at zero is the
substantive result of this task, not a formality.

- [ ] **Step 5: Verify behavior**

```bash
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: no datagen diff, all 108 tests passed.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "core: absorb factory, energy, storage, sorting and worktable

Move-manifest step 7.6. Each becomes a content directory under core; their
features/, gui/, tiles/ and client/ subpackages move unchanged per D6.

These 220 files enter checkBaseBoundary's scope for the first time - its
basePackages list has always been core/apiimpl/plugin/modules/compat, so
forestry.factory and friends were never scanned despite shipping in the base
jar. Tasks 1 and 2 severed the two leaks that would otherwise have surfaced
here. The gate staying at zero across 220 newly-measured files is the result
this step is really reporting.

factory merges into core.content.machines, which already held TileMill and
RenderMill from step 7.5."
```

---

### Task 10: Record 7a completion

**Files:**
- Modify: `docs/superpowers/specs/2026-07-30-feature-package-reorg-design.md`
- Modify: `docs/superpowers/specs/2026-07-30-phase-7-move-manifest.md`
- Modify: `.git-blame-ignore-revs`

- [ ] **Step 1: Confirm from a clean build**

```bash
./gradlew clean build 2>&1 | grep -E "checkApiBoundary|checkBaseBoundary|checkBaseBytecode|BUILD"
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `BUILD SUCCESSFUL`, api clean, `0 packaged leaking file(s), 20 datagen-only`,
`checkBaseBytecode` clean, all 108 tests passed.

- [ ] **Step 2: Add the move commits to the blame-ignore file**

```bash
git log --format='%H %s' -12 | grep -E "step 7\.|core: (platform|engine|fan out|absorb|registration)|api: group" 
```

Append each move commit's full sha to `.git-blame-ignore-revs` with its subject as a trailing
comment, matching whatever format the file already uses (`cat .git-blame-ignore-revs` first). Only the
mechanical moves belong there - **not** Tasks 1 and 2, which changed behavior.

- [ ] **Step 3: Update the manifest**

Mark steps 7.1 through 7.6 done, and record the four assignments the manifest was missing:
`apiimpl` and `core/features` stay, `core/data` stays until phase 8, `core/capabilities` goes to
`core.platform.capabilities`, and `forestry/plugin` is deferred to 7b because seven of its ten files
belong to content jars.

Also correct the manifest's own framing note - it says moves are "not a scripted step" and describes
IntelliJ operations; record that 7a was executed as prefix rewrites and why.

- [ ] **Step 4: Update the spec**

Mark phase 7a done in the `## Sequencing` block and add a paragraph recording: that
`checkBaseBoundary`'s `basePackages` had never covered the five absorbed modules, so two real leaks
(`FactoryRecipeTypes`, `CrateItems`) were invisible until they moved under `core`; that both had base
consumers and so needed the phase-4 relocation treatment rather than a one-line move; and that the
scripted rewrite caught 194 package-info annotations, 55 javadoc links and 30 inline FQ references
that an import-targeted rewrite would have missed.

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/specs .git-blame-ignore-revs
git commit -m "docs: record phase 7a completion, core in its target shape"
```

---

## Notes for 7b

- **`forestry/plugin` dissolves in 7b**, not here. `BeeTaxonomy`, `TreeTaxonomy`,
  `ButterflyTaxonomy`, `DefaultButterflySpecies` and the two analyzer plugins go to content jars;
  only `DefaultForestryPlugin` and `ForestryTaxonomy` are base, and the spec's target tree gives them
  no home, so 7b has to decide one. `forestry.core.plugin` is the obvious candidate.
- **`ItemFruit`** may still be in `core/items` after Task 8. Its destination depends on whether
  anything outside arboriculture instantiates it - resolve it with the arboriculture fan-out.
- The service file `forestry.api.plugin.IForestryPlugin` lists six implementations, four of which
  move in 7b (`apiculture`, `arboriculture`, `lepidopterology`, `farming` plugins) plus
  `forestry.compat.kubejs.KubeForestryPlugin` in step 7.12. `rewrite-refs.sh` handles the contents;
  `kubejs.plugins.txt` names `forestry.compat.kubejs.ForestryKubeJsPlugin` and is **not mentioned in
  the manifest** - do not lose it.
- The three Patchouli FQCNs are the manifest's one flagged hand-edit. `rewrite-refs.sh` covers
  `src/main/resources`, so they are handled automatically, but verify them explicitly - they have no
  compile-time or test oracle whatsoever.
- Re-run the `checkBaseBoundary` `basePackages` question at the start of 7b: once `compat` dissolves
  into `core.platform.compat`, the literal `'compat'` entry becomes dead and should be dropped.
