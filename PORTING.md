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
- Item/mob effect/client signature cleanup:
  - `ItemElectronTube` and `ItemPipette` now import `Item` for the 1.21 tooltip context signature instead of failing on `Item.TooltipContext`.
  - `PotionBeeEffect`, `PotionBeeEffectExclusive`, `DefaultForestryPlugin`, and `EventHandlerCore` now use holder-based mob effects, matching the 1.21.1 `MobEffectInstance`, `hasEffect`, and `removeEffect` APIs.
  - `BlockForestryFluid`, `ModUtil`, `DefaultForestryClientRegistration`, and `EntityUtil` now use 1.21.1-friendly constructor/signature forms for `LiquidBlock`, `ResourceLocation`, and `Mob#finalizeSpawn(...)`.
  - This clears the active compile failures around tooltip signatures, holder-based mob effects, `ResourceLocation` private constructors, and the `EntityUtil` spawn helper.
- Compat/client resource lookup cleanup:
  - `FluidComponent` now resolves fluids through `BuiltInRegistries.FLUID` and uses `ResourceLocation.parse(...)` / `fromNamespaceAndPath(...)` instead of removed `ForgeRegistries` and private constructors.
  - `BeeAnalyzerPlugin` now dereferences tag entries through `Holder#value()`.
  - `ResourceUtil`, `FluidMap`, and `JsonUtil` now use 1.21.1 `ResourceLocation` parsing helpers, and `JsonUtil` builds NBT-backed `ItemStack`s through `DataComponents.CUSTOM_DATA`.
  - This clears the active `FluidComponent`, `BeeAnalyzerPlugin`, `ResourceUtil`, `FluidMap`, and `JsonUtil` compile failures from the filtered compile.
- Recipe base-interface and helper cleanup:
  - `IForestryRecipe` now targets 1.21.1 `Recipe<RecipeInput>` semantics, including `HolderLookup.Provider` assembly/result signatures instead of the removed `Recipe<Container>` assumptions.
  - `RecipeUtils` now treats Forestry machine recipe types as `IForestryRecipe` lookups, fixes the fabricator melting lookup against `IFabricatorSmeltingRecipe`, and restores distinct fluid-filter helpers for `FluidIngredient`, `FluidStack`, and plain `Fluid` outputs.
  - Forestry machine recipe implementations now use the 1.21.1 `getResultItem(HolderLookup.Provider)` signature, and `FabricatorSmeltingRecipe.Serializer` now exposes `codec()` / `streamCodec()` with `RegistryFriendlyByteBuf`.
  - This clears the active `IForestryRecipe`, `RecipeUtils`, and filtered machine-recipe signature failures from the compile output.
- Item/menu/block interaction signature cleanup:
  - `ItemWithGui` and backpack item menus now open through `ServerPlayer.openMenu(...)` with `RegistryFriendlyByteBuf` extra data instead of removed `NetworkHooks` helpers.
  - `BlockBase`, `BlockStructure`, and `BlockGeneticFilter` now use the split 1.21.1 block interaction hooks (`useWithoutItem(...)` / `useItemOn(...)`) instead of the removed monolithic `use(...)` override.
  - Forestry item tooltip, armor-texture, use-duration, event-bus, and break-callback callers were updated for 1.21.1 in `ItemFluidContainerForestry`, `ItemSpectacles`, `ItemCircuitBoard`, `ItemBlockFarm`, `ItemLetter`, `ItemGrafter`, `ItemFruit`, `ContainerSocketedHelper`, `ItemBackpack`, `BackpackResupplyHandler`, and `BeekeepingLogic`.
  - This clears the direct compile failures in those item/menu/block interaction files from the filtered compile.

## Next Work Plan

1. Port the recipe/input API layer: `IForestryRecipe`, `RecipeUtils`, and `FabricatorSmeltingRecipe` still assume pre-1.21 `Recipe<Container>` and old serializer/network helpers.
2. Port the broader ItemStack/NBT migration: `Product`, `ItemStackUtil`, `InventoryUtil`, `ItemInventory`, and other callers still rely on removed `getTag` / `setTag` / `save` helpers.
3. Port the shared stream/NBT/block-entity interfaces: many `IStreamable`, `INbtReadable`, and `INbtWritable` implementors now need the new `RegistryFriendlyByteBuf` and lookup-provider signatures.

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
- Current verification command for the KubeJS compat cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 2 "ForestryKubeJsPlugin|ForestryClientEventJS|GeneticsEventJS|ApicultureEventJS|KubeJSPlugin|BindingsEvent|EventJS|error:"`
- Compile family reduced by the latest slice:
  - cleared the direct KubeJS compat API failures by moving `ForestryKubeJsPlugin` onto the 1.21.1 KubeJS plugin interface (`dev.latvian.mods.kubejs.plugin.KubeJSPlugin`), switching binding registration to `BindingRegistry`, registering event groups through `EventGroupRegistry`, and replacing Forestry’s event payload base class from `EventJS` to `KubeEvent`
- Current verification command for the worktable recipe cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 2 "WorktableTile|WorktableSlot|RecipeUtil|RecipeHolder|RecipeCraftingHolder|error:"`
- Compile family reduced by the latest slice:
  - cleared the active worktable recipe migration fallout by switching `WorktableTile` from the removed `RecipeUtil` references to `RecipeUtils` and updating `WorktableSlot` from the old inventory-side `RecipeHolder` import to `RecipeCraftingHolder`
- Current next blocker after the worktable recipe cleanup slice:
  - compile failures are now front-loaded by loot function serializer rewrites (`OrganismFunction`, `CountBlockFunction`), client/api moves like `TextureStitchEvent`, `PotionUtils`, `ForgeEventFactory`, `ForgeMod`, and broader Mojang-side 1.21 item/NBT/stream signature changes
- Current verification command for the item/mob effect/client signature cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 2 "ItemElectronTube|ItemPipette|PotionBeeEffect|PotionBeeEffectExclusive|AscensionBeeEffect|GuardianBeeEffect|IgnitionBeeEffect|DefaultForestryPlugin|EventHandlerCore|BlockForestryFluid|ModUtil|DefaultForestryClientRegistration|EntityUtil|error:"`
- Current verification command for the item/menu/block interaction signature cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 2 "ItemWithGui|ItemBackpack|ItemBackpackNaturalist|BackpackResupplyHandler|BeekeepingLogic|ItemFluidContainerForestry|ItemSpectacles|ItemCircuitBoard|ItemBlockFarm|ItemLetter|ItemGrafter|ItemFruit|ContainerSocketedHelper|BlockBase|BlockStructure|BlockGeneticFilter|error:"`
- Compile family reduced by the latest slice:
  - cleared the direct removed `NetworkHooks`, old block `use(...)`, tooltip signature, `getUseDuration(...)`, event-bus boolean-return, and `hurtAndBreak(...)` callback failures in the active Forestry item/menu/block interaction files
- Current next blocker after the item/menu/block interaction signature cleanup slice:
  - compile failures are now led by `ItemPipette` fluid-handler storage migration, `ResourceLocation` constructor removals, painting/worldgen structure API changes, packet/item-stack codec rewrites, and broader client/model signature changes
- Compile family reduced by the latest slice:
  - cleared the direct tooltip-signature, holder-based mob effect, `ResourceLocation` constructor, `LiquidBlock`, and `EntityUtil#finalizeSpawn(...)` failures from the filtered compile
- Current verification command for the compat/client resource lookup cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 2 "FluidComponent|BeeAnalyzerPlugin|ResourceUtil|FluidMap|JsonUtil|ForgeRegistries|error:"`
- Compile family reduced by the latest slice:
  - cleared the direct `FluidComponent`, `BeeAnalyzerPlugin`, `ResourceUtil`, `FluidMap`, `JsonUtil`, and `compat/jei/package-info.java` failures from the filtered compile
- Current next blocker after the compat/client resource lookup cleanup slice:
  - compile failures are now front-loaded by the 1.21 recipe/input migration (`RecipeUtils`, `IForestryRecipe`, `FabricatorSmeltingRecipe`), the cross-cutting ItemStack/NBT migration (`Product`, `ItemStackUtil`, `InventoryUtil`, `ItemInventory`), and the shared stream/NBT interface signature rewrite (`RegistryFriendlyByteBuf`, lookup-provider NBT methods)
- Current verification command for the recipe base-interface and helper cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 2 "IForestryRecipe|RecipeUtils|FabricatorSmeltingRecipe|HygroregulatorRecipe|FabricatorRecipe|MoistenerRecipe|StillRecipe|SqueezerRecipe|SqueezerContainerRecipe|FermenterRecipe|CarpenterRecipe|CentrifugeRecipe|FluidRecipeFilter|error:"`
- Compile family reduced by the latest slice:
  - cleared the direct `IForestryRecipe`, `RecipeUtils`, `FluidRecipeFilter`, and Forestry machine recipe `getResultItem(...)` / fabricator melting lookup failures from the filtered compile
- Current next blocker after the recipe base-interface and helper cleanup slice:
  - compile failures are now front-loaded by the cross-cutting ItemStack/NBT migration (`Product`, `ItemStackUtil`, `InventoryUtil`, `ItemInventory`) plus the shared stream/NBT interface rewrite (`RegistryFriendlyByteBuf`, lookup-provider NBT methods)
- Current verification command for the ItemStack/NBT migration slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 2 "forestry/api/core/Product|ItemStackUtil|InventoryUtil|ItemInventory|error:"`
- Compile family reduced by the latest slice:
  - cleared the direct `Product`, `ItemStackUtil`, `InventoryUtil`, and `ItemInventory` failures by moving product/item-stack tag handling onto item components, swapping legacy `FriendlyByteBuf` registry ID calls to `writeById` / `readById`, and serializing stored stacks through `ItemStack.CODEC`
- Current next blocker after the ItemStack/NBT migration slice:
  - compile failures are now front-loaded by the shared Forestry lookup-aware NBT and stream interface rewrite (`INbtReadable`, `INbtWritable`, `IStreamable`) across `TileTreeContainer`, `TileForestry`, `InventoryAdapter`, `InventoryPlain`, `NBTUtilForestry`, and related tile/helper classes, with broader datagen and client API migrations still behind that front edge
- Shared stream/NBT interface migration:
  - Moved Forestry block-entity and helper sync code onto NeoForge 1.21.1 `RegistryFriendlyByteBuf` and lookup-aware NBT signatures.
  - `NBTUtilForestry` now serializes streamables with registry-aware buffers, and `NetworkUtil` regained the tracking-packet helper used by Forestry tile/client update packets.
  - Updated the active arboriculture/core/farming/energy/mail sync classes off old `FriendlyByteBuf` / `load(...)` / `saveAdditional(...)` assumptions, including `TileTreeContainer`, `TileFruitPod`, `TileLeaves`, `TileForestry`, `TilePowered`, `TilePlanter`, `InventoryAdapter`, `InventoryPlain`, `OwnerHandler`, `TankManager`, `StandardTank`, `ForestryEnergyStorage`, `FarmManager`, `FarmHydrationManager`, `FarmFertilizerManager`, `PacketTileStream`, `TileAnalyzer`, `TileEscritoire`, `EscritoireGame*`, `TileMill`, `EngineBlockEntity`, and `TileTrader`.
  - This clears the shared stream/NBT signature family from the front of the compile output so recipe/tag/datagen API migration is leading again.
- Current verification command for the shared stream/NBT interface migration slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 1 "TileTreeContainer|TileFruitPod|TileLeaves|TileForestry|TilePowered|TilePlanter|NBTUtilForestry|InventoryAdapter|InventoryPlain|OwnerHandler|TankManager|StandardTank|ForestryEnergyStorage|FarmManager|FarmHydrationManager|FarmFertilizerManager|PacketTileStream|TileAnalyzer|TileEscritoire|EscritoireGame|EscritoireGameBoard|EscritoireGameToken|TileMill|EngineBlockEntity|TileTrader|error:"`
- Compile family reduced by the latest slice:
  - cleared the direct lookup-aware NBT / `RegistryFriendlyByteBuf` migration failures across the shared Forestry sync helpers and the active tile/helper classes touched above
- Current next blocker after the shared stream/NBT interface migration slice:
  - compile failures are now front-loaded by recipe and tag/datagen API changes (`ForestryRecipeProvider`, `ForestryBackpackTagProvider`, `ForestryItemTagsProvider`, `LootTableHelper`), with JEI subtype handling and a smaller set of remaining client/gui API migrations behind them
- Datagen and baseline signature cleanup follow-up:
  - Moved `ToolTier` onto the 1.21 `Tier#getIncorrectBlocksForDrops()` contract.
  - Fixed `ConditionLootModifier` and the loot helper/provider wiring for the 1.21 loot modifier and loot-table key APIs, including the new `MapCodec` return type and lookup-aware `GlobalLootModifierProvider` constructor.
  - Updated the shared saved-data / NBT base classes touched in this slice (`BreedingTracker`, `MultiblockLogic`, `MultiblockControllerForestry`, `FakeMultiblockController`, `FakeInventoryAdapter`) to the lookup-aware `write(..., HolderLookup.Provider)` / `read(..., HolderLookup.Provider)` signatures.
  - Ported the now-unused-but-still-compiled `BeeParticleType` class to the 1.21 particle `MapCodec` / `StreamCodec` contract.
  - Fixed the active datagen helper drift in `ForestryBackpackTagProvider`, `ForestryItemTagsProvider`, `ForestryBlockTagsProvider`, `ForestryBlockStateProvider`, `ForestryItemModelProvider`, and `FilledCrateModelBuilder` by switching moved NeoForge tag constants, `ResourceLocation` factory helpers, and current feature collection APIs.
  - Updated `VillagerTrade` to 1.21 `MerchantOffer` / `ItemCost` construction.
- Current verification command for the datagen and baseline signature cleanup follow-up slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 1 "ToolTier|ConditionLootModifier|LootTableHelper|ForestryLootModifierProvider|BreedingTracker|MultiblockLogic|MultiblockControllerForestry|FakeMultiblockController|FakeInventoryAdapter|BeeParticleType|ForestryBackpackTagProvider|ForestryItemTagsProvider|ForestryBlockTagsProvider|ForestryBlockStateProvider|ForestryItemModelProvider|FilledCrateModelBuilder|VillagerTrade|error:"`
- Recipe/datagen follow-up cleanup:
  - `JeiUtil` now imports the Forestry individual-item capability type again so the JEI subtype registration path compiles on 1.21.1.
  - `ForestryRecipeProvider` was updated for the current NeoForge tag constants (`STONES`, `STRINGS`, `LEATHERS`, `GLASS_BLOCKS_COLORLESS`, `SANDS`, etc.), switched off removed `Ingredient.merge(...)` onto `CompoundIngredient.of(...)`, and dropped the already-marked-for-removal volcanic propolis back-compat recipe now that `EnumPropolis.VOLCANIC` no longer exists in 1.21.1.
  - The butterfly mating special recipe path now targets the 1.21 crafting API: `ButterflyMatingRecipe` uses `CraftingInput` plus lookup-aware `assemble(...)`, and the datagen/serializer factories now construct it from `CraftingBookCategory`.
  - `CentrifugeRecipeBuilder` now reads recipe product NBT from `DataComponents.CUSTOM_DATA` instead of removed `ItemStack#getTag()`.
- Current verification command for the recipe/datagen follow-up cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 1 "JeiUtil|ForestryRecipeProvider|ButterflyMatingRecipe|LepidopterologyRecipes|CentrifugeRecipeBuilder|error:"`
- Compile family reduced by the latest slice:
  - cleared the active `ForestryRecipeProvider`, `JeiUtil`, butterfly special-recipe, and centrifuge builder compile failures so the front edge moved off recipe/datagen cleanup
- Genetics and GameProfile/NBT cleanup:
  - Added Forestry-side `NBTUtilForestry` helpers for `GameProfile` serialization and `ItemStack` custom-data access so the port no longer depends on removed Mojang `NbtUtils.readGameProfile(...)` / `writeGameProfile(...)` helpers or removed `ItemStack#getTag()` / `setTag()` accessors in the touched files.
  - `ItemResearchNote`, `AlleleUtil`, `Individual`, `ServerBreedingHandler`, and `SerializableIndividualHandlerItem` now target the 1.21 APIs for resource location parsing, saved-data factories, item custom data, and lookup-aware `INBTSerializable`.
  - Shared `GameProfile` NBT callers in `MailAddress`, `TradeStation`, and `MultiblockTileEntityForestry` now use the Forestry helper as well, so that migration is no longer blocked on the removed Mojang helpers.
- Current verification command for the genetics and GameProfile/NBT cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 1 "ItemResearchNote|AlleleUtil|ServerBreedingHandler|SerializableIndividualHandlerItem|MailAddress|TradeStation|MultiblockTileEntityForestry|NBTUtilForestry|error:"`
- Compile family reduced by the latest slice:
  - cleared the active genetics/GameProfile/NBT front-edge failures, including the remaining `ItemResearchNote`, `AlleleUtil`, breeding tracker saved-data, and removed Mojang GameProfile helper breakages
- Current next blocker after the genetics and GameProfile/NBT cleanup slice:
  - compile failures are now front-loaded by worldgen/core registration drift (`CoreFeatures`, `ForestryBiomeModifier`, `CoreBlocks`), older block/item override signatures (`BlockStructure`, `BlockBase`, `ItemFluidContainerForestry`), and the remaining lookup-aware block-entity save/load and stream method migrations (`TileAnalyzer`, `TileEscritoire`, `EngineBlockEntity`, related tile sync helpers)
- Compile family reduced by the latest slice:
  - cleared the direct abstract-method/signature failures in the touched tool tier, loot modifier, villager trade, particle type, and shared NBT base classes, and cleared the direct `ResourceLocation`/tag-helper/datagen-support errors in the touched provider/helper files
- Current next blocker after the datagen and baseline signature cleanup follow-up slice:
  - compile failures are now dominated by the large `ForestryRecipeProvider` tag/ingredient migration (`Tags.Items.*` renames, `Ingredient.merge(...)`, `special(...)`, and the removed `EnumPropolis.VOLCANIC` reference), with the next layer behind that still including JEI subtype handling, remaining ItemStack/NBT component migrations, and several client/model API updates
- Shared legacy NBT/network compatibility follow-up:
  - Added Forestry-side compatibility bridges so old one-arg `INbtReadable` / `INbtWritable` implementations and `FriendlyByteBuf` stream sync call sites still compile while delegating into the 1.21 lookup-aware / `RegistryFriendlyByteBuf` paths.
  - `TileForestry` and `MultiblockTileEntityBase` now provide compatibility overloads for legacy `load(...)`, `saveAdditional(...)`, `getUpdateTag()`, and `handleUpdateTag(...)` callers, which clears the remaining abstract/signature breakage from that migration layer.
  - `TileAnalyzer` and `TileEscritoire` now use lookup-aware save/load signatures and `ItemStack.STREAM_CODEC` instead of removed `RegistryFriendlyByteBuf#writeItem()` / `readItem()`.
  - `NetworkUtil` regained a generic `sendToServer(...)` helper and now has both `FriendlyByteBuf` and `RegistryFriendlyByteBuf` climate-state overloads, clearing the active GUI/climate sync fallout in the touched files.
  - Residual `ItemStack.save(...)` callers in `BeekeepingLogic`, `TileAlvearySwarmer`, `TileCentrifuge`, and `TileMoistener` now serialize through `ItemStack#saveOptional(...)`, so the old pre-1.21 NBT save signature is no longer part of the front edge.
- Current verification command for the shared legacy NBT/network compatibility follow-up slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 2 "INbtWritable|INbtReadable|IStreamable|TileForestry|MultiblockTileEntityBase|TileAnalyzer|TileEscritoire|sendToServer|writeClimateState|readClimateState|BeekeepingLogic|TileAlvearySwarmer|TileCentrifuge|TileMoistener|error:"`
- Compile family reduced by the latest slice:
  - cleared the remaining Forestry-local legacy NBT/network helper drift from the compile front, so the active failures have moved on to worldgen/core registration API changes, ItemStack component callers, recipe-holder adoption, and broader client/model porting
- Current next blocker after the shared legacy NBT/network compatibility follow-up slice:
  - compile failures are now front-loaded by worldgen/core registration drift (`CoreFeatures`, `ForestryBiomeModifier`, `CoreBlocks`), remaining ItemStack component migrations (`ItemInventory`, `ItemCircuitBoard`, `ItemAlyzer`), and the recipe-holder/client-model cleanup behind them
- Recipe holder and JEI fluid API cleanup:
  - `RecipeUtils` now unwraps `RecipeHolder<T>` at the Forestry helper boundary again, so machine tiles, Patchouli processors, and JEI registration consume raw Forestry recipe interfaces instead of mixing holder/value call sites.
  - `FakeCraftingInventory` now builds 1.21 `CraftingInput` instances instead of the removed transient `CraftingContainer` path, which clears the active `CarpenterRecipe` / `FabricatorRecipe` crafting-match breakage.
  - Forestry JEI machine categories now use the current `addFluidStack(fluid, amount)` overload, clearing the active fluid-slot API errors in carpenter, fabricator, fermenter, moistener, squeezer, and still recipe displays.
- Current verification command for the recipe holder and JEI fluid API cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n "CarpenterRecipe\\.java|FabricatorRecipe\\.java|CarpenterRecipeCategory\\.java|FabricatorRecipeCategory\\.java|FermenterRecipeCategory\\.java|MoistenerRecipeCategory\\.java|SqueezerRecipeCategory\\.java|StillRecipeCategory\\.java|FakeCraftingInventory\\.java|RecipeUtils\\.java|error:"`
- Compile family reduced by the latest slice:
  - cleared the direct `RecipeHolder`/`.value()` fallout in Forestry recipe helpers and machine callers, plus the current JEI fluid-slot overload and fake-crafting-input breakage
- Current next blocker after the recipe holder and JEI fluid API cleanup slice:
  - compile failures are now led by worldgen/core registration drift (`CoreFeatures`, `ForestryBiomeModifier`, `CoreBlocks`), the remaining factory recipe serializer `streamCodec()` rewrites, and broader 1.21 block/item/client signature changes
- ItemStack custom-data follow-up cleanup:
  - Ported the remaining direct Forestry `ItemStack#getTag()` / `setTag()` callers in the active compile path onto `NBTUtilForestry` custom-data helpers, including `ItemInventory`, `ItemAlyzer`, `CircuitManager`, `ItemCircuitBoard`, `BackpackDefinition`, butterfly cocoon age handling, mail letter serialization helpers/inventories, PO box / trade-station mail writes, and leaf item/model tree payload reads.
  - Fixed the writeback semantics for mutable item custom data in the touched files so copied `CUSTOM_DATA` payloads are explicitly written back to the owning `ItemStack` after mutation instead of silently editing detached tags.
  - This clears the remaining direct item-custom-data compile failures from `ItemInventory`, `ItemAlyzer`, and the related mail/arboriculture/lepidopterology helper classes that still assumed pre-1.21 root tag mutability.
- Current verification command for the ItemStack custom-data follow-up cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 2 "ItemInventory|ItemAlyzer|ItemCircuitBoard|CircuitManager|BackpackDefinition|ItemButterflyGE|BlockCocoon|ItemInventoryLetter|ItemLetter|LetterProperties|LetterUtils|POBox|TradeStation|ItemBlockLeaves|ModelLeaves|getTag\\(|setTag\\(|error:"`
- Compile family reduced by the latest slice:
  - cleared the direct `ItemStack#getTag()` / `setTag()` compile failures from the active Forestry item/mail/leaves/butterfly helpers, leaving only unrelated signature drift in a few of the touched files
- Current next blocker after the ItemStack custom-data follow-up cleanup slice:
  - compile failures are now front-loaded by worldgen and core registration drift (`CoreFeatures`, `ForestryBiomeModifier`, `CoreBlocks`), plus nearby 1.21 signature changes in `BlockStructure`, `BlockBase`, `ItemFluidContainerForestry`, and other item/gui/client override sites
- Arboriculture and cultivation block-constructor cleanup:
  - Updated Forestry arboriculture door, trapdoor, sign, wall sign, hanging sign, and wall hanging sign blocks to the 1.21.1 vanilla constructor order (`BlockSetType` / `WoodType` before `Properties`).
  - Updated `BlockForestryLeaves` to the 1.21.1 `BonemealableBlock#isValidBonemealTarget(LevelReader, BlockPos, BlockState)` signature.
  - Switched the affected Forestry block-item wrappers (`ItemBlockPlanter`, `ItemBlockLeaves`, `ItemBlockDecorativeLeaves`, `ItemBlockSign`, `ItemBlockHangingSign`) onto the feature registry's current `(block, Item.Properties)` constructor shape so the arboriculture/cultivation registration layer compiles again.
- Current verification command for the arboriculture and cultivation block-constructor cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 1 "BlockForestryDoor|BlockForestryTrapdoor|BlockForestryStandingSign|BlockForestryWallSign|BlockForestryHangingSign|BlockForestryWallHangingSign|BlockForestryLeaves|ArboricultureBlocks|CultivationBlocks|ItemBlockPlanter|ItemBlockLeaves|ItemBlockDecorativeLeaves|ItemBlockSign|ItemBlockHangingSign|error:"`
- Compile family reduced by the latest slice:
  - cleared the direct arboriculture/cultivation constructor-reference and `BonemealableBlock` signature failures, moving the compile front past these block/item registration classes
- Current next blocker after the arboriculture and cultivation block-constructor cleanup slice:
  - compile failures are now led by worldgen/core registration drift (`CoreFeatures`, `ForestryBiomeModifier`, `CoreBlocks`), plus broad remaining 1.21 item/gui/client/model signature changes and villager trade `ItemCost` migrations
- Core worldgen and block registration cleanup:
  - `CoreFeatures` and `ForestryBiomeModifier` now use NeoForge 1.21.1's `MapCodec`-backed biome modifier serializer registration instead of the old `Codec`-backed shape.
  - `ForestryBiomeModifier` now builds its serializer with `RecordCodecBuilder.mapCodec(...)` and returns `MapCodec<? extends BiomeModifier>` from `codec()`.
  - `CoreBlocks` now routes block-group enum arrays through `List.of(...)` so they match the current feature-registry `Collection<S>` overload, and ore/storage block property copies now use `BlockBehaviour.Properties.ofFullCopy(...)`.
  - The apatite ore registrations were also updated to the 1.21.1 `DropExperienceBlock(IntProvider, Properties)` constructor order.
- Current verification command for the core worldgen and block registration cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 1 "CoreFeatures|ForestryBiomeModifier|CoreBlocks|error:"`
- Compile family reduced by the latest slice:
  - cleared the active `CoreFeatures`, `ForestryBiomeModifier`, and `CoreBlocks` front-edge compile failures, moving the compile front deeper into remaining core block/item signature drift
- Current next blocker after the core worldgen and block registration cleanup slice:
  - compile failures are now front-loaded by older core block/item override and constructor migrations (`BlockStructure`, `BlockBase`, `ItemFluidContainerForestry`, `CorePaintings`, `EscritoireGameToken`), with broader client/model and serializer rewrites still behind them
- Block-entity HolderLookup serialization cleanup:
  - Ported `MultiblockTileEntityForestry`, `TileBeeHousingBase`, `TileHive`, `TileTreeContainer`, `TileSapling`, `TileCocoon`, and `TileTrader` onto the 1.21.1 `loadAdditional(...)` / `saveAdditional(...)` / update-tag hooks that take `HolderLookup.Provider`.
  - Added compatibility bridges where Forestry still manually calls `load(tag)` or `saveAdditional(tag)` outside vanilla block-entity deserialization, so item-driven leaf/cocoon/tree reads still compile while the rest of the port catches up.
  - Updated `MultiblockTileEntityBase` and `TileHive` to the current block-entity packet callback signature that carries the lookup provider.
- Current verification command for the block-entity HolderLookup serialization cleanup slice:
  - `./gradlew compileJava --console=plain 2>&1 | rg -n -C 2 "MultiblockTileEntityBase|MultiblockTileEntityForestry|TileTreeContainer|TileSapling|TileCocoon|TileTrader|TileBeeHousingBase|TileHive|ItemBlockLeaves|error:"`
- Compile family reduced by the latest slice:
  - cleared the direct stale `super.load(...)`, old update-tag/on-data-packet overrides, and missing tree/cocoon compatibility-hook failures from the active block-entity serialization layer
- Current next blocker after the block-entity HolderLookup serialization cleanup slice:
  - compile failures are now front-loaded by remaining 1.21 block/item/core signature drift (`BlockStructure`, `BlockBase`, `ItemFluidContainerForestry`, `CorePaintings`, `EscritoireGameToken`) plus packet/mail buffer rewrites (`PacketItemStackDisplay`, trader mail packets, recipe transfer packets)

## Content port: 1.20.1 engine/solar additions (2026-08-11)

This round is not an API-porting slice. The build already compiles; this brings across the
content 1.20.1 gained from EnderiumSmith's solar work and Spearkiller's PR #361, which 1.21.1
had never received. Branch: `port/engine-and-solar-additions`.

### Ported

- Solar Engine and Solar Panel, with the merged energy formula from #361 (array size bonus of
  `0.03 * (panels - 1)^2` FE/t, floating point insolation) and the 20 tick array sweep that
  recounts lit panels.
- Liquid Experience: the fluid, its `forge:experience` tag, the squeezer recipe, and the
  squeezer's glass bottle path (experience, honey and water bottles).
- Phosphor Torch, Phosphor Wall Torch, Phosphor Lantern, Tin Chain, Tin Nugget.
- Engine ledger showing fractional RF/t.
- Solar engine error conditions, engine retextures, and 24 new textures.
- GameTests for squeezer bottling and solar panel shading.

### Verification for this round

- `./gradlew compileJava compileTestJava --console=plain` — clean
- `./gradlew runData` — 33 files written, 4 stale removed. NOTE: runData does not exit on its
  own here; it finishes its work in ~30s, so watch `run/logs/latest.log` for
  `Caching: total files` and then kill it.
- `./gradlew runGameTestServer --console=plain` — all 109 pass

### Naming and API deviations worth remembering

- The engine registers as `solar_engine`, not 1.20.1's `engine_solar`: `EnergyBlocks` uses a
  SUFFIX identifier. Assets and lang keys follow the new name.
- `blockstates/solar_engine.json` must NOT be hand-written. `ForestryBlockStateProvider` loops
  the engine blocks, so datagen emits it; a hand-written copy is a duplicate-resource collision.
  `models/block/solar_engine.json` IS hand-written, same split as `peat_engine`.
- `BlockBehaviour.Properties.ofFullCopy` copies `drops`, which `Properties.copy` did not.
  Copying a vanilla block built with `.dropsLike(...)` therefore steals its loot table id — this
  bit the phosphor wall torch, which copies `SOUL_TORCH` rather than `SOUL_WALL_TORCH` as a
  result. Check for `dropsLike` before using `ofFullCopy` on a vanilla block.
- 1.21.1's `TorchBlock` takes a `SimpleParticleType` first, so a `DustParticleOptions` cannot be
  passed to super. The phosphor torches hand super a vanilla flame for the codec and spawn their
  own dust from `animateTick`.
- Item and block tags are `c:`; only fluid tags are still `forge:`.
- `PotionUtils` is gone; build potions with `PotionContents.createItemStack`, and compare them
  with `ItemStack.isSameItemSameComponents` since every potion is `Items.POTION`.

### Silicon and Solar Cell round (2026-08-11)

Silicon and the Solar Cell followed in a second pass, which also restored the Solar Engine's
crafting recipe. The engine registered a block, loot table, blockstate, model and lang key but
had no recipe at all, so it was uncraftable in survival.

Deviations for this pass:

- Ids are `silicon_block` and `silicon_electron_tube`, following this tree's naming rather than
  1.20.1's `resource_storage_silicon` and `electron_tube_silicon`.
- 1.21.1 rewrote electron tubes onto `ItemOverlay`, so every tube is a tint over the shared
  `item/thermionic_tubes.0/.1` pair. Silicon therefore needs a color, not a texture. The two
  colors are sampled from 1.20.1's `electron_tube_silicon.png`, avoiding a collision with
  Apatite's `0x579CD9`. The old texture is left in place but is now unused.
- Silicon joins the `c:storage_blocks` aggregate tag, which 1.20.1 leaves it out of.
- The engine recipe uses `Tags.Items.GLASS_BLOCKS_COLORLESS`, matching the other three engines.

**Silicon has no production recipe.** Its only route on 1.20.1 is the smelter, so the storage
block round trip and both fabricator recipes only move it around. A `todo` in
`ForestryRecipeProvider` marks the gap.
## Content parity round (2026-08-12)

Everything the 2026-08-11 gap audit found is now ported, in eight slices. The audit section it
replaces is deleted rather than kept, since almost every line of it is now wrong.

One correction to that audit worth recording: it read the factory roster off
`BlockTypeFactoryPlain` and concluded most machines were missing. They were not. This tree moved
them to `BlockTypeFactoryTesr`, and the Smelter was the only machine actually absent.

### What landed

| Slice | Contents |
| --- | --- |
| Small gaps | 4 painting variants, the iron gear, the proven scoop with its beekeeper trade, the ash block |
| Wax | The Wax fluid and bucket, the wax and refractory wax blocks, the three brick items, the two fabricator smelting recipes that melt wax |
| Smelter | The last unported machine, and with it a production route for silicon |
| Burn Barrel | Block, tile, menu, screen, inventory, blacklist tag, recipe |
| Combustion Engine | The engine, plus the whole engine circuit socket and its five upgrade circuits |
| Brewer backpack | The tenth backpack type |
| Decorative blocks | 135 blocks: the stone and brick families, 22 metal platings, 38 candles, turf, plywood, cork |
| Advancements | The trigger layer, and all 57 advancements |

### Bugs in 1.20.1 corrected rather than reproduced

Each carries a `// Deviation from 1.20.1:` comment where it lives.

- `InventorySmelter#removeResources` stopped one slot short of the last input slot, and its
  partial-slot branch recorded the shortfall rather than what the slot held. The second let the
  smelter pay a cost it could not afford, duplicating items. Pinned by GameTests that were
  confirmed to fail against the original.
- `InventoryBurnBarrel#setItem` dereferenced a level that is null during chunk load, so a barrel
  saved with ash threw on load. It also counted ash before writing the slot, so `HAS_ASH` stuck
  on after the last ash was removed.
- Bronze metal plating named the tin plating as its result, so bronze ingots made tin plating and
  bronze plating was uncraftable.
- All 16 jumbo candle dye recipes took the big candle tag as their base.
- Every cobbled stone family cut its stairs, slab and wall from the set's plain stone, so nine
  crafting recipes had the same pattern and ingredient as their non-cobbled twin and only one of
  each pair could fire.
- `chiseled_refractory_wax_bricks` was `COLOR_YELLOW` amid a `COLOR_RED` family.
- The root advancement's criterion required all seventeen combs at once, so it could not be
  earned, and its reward called the `grant_guide` loot table as if it were a function.
- Three lang typos: "Red Metal Lacquered Plating", "Big Green Cale", and trailing spaces.

### Defects found in this tree, not inherited

- The Solar Engine had no block entity renderer and the Solar Panel no cutout render layer, both
  from the earlier solar round.
- The Solar Engine and the Solar Panel had no crafting recipes.
- `ash_block` registered through an overload that creates no `BlockItem`, which is why it had no
  recipe and no creative tab entry.
- `ForestryItemModelProvider` tested `path.endsWith("woven")`, which the id rename broke, so all
  six woven backpacks generated with the plain model.
- Twelve `item.forestry.*_bag` display names no longer matched any id.
- The dye and map colour lookups for metal plating were static `HashMap`s, whose iteration order
  made recipe emission nondeterministic between datagen runs.

### Deliberately not done

- **The biogas engine socket rework.** 1.20.1's biogas engine is a socketed variant with a
  different burn model. Porting it would replace behaviour already ported here, so the biogas
  overclock circuit does not attach to it yet. The other four engine circuits work.
- **Ashen wax and crispy honey blocks** stay out of the creative tab, as on 1.20.1, where their
  smelting recipes are commented out too.
- **`get_smelter`** and the other advancements 1.20.1 leaves commented out. `get_smelter` is
  newly unblocked now the machine exists, if anyone wants it.

### Known pre-existing issues left alone

- `MKRecipeProvider.grid2x2` silently drops its `resultCount`, so several recipes that mean to
  yield 4 emit 1. It is a ModKit fix, and 1.20.1 has the same output.
- `registerFabricator` writes the Flexible Casing recipe under
  `fabricator/electron_tubes/flexible_casing`.
- Amber has no decomposition recipe, so amber blocks are a one-way trip.
- The hand-written lang file still carries pre-rename fluid keys (`bucket_glass`, the dotted
  `block.forestry.fluid.*` forms), so those read as datagen's auto-names in game.
- `c:dusts/ash` lists `forestry:ash` twice.

### Verification for this round

Every slice was verified before commit: `compileJava` across all four source sets, `runData` run
twice to confirm idempotence, and `runGameTestServer`. The suite is **113 tests, all passing**,
up from 109 (the four new ones cover the smelter fix). The creative tab baseline was regenerated
per slice and its diff checked to be additions only.

## Texture parity sweep (2026-08-12)

The content port was complete but the art was not. 1.20.1 ran fifteen-odd retexture passes that
1.21.1 never received, so blocks that were functionally correct still wore old sprites. This round
brings across every texture where this tree held a strictly older revision.

### How the gap was found

Compare every PNG in `ForestryCE-1.20.1/src/main/resources` against the union of this tree's four
resource roots, then trace each differing file's blob back through 1.20.1's history. A file whose
current bytes match some *ancestor* revision is stale; one that matches nothing is our own art.

**All 128 differing files resolved to an older 1.20.1 revision. None were port-original.** So there
was no judgement to make: every difference was a missed update.

Two traps worth remembering, both of which faked a clean result:

- **Compare against all four resource roots, not `src/main`.** 1.20.1 keeps everything in one
  `src/main/resources`; here `farms`, `mail` and `butterflies` own theirs. A `src/main`-only diff
  reports 26 farms and mail textures as missing when they are simply in another jar. See
  `docs` note in the per-jar resources work for the same trap on the generated side.
- **`git log --raw` marks renames `R100` and puts the new path in the *last* tab-separated field.**
  Splitting on the first tab keys the entry under `oldpath\tnewpath`, so every file that was ever
  renamed silently misses the blob lookup and reads as port-original. That mis-classified 48 of
  the 128, the engine bodies among them.

### What landed

127 textures, all verified byte-identical to 1.20.1 afterwards. 101 core, 14 mail, 12 farms.

| Group | Contents |
| --- | --- |
| Engines | The 5 heat trunks shared by every engine, the bronze and copper bodies, the biogas and peat engine GUIs. Clockwork, combustion and solar were already current, which is why the problem looked partial |
| Mail | Mailbox, philatelist, trade station, stamps |
| GUI atlas | Analyzer icons, error icons, slot icons, `mfarm`, `electricalengine`, the two socket GUIs |
| Farms | Arboretum, the five farm types and their particles, peat bog |
| Machines | Analyzer, the full rainmaker set, worktable |
| Apiculture | Apiary, bee house, the three naturalist chests |
| Items | Containers and capsules, tool kits, peat, ash, scoop, soldering iron, a painting |

### Held back

`block/escritoire.png` alone. 1.20.1 is 64x64 against this tree's 64x32, because that tree
converted the escritoire to a JSON block model - its `RenderEscritoire` has the BER texture line
commented out and no `createBodyLayer`. This tree still renders a hand-built `ModelPart` mesh whose
`texOffs` address a 64x32 sheet, so the new art would scramble it. Porting it is a model change,
not an asset swap.

### Verification for this round

- Dimensions compared per file before copying; only the escritoire differed, and it was held back
- No `.mcmeta` companions exist on either side, so no animation metadata to keep in sync
- All 127 pass PNG signature, IEND and per-chunk CRC validation
- Every change overwrote a file already in place, so no jar-routing mistakes: `git status` shows
  127 modifications and zero additions
- 1609 of the 1610 shared textures are now byte-identical to 1.20.1, up from 1482

## Bog earth and humus progression (2026-08-12)

Both blocks age through their `randomTick` and both carry the property that records it, but the
generated blockstate ignored the property and named one model, so every stage looked identical.
1.20.1 shows three stages of bog earth and three of humus. This round makes the models follow the
state.

The Java side was already correct and is untouched. `simpleBlock` was the whole bug.

### What landed

- `ForestryBlockStateProvider.agingSoil(Block, IntegerProperty)` reads the property's possible
  values and writes one `cube_all` model per value. Property-driven, so changing `MAX_MATURITY` or
  `MAX_DEGRADE` needs no datagen edit.
- Age 0 keeps the bare model name, `block/bog_earth` and `block/humus`, so the hand-written item
  models still parent it. Later ages get `_1` and `_2`.
- Four stage textures from 1.20.1's `481c2a760`, renamed off its dotted form to `bog_earth_1`,
  `bog_earth_2`, `humus_1`, `humus_2`.
- `block/humus.png` replaced. It was the *pre-progression* humus art, a chunky orange that looks
  nothing like the grey stages either side of it, and the rename from `humus.0.png` had hidden it
  from the texture sweep above. This also changes the humus item icon, which parents the block.

### The state ranges are right as they stand

This tree declares `maturity` and `degrade` as 0..2 where 1.20.1 declares 0..3, which looks like a
lost stage but is not. 1.20.1's `degradeSoil` converts to sand the moment `degrade` would reach 3,
and its bog earth converts to peat at `maturity < maturityDelimiter - 1`, so the top value of each
is unreachable and `humus.3.png` is dead art there. The three states here are exactly the three
1.20.1 displays.

`humus.3.png` is still available if a sandier final stage before the conversion is ever wanted.

### Deviation from 1.20.1

That tree's blockstates list each model four times, once per y rotation. A `cube_all` model wears
one texture on all six faces, so only the top face ever shows the rotation. One variant per age is
written instead, matching the choice already made for the turf blocks.

### Verification for this round

- All six textures confirmed pixel-identical to their 1.20.1 sources
- `compileJava` clean
- `runData` wrote 6 files; a second run wrote 0 and removed 0 stale, so it is idempotent
- `runGameTestServer` 113 passing
- No hand-written blockstate or block model exists for either block, so no duplicate-resource
  collision of the kind the solar engine hit
