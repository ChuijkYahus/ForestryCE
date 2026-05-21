package forestry.core.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SheetBlock extends Block {

	//I'm being a little lazy here. Facing is used to essentially say which way the 15/16ths of the block being empty is at.
	//That makes no sense. 'UP' means that the sheet block is placed on the top of a block.
	public static final DirectionProperty FACING = BlockStateProperties.FACING;

	private static final float DEPTH = 1/16f;

	public SheetBlock() {
		//TODO: Currently sheet blocks can only be wood. Tee hee, i suppose
		super(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS));
		this.registerDefaultState(this.getStateDefinition().any()
			.setValue(FACING, Direction.UP));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}


	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level,
							   BlockPos pos, CollisionContext context) {
		switch (state.getValue(FACING)) {
			case DOWN: return Shapes.box(0, 1 - DEPTH, 0, 1, 1, 1);
			case NORTH: return Shapes.box(0, 0, 1 - DEPTH, 1, 1, 1);
			case SOUTH: return Shapes.box(0, 0, 0, 1, 1, DEPTH);
			case EAST: return Shapes.box(0, 0, 0, DEPTH, 1, 1);
			case WEST: return Shapes.box(1 - DEPTH, 0, 0, 1, 1, 1);
			default: return Shapes.box(0, 0, 0, 1, DEPTH, 1);
		}
	}

	public BlockState getStateForPlacement(BlockPlaceContext context) {

		//I actually think this works less good. Maybe I'm wrong.
		/*if (context.getPlayer() != null && context.getPlayer().isCrouching())
			this.defaultBlockState().setValue(FACING, context.getClickedFace());*/

		Level level = context.getLevel();
		BlockPos againstPos = context.getClickedPos().relative(context.getClickedFace().getOpposite());
		BlockState againstState = level.getBlockState(againstPos);

		//Copy the orientation of other sheet blocks
		if (againstState.is(this)) {
			return this.defaultBlockState().setValue(FACING, againstState.getValue(FACING));
		}

		return this.defaultBlockState()
			.setValue(FACING, context.getClickedFace());
	}

	@Override
	public PushReaction getPistonPushReaction(BlockState state) {
		return PushReaction.DESTROY;
	}

}
