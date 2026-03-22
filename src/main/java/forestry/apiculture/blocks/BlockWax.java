package forestry.apiculture.blocks;

import forestry.core.fluids.ForestryFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

public class BlockWax extends Block {


	private final boolean MELTABLE;
	//Adding this for compatibility reasons. Other bee mods might want meltable wax blocks yknow.

	@Nullable private final Fluid MELTING_FLUID;

	public BlockWax(boolean melts, @Nullable Fluid meltingFluid) {
		super(Properties.copy(Blocks.HONEYCOMB_BLOCK)
			.sound(SoundType.HONEY_BLOCK)
			.ignitedByLava()
		);
		this.MELTABLE = melts;
		this.MELTING_FLUID = meltingFluid;
	}

	@Override
	public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		if (MELTABLE)
			return super.isFlammable(state, level, pos, direction);
		return false;
	}

	@Override
	public boolean canStickTo(BlockState state, BlockState other) {
		return false;
	}

	@Override
	public boolean isStickyBlock(BlockState state) {
		return false;
	}

	@Override
	public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return (MELTABLE) ? 45 : 0;
	}
	@Override
	public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return (MELTABLE) ? 45 : 0;
	}

	@Override
	public void onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction direction, @Nullable LivingEntity igniter) {
		super.onCaughtFire(state, level, pos, direction, igniter);
		if (MELTABLE && MELTING_FLUID != null)
			level.setBlock(pos, MELTING_FLUID.defaultFluidState().createLegacyBlock(), 3);
	}
}
