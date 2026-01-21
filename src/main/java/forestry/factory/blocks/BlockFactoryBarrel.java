package forestry.factory.blocks;

import forestry.core.blocks.BlockBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockFactoryBarrel extends BlockBase<BlockTypeFactoryBarrel> {

	@Override
	public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
		return Shapes.block();
	}
	public BlockFactoryBarrel(BlockTypeFactoryBarrel type) {
		super(type, Block.Properties.of());
	}
}
