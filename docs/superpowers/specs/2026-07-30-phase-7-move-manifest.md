# Phase 7 move manifest

Companion to `2026-07-30-feature-package-reorg-design.md`. Enumerates the moves phase 7
performs, in execution order.

## How to read this

Two kinds of entry:

- **Package move** - the whole package relocates. One IntelliJ Move Package operation.
  No per-file list needed.
- **Fan-out** - one package splits across several destinations. Per-file assignment is
  listed, because the IDE cannot infer it.

Steps are ordered so that each one leaves the tree compiling. Run `build_project` (IDE) or
`./gradlew compileJava` between steps; run `Data` and diff before declaring the phase done.

Files marked **(?)** need a decision before they move. They are listed at the end of the
step that would otherwise carry them.

Counts in this manifest were taken on 2026-07-31 and will drift as phases 1 through 6 land.
Re-derive before executing; the per-file assignments stay valid, the totals may not.

## Prerequisite

Phases 1 through 6 are complete. In particular phase 4 has already moved
`TileApiaristChest`, `TileArboristChest` and `TileLepidopteristChest` out of `core/tiles`,
`ApiaristPoolElement` out of `core/worldgen`, and `FeatureHelper` out of `core/worldgen`.
This manifest assumes those files are gone from `core`.

---

## Step 7.1 - api package moves

Package moves. Addresses `api` only; nothing here changes behavior.

| From | To |
| --- | --- |
| `forestry.api.genetics` | `forestry.api.core.genetics` |
| `forestry.api.multiblock` | `forestry.api.core.multiblock` |
| `forestry.api.circuits` | `forestry.api.core.circuits` |
| `forestry.api.climate` | `forestry.api.core.climate` |
| `forestry.api.recipes` | `forestry.api.core.machines` |
| `forestry.api.fuels` | `forestry.api.core.machines.fuels` |
| `forestry.api.storage` | `forestry.api.core.backpacks` |
| `forestry.api.farming` | `forestry.api.agriculture` |
| `forestry.api.util` | `forestry.api.core.util` |

`api.plugin`, `api.client`, `api.apiculture`, `api.arboriculture`, `api.lepidopterology`,
`api.mail`, `api.modules` and the loose `api` types stay where they are. The concern-first
grouping inside `api.plugin` and `api.client` is deliberate and is not touched.

Loose `api.core` types (`IProduct`, `IError`, `INbtReadable`, `TemperatureType`, ...) stay
in `api.core`.

---

## Step 7.2 - registration framework

Package move.

| From | To |
| --- | --- |
| `forestry.modules.features` | `forestry.core.platform.registration` |

`forestry.modules` itself (`BlankForestryModule`, `ForestryModuleManager`, `ModuleUtil`)
stays put - it is the module framework, not the registration framework.

Per-jar registration holders keep the name `features/` and do not move in this step.

---

## Step 7.3 - core to platform

Package moves. Every one of these is framework with no game content.

| From | To |
| --- | --- |
| `forestry.core.gui` | `forestry.core.platform.gui` |
| `forestry.core.network` | `forestry.core.platform.network` |
| `forestry.core.inventory` | `forestry.core.platform.inventory` |
| `forestry.core.utils` | `forestry.core.platform.util` |
| `forestry.core.fluids` | `forestry.core.platform.fluids` |
| `forestry.core.multiblock` | `forestry.core.platform.multiblock` |
| `forestry.core.render` | `forestry.core.platform.render` |
| `forestry.core.models` | `forestry.core.platform.models` |
| `forestry.core.commands` | `forestry.core.platform.commands` |
| `forestry.core.owner` | `forestry.core.platform.owner` |
| `forestry.core.errors` | `forestry.core.platform.errors` |
| `forestry.core.config` | `forestry.core.platform.config` |
| `forestry.core.client` | `forestry.core.platform.client` |
| `forestry.core.loot` | `forestry.core.platform.loot` |
| `forestry.core.particles` | `forestry.core.platform.particles` |
| `forestry.core.entities` | `forestry.core.platform.entities` |
| `forestry.core.tab` | `forestry.core.platform.tab` |
| `forestry.core.damage` | `forestry.core.platform.damage` |
| `forestry.core.registration` | `forestry.core.platform.villager` |
| `forestry.core.worldgen` | `forestry.core.platform.worldgen` |
| `forestry.core.recipes` | `forestry.core.platform.recipes` |

`core.registration` holds only `VillagerTrade`, hence the rename - `registration` is now
taken by step 7.2.

`core.gui` fans out; see step 7.5. Move the package first, then split.

`core.render` carries one passenger: `RenderMill` is mill-specific, so it leaves for
`core.content.machines` rather than staying in `core.platform.render`. Same reasoning as
`TileMill` in step 7.5.

---

## Step 7.4 - core to engine

Package moves.

| From | To |
| --- | --- |
| `forestry.core.genetics` | `forestry.core.engine.genetics` |
| `forestry.core.climate` | `forestry.core.engine.climate` |
| `forestry.core.circuits` | `forestry.core.engine.circuits` |

---

## Step 7.5 - core fan-out

Four packages split. This is the only step in phase 7 that needs per-file work inside
`core`.

### core/tiles (22 files after phase 4)

To `core.platform.tile`:

```
AdjacentTileCache      IActivatable          IFilterSlotDelegate
IForestryTicker        IItemStackDisplay     ILiquidTankTile
IPowerHandler          IRenderableTile       ITitled
TemperatureState       TileBase              TileForestry
TilePowered            TileUtil              TileNaturalistChest
```

To `core.content.machines`:

```
TileMill
```

To `core.content.escritoire`:

```
EscritoireGame   EscritoireGameBoard   EscritoireGameToken
EscritoireTextSource   TileEscritoire
```

To `core.content.analyzer`:

```
TileAnalyzer
```

`TileMill` reads as framework and is not. `TileMillRainmaker` is its only subclass - the
other mills were removed from the mod - so it belongs with the machines. It carries a
`todo` to merge the two outright, which would delete this move. Its two remaining
references follow it: `RenderMill`, pulled out of `core.render` in step 7.3, and
`BlockTypeFactoryTesr`, already inside `factory` and so covered by step 7.6.

`TileNaturalistChest` is the shared chest base; the three concrete chests left in phase 4.

### core/blocks (17 files)

To `core.platform.block`:

```
BlockForestry      BlockBase          BlockCore          BlockStructure
BlockTesr          BlockTypeCoreTesr  IBlockType         IColoredBlock
IMachineProperties IShapeProvider     ISimpleShapeProvider
MachineProperties  TileStreamUpdateTracker
```

To `core.content.soil`:

```
BlockBogEarth   BlockHumus
```

To `core.content.resources`:

```
BlockResourceStorage   EnumResourceType
```

### core/items (19 files + definitions/)

To `core.platform.item`:

```
ItemForestry     ItemBlockForestry   ItemBlockTesr   ItemOverlay
WithScreenItem   ItemFluidContainerForestry          ItemForestryFood
HasRemnants
```

To `core.content.tools`:

```
ForestersManualItem   ItemWrench   ItemPipette   SolderingIronItem
```

To `core.content.analyzer`:

```
PortableAnalyzerItem
```

To `core.content.resources`:

```
ItemCraftingMaterial   ItemElectronTube   ItemFertilizer   ItemAssemblyKit
```

`core/items/definitions` splits: `EnumCraftingMaterial` and `EnumElectronTube` to
`core.content.resources`; `ToolTier` to `core.content.tools`; `DrinkProperties`,
`EnumContainerType`, `FluidHandlerItemForestry`, `IColoredItem`, `ICraftingPlan` to
`core.platform.item`.

**(?) `ItemSpectacles`** - bucket D flags it as importing
`arboriculture.capabilities.SpectacleVision`. Phase 4 decides whether the item moves to
arboriculture or the capability moves to base. If the capability moved to base, it belongs
in `core.content.tools`.

**(?) `ItemFruit`** - a core item whose subject is tree fruit. Either
`arboriculture.fruit` or `core.platform.item`, depending on whether anything outside
arboriculture instantiates it.

### core/gui (after step 7.3, now core.platform.gui)

Pull out of `core.platform.gui`:

To `core.content.analyzer`:

```
ContainerAnalyzer   GuiAnalyzer   PortableAnalyzerMenu   PortableAnalyzerScreen
```

To `core.content.escritoire`:

```
ContainerEscritoire   GuiEscritoire
```

Everything else stays in `core.platform.gui`, including `ContainerNaturalistInventory`,
`GuiNaturalistInventory`, `INaturalistMenu` and `IPagedInventory` - those are the shared
naturalist-chest UI that all three species jars reuse.

### core loose files (8)

| File | To |
| --- | --- |
| `ModuleCore`, `ModuleFluids` | stay at `forestry.core` |
| `ClientsideCode`, `ForestryColors` | `core.platform.client` |
| `EventHandlerCore`, `PickupHandlerCore` | `core.platform` |
| `FluidProductTypes` | `core.platform.fluids` |
| `TranslationKeys` | `core.platform.util` |

---

## Step 7.6 - absorb the five modules into core/content

Package moves. Each top-level module becomes a content directory.

| From | To |
| --- | --- |
| `forestry.factory` | `forestry.core.content.machines` |
| `forestry.energy` | `forestry.core.content.energy` |
| `forestry.storage` | `forestry.core.content.backpacks` |
| `forestry.sorting` | `forestry.core.content.sorting` |
| `forestry.worktable` | `forestry.core.content.worktable` |

Their internal `features/`, `gui/`, `tiles/`, `blocks/`, `client/`, `network/`, `compat/`
subpackages move with them unchanged - per D6, plumbing stays organized by kind.

`ModuleFactory`, `ModuleEnergy`, `ModuleStorage`, `ModuleSorting` and `ModuleWorktable`
move with their packages and keep their module ids.

---

## Step 7.7 - apiculture fan-out

Package moves first:

| From | To |
| --- | --- |
| `forestry.apiculture.genetics` | `forestry.apiculture.bees.genetics` |
| `forestry.apiculture.hives` | `forestry.apiculture.hives` (unchanged) |
| `forestry.apiculture.multiblock` | `forestry.apiculture.alveary.multiblock` |
| `forestry.apiculture.villagers` | `forestry.apiculture.apiarist.villagers` |

Then the fan-outs.

### apiculture loose files (25)

| To | Files |
| --- | --- |
| `bees` | `BeeSpecies`, `BeekeepingLogic`, `FakeBeekeepingLogic`, `BeeHousingListener`, `BeeHousingModifier`, `InventoryBeeHousing`, `HasFlowersCache`, `SingleActivityType`, `CathemeralActivityType`, `CrepuscularActivityType`, `TagFlowerType`, `WaterTagFlowerType`, `PhotosynthesisFlowerType`, `ApicultureFilterRule`, `ApicultureFilterRuleType` |
| `apiary` | `IApiary`, `ApiaryBeeListener`, `ApiaryBeeModifier` |
| `beehouse` | `BeehouseBeeModifier` |
| `alveary` | `AlvearyBeeModifier` |
| `hives` | `VillageHive`, `WorldgenBeekeepingLogic` |
| `apiarist` | `ApiaristAI`, `ArmorApiaristHelper` |
| stays at root | `ModuleApiculture` |

### apiculture/blocks (7)

| To | Files |
| --- | --- |
| `alveary` | `BlockAlveary` |
| `apiary` | `BlockApiculture`, `BlockTypeApiculture` |
| `hives` | `BlockBeeHive`, `BlockHiveType` |
| `bees` | `BlockHoneyComb` |

**(?) `NaturalistChestBlockType`** - this is the enum `ArboricultureVillagers` imports, one
of the D1 graph problems. Its destination follows that decision: if the enum moves to base
it goes to `core.platform.block`; if the arboriculture edge is accepted it goes to
`apiculture.apiarist`.

### apiculture/tiles (6)

| To | Files |
| --- | --- |
| `bees` | `TileBeeHousingBase`, `FakeBeeHousingInventory` |
| `apiary` | `TileApiary` |
| `beehouse` | `TileBeeHouse` |
| `hives` | `TileHive`, `HiveBeeHousingInventory` |

### apiculture/items (16)

| To | Files |
| --- | --- |
| `bees` | `ItemBeeGE`, `EnumHoneyComb`, `ItemHoneyComb`, `ItemBlockHoneyComb`, `EnumPollenCluster`, `ItemPollenCluster`, `EnumPropolis`, `ItemPropolis`, `ItemBeesWax`, `ItemRefractoryWax`, `ItemAmbrosia` |
| `apiary` | `ItemHiveFrame`, `ItemCreativeHiveFrame` |
| `apiarist` | `ItemArmorApiarist`, `ItemScoop`, `ItemSmoker` |

### apiculture/gui (13)

| To | Files |
| --- | --- |
| `alveary` | `ContainerAlveary`, `ContainerAlvearyHygroregulator`, `ContainerAlvearySieve`, `ContainerAlvearySwarmer`, `GuiAlveary`, `GuiAlvearyHygroregulator`, `GuiAlvearySieve`, `GuiAlvearySwarmer` |
| `bees` | `ContainerBeeHousing`, `GuiBeeHousing`, `IContainerBeeHousing`, `IGuiBeeHousingDelegate`, `ContainerBeeHelper` |

### apiculture/inventory (5)

| To | Files |
| --- | --- |
| `alveary` | `InventoryAlvearySieve`, `InventoryHygroregulator`, `InventorySwarmer` |
| `apiary` | `IApiaryInventory`, `InventoryApiary` |

`apiculture/features`, `network`, `particles`, `render`, `models`, `entities`, `commands`,
`compat`, `recipes`, `proxy` stay at jar level per D6.

---

## Step 7.8 - arboriculture fan-out

Package moves first:

| From | To |
| --- | --- |
| `forestry.arboriculture.genetics` | `forestry.arboriculture.trees.genetics` |
| `forestry.arboriculture.charcoal` | `forestry.arboriculture.charcoal` (unchanged) |
| `forestry.arboriculture.capabilities` | `forestry.arboriculture.trees.capabilities` |

`arboriculture.worldgen` (61 files) stays as a jar-level package. It is tree generation
throughout, so a `trees.worldgen` nesting would add depth without separating anything.

### arboriculture loose files (13)

| To | Files |
| --- | --- |
| `trees` | `TreeSpecies`, `TreeManager`, `ArboricultureFilterRuleType` |
| `wood` | `ForestryWoodType`, `VanillaWoodType`, `IWoodTyped`, `WoodAccess`, `WoodHelper` |
| `fruit` | `Fruit`, `DummyFruit`, `PodFruit`, `RipeningFruit` |
| stays at root | `ModuleArboriculture` |

### arboriculture/blocks (29)

| To | Files |
| --- | --- |
| `wood` | `BlockForestryLog`, `BlockForestryPlank`, `BlockForestrySlab`, `BlockForestryStairs`, `BlockForestryFence`, `BlockForestryFenceGate`, `BlockForestryDoor`, `BlockForestryTrapdoor`, `BlockForestryButton`, `BlockForestryPressurePlate`, `BlockForestryStandingSign`, `BlockForestryWallSign`, `BlockForestryHangingSign`, `BlockForestryWallHangingSign` |
| `leaves` | `BlockAbstractLeaves`, `BlockForestryLeaves`, `BlockDefaultLeaves`, `BlockDefaultLeavesFruit`, `BlockDecorativeLeaves`, `BlockExtendedLeaves`, `ForestryLeafType`, `ILeafTypeBlock` |
| `fruit` | `BlockFruitPod`, `ForestryPodType` |
| `sapling` | `BlockSapling` |
| `charcoal` | `BlockCharcoal`, `BlockAsh`, `LogPileBlock`, `DecorativeLogPileBlock` |

### arboriculture/items (12)

| To | Files |
| --- | --- |
| `wood` | `ItemBlockWood`, `ItemBlockWoodDoor`, `ItemBlockWoodSlab`, `ItemBlockSign`, `ItemBlockHangingSign`, `ItemForestryBoat`, `ForestryBoatDispenserBehavior` |
| `leaves` | `ItemBlockLeaves`, `ItemBlockDecorativeLeaves`, `ItemBlockDefaultLeaves` |
| `trees` | `TreeItem`, `GrafterItem` |

### arboriculture/tiles (4)

| To | Files |
| --- | --- |
| `fruit` | `TileFruitPod` |
| `leaves` | `TileLeaves` |
| `sapling` | `TileSapling` |
| `trees` | `TileTreeContainer` |

`arboriculture/features`, `network`, `client`, `models`, `commands`, `loot`, `entities`,
`villagers`, `compat` stay at jar level.

---

## Step 7.9 - lepidopterology fan-out

Package move:

| From | To |
| --- | --- |
| `forestry.lepidopterology.genetics` | `forestry.lepidopterology.butterflies.genetics` |

### Fan-out

| To | Files |
| --- | --- |
| `butterflies` | `ButterflySpecies`, `ButterflySpawner`, `DummyButterflyEffect`, `LepidopterologyFilterRule`, `LepidopterologyFilterRuleType`, `items/ItemButterflyGE` |
| `cocoons` | `blocks/BlockCocoon`, `blocks/BlockSolidCocoon`, `tiles/TileCocoon` |
| stays at root | `ModuleLepidopterology`, `entities/`, `features/`, `render/`, `recipe/`, `commands/`, `compat/`, `proxy/` |

Note `BlockSolidCocoon` is one of the four open bugs in the dead-callback audit. Moving it
does not fix that; do not conflate the two.

---

## Step 7.10 - agriculture

Package moves. `farming` and `cultivation` merge under one jar.

| From | To |
| --- | --- |
| `forestry.farming.logic` | `forestry.agriculture.farmlogic` |
| `forestry.farming.multiblock` | `forestry.agriculture.multifarm.multiblock` |
| `forestry.farming.tiles` | `forestry.agriculture.multifarm.tiles` |
| `forestry.farming.blocks` | `forestry.agriculture.multifarm.blocks` |
| `forestry.farming.gui` | `forestry.agriculture.multifarm.gui` |
| `forestry.farming.items` | `forestry.agriculture.multifarm.items` |
| `forestry.farming.circuits` | `forestry.agriculture.multifarm.circuits` |
| `forestry.farming.features` | `forestry.agriculture.features` |
| `forestry.farming.client` | `forestry.agriculture.client` |
| `forestry.farming.compat` | `forestry.agriculture.compat` |
| `forestry.cultivation.tiles` | `forestry.agriculture.planter.tiles` |
| `forestry.cultivation.blocks` | `forestry.agriculture.planter.blocks` |
| `forestry.cultivation.gui` | `forestry.agriculture.planter.gui` |
| `forestry.cultivation.inventory` | `forestry.agriculture.planter.inventory` |
| `forestry.cultivation.items` | `forestry.agriculture.planter.items` |
| `forestry.cultivation.features` | merge into `forestry.agriculture.features` |
| `forestry.cultivation.proxy` | merge into `forestry.agriculture.client` |

Loose files: `farming.IFarmHousingInternal` and `cultivation.IFarmHousingInternal` to
`forestry.agriculture.farmlogic`. `ModuleFarming` and `ModuleCultivation` both move to
`forestry.agriculture` and keep their existing module ids per the spec's Deferred section -
they are not merged into one module.

`farming.logic.crops` and `farming.logic.farmables` move with `farming.logic`.

---

## Step 7.11 - mail fan-out

| To | Files |
| --- | --- |
| `letters` | `Letter`, `LetterProperties`, `LetterUtils`, `MailAddress`, `items/LetterItem`, `items/ItemStamp`, `items/EnumStampDefinition`, `postalstates/EnumDeliveryState`, `postalstates/ResponseNotMailable` |
| `postoffice` | `PostOffice`, `IWatchable`, `tiles/TileMailbox`, `tiles/TileStampCollector` |
| `tradestation` | `tiles/TileTrader`, `items/CatalogueItem` |
| `carriers` | `carriers/PostalCarriers` (unchanged) |
| stays at root | `ModuleMail`, `blocks/`, `features/`, `gui/`, `inventory/`, `network/`, `client/`, `commands/`, `compat/` |

`mail/blocks` holds only `BlockMail` and `BlockTypeMail`, both of which cover all three
mail machines, so they stay at jar level rather than picking one feature dir.

---

## Step 7.12 - dissolve compat

`forestry.compat` does not survive. Each integration moves to the jar whose content it
describes.

| From | To |
| --- | --- |
| `forestry.compat.jei` | split per jar into `<jar>.compat.jei` |
| `forestry.compat.patchouli` | split per jar into `<jar>.compat.patchouli` |
| `forestry.compat.kubejs` | `forestry.core.platform.compat.kubejs` |
| `forestry.compat.curios` | `forestry.core.platform.compat.curios` |
| `forestry.compat.ModuleCurios` | `forestry.core.platform.compat` |

Only shared plumbing stays in base. `ModuleCurios` keeps its module id.

**This step breaks hand-authored JSON.** The Patchouli book templates name three classes by
fully qualified name:

```
forestry.compat.patchouli.component.FluidComponent
forestry.compat.patchouli.processor.CarpenterProcessor
forestry.compat.patchouli.processor.FabricatorProcessor
```

IntelliJ will not update these. Fix them by hand as part of this step, and run the FQCN
resolution check from the spec's Verification section before moving on.

---

## After the last step

1. `Data` run configuration, then `git diff src/generated/resources`. Must be empty. Any
   diff is a defect, not a rebase artifact.
2. `GameTestServer` run configuration. Must be green.
3. FQCN resolution check across `src/main/resources` and `META-INF/services`.
4. ArchUnit boundary test.
5. Commit as one bulk move and add its sha to `.git-blame-ignore-revs`.
