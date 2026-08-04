# Feature-oriented package reorganization and jar split

Date: 2026-07-30, amended 2026-07-31 after adversarial review
Branch: `1.21.1-restructure`
Status: phases 1-9b complete (2026-08-03); publishing the six artifacts is the remaining work

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
| D2 | `core` gets an internal layer split (`platform` / `engine` / `content`); content jars go straight to feature subpackages | `core` absorbs ~700 files under D1, so its internal shape does the real work. Layers give one enforceable edge: nothing in `platform` or `engine` may import `content`. **Measured 2026-08-02, and the rationale only half holds: `engine` obeys it at 0 files, `platform` violates it in 26 files across 64 imports, and no phase ever gated it.** `checkCoreLayers` gates the engine half and prints the platform count every build. Severing platform's 26 is unscheduled content-relocation work that no phase owns; nothing about the jar split depends on it |
| D3 | `api` ships whole and undivided in the base artifact; only impl splits | `IForestryPlugin` names every module's registration type in its own signatures. Shipping api whole means those types always resolve; a missing content jar means no registered implementation, not a missing class. Verified: `IForestryPlugin` signatures name only api types, the `Forestry*Species` holders are pure `ResourceLocation` constants with no class-init side effects, and cross-jar `ServiceLoader` works in the FML game layer |
| D4 | The split is real and optional: a pack may install base + one content jar | Implies six mod ids and makes severing all cross-boundary leaks a hard prerequisite |
| D5 | One Gradle project, six source sets with explicit compile-classpath edges | Compiler-enforced dependency graph without a multi-project restructure. The build already proves the pattern with `addModdingDependenciesTo sourceSets.test`, and MDG's `ModModel` supports multiple mods with per-mod source sets |
| D6 | Content moves into feature dirs; registration holders, packets and client handlers stay as jar-level kind packages | `ApicultureItems` is naturally an index across all of apiculture. Splitting it five ways is churn, and it keeps `features` meaning what it already means in this codebase |
| D7 | Absent modules are represented by no-op managers carrying `isLoaded()`, not by throwing or `Optional` | Matches the established `Fake*` null-object idiom (`FakeErrorLogic`, `FakeClimateProvider`, `FakeBeekeepingLogic`, `FakeTankManager`, `FakeOwnerHandler`, `FakeErrorSource`). No API signature changes |

### The D3 safety condition, stated precisely

D3's mechanism is sound, but its safety condition is stricter than "no core imports".
The `jar` task's `exclude 'forestry/core/data/**'` rule documented an existing crash: FML's
annotation scanner resolves method signatures at boot via `getDeclaredMethods0`, and
`core/data/Data.java` is an `@EventBusSubscriber` whose lambda synthetics carry ModKit
parameter types, which crashes the JVM link step in production. The same failure mode
threatens this design.

**Phase 8 removed that particular instance at the root.** Datagen is its own source set,
`main` no longer compiles against ModKit, and the exclude rule is gone. The failure *mode*
is unchanged and still governs every jar boundary - it is why the phase-6 gate read the
constant pool rather than trusting imports - but the one known live example is closed.
**Phase 9a retired that gate**, because `main`'s compile classpath no longer contains any
content output: the compiler now rejects the descriptor-level reference the gate was
looking for, which is why it could be deleted rather than merely relaxed.

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

### Cross-jar recipes: settled in phase 9b, and not by conditions

The audit ran in phase 9b, once the reference closure made each file's real dependencies explicit.
It found **one** recipe naming two content jars, and the answer is a tag rather than
`neoforge:conditions`: an absent *item* is an unknown registry key and fails the recipe to parse, an
absent *tag* resolves empty. So a cross-jar ingredient becomes a tag both jars contribute to, the
recipe ships in base, and it crafts with whichever jars are installed instead of vanishing when one is
missing. See the phase 9b section.

**Cross-jar loot modifiers needed no redesign.** `ConditionLootModifier` already resolves each
sub-table at run time and skips the absent ones, so `abandoned_mineshaft.json` degrades on its own and
is pinned to base. `global_loot_modifiers.json` is the file that actually spans, and it is entry-split
rather than assigned.

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

  core/
    plugin/                 base's own IForestryPlugin (7b)
    data/                   datagen; partitions per jar in phase 8
      taxonomy/             the four Taxonomy classes (7b)
    platform/               no game content, no genetics
      compat/               shared JEI/Patchouli/KubeJS/Curios plumbing;
                            per-jar integrations live in their own jar (7b)
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

Phase 5 moved seven and left ten; phase 7b dissolved the rest. `DefaultButterflySpecies` and the
two analyzer plugins went to their jars, the four taxonomy classes to `core.data.taxonomy` and
`DefaultForestryPlugin` to `core.plugin` - so one of the seven listed above ended up base after
all, as base's own `IForestryPlugin`.

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
  features/  client/  compat/  plugin/  tab/
                          ModuleFarming + ModuleCultivation, NOT merged: the two
                          module ids stay, per Deferred

mail/                     JAR forestry_mail             -> core
  letters/  postoffice/  tradestation/  carriers/  postalstates/
  features/  network/  client/  gui/  commands/  compat/  ModuleMail
```

### Notes on placement

- `compat/` does not survive as a top-level directory. JEI plugins, Patchouli pages and
  KubeJS bindings are per-feature, and under an optional split the apiculture JEI plugin
  must ship in the apiculture jar. It dissolves into a `compat/` subpackage per jar, with
  only shared plumbing staying in base. **As executed 2026-08-02**: the per-jar split had already
  happened in earlier phases, so what was left in `forestry.compat` was shared plumbing only, and
  all 30 files went to `core.platform.compat` in one prefix rewrite. There is no `compat/` at the
  top level of base either - it is `core/platform/compat/`.
- ~~`TileApiaristChest`, `TileArboristChest` and `TileLepidopteristChest` are misfiled in
  `core/tiles` today. The shared `TileNaturalistChest` base stays in `core/platform/tile/`; the
  three concrete chests move to their jars.~~ **Corrected 2026-08-02.** They are not misfiled.
  Each imports only `CoreTiles`, `SpeciesUtil` and `TileNaturalistChest`, and every other part of
  the naturalist chest is base by prior decision - including `NaturalistChestBlockType`, which the
  Graph decisions table moved to base *to remove five leaks*. All four now sit together in
  `core/platform/tile/`. Splitting them would have put a base-registered block's BlockEntity in an
  optional jar and added two packaged leaks.
- `ItemFruit` stays base too, in `core/platform/item/`. `CoreItems.FRUITS` instantiates it.
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

**As built, phase 8 (2026-08-02).** One source set, `datagen`, at `src/datagen/java`, registered as a
third source set of the `forestry` mod and given Minecraft via `addModdingDependenciesTo`. ModKit sits
in its own `modkit` configuration extended by `runtimeOnly` and `datagenCompileOnly`, so it reaches
datagen's compile classpath and the dev runs' game layer but never `main`'s compile classpath. The
`exclude 'forestry/core/data/**'` hack is deleted. See the phase-8 paragraph under Sequencing for the
three MDG failure modes found while prototyping this.

The **`--output` question**, measured 2026-08-02 in phase 9b and answered on evidence rather than
preference. Of 10,206 generated files (excluding `.cache`), **9,477 - 92.9% - are addressable by
registry id**: models, blockstates, recipes, loot tables and advancements are all named after the
block or item they describe, and every feature already registers under a module id through
`ModFeatureRegistry`. So the owning jar of nine files in ten is known *in code* and does not have to
be guessed or hand-maintained.

The remaining **729 are enumerable rather than derivable**, and mostly resolve by folder:

| Folder | Files | Owner |
| --- | --- | --- |
| `data/forestry/tags` + `data/{c,minecraft,forge,curios}/tags` | 348 | by tag contents; the only genuinely hard group |
| `data/forestry/taxon` | 160 | core - datagen-owned since phase 7b |
| `data/forestry/{bee,tree,butterfly}_species` | 154 | apiculture / arboriculture / lepidopterology, by folder |
| `data/forestry/loot_modifiers` + `data/neoforge/loot_modifiers` | 17 | needs redesign, not partition |
| `data/forestry/flower_type` | 15 | core, as of phase 9b |
| `data/forestry/bee_effect` | 14 | apiculture |
| `data/forestry/worldgen`, `damage_type`, `curios`, `data_maps`, atlases, lang | 21 | case by case; atlases and lang merge at runtime |

**Decision: one data run, post-hoc split.** `runData` keeps writing to `src/generated/resources`, and
a Gradle task partitions that tree into per-jar resource directories using an ownership manifest that
datagen itself emits. Six data runs would require partitioning the *providers* first -
`ForestryRecipeProvider` alone emits recipes for every jar - which is far more work for the same
result, and would lose the byte-identical `runData` diff that has been the strongest oracle in this
project since phase 1. A downstream split keeps that oracle intact.

**The ownership manifest is the design, not an implementation detail.** It must be generated from the
same registry the features were created from, never hand-written; a hand-maintained mapping would rot
silently and no oracle would catch it.

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
5   DONE 2026-08-01                    bucket G: five plugins, still one jar,
                                       still one service file
5a  DONE 2026-08-01                    bucket I: registration chains go home,
                                       ClientHelper by ServiceLoader.
                                       32 -> 21, PluginManager deferred to 6
6   DONE 2026-08-01                    no-op managers + isLoaded(), the four
                                       PluginManager constructions, and
                                       checkBaseBytecode. 21 -> 20, all datagen
    ---- gate: the base artifact references no split-jar types ----
    MET for the packaged artifact 2026-08-01: checkBaseBoundary reports 0
    packaged leaks and checkBaseBytecode confirms it at the class-file level.
    The 20 remaining files are datagen, which the jar already strips; they
    dissolve in phase 8
    FULLY DISCHARGED 2026-08-02: phase 8 moved datagen to its own source set,
    so the 20 are no longer in src/main/java at all. The baseline file is
    deleted and checkBaseBoundary is a hard gate with nothing grandfathered
7a  DONE 2026-08-01                    manifest steps 7.1-7.6: api renames and
                                       core/{platform,engine,content}. Base is
                                       in its target shape
7b  DONE 2026-08-02                    manifest steps 7.7-7.12, plus
                                       forestry/plugin. The naturalist chests
                                       and ItemFruit stayed base instead
8   DONE 2026-08-02                    ONE datagen source set, not six. Deletes
                                       the exclude hack and the ratchet; takes
                                       ModKit off main's compile classpath
9a  DONE 2026-08-02                   six source sets with compile-classpath
                                       edges, still one jar and one mod id. The
                                       compiler now enforces D1; the two
                                       stand-in gates are deleted
9b  six jars                           IN PROGRESS 2026-08-02. Six mod ids and
                                       six jars build; resources partition from
                                       a generated ownership map; core+arbo+lepi
                                       boots clean. Remaining: core-only still
                                       logs 65 recipe + 28 tag errors, see the
                                       phase-9b notes; publishing
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

Phase 5 landed 2026-08-01. Bucket G is closed; `checkBaseBoundary` is at 32 of the original 68. The
split produced **five** plugins, not six: mail registers nothing through `IForestryPlugin`.

Most of it was mechanical for a checkable reason - `PluginManager` makes six passes over the loaded
plugins, the three content hooks are called by the species types themselves, and every hook has a
no-op default, so a plugin implementing only its own hooks behaves identically to one implementing
all nine.

Two ordering questions decided by reading the code rather than guessing. Errors keep their numeric
ids from insertion order and those shorts are the wire form, so `registerErrors` stayed whole in the
core plugin. Filter rules also get int ids, but `FilterManager` sorts alphabetically before assigning
them, so splitting `registerFilterRuleTypes` four ways was safe.

The one the plan got wrong: **species type registration order is not incidental.** The plan reasoned
that species types key by `ResourceLocation` and were therefore order-independent. That is true of
lookup and false of the taxonomy projection - `TaxonProvider` types each taxon by iterating the
species type map, so moving the bee type out alone regenerated `caldapis` and five other bee genera
as `forestry:butterfly_species` and dropped their `bee_effect` allele. `GenomeBaselineTest` caught it
through austere's activity flipping nocturnal to diurnal. All three species type blocks had to move
in one commit; the id sort then restores bee, tree, butterfly exactly.

`PluginManager` no longer names a plugin class to sort it first. It partitions on an explicit set of
base plugin ids - deliberately not on the `forestry` namespace, which `forestry:kubejs` also uses and
which would have run KubeJS's user script events before lepidopterology registered its species type.

Phase 5a landed 2026-08-01. Bucket I is closed except for one file; `checkBaseBoundary` is at 21 of
the original 68, and 20 of those 21 are bucket A.

The spec called this the hardest remaining work and warned that "check whether it is really a
misfiled type" would not apply. That was half right. Twenty-two leaks across twelve files resolved
into four kinds, and only two were the factory inversion the spec predicted. Eight of the twelve
were a registration and the builder it constructs, both belonging to one module, and every
registration was already constructed by its own module or by a baselined datagen provider - so the
chains went home without any new extension point. `ForestryApiImpl` was the same defect phase 4
found in `setBeeManager`: three setters typed to the impl while the fields and getters used the
interfaces.

`LepidopterologyRegistration`, `FarmingRegistration` and `WindfallFarmableBuilder` were clean and
had to move anyway - each is constructed only by a class that was moving, so leaving them behind
would have converted a cleared leak into a new one. That is the phase 4 `BeeClientManager` trap,
this time designed around rather than discovered.

The same-package effect ran in both directions this phase. The moved files had used
`SpeciesRegistration`, `SpeciesBuilder`, `Registrar` and `ModifiableRegistrar` as unimported
neighbours; all four are genuinely generic and stay in `apiimpl`, so the movers needed imports
added.

Two decisions could not be made by test. `SpeciesTypeBuilder`'s default research materials named a
bee comb, and it is the generic builder for all three species types - deleting it would have
silently changed tree and butterfly research values, and research materials are runtime-only, absent
from datagen and from every GameTest. It resolves `forestry:honey_comb` from the registry instead,
guarded on `!= Items.AIR` because `BuiltInRegistries.ITEM` is defaulted and seeding AIR would make an
empty hand count as research material. `ClientHelper` needed a mechanism rather than a move:
`ForestryClientApiImpl` holds it in a field initialiser, and it cannot become a settable field
because `ForestryLeafSprites` - in api - resolves the helper from a static initialiser and builds
twenty sprites immediately. `ServiceLoader` fits because api already resolves `IForestryApi` and
`IForestryClientApi` that way, and because the two lookups nest: the helper resolves inside
`IForestryClientApi.INSTANCE`'s own class init, so the ordering is structural rather than a matter
of lifecycle timing.

`PluginManager` did not clear and is carried to phase 6. The plan scoped it at two constructions;
it has four. Two of those the plan itself created - moving `FarmingRegistration` into farming turned
an implicit same-package reference into an import, and moving `TreeClientManager` into arboriculture
turned a clean import into a leak. All four are base assembling a content module's manager, which is
exactly what phase 6's no-op managers address, so inverting one of them alone would have left the
file baselined and established a fourth pattern for a problem phase 6 solves with one. There is a
`todo` on the file recording this and naming the inversion used elsewhere: hand `LOADED_PLUGINS` to
a module-side object, the way `handleSpeciesRegistration` already does.

Phase 6 landed 2026-08-01. D7 is implemented and the gate is met for the packaged artifact:
`checkBaseBoundary` reports **0 packaged leaking files**, with 20 datagen-only ones the jar already
strips.

The phase was smaller than the spec implies, for a measured reason. The six managers have **59 call
sites, of which only 8 are outside the owning jar** - everything else is a module calling its own
manager, which cannot happen when that jar is absent. Of those 8, two are datagen and the other six
already handled the degenerate case: `ForestryBiomeModifier` loops over `getHives()`,
`ItemRefractoryWax` already branches on null, `CoreClientHandler` iterates model collections, and
`IBeeEffect.doFX` calls a void method. **D7 needed no defensive rewriting of callers, only the null
objects themselves.**

`IClientHelper` was a live crash rather than a hypothetical. Phase 5a left the `ServiceLoader`
lookup as `orElseThrow` in a **field initialiser**, so an absent arboriculture jar would have thrown
while constructing `IForestryClientApi.INSTANCE` on every client start, not merely on the paths that
draw leaves.

Seven of the thirteen throwing getters deliberately still throw. `IErrorManager`, `IFilterManager`,
`IGeneticManager`, `ICircuitManager`, `IPollenManager`, `ITextureManager` and
`IGeneticClientManager` are supplied by base itself, so a null there is a lifecycle ordering bug,
not an absent module, and silencing it would turn a clear error into an NPE somewhere later.

`PluginManager` cleared through the same `IForestryModule` hook mechanism phase 4 introduced, and
the no-op work is what made it possible: once the field holds a working no-op, base has nothing left
to build, so `registerFarming` and the two client-manager assemblies simply move to their modules. A
module that implements neither hook leaves the no-op in place, which is exactly what an absent jar
looks like.

The nine imports orphaned by that move were found by checking each import against the file body.
A simple-name occurrence count - the technique the phase 5a plan reached for - is wrong for anything
a wildcard import also covers: `IdentityHashMap` scored 1 and was still in use.

**`checkBaseBytecode` found a real leak on its first run, and it closes the D3 question in the
expensive direction.** `PluginManager` called `FlowerTypeTypes.registerBuiltins()` and
`ApicultureProductTypes.registerBuiltins()` **fully qualified, with no import**, so six phases of
import-driven work never saw them - exactly the descriptor-level reference D3 warns about. Both
moved to `ApicultureForestryPlugin.registerGenetics`, which runs earlier in the same method, so the
ordering both comments require still holds. The lesson generalizes: the import gate is necessary but
not sufficient, and phase 7 should run the bytecode gate after each move step rather than only at
the end.

Phase 7a landed 2026-08-01. Manifest steps 7.1 through 7.6 are done and `forestry.core` is in
its target shape: `platform` (23 packages), `engine` (3) and `content` (10). Both gates held
throughout - 0 packaged leaks, `checkBaseBytecode` clean - and the datagen tree stayed
byte-identical at every step.

**The gate had a blind spot exactly where this phase needed it not to.** `checkBaseBoundary`'s
`basePackages` list is `core, apiimpl, plugin, modules, compat`, so `factory`, `energy`, `storage`,
`sorting` and `worktable` - 220 files that ship in the base jar - had **never been scanned**. Step
7.6 moves all five under `core.content`, which brings them into scope. Two leaked, and both were
already decided in the Graph decisions table above and never executed because nothing could see
them: `FactoryRecipeTypes` registered an apiculture serializer, and `CrateItems` named four
apiculture classes. Neither was a one-line move - `FactoryRecipeTypes.HYGROREGULATOR` had two base
consumers and the crated bee products had one in `CoreClientHandler` - so each needed the phase-4
relocation treatment. They were severed first, which is why step 7.6 reports zero across 220
newly-measured files.

The moves were scripted rather than IDE-driven, against this spec's `### Who performs phase 7`. The
reasoning there was sound when written; both premises had weakened by the time it ran, and the
manifest now records why. What matters for 7b: a prefix rewrite catches 194 `package-info`
annotations, 55 javadoc `{@link}`s and 30 inline fully-qualified references that an import-targeted
rewrite does not, and it reaches resources the IDE cannot touch at all.

Three failure modes worth carrying into 7b, none of which a package-level move hits:

- **A per-file fan-out does not rewrite package declarations.** `move-package.sh` gets them free as
  part of the prefix; step 7.5 moved 81 files individually and every one needed its `package` line
  repaired from its new directory path.
- **The same-package effect runs in both directions and cascades hard.** 32 imports had to be added
  for classes that moved away from files that stayed, and 9 for classes that stayed behind files
  that moved. `ItemFruit` alone losing its `ItemForestryFood` superclass produced 754 compile
  errors from one missing import.
- **`protected` access is package-scoped.** `GuiAnalyzer` read `analyzer.tile` and
  `PortableAnalyzerScreen` read `container.inventory`; both compiled only while they shared a
  package with the container. Public accessors already existed. Nothing but the compiler catches
  this, and it will recur in every fan-out that separates a screen from its container.

Phase 7b landed 2026-08-02. Manifest steps 7.7 through 7.12 are done, `forestry/plugin` and
`forestry/compat` no longer exist, and every package in this spec's target tree is now in place.
Ten commits, eight of them pure moves and in `.git-blame-ignore-revs`. Byte-identical datagen and
108 GameTests at every step.

The three per-file failure modes listed just above were closed in tooling rather than left to the
compiler, and it worked: two new scripts (`explode-package-imports.sh`, which makes every
intra-package reference explicit before a package fans out, and `expand-wildcard.sh`, which
replaces a wildcard import with the classes actually used) meant the apiculture fan-out - 186 files
across six splitting packages - compiled clean on the first attempt, where the comparable 7a step
produced 754 errors. `protected` access never bit, because no 7b fan-out separated a screen from
its container. Only one same-package break got through, in `ModuleFarming`: the plan omitted an
explode pass on the `agriculture` root before moving six loose files out of it.

Four decisions 7b had to make that the spec and manifest had left open or wrong:

- **The three naturalist chests and `ItemFruit` stayed base**, reversing two statements in this
  spec's `### Notes on placement` and one in the manifest's 7a corrections table. Both entries are
  corrected in place above. The chests are 13-line subclasses referencing nothing outside base, and
  every other part of the naturalist chest - enum, block group, item, recipes, tags, models, BEWLR,
  renderer, creative tab - is base by prior decision, several of them made in 7a *specifically to
  remove leaks*. `ItemFruit` is instantiated by `CoreItems.FRUITS`. Moving either would have added
  packaged leaks rather than removed them.
- **`BeeTaxonomy`, `TreeTaxonomy`, `ButterflyTaxonomy` and `ForestryTaxonomy` went to
  `core.data.taxonomy`**, not to the three content jars. Their only consumer chain is
  `TaxonProvider`, so they are datagen input. Sending them to content jars would have made
  `ForestryTaxonomy` import all three split modules and grown a baseline whose header says it never
  does - for nothing, since phase 8 partitions all of `core/data` per jar and would carry them
  along regardless.
- **`DefaultForestryPlugin` went to `core.plugin`**, symmetric with every content jar's `plugin/`.
- **`mail/postalstates` did not survive**, contradicting this spec's content tree; the manifest's
  per-file assignment won as the more specific document.

The 7a lesson about gate blind spots recurred in a second form. `checkBaseBoundary` and
`checkBaseBytecode` name their packages by literal, so after `farming`, `cultivation`, `plugin` and
`compat` ceased to exist the gates kept reporting green while measuring strictly less. Nothing
failed; the agriculture edge simply stopped being checked. Both lists were retargeted to
`['core', 'apiimpl', 'modules']` and `['apiculture', 'arboriculture', 'lepidopterology',
'agriculture', 'mail']`, and the retargeted gate was verified to fail on a planted
`forestry.agriculture` import in `ModuleCore`. A gate that names things by literal has to move with
the tree, and it fails silent rather than loud when it does not.

Phase 8 landed 2026-08-02. `forestry.core.data` (55 files) moved from `src/main/java` to a `datagen`
source set at `src/datagen/java`, with no Java change at all - the package name is unchanged and
nothing in `main` referenced it. Three commits.

**One source set, not six.** This spec's Build structure section names `coreData`, `apicultureData`
and so on, and that is still the end state, but it is not reachable yet and building a rename toward
it now would be wasted. Phase 8's actual deliverables - "deletes the exclude hack" and discharging the
20 datagen files from the gate - are both fully met by one source set. Six requires the *providers* to
be partitioned by owning jar, and they are not partitionable today: `ForestryRecipeProvider` alone
emits recipes for every jar, and `ForestryBlockLootTables` and `ForestryDataMapProvider` have the same
shape. That partition is phase 9's work, `main` is still one source set until phase 9, and six datagen
source sets over one main source set would be five empty directories. **The per-jar datagen split moves
to phase 9.**

**The real invariant is ModKit, not the filename.** The `exclude 'forestry/core/data/**'` jar rule was
treating a symptom. The cause is that datagen providers are the only code in the repo touching ModKit,
a dev-only dependency, so every datagen lambda put a ModKit type into a method descriptor that FML's
boot-time annotation scanner then tried to resolve from the shipped jar. `main` now does not compile
against ModKit at all - asserted directly, `compileClasspath` has zero ModKit entries - and the jar is
built from `main`'s output, so datagen cannot reach it by construction. The jar has 14,666 files and
zero `forestry/core/data` entries with no filter involved.

This spec warned that the datagen build story is "the least-supported part" and "should be prototyped
early". That was correct and the advice was taken; three things failed before the working configuration
was found, and they are worth recording because phase 9 will hit the same layer rules:

1. `datagenImplementation 'ModKit'` is not sufficient. MDG 2.0.140 offers no way to scope a mod's
   source set to one run - `ModModel.sourceSet` is additive and global - so *every* dev run loads
   `sourceSets.datagen`, and FML's `AutomaticEventSubscriber` calls `getDeclaredMethods0` on `Data` at
   construction. `runData` died with exactly the `NoClassDefFoundError: thedarkcolour/modkit/data/MKTagsProvider`
   the jar exclude was documented to prevent: the same crash, relocated from production into the data run.
2. `additionalRuntimeClasspath` does not exist on MDG 2.0.140's `RunModel`. The property is
   `additionalRuntimeClasspathConfiguration`, a `Configuration`.
3. That configuration is loaded **outside the game layer**. With ModKit there the run got one error
   further and died on `NoClassDefFoundError: net/minecraft/data/tags/TagsProvider`, because
   `MKTagsProvider` extends a Minecraft class. ModKit has to ride in on `main`'s `runtimeClasspath`.
   `runtimeOnly` does that while keeping it off `main`'s *compile* classpath, which is the objective,
   and the published POM strips dependency nodes anyway.

So datagen loads in the client, server and gameTest runs as well as the data run. That is exactly the
status quo - `core/data` was in `main` and ModKit was an `implementation` dependency - so nothing
regresses; it is simply now explicit.

**The ratchet is retired.** `gradle/base-boundary-baseline.txt` is deleted and `checkBaseBoundary` is a
hard gate: no base file may reference a split module, nothing grandfathered. The ratchet did its job
across six phases, 68 -> 20 -> 0, and its stale-baseline arm is what proved the discharge - moving
datagen out made it fail with all 20 entries listed as no longer leaking. `checkBaseBytecode` lost its
`core/data/` skip, which is now dead, and `checkResourceFqcn` resolves class names against both source
roots. The new gate was verified to fail on a planted `forestry.apiculture` import in `ModuleCore`.

One deliberate consequence: the published **sources jar no longer contains datagen**, since it is built
from `sourceSets.main.allJava`. Datagen is dev-only tooling and does not belong in a sources artifact.

Phase 9a landed 2026-08-02. The five content modules moved to `src/<name>/java` as five source sets
alongside `main`, each with only what D1 allows on its compile classpath. Four commits.

**The graph was already right.** Measured before starting: 1,220 cross-package imports out of the five
content modules, of which exactly **one** was illegal - `FarmableGE` naming `ArboricultureBlocks`, the
edge the Graph decisions table resolved on 2026-07-31 and which nothing had yet executed. Wiring all
six source sets and compiling produced exactly two errors, both in that file. No hidden cycle, no split
package, no surprise from `apiimpl` or `modules`. That is the payoff for phases 1 through 8: 9a only
asked the build system to start believing what was already true.

`FarmableGE` now reads a new `forestry:tree_saplings` block tag. `BlockTags.SAPLINGS` was the tempting
option - `ForestryBlockTagsProvider` already puts `SAPLING_GE` in it - but that tag also holds every
vanilla sapling, and widening the *genetic* farmable to match them would have been a silent gameplay
change. The new tag is arboriculture-populated and resolves empty without it, which is exactly the
"degrades rather than breaks" the graph decision asked for.

**`sourceSets.main` is core.** The Build structure sketch shows `src/core/java`, and that was not
followed: Gradle's `jar`, `processResources`, `components.java` and MDG's `accessTransformers` all
attach to `main` by convention, and core is precisely the artifact those conventions should describe.
Renaming would have bought a matching directory name and cost every one of those.

**`checkBaseBoundary` and `checkBaseBytecode` are deleted.** Not because the boundary stopped
mattering, but because they became unfalsifiable: `main`'s compile classpath contains no content
output, so neither task can fail, and a gate that cannot fail measures nothing. The classpath edges
were verified to *reject* rather than merely to compile - a planted `forestry.apiculture` import in
`ModuleMail` fails `compileMailJava`, and a planted `forestry.lepidopterology` import in
`ModuleArboriculture` fails `compileArboricultureJava`, which is the one that matters, since it proves
the allowed lepidopterology -> arboriculture edge is one-way.

### The finding: a data-level lepidopterology -> apiculture edge

Splitting the source sets broke ten butterfly GameTests, and the cause is worth recording in full
because no gate in this project could have caught it.

FML's annotation scan enumerates classes in classpath order, `ForestryModuleManager.discoverModules`
kept that order, and `ModuleCore` dispatches `registerReloadListeners` in module order. Going from one
classes directory to six reordered the modules, so lepidopterology's reload listener began running
before apiculture's, and every butterfly failed to project with
`No flower type was registered with the ID: forestry:flower_type_vanilla`.

The proximate bug is that **module load order depended on classpath scan order at all**. That is fixed:
`discoverModules` now sorts by `isCore()` then by module id, so order depends on the ids and nothing
else. Datagen stayed byte-identical and the creative-tab baseline held, so the new order is
observationally equivalent to the old one.

The underlying issue is not fixed and is 9b's problem. `ButterflyChromosomes.FLOWER_TYPE` **is**
`BeeChromosomes.FLOWER_TYPE`, and its default value `forestry:flower_type_vanilla` is registered only
by `ApicultureForestryPlugin`. Butterflies therefore depend on apiculture at the *data* level, through
a `ResourceLocation` in a karyotype default. D1 forbids that edge, and:

- The compiler cannot see it - it is a `ResourceLocation`, not a type reference.
- `checkBaseBoundary` and `checkBaseBytecode` could never have seen it either; both scanned base, and
  this is content-to-content.
- It is invisible while everything ships in one jar, and fatal the moment lepidopterology can be
  installed without apiculture, which is exactly what 9b enables.

**9b must relocate flower-type registration to core before splitting the jars.** `FlowerTypeTypes`
lives in `apiculture.bees.genetics` but flower types are a shared genetics concept that both bees and
butterflies read; `core.engine.genetics` is the natural home, and this is ordinary phase-4-shaped
relocation work. It also raises the question of whether any *other* karyotype default crosses a jar
boundary - nothing has ever audited that, and a `ResourceLocation`-valued default is precisely the
shape that hides from every gate this project has.

Phase 9b is in progress. Two of its eight tasks have landed.

**Flower types are now a base concept.** The finding recorded under phase 9a is fixed at the root
rather than worked around. `IFlowerTypeManager` is a new api interface reached through
`IForestryApi.getFlowerTypeManager()`; `FlowerTypeTypes`, `FlowerTypeManager` and the three
`IFlowerType` implementations moved from `apiculture.bees` to `core.engine.genetics`;
`registerFlowerType` moved from `IApicultureRegistration` to `IGeneticRegistration`, because an addon
must be able to register a flower type with no apiculture jar installed; the datapack reload listener
and the login sync moved from `ModuleApiculture` to `ModuleCore`; and `BeeChromosomes.FLOWER_TYPE` -
which *is* `ButterflyChromosomes.FLOWER_TYPE` - now resolves through the base manager rather than
through `BEE_TYPE.get()`. `IBeeSpeciesType.getFlowerType` and `getFlowerTypeSafe` are gone.

The sync packet moved with the map, since a packet carrying the flower types has to be registered by
whichever jar owns them. Its wire id is deliberately unchanged: it moved from `ApiculturePacketIds` to
`PacketIdClient` but kept the path `flower_type_sync`, so `forestry:flower_type_sync` still identifies
the same payload.

This is an api break - two methods removed from `IBeeSpeciesType`, one moved between registration
interfaces - which the "one breaking wave" decision already accounts for. Datagen stayed
byte-identical, and the GameTest suite went from 108 to 109: the new one resolves every built-in
flower type through the base manager without touching the bee species type, which is the assertion
that would have failed before this change.

**The `--output` question is answered**; see the Build structure section for the measurement and the
decision. In short: 92.9% of generated files are addressable by registry id, the remaining 729 are
enumerable by folder, and the chosen strategy is one data run plus a post-hoc split driven by an
ownership manifest that datagen emits - which preserves the byte-identical `runData` diff that six
data runs would have destroyed.

### Phase 9b, as far as it got on 2026-08-02

Six mod ids (`forestry`, `forestry_{apiculture,arboriculture,lepidopterology,agriculture,mail}`, plus
a dev-only `forestry_gametest`) and six jars. Core is 4,164 files with **zero** content classes; each
content jar carries only its own package tree, its own `neoforge.mods.toml` and its own plugin service
file. The full install is unchanged: byte-identical datagen, 109 GameTests, all gates green.

**The partition is generated, not written.** `src/generated/ownership.json` maps 1,890 registry ids to
jars and is written by datagen from the live feature registries. `generateResourceOwners` turns that
into `src/generated/resource-owners.json`, a checked-in path-to-jar map for all 12,165 resource files,
so a file changing jars shows up as a diff rather than as silent behaviour. Rules, in order: explicit
folder rules, exact file-name match, loot sub-table by module name, worldgen by feature name, tag by
the jar its entries belong to, **recipe by the jar that owns its result**, longest-prefix on the file
name, texture by the models that reference it, then core.

Two of those orderings are load-bearing and were found the hard way. Longest-prefix must come *last*,
because it is greedy: `forestry:chestnut` is a **core fruit**, so `chestnut_logs.json` resolved to core
and stranded arboriculture's log tag there, breaking 68 tags. And the recipe rule must match the
result **exactly** for the same reason - `papaya_fireproof_wood` prefix-matches the papaya fruit.

**The partition happens in `processResources`, not only in the jar tasks.** Filtering at packaging time
alone would have left the boot configurations testing an unsplit install, which is the one thing they
exist to catch.

Three real defects that only a partial-install boot could find:

- **`AgricultureForestryPlugin` registered the `MACHINE_UPGRADE` circuit layout** while core registered
  the circuits against it. A core-only boot crashed outright. The layout moved to core.
- **One biome modifier named all four placed features.** A placed feature from an absent jar is an
  unbound registry value and fails world load before any runtime guard can help. `ForestryBiomeModifier`
  now takes all four as `Optional`, and each jar ships a modifier naming only what it owns.
- **The GameTest suite crashed partial installs.** NeoForge's gametest scanner resolves every
  `@GameTest` method descriptor at registration, so a test naming an apiculture type killed a core-only
  boot - the D3 safety condition, exactly. The suite is now its own mod id, which is what lets the boot
  configurations leave it out.

`runLepidopterologyNoBeesServer` **boots clean**: `Done (0.921s)`, 35 butterfly species, zero projection
failures. That is the configuration the flower-type work exists for, and it is now proven rather than
argued.

### Phase 9b, completed 2026-08-03

The 2026-08-02 pass left `runCoreOnlyServer` failing, and re-running it against that day's final
commit showed the state was worse than recorded: not 65 recipe and 28 tag errors, but a **hard crash**.
`data/forestry/recipe/butterfly_mutation/bombyx_mori_1.json` shipped in core because `folderOwners`
listed `bee_mutation` and `tree_mutation` and not `butterfly_mutation`. Reading it resolves the
karyotype through `GeneticManager.getSpeciesType`, which throws `IllegalStateException` - and
`RecipeManager.apply` catches `JsonParseException`, not that. One misfiled file killed the server.

**The rule was answering the wrong question.** Every rule in the cascade - folder, exact name, loot
module, worldgen, tag entries, recipe result, longest prefix, texture - answers *what is this file
about*. Correctness turns on a different question: *what can this file see*. A fabricator recipe whose
result is a core item and whose ingredient is an arboriculture log is **about** core and **belongs to**
arboriculture. Each rule picked one signal and ignored the rest, which is why each looked right in
isolation and was wrong in aggregate.

So the closure became the primary rule and the cascade demoted to a proposal:

> A jar may ship a file only if every forestry id the file names is present wherever that jar is.
> The cascade proposes an owner; the closure promotes it to the least derived jar that satisfies
> that, and **fails the build** when no jar does.

Least derived, because core reaches the most installs. The build failing is the point: a file naming
content no single jar can see is not a mapping problem and no assignment fixes it.

Measured effect: **109 files promoted**, and the build stopped on the one file that genuinely spanned
two content jars. A static cross-reference oracle written independently agreed on all of them.

**Three things the closure needed that did not exist.**

- *The manifest was missing most of the game.* It walked `getFeatures()`, which covers blocks, items,
  tiles and menus - 1,890 ids. Point-of-interest types take a `DeferredRegister` straight off the
  module and never become features, and `recipeType()` was **the only factory in `FeatureRegistry`
  that never called `register()`**, so recipe types were absent from `getFeatures` entirely. The
  manifest now walks every `DeferredRegister` each module owns: **5,361 ids**. That subsumes the
  `WoodAccess` fix predicted above - those ids arrive for free, because they were always registered
  through a module-scoped deferred register.
- *Bare ids are not unique across registries.* `forestry:refractory_wax` is a core **item** and an
  apiculture **particle**; `forestry:escritoire` is a core **block** and an apiculture **poi**. The
  manifest now writes a registry-qualified key beside each bare one, resolves an ambiguous bare key to
  the jar that constrains least, and logs the pair. Without this, every fireproof-wood recipe was told
  it needed apiculture.
- *Species and taxa carry no registry id at all.* Species type ids come from the species folder rules;
  a genus is claimed by the species definitions naming it; and the ranks above propagate upward from
  their children, stopping where children disagree. **13 taxa remain in core** - `animalia`,
  `arthropoda`, `insecta` and the kingdom-level ranks - which is correct, since no one jar can hold a
  rank spanning bees and butterflies, and an unreferenced taxon is inert.

**Some files cannot be owned by any jar, and must not be.** A tag, a NeoForge data map and
`global_loot_modifiers.json` are entry-keyed collections that the game assembles from every pack
supplying one. Their unit of ownership is the entry, not the file, so `partitionSharedResources` gives
each jar a variant holding only its own entries. This replaces the `addOptional` fix predicted above,
and is better than it: entries ship **with their jar** rather than being silently dropped from core.
Ten files split, and the sharpest is `global_loot_modifiers.json` - it names the arboriculture grafter,
so promoting it would have left a core-only install with **no loot modifier list at all**.

en_us splits the same way: a generated key carries its registry id, so it partitions on the manifest,
and gui strings, errors and allele names stay in core.

**Cross-jar recipes now have the policy the spec deferred**, and it is not `neoforge:conditions`. The
genetic filter is a core machine whose recipe named `forestry:caterpillar` and `forestry:propolis`
directly. The fix is a tag both jars contribute to (`forestry:genetic_samples`): an absent *item* is an
unknown registry key and fails the recipe, an absent *tag* resolves empty. So the recipe ships in core
and crafts with whichever jars are installed, rather than vanishing when one is missing. The general
rule the closure encodes: **a `"tag"` reference places no ownership constraint; everything else does.**

The cross-jar loot modifier needed no redesign after all. `ConditionLootModifier` already resolves each
sub-table at run time and skips the absent ones, so a chest modifier degrades on its own; the file is
pinned to core by folder rule. What *was* broken is that `nether_bridge.json` longest-prefix-matched
the agriculture feature `forestry:nether`, so a core-plus-apiculture install silently lost its nether
fortress bee loot.

**Defence in depth:** `MutationRecipe.Serializer` now reports a missing species type as a
`JsonParseException`, so a misfiled mutation recipe is logged and skipped instead of taking the server
down. That is what the crash above deserved to be.

**One defect no boot configuration could have caught.** An `exclude` at the top of a Gradle `Copy` task
applies to every source the task has, and a split file's owner is `split` rather than `core`, so base's
`processResources` excluded base's own partitioned share. The base jar shipped **no `en_us` and no
`global_loot_modifiers.json`**. Every boot still reached `Done` and logged nothing, because a missing
loot modifier list is not an error - it just stops injecting loot - and a missing lang file falls back
to rendering raw keys, which a headless server never renders. The content jars were unaffected, since
their filter is scoped inside `from(root) { }` rather than at the top of the task.

What found it was inspecting the built artifacts rather than the runtime: per-jar counts of files,
lang keys, loot modifier entries and tag entries, checked against what `partitionSharedResources`
reported writing. **A boot proves what loads; only the artifact proves what shipped.** Both are needed,
and the artifact check is the cheaper of the two. `checkJarPartition` now does it on every `check`:
no jar may carry another's classes, and every variant the partition wrote must reach the jar it was
written for.

**A trap when rerunning the boot configurations.** A server run never stops on its own, so the forked
JVM outlives the gradle task that started it and keeps holding its port. The next configuration then
fails to bind and stops *before loading a single datapack* - which in the log looks exactly like a
clean install, zero recipe errors and zero tag errors, and no `Done`. Each configuration now binds its
own port (25566 through 25569), so a survivor can only ever block a rerun of itself. Read the `Done`
line, not the absence of errors.

**All five boot configurations reach `Done` with zero recipe errors and zero tag errors**, and the six
jars carry the partition the map describes: base 3,701 files with 2,250 lang keys and the 15 chest loot
modifiers, arboriculture 9,252 with 1,249 lang keys and the grafter, and **zero foreign classes in any
of the six**. That is the first end-to-end evidence that D4 delivers what it promised. Publishing the
six artifacts is still to do.

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
  carry FQCNs of classes that move in phases 5 through 9. **Done 2026-08-02**: the
  `checkResourceFqcn` Gradle task scans `src/main/resources` and `src/generated/resources` for
  dotted runs beginning `forestry`, resolves the first segment that starts with an uppercase letter
  as the class, and fails if no matching `.java` exists. It also reads `META-INF/services` file
  names. 17 names resolve today. It runs as part of `check`, and was verified to fail on a planted
  bad name before being relied on. All three Patchouli classes moved to
  `forestry.core.platform.compat.patchouli.*` in phase 7b, mechanically, and this is what proved it.
- **GameTests.** `./gradlew runGameTestServer` stays green throughout. It was green as of
  2026-07-28.
- **Boundary test.** An ArchUnit-style test is added at phase 1 and tightened at each
  phase, enforcing the dependency graph long before the compiler can at phase 9. Until
  phase 9 this is the *only* proof available for the phase-6 gate.
  **Discharged 2026-08-02 without ever being written.** The role was filled instead by
  `checkBaseBoundary` (imports, phase 2) and `checkBaseBytecode` (constant pool, phase 6),
  and phase 9a handed the job to the compiler, which is strictly stronger than either.
  Both gates are now deleted. Two boundaries the compiler still cannot see keep their own
  gates: `checkApiBoundary`, because `api` ships inside core, and `checkCoreLayers`, because
  D2's layers are inside one source set. A third kind of boundary has **no** gate and cannot
  easily have one - a cross-jar reference carried as data rather than as a type. See the
  butterfly flower-type finding under phase 9a.
  **That third boundary got its gate on 2026-08-03.** The reference closure in
  `generateResourceOwners` reads every forestry id each resource names, resolves it against the
  ownership manifest, and fails the build when no jar can see them all. It is the data-level
  counterpart to what the compiler does for types, and it is what turned the whole class of
  cross-jar data defect from a runtime crash into a build failure.
- **Boot configurations.** Done 2026-08-03. All five run and all five reach `Done` with zero recipe
  errors and zero tag errors: `runCoreOnlyServer` (0.8s), `runApicultureServer` (3.4s),
  `runLepidopterologyNoBeesServer`, `runAllJarsServer`. Base-plus-arboriculture-plus-lepidopterology
  and base-plus-lepidopterology-without-apiculture are the same configuration, since lepidopterology
  declares a REQUIRED dependency on arboriculture.

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
  with no generated counterpart to derive per-jar key ownership from, and they ship whole in base. A
  player with only base installed therefore carries translations for content they do not have, which
  is harmless: an unused key is never looked up. `en_us` does split, because its generated half names
  registry ids.
- **The 13 taxa that stay in base.** `animalia`, `arthropoda`, `insecta` and the ranks above them span
  bees and butterflies, so no one jar can hold them. They are inert where unreferenced, but nothing
  yet decides whether a rank should be entry-split the way a tag is.

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
