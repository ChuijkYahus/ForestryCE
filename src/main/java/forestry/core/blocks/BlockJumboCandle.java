package forestry.core.blocks;

import forestry.api.ForestryTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

public class BlockJumboCandle extends Block {

	public enum CandleShape implements StringRepresentable {
		SINGLE,
		TOP,
		MIDDLE,
		BOTTOM;

		@Override
		public String getSerializedName() {
			return name().toLowerCase();
		}
	}

	public static final EnumProperty<CandleShape> SHAPE = EnumProperty.create("shape", CandleShape.class);
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public BlockJumboCandle(BlockTypeJumboCandle type){
		super(BlockBehaviour.Properties.copy(Blocks.CANDLE)
			.mapColor(type.getMapColor())
			.lightLevel(b -> {
				if (b.getValue(LIT)) return 15;
				return 0;
			})
		);
		this.registerDefaultState(this.getStateDefinition().any()
			.setValue(LIT, false)
			.setValue(SHAPE, CandleShape.SINGLE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
		builder.add(SHAPE);
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {

		BlockState newState;

		//Should allow different candle colours to be stacked on top of each other
		boolean above = level.getBlockState(pos.above()).is(ForestryTags.Blocks.JUMBO_CANDLE);
		boolean below = level.getBlockState(pos.below()).is(ForestryTags.Blocks.JUMBO_CANDLE);

		//Update the shape of the candle first.
		if (above && below) {
			newState = state.setValue(SHAPE, CandleShape.MIDDLE);
		}
		else if ((!above) && below) {
			newState = state.setValue(SHAPE, CandleShape.TOP);
		}
		else if (above && (!below)) {
			newState = state.setValue(SHAPE, CandleShape.BOTTOM);
		}
		else {
			newState = state.setValue(SHAPE, CandleShape.SINGLE);
		}

		//If there are any blocks above, light/extinguish the candle
		if (direction == Direction.UP){
			BlockState aboveState = level.getBlockState(pos.above());
			FluidState fluidState = aboveState.getFluidState();

			boolean hasFluid = !fluidState.isEmpty();
			boolean isLava = fluidState.is(FluidTags.LAVA);

			//Extinguish
			if (newState.getValue(LIT)) {

				if ((hasFluid && !isLava) || aboveState.isSuffocating(level, pos.above())) {
					newState.setValue(LIT, false);
					if (level instanceof ServerLevel serverLevel)
						serverLevel.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
				}
			}
			//LIGHT
			else {
				if ((hasFluid && isLava) || aboveState.is(BlockTags.FIRE)){
					newState.setValue(LIT, true);
				}
			}
		}

		return newState;

	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) return;
		if (state.getValue(SHAPE) == CandleShape.BOTTOM || state.getValue(SHAPE) == CandleShape.MIDDLE ) return;

		if (random.nextInt(3) == 0) {
			float x = pos.getX() + 0.5f;
			float y = pos.getY() + 1.125f;
			float z = pos.getZ() + 0.5f;

			level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);

			if (random.nextInt(2) == 0) {
				level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.001, 0.001, 0.001);
			}
		}
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos,
								 Player player, InteractionHand hand, BlockHitResult hit) {

		ItemStack stack = player.getItemInHand(hand);

		if (!state.getValue(LIT)
			&& state.getValue(SHAPE) != CandleShape.BOTTOM
			&& state.getValue(SHAPE) != CandleShape.MIDDLE
			&& !player.isShiftKeyDown()) {

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
						stack = stack.copyWithCount(stack.getCount() - 1);
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
	public PushReaction getPistonPushReaction(BlockState state) {
		return PushReaction.DESTROY;
	}

}
