# Feature-oriented package reorganization and jar split

Date: 2026-07-30, amended 2026-07-31 after adversarial review
Branch: `1.21.1-restructure`
Status: design approved, not started

## Problem

Code is organized into 13 runtime modules. Modules are not a granular enough unit:
`core` alone is 483 files and 39,697 lines, and holds four unrelated concerns.
`apiculture` and `arboriculture` each hold three or four distinct game features whose
classes are interleaved because the inner axis is *kind* (`blocks/`, `tiles/`, `items/`,
`gui/`) rather than *feature*. `apiculture/blocks/` contains the alveary, the apiary and
the wild hives side by side.

Two outcomes are wanted:

1. Finding code by feature. Open one directory, see everything that implements a game
   feature.
2. Splitting the mod into multiple jars so a pack can install a subset.

### Measured starting state

| package | files | lines |
| --- | --- | --- |
| `core` | 483 | 39,697 |
| `api` | 301 | 12,352 |
| `apiculture` | 191 | 13,014 |
| `arboriculture` | 185 | 11,520 |
| `factory` | 94 | 7,107 |
| `farming` | 84 | 5,700 |
| `mail` | 74 | 4,958 |
| `lepidopterology` | 53 | 3,582 |
| `worktable` | 34 | 1,456 |
| `apiimpl` | 34 | 3,194 |
| `sorting` | 33 | 1,838 |
| `modules` | 32 | 1,924 |
| `energy` | 30 | 1,806 |
| `cultivation` | 30 | 1,230 |
| `compat` | 30 | 1,070 |
| `storage` | 29 | 1,456 |
| `plugin` | 16 | 3,352 |

Resources: 10,232 generated + 1,959 hand-authored = 12,191 files. 11 locales. 40 test files.

## Decisions

| # | Decision | Rationale |
| --- | --- | --- |
| D1 | Six jars: `core`, `apiculture`, `arboriculture`, `lepidopterology`, `agriculture`, `mail` | Stated by project owner. Deps: everything requires `core`; `lepidopterology` also requires `arboriculture`. The code contradicted this; every edge was resolved on 2026-07-31 without adding one - see Graph decisions |
| D2 | `core` gets an internal layer split (`platform` / `engine` / `content`); content jars go straight to feature subpackages | `core` absorbs ~700 files under D1, so its internal shape does the real work. Layers give one enforceable edge: nothing in `platform` or `engine` may import `content` |
| D3 | `api` ships whole and undivided in the base artifact; only impl splits | `IForestryPlugin` names every module's registration type in its own signatures. Shipping api whole means those types always resolve; a missing content jar means no registered implementation, not a missing class. Verified: `IForestryPlugin` signatures name only api types, the `Forestry*Species` holders are pure `ResourceLocation` constants with no class-init side effects, and cross-jar `ServiceLoader` works in the FML game layer |
| D4 | The split is real and optional: a pack may install base + one content jar | Implies six mod ids and makes severing all cross-boundary leaks a hard prerequisite |
| D5 | One Gradle project, six source sets with explicit compile-classpath edges | Compiler-enforced dependency graph without a multi-project restructure. The build already proves the pattern with `addModdingDependenciesTo sourceSets.test`, and MDG's `ModModel` supports multiple mods with per-mod source sets |
| D6 | Content moves into feature dirs; registration holders, packets and client handlers stay as jar-level kind packages | `ApicultureItems` is naturally an index across all of apiculture. Splitting it five ways is churn, and it keeps `features` meaning what it already means in this codebase |
| D7 | Absent modules are represented by no-op managers carrying `isLoaded()`, not by throwing or `Optional` | Matches the established `Fake*` null-object idiom (`FakeErrorLogic`, `FakeClimateProvider`, `FakeBeekeepingLogic`, `FakeTankManager`, `FakeOwnerHandler`, `FakeErrorSource`). No API signature changes |

### The D3 safety condition, stated precisely

D3's mechanism is sound, but its safety condition is stricter than "no core imports".
`build.gradle:286-297` documents an existing crash: FML's annotation scanner resolves
method signatures at boot via `getDeclaredMethods0`, and `core/data/Data.java:23` is an
`@EventBusSubscriber` whose lambda synthetics carry ModKit parameter types, which crashes
the JVM link step in production. The same failure mode threatens this design.

The real invariant is therefore: **the base artifact must contain zero references to
split-jar types anywhere**, not merely zero imports in `forestry/core`. Any base class
reached by reflection whose signature names an absent class hard-crashes at boot.

### Naming

`forestry.modules.features` is the registration framework (`FeatureBlock`, `FeatureItem`,
`ModFeatureRegistry`, `FeatureTable`) and every module has a `features/` subpackage. That
word is taken, so it is not used for the new content directories. The framework moves to
`forestry.core.platform.registration`; per-jar registration holders keep the name
`features/`.

## Graph decisions

D1's dependency graph did not hold in code. Each edge was resolved on 2026-07-31; none of
them ended up requiring a new edge in the jar graph.

| Edge | Evidence | Decision |
| --- | --- | --- |
| arboriculture -> apiculture | `arboriculture/villagers/ArboricultureVillagers.java:52` uses `apiculture.blocks.NaturalistChestBlockType` | Not a graph edge - the enum is misfiled. Its consumers are `CoreBlocks`, `CoreTiles`, `ForestryCreativeTabs`, `ForestryBewlr` and `ForestryRecipeProvider`, all base, plus one arboriculture file. It moves to `core.platform.block`, which also removes five core -> apiculture references. Folded into bucket D |
| agriculture -> arboriculture | `farming/logic/farmables/FarmableGE.java:47` calls `ArboricultureBlocks.SAPLING_GE.blockEqual(state)` | Sever. One call site; resolve the sapling by block tag or registry id instead. Without arboriculture the Arboretum still farms vanilla saplings and simply never matches Forestry ones - degrades rather than breaks |
| base -> apiculture (recipe type) | `factory/features/FactoryRecipeTypes.java:21` registers `HygroregulatorRecipe` | Not a graph edge - one misplaced registration line. The hygroregulator is an alveary component, `IHygroregulatorRecipe` is already in api, and the impl already lives in apiculture. Only the `REGISTRY.recipeType(...)` call moves |
| base -> apiculture (crates) | `storage/features/CrateItems.java:95-100` registers crated bee products | Needs a crate extension point so apiculture registers its own crates. Base cannot register apiculture items and the only alternative is abandoning the split. Bucket-B-shaped work |
| base -> apiculture (analyzer fuel) | `core/inventory/PortableAnalyzerInventory.java:42` gates its fuel slot on `ForestryTags.Items.DROP_HONEY`, populated only by `ApicultureItems.HONEY_DROP` and `HONEYDEW` | Move `HONEY_DROP` and `HONEYDEW` to `core.content.resources`. This also populates the `drop_honey` tag from base, so the tag resolves without apiculture |
| mail -> apiculture (data level) | `data/forestry/recipe/stamp_1n.json` references tag `forestry:drop_honey` | Dissolved by the decision above. See the correction below |

### Correction: mail is not gameplay-dead without apiculture

An earlier draft recorded that mail installed without apiculture has uncraftable stamps. That
is wrong. The `Z` key in `stamp_1n.json` is an alternatives list:

```json
"Z": [ { "tag": "forestry:drop_honey" }, { "item": "minecraft:slime_ball" } ]
```

Slime balls are vanilla, so stamps were always craftable. The real exposure was narrower - the
tag file `data/forestry/tags/item/drop_honey.json` lists two items that would not exist, which
fails datapack load. Moving `HONEY_DROP` and `HONEYDEW` to base resolves that too.

### Accepted cost: the Portable Analyzer is bee-gated

Honey drops are produced only by centrifuging bee combs. Moving the items to base makes them
registerable, not obtainable, so a base-only install has a Portable Analyzer whose fuel slot
accepts an item nothing in base can produce. This was accepted deliberately over adding a
vanilla-sourced recipe, which would have been a balance change visible to every player
including those who never split the jars. The tool is inert rather than broken, in the same
spirit as D7's no-op managers, but with no in-game signposting of why.

### Cross-jar recipes: no general policy yet

The one known instance dissolved by relocation rather than by conditions, so no
`neoforge:conditions` strategy is committed. No systematic audit of cross-jar recipes has been
run. That audit belongs to phase 9, when the resource partition makes each recipe's owning jar
explicit; if it turns up further instances, a conditions-and-alternates policy is the expected
answer.

**Cross-jar loot modifiers likewise.** `data/forestry/loot_modifiers/chests/abandoned_mineshaft.json`
is a single file whose `extensions` list spans `["apiculture", "factory", "storage"]`.
These must be redesigned into per-module modifiers, not merely partitioned. Mitigating:
the chest sub-tables are already per-module (`chests/<chest>/<module>.json`) and
`global_loot_modifiers.json` merges across packs.

## Target package tree

### Base jar: `forestry`

```
forestry/
  api/                      whole, undivided, ships in base
    plugin/                 UNCHANGED, flat, concern-first
    client/                 UNCHANGED, concern-first
    core/                   was api/{genetics,recipes,fuels,storage,
                            multiblock,circuits,climate} + loose types
    apiculture/  arboriculture/  lepidopterology/  mail/
    agriculture/            was api/farming

  apiimpl/                  api implementations + the plugin manager;
                            stays in base, but must stop constructing
                            content classes - see bucket I
  modules/                  module framework; clean today, no content refs
  compat/                   only the shared plumbing stays; per-mod
                            integrations move to their owning jar

  core/
    platform/               no game content, no genetics
      block/  tile/  item/  inventory/  gui/  network/
      fluids/               tanks, filters, FluidHelper
      multiblock/           the framework only
      registration/         was modules/features
      owner/  errors/  config/  commands/  loot/  particles/
      render/  models/  client/
      util/
    engine/
      genetics/             Genome, Karyotype, Species, SpeciesType,
                            alleles/, mutations/, root/, BreedingTracker
      climate/
      circuits/
    content/
      machines/             was factory, plus its recipes
      energy/               biogas + peat engines
      backpacks/            was storage
      sorting/              genetic filter
      worktable/
      escritoire/           EscritoireGame*, TileEscritoire
      analyzer/             TileAnalyzer, PortableAnalyzerItem
      soil/                 BlockHumus, BlockBogEarth
      tools/                Wrench, Pipette, Spectacles, SolderingIron,
                            ForestersManual
      resources/            BlockResourceStorage, ItemCraftingMaterial,
                            ItemElectronTube, ItemFertilizer
```

`forestry/plugin` does not survive as a base package. Its seven content-registering
classes (`DefaultBeeSpecies`, `DefaultTreeSpecies`, `DefaultWoods`, `DefaultFarms`,
`DefaultForestryPlugin`, `client/BeeAnalyzerPlugin`,
`client/DefaultForestryClientRegistration`) distribute to their owning jars in phase 5.

### Content jars

```
apiculture/               JAR forestry_apiculture       -> core
  bees/  apiary/  beehouse/  alveary/  hives/  apiarist/
  features/  network/  client/  commands/  compat/
  ModuleApiculture

arboriculture/            JAR forestry_arboriculture    -> core
  trees/                  TreeSpecies, TreeManager, genetics/
  wood/                   ForestryWoodType, VanillaWoodType, WoodAccess,
                          WoodHelper + the 20 BlockForestry{Log,Plank,
                          Fence,Stairs,Sign,...} classes
  leaves/                 BlockAbstractLeaves, BlockDefaultLeaves,
                          ForestryLeafType, ILeafTypeBlock
  fruit/                  Fruit, PodFruit, RipeningFruit, BlockFruitPod
  sapling/  charcoal/  worldgen/
  features/  network/  client/  commands/  compat/
  ModuleArboriculture

lepidopterology/          JAR forestry_lepidopterology  -> core, arboriculture
  butterflies/  cocoons/  entities/
  features/  client/  commands/  compat/
  ModuleLepidopterology

agriculture/              JAR forestry_agriculture      -> core
  farmlogic/              FarmLogic* (17) + crops/ + farmables/ + FarmType,
                          shared by both farm implementations
  multifarm/              was farming: the multiblock
  planter/                was cultivation: Arboretum, Bog, Crops, Ender,
                          Gourd, Mushroom, Nether
  features/  client/  compat/  ModuleAgriculture

mail/                     JAR forestry_mail             -> core
  letters/  postoffice/  tradestation/  carriers/  postalstates/
  features/  network/  client/  gui/  commands/  compat/  ModuleMail
```

### Notes on placement

- `compat/` does not survive as a top-level directory. JEI plugins, Patchouli pages and
  KubeJS bindings are per-feature, and under an optional split the apiculture JEI plugin
  must ship in the apiculture jar. It dissolves into a `compat/` subpackage per jar, with
  only shared plumbing staying in base.
- `TileApiaristChest`, `TileArboristChest` and `TileLepidopteristChest` are misfiled in
  `core/tiles` today. The shared `TileNaturalistChest` base stays in
  `core/platform/tile/`; the three concrete chests move to their jars.
- `arboriculture/wood` stays inside arboriculture. It is ~25 classes with no genetic
  coupling and could plausibly be a seventh jar. Splitting a jar out later is cheaper
  than merging two back, so this is deferred.
- `core/content/resources` is an odds-and-ends bucket. Deliberately deferred; easier to
  refine once the move is done.

## Build structure

```
src/core/java              forestry/{api,apiimpl,modules,core}/**
src/core/resources         + templates/META-INF/neoforge.mods.toml
src/core/generated
src/apiculture/java        forestry/apiculture/**
src/apiculture/resources
src/apiculture/generated
...  arboriculture, lepidopterology, agriculture, mail

sourceSets {
  core
  apiculture      { compileClasspath += core.output }
  arboriculture   { compileClasspath += core.output }
  lepidopterology { compileClasspath += core.output + arboriculture.output }
  agriculture     { compileClasspath += core.output }
  mail            { compileClasspath += core.output }
}
```

**Mod ids.** A jar loads as a mod only if it carries `neoforge.mods.toml`, so optional
content jars become six mod ids: `forestry`, `forestry_apiculture`,
`forestry_arboriculture`, `forestry_lepidopterology`, `forestry_agriculture`,
`forestry_mail`. Verified that this is forced rather than chosen: `FMLModType` library
jars do not become resource packs (`ResourcePackLoader` builds packs from `ModList` mod
files only), and multiple `[[mods]]` blocks in one file give no optionality. Registering
`forestry:` names from a mod whose id is `forestry_apiculture` is unrestricted - NeoForge
21.1.230 has no alternative-prefix check. Each content jar declares a `REQUIRED` dependency
on `forestry` at a pinned version range; `lepidopterology` also requires
`forestry_arboriculture`. Nothing in-game changes for players.

**Service files.** `META-INF/services/forestry.api.plugin.IForestryPlugin` currently names
two classes: `forestry.plugin.DefaultForestryPlugin` and
`forestry.compat.kubejs.KubeForestryPlugin`. The first is one class registering bees,
trees, butterflies, farms, circuits and errors together; it becomes one plugin per jar,
each jar shipping its own service entry. This is the mechanism that makes optional
installs work: no apiculture jar means no apiculture plugin on the service path, so
nothing registers and `registerApiculture` is never called. Plugins are id-sorted, so
cross-jar ordering stays stable. `IForestryApi` and `IForestryClientApi` stay single
implementations in base.

**Access transformers** stay in base. They are global to the runtime; splitting them buys
nothing and risks ordering surprises.

**Resources.** Only the 1,959 hand-authored files under `src/main/resources` need real
partitioning. Generated resources are disposable and follow their providers automatically,
then get regenerated. Runtime merging verified against 1.21.x sources: `ClientLanguage`
merges via `getResourceStack` and server-side `LanguageHook.java:63` likewise; tags merge;
atlases merge via `SpriteSourceList.java:85`, so this repo's
`assets/forestry/atlases/{blocks,gui}.json` split cleanly; models, blockstates, recipes and
loot tables have unique per-item paths. The exception is cross-jar loot *modifiers*, noted
above under Graph decisions.

**Creative tabs.** Per-jar tabs are viable: NeoForge's `CreativeModeTabRegistry` null-checks
both endpoints of a sort edge, so `withTabsBefore/After` across optional jars degrades
gracefully. Only the main FORESTRY tab needs a real extension point.

**Lang.** `generateEnUsLang` merges the hand-written `en_us.json` over the generated one,
manual winning. It becomes a parameterized task instantiated six times. The 10 non-English
locales are hand-authored monoliths with no generated counterpart, so splitting them needs
tooling that does not exist yet - see Deferred.

**Datagen.** `core/data` is currently excluded from the production jar because ModKit types
in datagen lambdas crash FML's boot-time annotation scanner. Datagen becomes its own source
set per jar (`coreData`, `apicultureData`, ...) that is never packaged, which removes the
`exclude 'forestry/core/data/**'` hack and its failure mode. Note that a data run takes one
`--output`, so this needs six data run configurations with chained `--existing`, or a
post-hoc split. This is the least-supported part of the build story and should be
prototyped early.

## Gating work

The base artifact ships `api`, `apiimpl`, `modules`, `compat`, `plugin`, `Forestry.java`
and `core`. Measured leaks into the five split modules:

| package | files | imports |
| --- | --- | --- |
| `core` | 49 | 216 |
| `apiimpl` | 12 | 22 |
| `plugin` | 7 | 42 |
| `modules`, `compat`, `Forestry.java` | 0 | 0 |
| **base impl total** | **68** | **280** |
| `api` (separate concern) | 16 | 10 crossing a jar boundary, 6 core-only |

None of this is fixed by moving directories.

| Bucket | Files | Work |
| --- | --- | --- |
| A. Datagen providers | 20 | Relocate into per-jar datagen source sets. `ForestryRecipeProvider` alone has 27 content imports and splits by recipe owner |
| B. Central indexes | 7 | `ForestryCreativeTabs` (19 imports), `PacketIdClient`, `PacketIdServer`, `CoreBlocks`, `CoreItems`, `CoreTiles`, `CoreDataComponents`. Each becomes an extension point jars contribute to. `IPacketRegistry` already exists in `api/modules`; the packet-id enums have not caught up to it |
| C. Species-type-aware engine | 11 | Six `*SyncPacket`s plus `GeneticsReloadHandler`, `ProductTypes`, `GeneticsUtil`, `TreeUtil`, `CoreLootFunctions`. **Note:** `GeneticsReloadHandler.rebuildMutations` already iterates registered species types. The 12 content imports live in the typed projector methods (`rebuildSpecies`, `rebuildTreeSpecies`, `rebuildButterflySpecies`), which take per-type definition maps and cannot be iterated - they dissolve into the per-jar reload managers that bucket H relocates |
| D. Misfiled content | 3 | `ApiaristPoolElement` to apiculture, `FeatureHelper` to arboriculture, `ItemSpectacles` (imports `arboriculture.capabilities.SpectacleVision`) to arboriculture or the capability moves to base. Plus the three naturalist chests |
| E. Render and GUI plumbing | 5 | `ParticleRender`, `ForestryBewlr`, `ModelBakerModel`, `TankWidget`, `LevelStructureView`. Mostly interface extraction. `ParticleRender` matters extra because `api/apiculture/genetics/IBeeEffect` imports it |
| H. Central lifecycle wiring | 3 | `ModuleCore` registers every species manager (`BeeSpeciesManager`, `TreeSpeciesManager`, `ButterflySpeciesManager`, `BeeEffectManager`, `FlowerTypeManager`, `TaxonManager`) plus `GrafterLootModifier`; `EventHandlerCore` wires apiculture AI, effects and villagers; `CoreClientHandler` registers four jars' blocks and items. Each jar registers its own via its own module and plugin |
| I. api implementation wiring | 12 | `apiimpl`. `ForestryApiImpl:14-19` directly constructs `HiveManager`, `TreeManager`, `FarmingManager`. `plugin/PluginManager:35-36,187-194` imports `FarmingManager` and `FilterManager` and hard-wires `registerFarming()` by constructing `FarmingRegistration`. The registration builders (`ApicultureRegistration`, `ArboricultureRegistration`, `BeeSpeciesBuilder`, `TreeSpeciesBuilder`, `ButterflySpeciesBuilder`, `HiveBuilder`, `FarmTypeBuilder`, `SpeciesTypeBuilder`, `client/ClientHelper`, `client/TreeClientManager`) implement api/plugin interfaces that D3 ships in base while constructing content classes. Needs a per-module registration extension point in `PluginManager` - the same kind of work as bucket B, on the most central class in the design |
| F. api to impl | 16 | 10 cross a jar boundary and are hard blockers. The other 6 reach only core impl and are cleanup |
| G. Default content plugins | 7 | `plugin/DefaultForestryPlugin` plus `DefaultBeeSpecies`, `DefaultTreeSpecies`, `DefaultWoods`, `DefaultFarms`, `client/BeeAnalyzerPlugin`, `client/DefaultForestryClientRegistration`. Splits six ways |

Buckets A, B, C, D, E and H account for all 49 `core` files (20 + 7 + 11 + 3 + 5 + 3).
Bucket I accounts for all 12 `apiimpl` files, bucket G for all 7 `plugin` files, and
bucket F for the 16 `api` files.

### F is cheaper than it looks

`ForestryTags` is the sharpest api blocker because it makes *base* depend on arboriculture.
It is 84 constants - 42 block tags and 42 item tags - that are pure aliases for
`ForestryWoodType.X.blockTag` and `.itemTag`. There are 43 wood types; `SOUR_CHERRY` has no
alias. Line 36 already carries `// todo remove in favor of directly using ITreeManager`.
(The comment near line 268 is about `ForestryTags` / `ForestryWoodType` class-init
circularity, not the jar edge.)

Deleting them in favor of `ITreeManager` removes a hand-maintained list that can drift from
the wood types, and in-repo the deletion is nearly free - the sole consumer is
`arboriculture/PodFruit.java`. Two caveats: `ITreeManager.java:3-4` itself imports
`ForestryWoodType` and `VanillaWoodType` in javadoc only, which must also go; and the
replacement API `getLogBlockTag(IWoodType, boolean)` is instance-based, so an addon
replacing a deleted constant must obtain an `IWoodType`, which today means the arboriculture
enum. This is one of three api-to-arboriculture edges, not all of them; phase 1 covers the
rest.

### Absent-module semantics

`IForestryApi` exposes `getHiveManager()`, `getTreeManager()` and `getFarmingManager()`.
`IForestryClientApi` exposes `getBeeManager()`, `getTreeManager()` and
`getButterflyManager()`. All six can be queried with the owning jar absent.

Per D7, base supplies no-op implementations. Each manager interface gains `isLoaded()`,
returning `true` from real implementations and `false` from the no-ops, so addons can check
functionality without reaching for `IModuleManager` and a module id. Signatures stay
non-null and non-throwing, so defensive addon code does not crash on a partial install.

## Sequencing

Everything that makes the split real lands before a single package moves. Phases 1 through
6 are independently reviewable and shippable on the current layout.

```
0a  prerequisites  DONE 2026-07-31     every graph edge resolved by severing
                                       or relocating, none added; runData
                                       determinism proven by two consecutive
                                       runs with written: 0 and no diff
1a  DONE 2026-07-31                    checkApiBoundary gate, 3 javadoc-only
                                       imports, the 84 ForestryTags aliases,
                                       the 3 life stage enums. 16 -> 9 files
1b  DONE 2026-07-31                    5 relocations into api, 2 SPI
                                       inversions, chromosome machinery
                                       moved. 9 -> 0 files, gate green
2   DONE 2026-08-01                    creative tabs, packet ids,
                                       Core{Blocks,Items,Tiles,DataComponents}
3   DONE 2026-08-01                    species sync packets, TaxonManager,
                                       TreeUtil, GeneticsReloadHandler split
4   DONE 2026-08-01                    SpectacleVision, FeatureHelper, the
                                       apiarist house, ParticleRender split,
                                       patternTypeId, per-module lifecycle
5   split the default plugins          bucket G: six plugins, still one jar,
                                       still one service file
5a  PluginManager extension point      bucket I: apiimpl stops constructing
                                       content classes
6   no-op managers + isLoaded()
    ---- gate: the base artifact references no split-jar types ----
7   package moves                      api renames, core/{platform,engine,
                                       content}, feature dirs. Twelve ordered
                                       steps in the phase 7 move manifest
8   datagen -> per-jar source sets     deletes the exclude hack
9   build split                        six source sets, mods.toml, service
                                       files, resource partition, lang merge
10  publish six artifacts
```

Phase 1 landed 2026-07-31. `checkApiBoundary` is green: `forestry.api` imports nothing outside
`forestry.api`, and it now runs as part of `check`. Bucket F is closed.

Seven types moved into api - `IWatchable`, `IFilterSlotDelegate`, `IInventoryAdapter`,
`VillageHive`, `IGeneticTooltipProvider`, `Chromosome`, `ChromosomeFactory` - and two were
added: `IIndividualItem` and `GeneticTranslationKeys`. `IGeneticManager` gained
`genomeComponent()`. Two api default methods were inverted behind api interfaces the impl
implements.

Five of those seven turned out to reference only api or Minecraft types already, so they were
relocations rather than the new wrapper interfaces the bucket-F estimate assumed. Every step
was verified against a byte-identical `runData` diff and the GameTest suite, which grew from
96 to 98 with the life stage guard.

Phase 2 landed 2026-08-01. Bucket B is closed and `checkBaseBoundary` now ratchets the whole base
artifact against a checked-in baseline: 68 files at the start of the phase, 60 remaining. It fails
both on a leak absent from the baseline and on a baseline entry that no longer leaks, so the count
can only fall.

Only one of the seven bucket-B files needed an extension point. Four were misfiled types and two
were per-module constant holders. Two prerequisites surfaced during execution that file-level import
analysis could not see: `addGeneticBasics`, shared by four creative tabs, named two apiculture items,
and the lepidopterology tab named the apiculture scoop - so `SCOOP`, `HONEY_DROP` and `HONEYDEW`
moved to core first. Two new golden-master GameTests were added, for creative tab membership and for
data component registration, because neither datagen nor any existing test saw those.

Phase 3 landed 2026-08-01. Bucket C is closed; `checkBaseBoundary` is at 49 of the original 68.
For the third phase running the bucket was mostly relocation rather than redesign: one leak was a
dead import, six files belonged to a content module outright, three were single registrations in the
wrong jar, and only `GeneticsReloadHandler` needed splitting. The spec's prescription for bucket C -
make the typed rebuild methods iterate registered species types - was wrong; they take typed
definition maps and call per-type projectors, so they had to move instead.

Two smaller corrections to the plan surfaced during execution. `ProductTypes.register` was already
public, so the "one small extension point" this phase was supposed to add did not need adding.
`ModuleApiculture.doSelfPollination` went to `ModuleArboriculture` rather than to a core config: it
has exactly one reader, `TreeUtil.canPollinate`, and no config binding of any kind, so a core surface
would have been dead weight.

Phase 4 landed 2026-08-01. Buckets D, E and H are closed; `checkBaseBoundary` is at 39 of the
original 68. The dead-import sweep that paid off in phase 3 came back empty here, over all ten files
and all 45 leaking imports - it is settled for these buckets.

Buckets D and E stayed true to the pattern: `SpectacleVision` was a misfiled 14-line enum whose old
package held nothing else, `FeatureHelper` was 647 lines of tree worldgen whose 47 users were all
already in `arboriculture.worldgen`, and `TankWidget`'s farm special case vanished once
`IContainerTank` was split out of `IContainerLiquidTanks` - `ContainerFarm.getTank` already had the
exact signature. Only `LevelStructureView` needed a real extension point, and the abstraction was
already next door: `MultiblockTileEntityForestry` declares an abstract `getPattern()`, so
`patternTypeId()` is the same shape.

Bucket H was different in kind and is the template for phase 5a. `ModuleCore`'s reload and sync
blocks encoded a cross-module ordering the data genuinely requires, and it survives relocation only
because `ForestryModuleManager` topologically sorts by `getModuleDependencies`. Apiculture and
arboriculture declared no dependencies at all, so their position relative to core was discovery
order; both now declare `CORE`. Core stays the sequencer and iterates modules through two new
`IForestryModule` hooks. There is a `todo` on that loop asking whether an event with explicit
ordering phases would beat leaning on module dependency order.

Three plan errors that execution caught, all of the same kind - assuming a survey was complete.
`ModelLeaves.TRANSFORMS` had three users, not one; the two extra were in its own package and so had
no import to find them by. `BeeClientManager` was assumed baselined and was not, so moving
`addBeeHiveFX` would have relocated a leak rather than removed one - `checkBaseBoundary` failed and
the fix was to give apiculture its client manager, which needed `setBeeManager` to take
`IBeeClientManager` like its two siblings already did. And `CoreItems.HONEY_DROP` sits under an
`// Apiculture` comment in the colour handlers but has been a core item since phase 2.

Phase 7 is the reorganization originally asked for, and it is the cheapest phase, but see
the oracle blind spots below before treating it as purely mechanical. The difficulty lives
in phases 1 through 6, which no amount of directory rearrangement addresses.

The per-step move lists live in `2026-07-30-phase-7-move-manifest.md`: twelve ordered steps,
package-level moves where a whole package relocates and per-file assignment where one fans
out. Files needing a decision before they move are marked there.

### Who performs phase 7

The JetBrains MCP server exposes exactly one refactoring, `rename_refactoring`, which
renames a symbol in place. There is no move-class or change-package tool, and `execute_tool`
only dispatches to the same tool set. Phase 7 is overwhelmingly *move* operations, so:

- **A human drives the moves** in IntelliJ (Move Package / Move Class, `F6`). This is not a
  scripted step. `rename_refactoring` covers only leaf renames where the parent package is
  unchanged, such as `ModuleFarming` to `ModuleAgriculture`.
- **The oracles are drivable by tooling.** The IDE exposes `Data` and `GameTestServer` as
  run configurations, so the datagen regeneration, the byte-diff and the GameTest suite can
  all be run without a manual Gradle invocation. `build_project`, `get_file_problems` and
  `lint_files` give a fast compile-and-diagnose between steps.
- **`analyze_calls` belongs to phases 1 through 6, not phase 7.** IDE call-hierarchy data is
  what makes severing `ForestryCreativeTabs`' 19 content references and designing the
  `PluginManager` extension point tractable. It adds nothing to a mechanical move.

One measurement note. The 280-import figure comes from grepping `^import`, which misses
inline fully-qualified references. There are exactly two - `apiimpl/plugin/PluginManager.java:151`
and `core/ModuleCore.java:188`, both calling `forestry.apiculture.genetics.*` without an
import - and both files are already counted in buckets I and H, so no file count changes.
The enforcement gate is unaffected either way: ArchUnit reads bytecode, where a fully
qualified reference compiles identically to an imported one.

The phase-6 gate is stated as an artifact-level property, not a package-level one, per the
D3 safety condition. Note that it can only be *proven* by the ArchUnit test until phase 9:
module discovery is annotation-scan based (`ModuleUtil.forEachAnnotated`) with no observed
config off-switch, so nothing can boot without a module until the jars actually separate.

This spec covers the whole project deliberately, because the phase ordering is the main
design decision in it. It is too large for one implementation plan. Each phase gets its
own plan, written when that phase starts, so earlier phases can inform later ones.

## Verification

- **Datagen diff.** For phase 7, a pure move, regenerating must produce a byte-identical
  tree; any diff is a real defect. For phases 1 through 6 every diff must be explained.
  **This oracle must be validated before it is relied on**: run `runData` twice and diff,
  at phase 0a. Determinism has been broken and fixed at least twice here - commit
  `b8b4f9abc` ("en_us.json now generates deterministically") and the explicit sort at
  `TaxonProvider.java:71` working around an `IdentityHashMap`. `BeeSpeciesProvider.java:97`
  still uses an `IdentityHashMap`, currently harmless because it is reverse-lookup only.
- **FQCN references in hand-authored files.** The datagen diff cannot see these and
  GameTests do not exercise them. Hand-authored JSON names Java classes directly:
  `forestry.compat.patchouli.component.FluidComponent`,
  `forestry.compat.patchouli.processor.CarpenterProcessor`,
  `forestry.compat.patchouli.processor.FabricatorProcessor`. `META-INF/services` files
  carry FQCNs of classes that move in phases 5 through 9. Add a grep-based check that every
  FQCN appearing in a resource file resolves to an existing class.
- **GameTests.** `./gradlew runGameTestServer` stays green throughout. It was green as of
  2026-07-28.
- **Boundary test.** An ArchUnit-style test is added at phase 1 and tightened at each
  phase, enforcing the dependency graph long before the compiler can at phase 9. Until
  phase 9 this is the *only* proof available for the phase-6 gate.
- **Boot configurations.** Base alone, base + apiculture, and all six. Only runnable from
  phase 9.

## Risks

**Branch divergence, worse than it looks.** `1.20.1` is not merely an older version - it
builds with `net.neoforged.moddev.legacyforge` 2.0.80, i.e. legacy Forge, against
NeoForge on `1.21.1`. Phases 1 through 6 will therefore be manual ports rather than
cherry-picks, and bucket B's work (packet registration, creative tabs) sits precisely where
the Forge 1.20.1 and NeoForge 1.21.1 APIs diverge most. After phase 7 even manual porting
becomes impractical. Decide early whether `1.20.1` is frozen.

**One breaking wave, deliberately.** Phase 1 removes 84 public constants and phase 7 renames
api packages; both break any addon that touches the code. No release is cut ahead of this
work: the whole restructure ships as a single breaking change so addon authors migrate once
rather than tracking a sequence of them. That is why there is no phase 0.
`ForestryCE Migration Guide.md` is the natural home for the mapping table.

The cost is accepted knowingly: without a fresh tag there is no recent known-good build to
bisect against if phases 1 through 6 regress something. The GameTest suite and the
byte-identical `runData` diff are the compensating controls.

**Registration-order shift.** Six mods register in mod-sort order instead of one mod's
module order, and plugins sort by id, so six ids replace one. Mostly benign, but it will
churn phase-9 datagen diffs and any order-sensitive display.

**Blame.** Use `git mv` and add the phase-7 bulk move to `.git-blame-ignore-revs`.

## Deferred

- Whether `arboriculture/wood` becomes a seventh jar.
- The exact contents of `core/content/resources`.
- Whether module ids rename to match jar names. The 13 existing module ids are referenced
  by config, so they stay as-is for now; `farming` and `cultivation` both map into the
  `agriculture` jar without either id changing.
- **Patchouli book ownership.** One `foresters_manual` book documents all modules.
  Splitting entries per jar versus keeping the book and the REQUIRED patchouli dependency
  in base is undecided.
- **Locale split tooling.** The 10 non-English locale files are hand-authored monoliths
  with no generated counterpart to derive per-jar key ownership from.

## Review history

2026-07-31: adversarially reviewed against the codebase and against NeoForge/MDG sources.
The review confirmed every measured figure in the original draft, and confirmed D3's
mechanism, the six-mod-id conclusion, the D5 build shape and the runtime resource-merging
claims. It found one substantive defect: the gating audit scoped to `forestry/core` and
`forestry/api`, but the base artifact also ships `apiimpl` and `plugin`, leaving 19 files
and 64 imports uncounted - including `ForestryApiImpl` and `PluginManager`, the two classes
D3 depends on most. That produced buckets I and G, the restated phase-6 gate, and the graph
problems that Graph decisions now resolves. It also corrected the alias count from 86 to 84,
corrected bucket C's prescription, and established that `1.20.1` is a legacy-Forge branch.

2026-07-31: phase 0a resolved every graph edge, dropped phase 0, and corrected the review's
claim that mail is gameplay-dead without apiculture - the stamp recipe already accepts vanilla
slime balls. Added the Portable Analyzer fuel finding, which the review did not catch either.
