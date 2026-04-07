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

## Next Work Plan

1. Port the shared feature-registration layer under `src/main/java/forestry/modules/features/`.
2. Port capability usage patterns across block entities and items.
3. Port recipe/data generation APIs.
4. Port JEI integration against the NeoForge 1.21.1 API.

## Session Notes

- JMCP session configured with:
  - project: `/home/kali/Desktop/jmcp-testing/ForestryCE`
  - old source tree: `/home/kali/Desktop/jmcp-testing/ModKit`
  - source version: `1.20.1`
  - target version: `21.1.213`
- Target NeoForge sources are available in JMCP under version key `21.1.213`.
- Old source registration still needs the correct JMCP-resolvable 1.20.1 coordinate if class diffs are needed across versions.
