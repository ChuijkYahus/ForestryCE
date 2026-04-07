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

## Next Work Plan

1. Finish the remaining compile errors in the item-capability slice that were exposed once the old capability package was removed:
   - `src/main/java/forestry/core/tiles/TileForestry.java`
   - `src/main/java/forestry/cultivation/tiles/TilePlanter.java`
   - `src/main/java/forestry/core/tiles/TilePowered.java`
2. Replace remaining Forge registry helper usage:
   - `src/main/java/forestry/core/utils/ModUtil.java`
   - `src/main/java/forestry/core/utils/datastructures/FluidMap.java`
3. Port recipe/data generation APIs.
4. Port remaining JEI NeoForge integration off `mezz.jei.api.forge.ForgeTypes`.

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
- Compile family reduced by the latest slice:
  - cleared the direct `ItemArmorApiarist`, `ModuleApiculture`, `ModuleCore`, and `JeiUtil` errors from the filtered compile
- Current next blocker after the item-capability-adjacent cleanup:
  - `src/main/java/forestry/core/utils/ModUtil.java` still imports removed `ForgeRegistries` / `ObjectHolderRegistry`
