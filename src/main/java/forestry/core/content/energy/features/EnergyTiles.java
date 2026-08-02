package forestry.core.content.energy.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.content.energy.blocks.EngineBlockType;
import forestry.core.content.energy.tiles.BiogasEngineBlockEntity;
import forestry.core.content.energy.tiles.ClockworkEngineBlockEntity;
import forestry.core.content.energy.tiles.PeatEngineBlockEntity;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.FeatureTileType;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

@FeatureProvider
public class EnergyTiles {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.ENERGY);

	public static final FeatureTileType<BiogasEngineBlockEntity> BIOGAS_ENGINE = REGISTRY.tile(BiogasEngineBlockEntity::new, "biogas_engine", () -> EnergyBlocks.ENGINES.get(EngineBlockType.BIOGAS).collect());
	public static final FeatureTileType<ClockworkEngineBlockEntity> CLOCKWORK_ENGINE = REGISTRY.tile(ClockworkEngineBlockEntity::new, "clockwork_engine", () -> EnergyBlocks.ENGINES.get(EngineBlockType.CLOCKWORK).collect());
	public static final FeatureTileType<PeatEngineBlockEntity> PEAT_ENGINE = REGISTRY.tile(PeatEngineBlockEntity::new, "peat_engine", () -> EnergyBlocks.ENGINES.get(EngineBlockType.PEAT).collect());
}
