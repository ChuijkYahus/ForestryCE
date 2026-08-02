package forestry.core.content.energy.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.item.ItemBlockTesr;
import forestry.core.content.energy.blocks.EngineBlock;
import forestry.core.content.energy.blocks.EngineBlockType;
import forestry.core.platform.registration.FeatureBlockGroup;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

import java.util.List;

@FeatureProvider
public class EnergyBlocks {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.ENERGY);

	public static final FeatureBlockGroup<EngineBlock, EngineBlockType> ENGINES = REGISTRY.blockGroup(EngineBlock::new, List.of(EngineBlockType.VALUES)).item(ItemBlockTesr::new).identifier("engine", forestry.core.platform.registration.FeatureGroup.IdentifierType.SUFFIX).create();
}
