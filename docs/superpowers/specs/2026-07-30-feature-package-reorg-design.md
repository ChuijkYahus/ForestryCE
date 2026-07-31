# Feature-oriented package reorganization and jar split

Date: 2026-07-30
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
| D1 | Six jars: `core`, `apiculture`, `arboriculture`, `lepidopterology`, `agriculture`, `mail` | Stated by project owner. Deps: everything requires `core`; `lepidopterology` also requires `arboriculture` |
| D2 | `core` gets an internal layer split (`platform` / `engine` / `content`); content jars go straight to feature subpackages | `core` absorbs ~700 files under D1, so its internal shape does the real work. Layers give one enforceable edge: nothing in `platform` or `engine` may import `content` |
| D3 | `api` ships whole and undivided in the base artifact; only impl splits | `IForestryPlugin` names every module's registration type in its own signatures. Shipping api whole means those types always resolve; a missing content jar means no registered implementation, not a missing class |
| D4 | The split is real and optional: a pack may install base + one content jar | Implies six mod ids and makes severing all cross-boundary leaks a hard prerequisite |
| D5 | One Gradle project, six source sets with explicit compile-classpath edges | Compiler-enforced dependency graph without a multi-project restructure. The build already proves the pattern with `addModdingDependenciesTo sourceSets.test` |
| D6 | Content moves into feature dirs; registration holders, packets and client handlers stay as jar-level kind packages | `ApicultureItems` is naturally an index across all of apiculture. Splitting it five ways is churn, and it keeps `features` meaning what it already means in this codebase |
| D7 | Absent modules are represented by no-op managers carrying `isLoaded()`, not by throwing or `Optional` | Matches the established `Fake*` null-object idiom (`FakeErrorLogic`, `FakeClimateProvider`, `FakeBeekeepingLogic`, `FakeTankManager`, `FakeOwnerHandler`, `FakeErrorSource`). No API signature changes |

### Naming

`forestry.modules.features` is the registration framework (`FeatureBlock`, `FeatureItem`,
`ModFeatureRegistry`, `FeatureTable`) and every module has a `features/` subpackage. That
word is taken, so it is not used for the new content directories. The framework moves to
`forestry.core.platform.registration`; per-jar registration holders keep the name
`features/`.

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
  only shared plumbing staying in `core/platform`.
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
src/core/java              forestry/{api,core}/**
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
`forestry_mail`. Each content jar declares a `REQUIRED` dependency on `forestry` at a
pinned version range; `lepidopterology` also requires `forestry_arboriculture`. Registry
entries keep the `forestry:` namespace and assets stay under `assets/forestry/`, so
nothing in-game changes for players. Patchouli's `REQUIRED` dependency and JEI's
`OPTIONAL` one move to the jars that actually use them.

**Service files.** `META-INF/services/forestry.api.plugin.IForestryPlugin` currently names
`forestry.plugin.DefaultForestryPlugin`, one class registering bees, trees, butterflies,
farms, circuits and errors together. It becomes one plugin per jar, each jar shipping its
own service entry. This is the mechanism that makes optional installs work: no apiculture
jar means no apiculture plugin on the service path, so nothing registers and
`registerApiculture` is never called. `IForestryApi` and `IForestryClientApi` stay single
implementations in base.

**Access transformers** stay in base. They are global to the runtime; splitting them buys
nothing and risks ordering surprises.

**Resources.** Only the 1,959 hand-authored files under `src/main/resources` need real
partitioning. Generated resources are disposable and follow their providers automatically,
then get regenerated. Runtime is safe because Minecraft merges what needs merging:
`ClientLanguage` merges every pack's lang file, tags merge, and models, blockstates,
recipes and loot have unique per-item paths that cannot collide.

**Lang.** `generateEnUsLang` merges the hand-written `en_us.json` over the generated one,
manual winning. It becomes a parameterized task instantiated six times, and the 11 locale
files split the same way. Because lang merges across packs at runtime, a partial install
yields fewer keys rather than fallback weirdness.

**Datagen.** `core/data` is currently excluded from the production jar because ModKit
types in datagen lambdas crash FML's boot-time annotation scanner. Datagen becomes its own
source set per jar (`coreData`, `apicultureData`, ...) that is never packaged, which
removes the `exclude 'forestry/core/data/**'` hack and its failure mode. `runData` spans
the six datagen source sets.

## Gating work

`core` has 216 imports across 49 files reaching into content modules (apiculture 88,
arboriculture 63, mail 25, farming 17, lepidopterology 14, cultivation 9). `api` has 16
files importing impl, 10 of which cross a jar boundary. None of this is fixed by moving
directories.

| Bucket | Files | Work |
| --- | --- | --- |
| A. Datagen providers | 20 | Relocate into per-jar datagen source sets. `ForestryRecipeProvider` alone has 27 content imports and splits by recipe owner |
| B. Central indexes | 7 | `ForestryCreativeTabs` (19 imports), `PacketIdClient`, `PacketIdServer`, `CoreBlocks`, `CoreItems`, `CoreTiles`, `CoreDataComponents`. Each becomes an extension point jars contribute to. `IPacketRegistry` already exists in `api/modules`; the packet-id enums have not caught up to it |
| C. Species-type-aware engine | 11 | Six `*SyncPacket`s plus `GeneticsReloadHandler` (12 imports), `ProductTypes`, `GeneticsUtil`, `TreeUtil`, `CoreLootFunctions`. Iterate registered species types instead of naming bees, trees and butterflies |
| D. Misfiled content | 3 | `ApiaristPoolElement` to apiculture, `FeatureHelper` to arboriculture, `ItemSpectacles` (imports `arboriculture.capabilities.SpectacleVision`) to arboriculture or the capability moves to base. Plus the three naturalist chests |
| E. Render and GUI plumbing | 5 | `ParticleRender`, `ForestryBewlr`, `ModelBakerModel`, `TankWidget`, `LevelStructureView`. Mostly interface extraction. `ParticleRender` matters extra because `api/apiculture/genetics/IBeeEffect` imports it |
| H. Central lifecycle wiring | 3 | `ModuleCore` registers every species manager (`BeeSpeciesManager`, `TreeSpeciesManager`, `ButterflySpeciesManager`, `BeeEffectManager`, `FlowerTypeManager`, `TaxonManager`) plus `GrafterLootModifier`; `EventHandlerCore` wires apiculture AI, effects and villagers; `CoreClientHandler` registers four jars' blocks and items. Each jar registers its own via its own module and plugin |
| F. api to impl | 16 | 10 cross a jar boundary and are hard blockers. The other 6 reach only core impl and are cleanup |
| G. `DefaultForestryPlugin` | 1 | Splits six ways. Single largest file-level job |

Buckets A through E and H account for all 49 core files (20 + 7 + 11 + 3 + 5 + 3). F counts
api files and G counts one file already inside bucket-free `plugin/`.

### F is cheaper than it looks

`ForestryTags` is the sharpest blocker because it makes *base* depend on arboriculture. It
is 86 constants (43 block tags, 43 item tags) that are pure aliases for
`ForestryWoodType.X.blockTag` and `.itemTag`. Line 36 already carries
`// todo remove in favor of directly using ITreeManager`, and line 267 notes the circular
dependency they cause. Deleting them in favor of `ITreeManager` severs the edge outright
and removes a hand-maintained list that can drift from the wood types.

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
0   cut a release, tag it              the branch-divergence freeze point
1   sever api -> impl                  ForestryTags todo first (deletes 86
                                       aliases), then the 9 other crossing
                                       files, then the 6 core-impl ones
2   central indexes -> extension       creative tabs, packet ids,
    points                             Core{Blocks,Items,Tiles,DataComponents}
3   species-aware engine               bucket C
4   misfiled content, plumbing,        buckets D, E and H
    lifecycle wiring
5   split DefaultForestryPlugin        six plugins, still one jar,
                                       still one service file
6   no-op managers + isLoaded()
    ---- core no longer references content; split is provable, one jar ----
7   package moves                      api renames, core/{platform,engine,
                                       content}, feature dirs. Mechanical
8   datagen -> per-jar source sets     deletes the exclude hack
9   build split                        six source sets, mods.toml, service
                                       files, resource partition, lang merge
10  publish six artifacts
```

Phase 7 is the reorganization originally asked for. It is the easiest phase: IDE-driven
moves verified by a byte-identical datagen diff. The difficulty lives in phases 1 through
6, which no amount of directory rearrangement addresses.

This spec covers the whole project deliberately, because the phase ordering is the main
design decision in it. It is too large for one implementation plan. Each phase gets its
own plan, written when that phase starts, so earlier phases can inform later ones.

## Verification

- **Datagen diff.** `runData` is deterministic, so for phase 7, a pure move, regenerating
  must produce a byte-identical tree. Any diff is a real defect. For phases 1 through 6
  every diff must be explained. This is the primary oracle for the whole project.
- **GameTests.** `./gradlew runGameTestServer` stays green throughout. It was green as of
  2026-07-28.
- **Boundary test.** An ArchUnit-style test is added at phase 1 and tightened at each
  phase, enforcing the dependency graph long before the compiler can at phase 9.
- **Boot configurations.** Base alone, base + apiculture, and all six.

## Risks

**Branch divergence.** Current work is on `1.21.1` with `1.20.1` as main. After phase 7,
cherry-picks between them stop working in any practical sense. Either accept that `1.20.1`
is frozen at that point, or land phases 1 through 6 (ordinary refactors that cherry-pick
fine) and defer 7 through 10 until the older branch can be closed.

**Two breaking waves.** Phase 1 removes 86 public constants; phase 7 renames api packages.
Both break addons. Batch them into one major version rather than shipping two migrations.
`ForestryCE Migration Guide.md` is the natural home for the mapping table.

**Blame.** Use `git mv` and add the phase-7 bulk move to `.git-blame-ignore-revs`.

## Deferred

- Whether `arboriculture/wood` becomes a seventh jar.
- The exact contents of `core/content/resources`.
- Whether module ids rename to match jar names. The 13 existing module ids are referenced
  by config, so they stay as-is for now; `farming` and `cultivation` both map into the
  `agriculture` jar without either id changing.
