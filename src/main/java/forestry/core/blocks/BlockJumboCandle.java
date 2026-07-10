package forestry.core.blocks;

import forestry.Forestry;
import forestry.api.ForestryTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.Vec3;

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
	private final SimpleParticleType FLAME;

	public BlockJumboCandle(BlockTypeJumboCandle type){
		super(BlockBehaviour.Properties.copy(Blocks.CANDLE)
			.mapColor(type.getMapColor())
			.lightLevel(b -> {
				if (type == BlockTypeJumboCandle.REFRACTORY && b.getValue(LIT)) return 10;
				if (b.getValue(LIT)) return 15;
				return 0;
			})
		);
		this.registerDefaultState(this.getStateDefinition().any()
			.setValue(LIT, false)
			.setValue(SHAPE, CandleShape.SINGLE));
		if (type == BlockTypeJumboCandle.REFRACTORY) FLAME = ParticleTypes.SOUL_FIRE_FLAME;
		else FLAME = ParticleTypes.FLAME;
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
			newState = newState.setValue(LIT, false);
		}
		else if ((!above) && below) {
			newState = state.setValue(SHAPE, CandleShape.TOP);
		}
		else if (above && (!below)) {
			newState = state.setValue(SHAPE, CandleShape.BOTTOM);
			newState = newState.setValue(LIT, false);
		}
		else {
			newState = state.setValue(SHAPE, CandleShape.SINGLE);
		}

		//If there are any blocks above, light/extinguish the candle
		BlockState aboveState = level.getBlockState(pos.above());
		FluidState fluidState = aboveState.getFluidState();

		boolean hasFluid = !fluidState.isEmpty();
		boolean isLava = fluidState.is(FluidTags.LAVA);
		boolean isSolid = aboveState.isSolidRender(level, pos.above());

		//Extinguish
		if (state.getValue(LIT)) {
			if ((hasFluid && !isLava) || isSolid) {
				newState = newState.setValue(LIT, false);
				if (level instanceof ServerLevel serverLevel)
					serverLevel.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
			}
		}
		//LIGHT
		else {
			if ((hasFluid && isLava) || aboveState.is(BlockTags.FIRE)){
				newState = newState.setValue(LIT, true);
			}
		}

		return newState;

	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) return;
		if (state.getValue(SHAPE) == CandleShape.BOTTOM || state.getValue(SHAPE) == CandleShape.MIDDLE ) return;

		addParticlesAndSound(level, pos.getCenter().add(0, 0.6875f, 0), random, FLAME);
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
			&& state.getValue(SHAPE) != CandleShape.BOTTOM
			&& state.getValue(SHAPE) != CandleShape.MIDDLE
			&& !player.isShiftKeyDown()
			&& !level.getBlockState(pos.above()).isSolidRender(level, pos.above())) {

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
						stack.setCount(stack.getCount() - 1);
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
