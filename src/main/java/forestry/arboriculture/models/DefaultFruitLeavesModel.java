package forestry.arboriculture.models;

import forestry.arboriculture.leaves.DefaultFruitLeavesBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;

public class DefaultFruitLeavesModel extends DecorativeLeavesModel<DefaultFruitLeavesBlock> {
	public DefaultFruitLeavesModel() {
		super(DefaultFruitLeavesBlock.class);
	}

	@Override
	protected DefaultLeavesModel.Key getInventoryKey(ItemStack stack) {
		Block block = Block.byItem(stack.getItem());
		return new DefaultLeavesModel.Key(((DefaultFruitLeavesBlock) block).getSpeciesId(), Minecraft.useFancyGraphics());
	}

	@Override
	protected DefaultLeavesModel.Key getWorldKey(BlockState state, ModelData extraData) {
		Block block = state.getBlock();
		return new DefaultLeavesModel.Key(((DefaultFruitLeavesBlock) block).getSpeciesId(), Minecraft.useFancyGraphics());
	}

	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
		return ChunkRenderTypeSet.of(RenderType.cutoutMipped());
	}
}
