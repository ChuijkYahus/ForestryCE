package forestry.apiculture.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.apiculture.blocks.*;
import forestry.apiculture.items.EnumHoneyComb;
import forestry.apiculture.items.ItemBlockHoneyComb;
import forestry.core.fluids.ForestryFluids;
import forestry.core.items.ItemBlockForestry;
import forestry.modules.features.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

@FeatureProvider
public class ApicultureBlocks {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.APICULTURE);

	public static final FeatureBlockGroup<BlockApiculture, BlockTypeApiculture> BASE = REGISTRY.blockGroup(BlockApiculture::new, BlockTypeApiculture.values()).item((block) -> new ItemBlockForestry<>(block, new Item.Properties())).create();

	public static final FeatureBlockGroup<BlockBeeHive, BlockHiveType> BEEHIVE = REGISTRY.blockGroup(BlockBeeHive::new, BlockHiveType.values()).itemWithType((block, type) -> new ItemBlockForestry<>(block, new Item.Properties())).identifier("beehive").create();

	public static final FeatureBlockGroup<BlockHoneyComb, EnumHoneyComb> BEE_COMB = REGISTRY.blockGroup(BlockHoneyComb::new, EnumHoneyComb.VALUES).item(ItemBlockHoneyComb::new).identifier("block_bee_comb").create();
	public static final FeatureBlockGroup<BlockAlveary, BlockAlvearyType> ALVEARY = REGISTRY.blockGroup(BlockAlveary::new, BlockAlvearyType.VALUES).item(blockAlveary -> new ItemBlockForestry<>(blockAlveary, new Item.Properties())).identifier("alveary").create();

	public static final FeatureBlock<Block, BlockItem> WAX_BLOCK = REGISTRY.block(() -> new BlockWax(true, ForestryFluids.WAX.getFluid()), ItemBlockForestry::new, "wax_block");

	public static final FeatureBlock<Block, BlockItem> REFRACTORY_WAX_BLOCK = REGISTRY.block(() -> new BlockWax(false, null), ItemBlockForestry::new, "wax_block_refractory");

}
