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
3. Keep commits scoped by subsystem, not by file type.
   Good commit examples:
   - `port item capability registration to RegisterCapabilitiesEvent`
   - `port block entity fluid and energy capabilities`
   - `port recipe builders off FinishedRecipe`
4. Before touching a new slice, run a filtered compile grep against the target files so the next session can see whether the slice is still the active blocker.
5. Prefer JMCP-backed notes over memory.
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

## Next Work Plan

1. Port recipe/data generation APIs.
2. Port remaining JEI NeoForge integration off `mezz.jei.api.forge.ForgeTypes`.
3. Sweep remaining tick-event package moves in:
   - complete for the first remaining users; next likely tick-related blockers are now outside this short list

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
