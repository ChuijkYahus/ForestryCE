# Forestry 1.21.1 Porting Progress

## Build Environment

- Fixed Gradle wrapper startup to prefer a full JDK from `~/.gradle/jdks` when the system Java is only a JRE.
- Verified `./gradlew --version` now launches on JetBrains JDK 21.
- Verified `./gradlew compileJava --console=plain` now gets past `:createMinecraftArtifacts`.

## Current Compile State

As of 2026-04-07, the build reaches Forestry source compilation and fails with large API-porting clusters.

Primary error families from `./gradlew compileJava --console=plain`:

- Removed or moved NeoForge registry helpers:
  `RegistryObject`, `ForgeRegistries`, `ForgeFlowingFluid`, `ForgeMod`, `ObjectHolderRegistry`
- Capability API rewrite:
  `Capability`, `ICapabilityProvider`, `LazyOptional`, `ForgeCapabilities`, `RegisterCapabilitiesEvent`
- Removed Forge-era helpers:
  `IPlantable`, `PlantType`, `IShapedRecipe`, `StrictNBTIngredient`
- 1.21 recipe/data API changes:
  `FinishedRecipe`
- JEI NeoForge integration changes:
  `mezz.jei.api.forge.ForgeTypes`

## Session Strategy

To keep progress coherent across Codex sessions:

1. Work in narrow compile slices and make one commit per slice.
2. Update this file at the end of each slice with:
   - what was ported
   - what compile family was reduced
   - the exact next blocker
   - the exact command used to verify progress
3. Make a git commit immediately after each completed porting round.
   Use one commit per verified slice so future Codex sessions can resume from clean checkpoints.
4. Keep commits scoped by subsystem, not by file type.
   Good commit examples:
   - `port item capability registration to RegisterCapabilitiesEvent`
   - `port block entity fluid and energy capabilities`
   - `port recipe builders off FinishedRecipe`
5. Before touching a new slice, run a filtered compile grep against the target files so the next session can see whether the slice is still the active blocker.
6. Prefer JMCP-backed notes over memory.
   If a class moved or an API rename is confirmed, add it to session notes here and save a JMCP porting pattern when the mapping is stable.

## Completed Slices

- Shared feature/deferred registration layer:
  - Replaced the old `RegistryObject`-centric assumptions in `src/main/java/forestry/modules/features/`.
  - This slice no longer dominates the compile output.
- Item capability registration:
  - Forestry item capabilities now use `net.neoforged.neoforge.capabilities`.
  - `RegisterCapabilitiesEvent` listeners now register explicit item providers for:
    - apiarist armor bee protection
    - naturalist spectacles vision
    - bee, tree, and butterfly genetic item handlers
    - vanilla sapling genetic handlers
    - pipette and Forestry fluid container item fluid handlers
  - Forestry item-capability consumers were updated off `LazyOptional` for the migrated custom item capabilities.
- Item-capability-adjacent cleanup:
  - `ItemArmorApiarist` now uses the 1.21.1 holder-based `ArmorMaterial` API with `ArmorMaterial.Layer`.
  - `ModuleApiculture` brewing recipe setup now creates potion stacks through `PotionContents.createItemStack(...)`.
  - `ModuleCore` now uses `ItemEntityPickupEvent`, `LevelTickEvent.Post`, and `NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS`.
  - `JeiUtil` now reads shaped recipe dimensions from `ShapedRecipe#getWidth()` / `getHeight()`.
  - Added apiarist armor layer texture copies under `assets/forestry/textures/models/armor/` for the new armor-material texture lookup.
- Registry-helper cleanup:
  - `ModUtil` now reads vanilla item, block, fluid, and particle registry keys through `BuiltInRegistries`.
  - `FluidMap` now resolves `ResourceLocation` and string fluid keys through `BuiltInRegistries.FLUID`.
  - `FeatureRegistry` now runs post-registration callbacks from its own `RegisterEvent` listener instead of relying on removed `ObjectHolderRegistry`.
  - `ModuleCore` now runs its item post-registration setup from common setup enqueue work.
- Core and cultivation block capability providers:
  - `TileForestry`, `TilePowered`, and `TilePlanter` now expose plain item, energy, and fluid handler accessors instead of overriding removed Forge-era `getCapability(...)` APIs.
  - `ModuleCore` now registers block entity capabilities for analyzer, escritoire, and naturalist chest tile entities with `RegisterCapabilitiesEvent.registerBlockEntity(...)`.
  - `ModuleCultivation` now registers item, energy, and fluid block capabilities for all planter tile entity types.
  - `TileAnalyzer` no longer overrides the removed Forge-era fluid capability hook and now relies on module capability registration.
- Tick-event package cleanup:
  - `MultiblockServerTickHandler` now uses `LevelTickEvent.Pre`.
  - `ModuleStorage` now uses `LevelTickEvent.Post` and `ItemEntityPickupEvent`.
  - `NonStackingBeeEffect` now listens on `LevelTickEvent.Pre`.
- Capability consumer cleanup:
  - `ItemInventory` no longer implements the removed Forge-era `ICapabilityProvider`.
  - `TileUtil` now reads item and generic block capabilities through `Level#getCapability(...)`.
  - `EnergyHelper` now pushes and probes energy through `Capabilities.EnergyStorage.BLOCK`.
- Energy and alveary block capability registration:
  - `EngineBlockEntity`, `BiogasEngineBlockEntity`, `TileAlveary`, `TileAlvearyClimatiser`, and `TileAlvearyHygroregulator` now expose explicit item, energy, and fluid provider methods instead of overriding removed Forge-era `getCapability(...)`.
  - `ModuleEnergy` now registers engine block entity energy providers for all engine tile types and the biogas engine fluid provider through `RegisterCapabilitiesEvent`.
  - `ModuleApiculture` now registers alveary block entity item providers for all alveary tile types, plus energy for fan/heater and fluid for the hygroregulator.
- Core datagen provider cleanup:
  - `ForestryAdvancementProvider` now targets the 1.21.1 advancement API via NeoForge `AdvancementProvider`, `AdvancementType`, and the new advancement reward builder.
  - `ForestryLootTableProvider`, `ForestryBlockLootTables`, and `ForestryChestLootTables` now thread `HolderLookup.Provider` through the 1.21.1 loot table APIs, including `ResourceKey<LootTable>` outputs and holder-backed enchantment lookups.
  - `ForestryFeaturesProvider` now uses `BootstrapContext`, and `ForestryAtlasProvider` now uses the lookup-aware `SpriteSourceProvider` constructor with `gather()`.
  - `Data` now wires the updated providers and uses the standalone `@EventBusSubscriber` annotation form.
- Deferred registration holder cleanup:
  - `ForestryItemModelProvider` now iterates item deferred entries as `DeferredHolder<Item, ? extends Item>` instead of the removed `RegistryObject`.
  - `CoreParticles`, `ApicultureVillagers`, and `ArboricultureVillagers` now store deferred registration handles as `DeferredHolder`, matching NeoForge 1.21.1 `DeferredRegister#register(...)`.
  - This clears the remaining direct `RegistryObject` compile errors from the datagen/registry layer.
- Plant/tool helper cleanup:
  - `BlockForestryLog` now uses NeoForge 1.21.1 `ItemAbility` / `ItemAbilities` for axe stripping instead of the removed `ToolAction` / `ToolActions`.
  - Forestry tree placement no longer depends on removed `IPlantable` / `PlantType`; `Tree` and `TreeDecorator` now use Forestry-side soil checks backed by `BlockTags.DIRT`, which already includes humus in Forestry tags.
  - `BlockHumus`, `BlockBogEarth`, `FertileBeeEffect`, and `EntityButterfly` no longer reference the removed Forge plantable helpers.
- Event subscriber cleanup:
  - `EventHandlerCore`, `MultiblockServerTickHandler`, and `MultiblockEventHandler` now use the standalone `@EventBusSubscriber` annotation import.
  - `EventHandlerCore` now listens with `LivingIncomingDamageEvent` instead of the removed `LivingAttackEvent`.
- Farm, sorting, and factory block capability cleanup:
  - `ModuleFarming` now registers gearbox energy, hatch item, and valve fluid block-entity capabilities through NeoForge 1.21.1 `RegisterCapabilitiesEvent`.
  - `TileFarmGearbox`, `TileFarmHatch`, and `TileFarmValve` now expose explicit energy/item/fluid handlers instead of removed `LazyOptional`-based `getCapability(...)` overrides.
  - `ForestryCapabilities` now defines a NeoForge `BlockCapability` for sorting filter logic, and `ModuleSorting` registers both the genetic filter item handler and filter-logic block capability.
  - `TileGeneticFilter` now exposes its sided item handler through an explicit accessor for capability registration.
  - `ModuleFactory` now registers item, energy, and fluid block capabilities for factory machine block entities, and the liquid factory tiles now expose explicit `getFluidHandler(...)` accessors instead of Forge-era capability overrides.
- Registry and fluid helper cleanup:
  - Remaining `ForgeRegistries` users in `FilteredTank`, `ForestersManualItem`, `FakeAlvearyController`, `FarmController`, `FermenterRecipe`, and `InventoryBottler` now use `BuiltInRegistries` or simple vanilla fluid checks.
  - Remaining `LazyOptional`-based fluid helper callers in `ContainerLiquidTanksHelper`, `FluidHelper`, `BottlerRecipe`, `InventoryRaintank`, `TileBottler`, and `TileRaintank` now use NeoForge 1.21.1 `FluidUtil`'s `Optional`-returning APIs.
  - This clears the active `ForgeRegistries` and helper-side `LazyOptional` compile failures from those core/factory/apiculture/farming helper files.
- Compat entity-capability cleanup:
  - `ForestryChestBoat` no longer overrides removed Forge-era entity capability hooks or stores a `LazyOptional`; it now exposes a plain `InvWrapper` accessor.
  - `ModuleArboriculture` now registers the chest boat inventory through `RegisterCapabilitiesEvent.registerEntity(...)` with `Capabilities.ItemHandler.ENTITY`.
  - `CuriosCompat` no longer depends on removed NeoForge `CapabilityManager` / `CapabilityToken` lookup code and now queries Curios inventories through `CuriosApi.getCuriosInventory(player)`.
  - This clears the direct `ForestryChestBoat` and `CuriosCompat` capability compile failures from the active compat slice.

## Next Work Plan

1. Port remaining JEI NeoForge integration off `mezz.jei.api.forge.ForgeTypes` and the lingering old capability access in the factory JEI plugin.
2. Port remaining compatibility-side old capability users such as `ForestryChestBoat` and `CuriosCompat`.
3. Continue the Mojang-side 1.21 signature migrations once the removed Forge/NeoForge API references are no longer dominating the compile.

## Session Notes

- JMCP session configured with:
  - project: `/home/kali/Desktop/jmcp-testing/ForestryCE`
  - old source tree: `/home/kali/Desktop/jmcp-testing/ModKit`
  - source version: `1.20.1`
  - target version: `21.1.213`
- Target NeoForge sources are available in JMCP under version key `21.1.213`.
- Old source registration still needs the correct JMCP-resolvable 1.20.1 coordinate if class diffs are needed across versions.
- Current verification command for the item-capability slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg "ItemGE|IIndividualHandlerItem|ModuleArboriculture|ItemArmorApiarist|ModuleApiculture|ModuleCore|ModuleFluids|ModuleLepidopterology|JeiUtil"`
- Current verification command for the item-capability-adjacent cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 2 "ItemArmorApiarist|ModuleApiculture|ModuleCore|JeiUtil|error:"`
- Current verification command for the registry-helper cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 2 "ModUtil|FluidMap|FeatureRegistry|ModuleCore|ForgeRegistries|ObjectHolderRegistry|error:"`
- Current verification command for the base block-capability cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 2 "TileForestry|TilePlanter|TilePowered|TileAnalyzer|ModuleCultivation|ModuleCore|Capability|ForgeCapabilities|LazyOptional|error:"`
- Current verification command for the tick-event cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 2 "MultiblockServerTickHandler|ModuleStorage|NonStackingBeeEffect|TickEvent|ItemEntityPickupEvent|error:"`
- Current verification command for the capability-consumer cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 2 "ItemInventory|TileUtil|EnergyHelper|ForgeCapabilities|ICapabilityProvider|error:"`
- Current verification command for the engine/alveary block-capability slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 2 "EngineBlockEntity|BiogasEngineBlockEntity|TileAlveary|TileAlvearyClimatiser|TileAlvearyHygroregulator|ModuleEnergy|ModuleApiculture|ForgeCapabilities|LazyOptional|error:"`
- Compile family reduced by the latest slice:
  - cleared the direct `EngineBlockEntity`, `BiogasEngineBlockEntity`, `TileAlveary`, `TileAlvearyClimatiser`, and `TileAlvearyHygroregulator` block-capability provider errors from the filtered compile
- Current next blocker after the engine/alveary block-capability slice:
  - recipe/data generation APIs now dominate the compile failures, with remaining Forge-era capability users still present in farming, sorting, factory, and compat code
- Current verification command for the core datagen provider cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n "ForestryAdvancementProvider|ForestryLootTableProvider|ForestryBlockLootTables|ForestryChestLootTables|ForestryFeaturesProvider|ForestryAtlasProvider|forestry/core/data/Data.java"`
- Compile family reduced by the latest slice:
  - cleared the direct `ForestryAdvancementProvider`, `ForestryLootTableProvider`, `ForestryBlockLootTables`, `ForestryChestLootTables`, `ForestryFeaturesProvider`, `ForestryAtlasProvider`, and `Data` datagen API errors from the filtered compile
- Current next blocker after the core datagen provider cleanup slice:
  - remaining datagen/registry cleanup now starts with `src/main/java/forestry/core/data/models/ForestryItemModelProvider.java` still using removed `RegistryObject`, while non-datagen compile failures remain concentrated in Forge-era capability users and other 1.21 API migrations
- Current verification command for the deferred registration holder cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | grep -E "ForestryItemModelProvider|CoreParticles|ApicultureVillagers|ArboricultureVillagers|RegistryObject|error:"`
- Compile family reduced by the latest slice:
  - cleared the direct `RegistryObject` failures in `ForestryItemModelProvider`, `CoreParticles`, `ApicultureVillagers`, and `ArboricultureVillagers`
- Current next blocker after the deferred registration holder cleanup slice:
  - compile failures are now led by remaining NeoForge 1.21 API migrations around removed Forge-era plant/tool helpers (`IPlantable`, `PlantType`, `ToolAction`, `ToolActions`), removed capability APIs still present in farming/factory/sorting/compat code, and several Mojang-side 1.21 signature changes
- Current verification command for the plant/tool helper cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | grep -E "BlockForestryLog|BlockHumus|BlockBogEarth|TreeDecorator|forestry/arboriculture/genetics/Tree|FertileBeeEffect|EntityButterfly|IPlantable|PlantType|ToolAction|ToolActions|error:"`
- Compile family reduced by the latest slice:
  - cleared the direct `IPlantable`, `PlantType`, `ToolAction`, and `ToolActions` failures from the active Forestry tree/soil and butterfly/bee files
- Current verification command for the event subscriber cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | grep -E "EventHandlerCore|MultiblockServerTickHandler|MultiblockEventHandler|LivingAttackEvent|EventBusSubscriber|error:"`
- Compile family reduced by the latest slice:
  - cleared the direct `LivingAttackEvent` and `@Mod.EventBusSubscriber` failures in the Forestry core event subscriber classes
- Current next blocker after the event subscriber cleanup slice:
  - compile failures are now front-loaded by removed `ForgeRegistries` users, remaining `LazyOptional`/old capability API consumers in farming, sorting, factory, and compat code, plus several isolated Mojang/NeoForge 1.21 API signature changes
- Current verification command for the farm/sorting/factory block capability cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 2 "ModuleFarming|TileFarmGearbox|TileFarmHatch|TileFarmValve|ModuleSorting|TileGeneticFilter|ModuleFactory|TileBottler|TileCarpenter|TileFabricator|TileFermenter|TileMoistener|TileRaintank|TileSqueezer|TileStill|LazyOptional|ForgeCapabilities|RegisterCapabilitiesEvent|error:"`
- Compile family reduced by the latest slice:
  - cleared the direct removed `Capability`/`ForgeCapabilities`/`LazyOptional` block-provider failures in farming, sorting, and the core set of factory machine tiles by moving them onto NeoForge 1.21.1 block capability registration
- Current next blocker after the farm/sorting/factory block capability cleanup slice:
  - compile failures are now led by remaining `ForgeRegistries` users, `LazyOptional` consumers in core/factory recipe and compat helpers, JEI `ForgeTypes`, plus broader Mojang-side 1.21 API signature changes
- Current verification command for the registry and fluid helper cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 2 "FilteredTank|ForestersManualItem|FakeAlvearyController|FarmController|FermenterRecipe|InventoryBottler|ContainerLiquidTanksHelper|FluidHelper|BottlerRecipe|InventoryRaintank|TileBottler|TileRaintank|ForgeRegistries|LazyOptional|error:"`
- Compile family reduced by the latest slice:
  - cleared the direct `ForgeRegistries` failures in the active core/apiculture/farming/factory helper files and moved the active fluid helper callers off removed `LazyOptional` APIs onto `Optional`
- Current next blocker after the registry and fluid helper cleanup slice:
  - compile failures are now front-loaded by JEI `ForgeTypes`, old capability users in compat entities/helpers, and broader Mojang-side 1.21 API signature changes
- Current verification command for the compat entity-capability cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 2 "ForestryChestBoat|ModuleArboriculture|CuriosCompat|Capabilities\\.ItemHandler\\.ENTITY|CuriosApi|LazyOptional|ForgeCapabilities|error:"`
- Compile family reduced by the latest slice:
  - cleared the direct `ForestryChestBoat` and `CuriosCompat` removed capability API failures by moving the chest boat inventory onto NeoForge entity capability registration and switching Curios access over to `CuriosApi`
- Current next blocker after the compat entity-capability cleanup slice:
  - compile failures are now front-loaded by KubeJS API removals (`KubeJSPlugin`, `BindingsEvent`, `EventJS`), worktable/recipe migration fallout (`RecipeUtil`, `RecipeHolder`), loot function serializer rewrites, and broader Mojang-side 1.21 signature changes
