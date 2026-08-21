package forestry.arboriculture.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.arboriculture.charcoal.BlockAsh;
import forestry.arboriculture.charcoal.BlockCharcoal;
import forestry.arboriculture.charcoal.DecorativeLogPileBlock;
import forestry.arboriculture.charcoal.LogPileBlock;
import forestry.core.platform.item.ItemBlockForestry;
import forestry.core.platform.registration.FeatureBlock;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;

@FeatureProvider
public class CharcoalBlocks {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.ARBORICULTURE);

	public static final FeatureBlock<BlockCharcoal, ItemBlockForestry<BlockCharcoal>> CHARCOAL = REGISTRY.block(BlockCharcoal::new, BlockBehaviour.Properties::of, ItemBlockForestry::new, Item.Properties::new, "charcoal_block");
	public static final FeatureBlock<LogPileBlock, BlockItem> LOG_PILE = REGISTRY.block(LogPileBlock::new, BlockBehaviour.Properties::of, ItemBlockForestry::new, Item.Properties::new, "log_pile");
	public static final FeatureBlock<DecorativeLogPileBlock, BlockItem> DECORATIVE_LOG_PILE = REGISTRY.block(DecorativeLogPileBlock::new, BlockBehaviour.Properties::of, ItemBlockForestry::new, Item.Properties::new, "decorative_log_pile");
	// The block registered without a BlockItem, so nothing could hold it: no recipe result, no creative tab entry
	public static final FeatureBlock<BlockAsh, BlockItem> ASH = REGISTRY.block(BlockAsh::new, BlockBehaviour.Properties::of, ItemBlockForestry::new, Item.Properties::new, "ash_block");
}
