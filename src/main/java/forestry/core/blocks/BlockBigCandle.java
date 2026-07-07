package forestry.core.blocks;

import forestry.api.ForestryTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockBigCandle extends Block implements SimpleWaterloggedBlock {

	//TODO: Wouldn't it be be cool if you could STACK BIG CANDLES ON TOP OF EACH OTHER???????
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	public static final BooleanProperty WATERLOGGED =  BlockStateProperties.WATERLOGGED;

	private final SimpleParticleType FLAME;

	public BlockBigCandle(BlockTypeBigCandle type){
		super(Properties.copy(Blocks.CANDLE)
			.lightLevel(b -> {
				if (type == BlockTypeBigCandle.REFRACTORY && b.getValue(LIT)) return 10;
				if (b.getValue(LIT)) return 15;
				return 0;
			})
		);
		this.registerDefaultState(this.getStateDefinition().any()
			.setValue(LIT, false)
			.setValue(WATERLOGGED, false));
		if (type == BlockTypeBigCandle.REFRACTORY) FLAME = ParticleTypes.SOUL_FIRE_FLAME;
		else FLAME = ParticleTypes.FLAME;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
		builder.add(WATERLOGGED);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) return;
		addParticlesAndSound(level, pos.getCenter().add(0, 0.4375f, 0), random, FLAME);
	}

	//Lifted from the vanilla candle code.
	private static void addParticlesAndSound(Level level, Vec3 offset, RandomSource random, SimpleParticleType flame) {
		float f = random.nextFloat();
		if (f < 0.3F) {
			level.addParticle(ParticleTypes.SMOKE, offset.x, offset.y, offset.z, 0.0, 0.0, 0.0);
			if (f < 0.17F) {
				level.playLocalSound(offset.x + 0.5, offset.y + 0.5, offset.z + 0.5, SoundEvents.CANDLE_AMBIENT, SoundSource.BLOCKS, 1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F, false);
			}
		}
		level.addParticle(flame, offset.x, offset.y, offset.z, 0.0, 0.0, 0.0);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos,
								 Player player, InteractionHand hand, BlockHitResult hit) {

		ItemStack stack = player.getItemInHand(hand);

		if (!state.getValue(LIT)
			&& !player.isShiftKeyDown()
			&& !state.getValue(WATERLOGGED)) {

			if (stack.is(Items.FLINT_AND_STEEL)) {
				if (!level.isClientSide) {
					level.setBlock(pos, state.setValue(LIT, true), 3);
					stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
				}
				level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS);
				return InteractionResult.sidedSuccess(level.isClientSide);
			}
			else if (stack.is(Items.FIRE_CHARGE)){
				if (!level.isClientSide) {
					level.setBlock(pos, state.setValue(LIT, true), 3);
					if (!player.isCreative())
						stack.setCount(stack.getCount()-1);
				}
				level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS);
				return InteractionResult.sidedSuccess(level.isClientSide);
			}
		}
		else if (state.getValue(LIT)
			&& stack.isEmpty()
		){
			level.setBlock(pos, state.setValue(LIT, false), 3);
			level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
			return InteractionResult.sidedSuccess(level.isClientSide);
		}

		return InteractionResult.PASS;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
		return this.defaultBlockState().setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (state.getValue(WATERLOGGED)) {
			state = state.setValue(LIT, false);
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level,
							   BlockPos pos, CollisionContext context) {
		return Shapes.box(0.25,0,0.25,0.75,0.75,0.75);
	}

	@Override
	public boolean canStickTo(BlockState state, BlockState other) {
		return false;
	}

	@Override
	public PushReaction getPistonPushReaction(BlockState state) {
		return PushReaction.DESTROY;
	}

}
