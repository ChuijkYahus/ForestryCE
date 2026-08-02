package forestry.core.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.joml.Vector3f;

public class BlockWaterloggableWallTorch extends WallTorchBlock implements SimpleWaterloggedBlock {
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	public BlockWaterloggableWallTorch() {
		super(Properties.copy(Blocks.SOUL_WALL_TORCH).lightLevel(s ->  13), new DustParticleOptions(new Vector3f(0.68f, 1.0f, 0.89f), 0.8f));
		this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(WATERLOGGED);
	}

	//Phosphor Torches don't 'burn' per se so don't emit smoke
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		Direction direction = (Direction)state.getValue(FACING);
		double d0 = (double)pos.getX() + (double)0.5F;
		double d1 = (double)pos.getY() + 0.7;
		double d2 = (double)pos.getZ() + (double)0.5F;
		double d3 = 0.22;
		double d4 = 0.27;
		Direction direction1 = direction.getOpposite();
		level.addParticle(this.flameParticle, d0 + 0.27 * (double)direction1.getStepX(), d1 + 0.17, d2 + 0.27 * (double)direction1.getStepZ(), (double)0.0F, (double)0.0F, (double)0.0F);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState blockstate = this.defaultBlockState();
		LevelReader levelreader = context.getLevel();
		BlockPos blockpos = context.getClickedPos();
		Direction[] adirection = context.getNearestLookingDirections();

		for(Direction direction : adirection) {
			if (direction.getAxis().isHorizontal()) {
				Direction direction1 = direction.getOpposite();
				blockstate = (BlockState)blockstate.setValue(FACING, direction1);
				if (blockstate.canSurvive(levelreader, blockpos)) {
					FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
					return blockstate.setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
				}
			}
		}
		return null;
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (state.getValue(WATERLOGGED)) {
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
	}
}
