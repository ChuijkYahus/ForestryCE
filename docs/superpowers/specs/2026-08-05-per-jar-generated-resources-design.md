# Per-jar generated resource directories

Date: 2026-08-05
Branch: `1.21.1-restructure`
Status: design
Implements the "Successor work" section of `2026-08-05-mod-id-rename-merge-design.md`
Deletes the ownership machinery introduced by `89f82bf5a` and extended through phase 9b

## Problem

`generateResourceOwners` infers ownership that datagen already knows. It reconstructs which jar
owns `farm_crops_managed.json` by longest-prefix matching the file name against registry ids, then
scans the file text for `forestry:` references and promotes the file to the least derived jar that
can see all of them. `partitionSharedResources` then re-splits the five files whose unit of
ownership is the entry rather than the file.

The information was never missing. Inside a provider the live `IModFeature` is in hand and the
registering module is exact. The inference layer exists only because provider output all landed in
one directory, so something downstream had to take it apart again.

Every ownership bug on record came from that layer: `nether_bridge.json` colliding under
longest-prefix with a feature named `forestry:nether`, the `drop_honey` tag, the arboriculture
point-of-interest tag reaching a core-only install, and the `genetic_samples` short-circuit fixed
during the four-jar merge. Two further defects are open and recorded in the predecessor spec.

The scale is also wrong for the mechanism. Of 12,168 mapped files, 11,497 say `core`. About 600
lines of Groovy exist to place 671 files, and 97 of those are hand-written files that a `git mv`
places correctly with no mechanism at all.

## Target

Datagen writes to one root per jar. Each source set picks up its own with a plain `srcDir`. No
build step reads a resource file to decide where it belongs.

`src/generated/resources` holds 10,207 files today. They divide as follows, and each root below ends
up with the files its jar owns:

| root | source set | files after |
| --- | --- | --- |
| `src/generated/resources` | `main` | 9,633 plus core's share of the entry-keyed files |
| `src/generated/resources_farms` | `farms` | 428 |
| `src/generated/resources_butterflies` | `butterflies` | 82 |
| `src/generated/resources_mail` | `mail` | 64 |

Core's path does not change, so 9,633 files stay put and the diff is the 574 that move, plus the
three entry-keyed files that gain a second copy. The other 1,864 core-owned files are hand-written
under `src/main/resources` and never move either.

The 97 hand-written non-core files move out of `src/main/resources` into `src/farms/resources`,
`src/mail/resources` and `src/butterflies/resources` by `git mv`. They are textures, Patchouli
entries and a few hand-authored models.

## Design

### 1. Datagen lives in the source set that owns it

`src/datagen` is deleted. `sourceSets.datagen` goes with it. Each jar's providers move into that
jar's own source set, in a `data` subpackage of the jar's root package.

| source set | package | entry point |
| --- | --- | --- |
| `main` | `forestry.core.data` | `Data` |
| `farms` | `forestry.agriculture.data` | `AgricultureData` |
| `mail` | `forestry.mail.data` | `MailData` |
| `butterflies` | `forestry.lepidopterology.data` | `LepidopterologyData` |

Four entry points, one per jar. Core's cannot register a farms provider without naming a farms
type, so the entry points cannot be merged. That is the mechanism, not a cost.

Only core's carries `@EventBusSubscriber(modid = ForestryConstants.MOD_ID)`. The other three are
plain classes that core loads through a `ServiceLoader` SPI, `IForestryDataProvider`, listed in
each source set's own `META-INF/services`. A second `@EventBusSubscriber` naming another mod's id
is not injected, because `AutomaticEventSubscriber` requires the annotation's `modid` to equal the
mod being injected; naming all four through `--mod` instead would give each its own
`DataGenerator`, and the four would share one `.cache` and one output folder. One generator, one
`HashCache` and one `ExistingFileHelper` is safer, and the SPI is the pattern `IForestryPlugin`
already uses. Core still never names a content type, so section 2's guarantee is unaffected.

### 2. The compile classpath enforces the reference closure

The predecessor spec planned to keep `ownership.json` and the closure as a validator, on the
grounds that a directory says what a file is *about* while correctness turns on what a file can
*see*. That reasoning is right and this design satisfies it more strongly than a validator can.

`src/farms/java` already compiles against core plus farms and nothing else. A provider that lives
there cannot name `LepidopterologyBlocks`, so every file it writes references only ids present
wherever the farms jar is. The closure stops being a property checked after the fact and becomes a
property of the compile classpath, the same move phase 9a made when `checkBaseBoundary` and
`checkBaseBytecode` were retired in favour of the compiler.

This is what removes `ownership.json` as well as `resource-owners.json`. A validator that re-derives
from file text cannot be stronger than a compiler that reads the actual references.

Two recipes stop compiling in core and must move, `data/forestry/recipe/foresters_manual_butterfly.json`
and `data/forestry/recipe/lepidopterists_chest.json`, both to `forestry.lepidopterology.data`.

A third, `data/forestry/recipe/fabricator/electron_tubes/ender.json`, was listed here as a promotion
and is not one. It names `forestry:ender_electron_tube` and `forestry:liquid_glass`, both core. The
map assigns it to farms because its file name begins `ender` and the cultivation module registers a
block entity type called `forestry:ender`, so the cascade's longest-prefix name walk claims it. It
stays in core.

### 2a. Ten core files the map mis-ships today

That prefix walk is not a one-off. The cultivation module registers block entity types named
`forestry:ender`, `forestry:gourd`, `forestry:mushroom` and `forestry:nether`, and the walk hands
farms every core file whose name starts with one of those words. Ten files are affected, none of
which names a single farms-owned id:

| file | what it actually names |
| --- | --- |
| `recipe/fermenter/mushroom.json`, `mushroom_honey.json`, `mushroom_juice.json` | `forestry:biomass`, `forestry:honey`, `forestry:fruit_juice` |
| `recipe/carpenter/crates/unpack/minecraft/nether_bricks.json`, `nether_wart.json` | the core crate items |
| `recipe/carpenter/ender_pearl.json` | `forestry:pulsating_mesh` |
| `recipe/fabricator/electron_tubes/ender.json` | `forestry:ender_electron_tube`, `forestry:liquid_glass` |
| `tags/block/flowers/gourd.json`, `flowers/nether.json` | vanilla blocks only |
| `tags/block/hive_grounds/nether_extra_replaceable.json` | vanilla blocks only |

Because core's `processResources` excludes anything the map does not assign to core, all ten ship in
`forestryfarms` and in no other jar. Verified against the built artifacts: `core=0 farms=1` for each.
So a core-only install today has no fermenter mushroom recipes, no ender pearl carpenter recipe, and
no `flowers/gourd` or `flowers/nether` bee flower tags, which is a gameplay bug and not only a
packaging one.

This is the same failure the reorg spec records for `nether_bridge.json`. Nothing fixes it except
removing the inference: under per-jar output the ten are generated by core's providers into core's
root and ship in core. The last task must verify exactly that.

### 3. Datagen classes must not ship

Putting datagen back into the production source sets reinstates the arrangement `646e21a33`
removed. The failure it guards against is documented in `c540144fc`: lambda method references in
an `@EventBusSubscriber` class carry ModKit types in their synthetic method descriptors, FML's
annotation scanner calls `getDeclaredMethods0` at boot, and the JNI link step throws
`NoClassDefFoundError` when ModKit is absent. Excluding the package from the jar fixed it then and
fixes it now, because the scanner cannot see a class that is not in the jar.

What the datagen source set additionally bought was keeping ModKit off main's compile classpath.
That is replaced by an artifact-level gate, which is stronger:

- ModKit moves to `compileOnly` on each of the four source sets, and stays on the dev runs'
  runtime classpath as it already is.
- Each jar task carries `exclude 'forestry/**/data/**'`. No production package named `data` exists
  in any of the four source sets, so the pattern is unambiguous.
- `checkJarPartition` gains a bytecode assertion: no class in any built jar may reference
  `thedarkcolour/modkit`. This reads the shipped artifact, which a source set arrangement never
  did.

### 4. The ModKit change

Branch `1.21-neoforge`, whose tip `51b8760` is the pinned commit. Every `MK*Provider` already takes
a `PackOutput` in its constructor. The only reason they cannot be redirected is that `DataHelper`
calls `event.getGenerator().getPackOutput()` inside each `create*`, and `MKTagsProvider` reaches
through `helper.event` for its own. `PackOutput.PathProvider` has a package-private constructor, so
a shim is not available, but none is needed: `new PackOutput(otherRoot)` produces correctly rooted
path providers.

`DataHelper` gains a nested `Builder`. Its constructor takes `(String modid, GatherDataEvent
event)` and every value `DataHelper` currently reads off the event becomes an overridable field
defaulting to what the event supplies:

| builder method | default |
| --- | --- |
| `packOutput(PackOutput)` | `event.getGenerator().getPackOutput()` |
| `existingFileHelper(ExistingFileHelper)` | `event.getExistingFileHelper()` |
| `lookupProvider(CompletableFuture<HolderLookup.Provider>)` | `event.getLookupProvider()` |
| `includeServer(boolean)` | `event.includeServer()` |
| `includeClient(boolean)` | `event.includeClient()` |
| `addProvider(ProviderRegistrar)` | `DataGenerator::addProvider` |
| `entryFilter(Predicate<ResourceLocation>)` | `id -> true` |
| `logger(Logger)` | `LoggerFactory.getLogger(ModKit.ID + "/" + modid)` |

```java
@FunctionalInterface
public interface ProviderRegistrar {
	void addProvider(DataGenerator generator, boolean run, DataProvider provider);
}
```

`DataHelper(String, GatherDataEvent)` stays and delegates to `new Builder(modid, event).build()`,
so every existing ModKit consumer compiles unchanged. `MKTagsProvider` reads `helper.packOutput`,
`helper.lookupProvider` and `helper.existingFileHelper` instead of reaching through `helper.event`.

`entryFilter` gates `MKEnglishProvider.addTranslations()`, which is the only registry-walking
autogen Forestry reaches. `MKItemModelProvider.registerModels()` autogenerates only when one of its
three booleans is true and Forestry passes `createItemModels(false, false, false, ...)`, but it
takes the same filter so the two cannot diverge later.

`MKTagsProvider.copy()` resolves the block tag provider through `helper.tags`, which is per-helper.
Forestry's ten `copy()` calls are all core wood and charcoal tags and stay in core. A content jar
that needs a copy must add both tags explicitly. This is a documented constraint, not a blocker.

### 5. Each jar declares its modules

`entryFilter` needs "is this id registered by one of my modules". Module ids all live in
`ForestryModuleIds`, which is core API, so no jar names another jar's classes to answer it.

Each entry point declares its own set: core declares `CORE`, `FLUIDS`, `FACTORY`, `ENERGY`,
`STORAGE`, `SORTING`, `WORKTABLE`, `CURIOS`, `APICULTURE`, `ARBORICULTURE`; farms declares
`FARMING` and `CULTIVATION`; mail declares `MAIL`; butterflies declares `LEPIDOPTEROLOGY`. The
filter walks `ModFeatureRegistry` for those modules and matches on the registered id.

Core's `Data` asserts that the four declared sets union to the live module set from
`ModFeatureRegistry.getRegistries()`, failing datagen otherwise. This replaces
`OwnershipManifest`'s `unmapped` throw and keeps the property that a new module cannot silently
lose its resources.

### 6. The split, provider by provider

Removing the content jars from core datagen's classpath makes javac emit the work list. Every
provider hand-lists its content entries with direct imports, so each becomes a compile error naming
what must move. Known sites:

- `ForestryBlockStateProvider` imports `FarmingBlocks`, `CultivationBlocks`, `MailBlocks`,
  `EnumFarmBlockType`, `EnumFarmMaterial`, `FarmBlock`, `BlockTypeMail`
- `ForestryItemModels` imports `MailItems`
- `ForestryItemModelProvider` imports `LepidopterologyItems`
- `ForestryBlockLootTables` imports `LepidopterologyBlocks`
- `ForestryRecipeProvider` holds `registerFarmingRecipes`, `registerCultivationRecipes`,
  `registerMailRecipes`, `registerLepidopterologyRecipes` whole, plus a slice of
  `registerCarpenter` (8 mail). The `registerCarpenter`, `registerFabricator` and `registerFermenter`
  recipes this line once assigned to farms are the core files section 2a lists: the name walk
  claimed them, and every one of them names core ids only. They stay in core, and
  `AgricultureRecipeProvider` carries no machine recipes at all.

What each content entry point ends up owning:

| jar | providers |
| --- | --- |
| farms | blockstates, block models, item models, block loot tables, the `farming` and `cultivation` chest loot sub-tables, 3 block tags, recipes and their generated advancements |
| mail | blockstates, block models, item models, block loot tables, the `mail` chest loot sub-table, 1 item tag, recipes and their generated advancements |
| butterflies | 35 species, 31 taxa, butterfly mutations, blockstates, block models, item models, block loot tables, the `lepidopterology` chest loot sub-table, 4 recipes |

`ButterflySpeciesProvider` is already single-purpose and moves whole. `TaxonProvider` splits along
`ButterflyTaxonomy`; ranks above a species type stay in core, which is where the fixpoint pass
leaves them today. `MutationProvider` splits along its butterfly section.

Chest loot sub-tables are named `data/forestry/loot_table/chests/<chest>/<module>.json`, one file
per contributing module, so `lootModuleJars` becomes four providers each writing its own files.

### 7. Entry-keyed files stop being a special case

Five files are assembled by the game from every pack that supplies one. Each becomes two or more
providers writing to their own roots:

| file | contributors |
| --- | --- |
| `data/forestry/tags/item/genetic_samples.json` | core, butterflies |
| `data/minecraft/tags/block/mineable/pickaxe.json` | core, farms |
| `data/neoforge/data_maps/item/compostables.json` | core, farms |
| `data/neoforge/loot_modifiers/global_loot_modifiers.json` | every jar that ships a modifier |
| `assets/forestry/lang/en_us.json` | all four, via `entryFilter` |

`global_loot_modifiers.json` closes the first open defect from the predecessor spec by
construction: there is no longer a code path that can ship the whole list from one jar. The second
defect, the closure disagreeing with `ownerOfEntry` on an unresolvable `#forestry:` reference, dies
with the code that held it.

`generateEnUsLang` stays, reduced: it merges hand-written core `en_us` with generated core `en_us`
only. The other ten locales remain whole in core.

### 8. The data run

`--output` becomes `src/generated`, one level above the four roots, so `DataGenerator`'s
`HashCache` spans all of them and stale-file purging still works across a file changing jars. Each
entry point builds its own `PackOutput` explicitly; none uses `event.getGenerator().getPackOutput()`.

One consequence:

- `.cache` leaves `src/generated/resources`, which is a `srcDir`. Gradle's `Copy` includes hidden
  directories, so the cache files currently reach `build/resources/main` and the jar. Moving the
  root fixes that as a side effect.

`--existing` gains the three content resource roots so model parent validation resolves
hand-written models such as `assets/forestry/models/block/farm_parent.json` after they move.
`--existing` is repeatable.

`systemProperty 'forestry.ownershipManifest'` is deleted.

### 9. What comes out of build.gradle

Deleted: `generateResourceOwners` with its six-rule cascade, reference closure, taxon fixpoint,
texture-owner inference and `.mcmeta` follow pass; `partitionSharedResources`; `resourceOwnerOf`;
`idOwnerOf`; `qualifiedIdOwnerOf`; `entryKeyed`; `folderOwners`; `lootModuleJars`;
`worldgenOwners`; `visibility`; `partitionDir`; the `processResources` exclude filter; the four
per-module `from(...) include {...}` filters; `sourceSets.datagen` with its classpath wiring and
its `datagenCompileOnly` and `datagenImplementation` configurations.

Deleted files: `src/generated/ownership.json`, `src/generated/resource-owners.json`,
`src/datagen/java/forestry/core/data/OwnershipManifest.java`.

Added: four `srcDir` lines, four `exclude 'forestry/**/data/**'` lines, ModKit on four
`compileOnly` configurations, one bytecode assertion in `checkJarPartition`, three repeated
`--existing` arguments.

`testImplementation sourceSets.datagen.output` becomes plain `sourceSets.main.output` plus the
content source sets, which the test source set already has.

## Verification

**The relocation gate.** Every file currently under `src/generated/resources` must appear exactly
once across the four roots with identical bytes, except the five entry-keyed files, which must
recombine to their current content. This makes the whole change a pure relocation and is
mechanically checkable against the pre-change tree.

**The existing gates**, in order:

1. `./gradlew runData`, then the relocation gate above.
2. `./gradlew runGameTestServer` green.
3. `rm -rf run/boot-*`, then all five boot configurations reaching `Done`. The leftover-world and
   leftover-output traps from `jar-split-reference-closure` still apply.
4. `./gradlew check`, which runs `checkJarPartition` against the built jars, now including the
   ModKit bytecode assertion.

## Out of scope

- The ten non-English locales. They are hand-authored monoliths with no per-key metadata and stay
  whole in core, as deferred by the reorg spec.
- Splitting the Patchouli book beyond the 15 entries the current map already assigns to farms and
  mail. Those move with the `git mv`; the book's structure is unchanged.
- Validation of hand-written resources. After the 97 files move, nothing checks their references.
  They are textures, models and Patchouli entries, where a bad reference is a missing texture
  rather than a datapack load failure. A hand-written `data/` file under a content jar would not be
  covered; none exists today.
- `ModKit`'s newer branches. Only `1.21-neoforge` is touched.

## Risks

**The ModKit dependency is pinned by commit sha, so Forestry cannot build against an unpublished
change.** Development runs against `publishToMavenLocal` (`thedarkcolour.modkit:modkit:1.0`) with
`mavenLocal()` added to the repository list. The JitPack sha is pinned and `mavenLocal()` removed
as the last step, after the ModKit commit is pushed.

**Datagen classes in production source sets can drift back into the jar.** The exclude and the
bytecode assertion both run under `check`, and `checkJarPartition` reads the built artifact rather
than the source tree. Trap 1 of `jar-split-reference-closure` applies: a boot proves what loads,
only the artifact proves what shipped.

**A file can silently reach no root.** If a provider moves to a content entry point but its
registration is dropped from core's `Data` without being added to the content entry point, the
file simply stops being generated. The relocation gate catches this, which is why it compares the
full pre-change file set rather than spot-checking.

**Core's autogen filter is a positive list.** A new core module missing from core's declared set
would lose its generated lang. The union assertion in section 5 fails datagen in that case rather
than shipping a gap.
