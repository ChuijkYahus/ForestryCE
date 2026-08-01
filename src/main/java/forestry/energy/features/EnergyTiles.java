package forestry.energy.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.energy.blocks.EngineBlockType;
import forestry.energy.tiles.*;
import forestry.modules.features.FeatureProvider;
import forestry.modules.features.FeatureTileType;
import forestry.modules.features.IFeatureRegistry;
import forestry.modules.features.ModFeatureRegistry;

@FeatureProvider
public class EnergyTiles {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.ENERGY);

	public static final FeatureTileType<BiogasEngineBlockEntity> BIOGAS_ENGINE = REGISTRY.tile(BiogasEngineBlockEntity::new, "biogas_engine", () -> EnergyBlocks.ENGINES.get(EngineBlockType.BIOGAS).collect());
	public static final FeatureTileType<ClockworkEngineBlockEntity> CLOCKWORK_ENGINE = REGISTRY.tile(ClockworkEngineBlockEntity::new, "clockwork_engine", () -> EnergyBlocks.ENGINES.get(EngineBlockType.CLOCKWORK).collect());
	public static final FeatureTileType<PeatEngineBlockEntity> PEAT_ENGINE = REGISTRY.tile(PeatEngineBlockEntity::new, "peat_engine", () -> EnergyBlocks.ENGINES.get(EngineBlockType.PEAT).collect());
	public static final FeatureTileType<CombustionEngineBlockEntity> COMBUSTION_ENGINE = REGISTRY.tile(CombustionEngineBlockEntity::new, "combustion_engine", () -> EnergyBlocks.ENGINES.get(EngineBlockType.COMBUSTION).collect());
	public static final FeatureTileType<SolarEngineBlockEntity> SOLAR_ENGINE = REGISTRY.tile(SolarEngineBlockEntity::new, "solar_engine", () -> EnergyBlocks.ENGINES.get(EngineBlockType.SOLAR).collect());
}
