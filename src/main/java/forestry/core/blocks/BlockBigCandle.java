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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockBigCandle extends Block {

	//TODO: Wouldn't it be be cool if you could STACK BIG CANDLES ON TOP OF EACH OTHER???????
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public BlockBigCandle(BlockTypeBigCandle type){
		super(Properties.copy(Blocks.CANDLE)
			.lightLevel(b -> {
				if (b.getValue(LIT)) return 15;
				return 0;
			})
		);
		this.registerDefaultState(this.getStateDefinition().any()
			.setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) return;

		if (random.nextInt(3) == 0) {
			float x = pos.getX() + 0.5f;
			float y = pos.getY() + 0.875f;
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
