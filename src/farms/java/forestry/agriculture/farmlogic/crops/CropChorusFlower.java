package forestry.agriculture.farmlogic.crops;

import forestry.core.platform.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;

import java.util.ArrayList;
import java.util.List;

public class CropChorusFlower extends Crop {
	private static final BlockState BLOCK_STATE = Blocks.CHORUS_FLOWER.defaultBlockState();

	public CropChorusFlower(Level world, BlockPos position) {
		super(world, position);
	}

	@Override
	protected boolean isCrop(Level world, BlockPos pos) {
		return world.getBlockState(pos).getBlock() == Blocks.CHORUS_FLOWER;
	}

	@Override
	protected List<ItemStack> harvestBlock(Level level, BlockPos pos) {
		if (level instanceof ServerLevel serverLevel) {
			CommonHooks.handleBlockDrops(
				serverLevel,
				pos,
				BLOCK_STATE,
				level.getBlockEntity(pos),
				new ArrayList<>(List.of(new ItemEntity(
					level,
					// We could use pos.getCenter(), but let's avoid importing Vec3 for this.
					pos.getX() + 0.5D,
					pos.getY() + 0.5D,
					pos.getZ() + 0.5D,
					new ItemStack(Blocks.CHORUS_FLOWER)
				))),
				null,
				ItemStack.EMPTY
			);
		}

		BlockUtil.sendDestroyEffects(level, pos, BLOCK_STATE);
		level.removeBlock(pos, false);

		return List.of();
	}
}