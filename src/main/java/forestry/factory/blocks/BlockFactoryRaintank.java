package forestry.factory.blocks;

import forestry.core.blocks.BlockBase;
import forestry.factory.tiles.TileRaintank;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockFactoryRaintank extends BlockBase<BlockTypeFactoryRaintank> {

	public BlockFactoryRaintank(BlockTypeFactoryRaintank type) {
		super(type, Block.Properties.of());
	}

	@Override
	public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
		return Shapes.block();
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state){
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {

		BlockEntity b = level.getBlockEntity(pos);
		if (b instanceof TileRaintank tank && tank.getTankManager().getFluid(0) != null) {

			int maxAmount = tank.getTankManager().getTankCapacity(0);
			int fillAmount = tank.getTankManager().getFluid(0).getAmount();
			float fillRatio = (float)fillAmount/maxAmount;

			if (fillRatio > 0) {
				return Mth.ceil(fillRatio * 15);
			}
		}
		return 0;
	}
}
