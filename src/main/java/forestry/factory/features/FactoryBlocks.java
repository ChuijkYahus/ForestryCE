package forestry.factory.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.item.ItemBlockForestry;
import forestry.core.platform.item.ItemBlockTesr;
import forestry.factory.blocks.BlockFactoryPlain;
import forestry.factory.blocks.BlockFactoryTESR;
import forestry.factory.blocks.BlockTypeFactoryPlain;
import forestry.factory.blocks.BlockTypeFactoryTesr;
import forestry.core.platform.registration.FeatureBlockGroup;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

import java.util.List;

@FeatureProvider
public class FactoryBlocks {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.FACTORY);

	public static final FeatureBlockGroup<BlockFactoryTESR, BlockTypeFactoryTesr> TESR = REGISTRY.blockGroup(BlockFactoryTESR::new, List.of(BlockTypeFactoryTesr.values())).item(ItemBlockTesr::new).create();
	public static final FeatureBlockGroup<BlockFactoryPlain, BlockTypeFactoryPlain> PLAIN = REGISTRY.blockGroup(BlockFactoryPlain::new, List.of(BlockTypeFactoryPlain.values())).item(ItemBlockForestry::new).create();
}
