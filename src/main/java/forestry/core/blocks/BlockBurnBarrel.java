package forestry.core.blocks;

import forestry.arboriculture.features.ArboricultureTiles;
import forestry.core.features.CoreTiles;
import forestry.core.gui.ContainerBurnBarrel;
import forestry.core.tiles.TileBurnBarrel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

//This class is it's own unique thing, seperate from TileForestry or anything like that for reasons that made sense to me at the time. I think it's mainly because I wanted to be able to easily define a custom shape and properties and shapes. Was it worth it? We'll have to see if anything breaks.
public class BlockBurnBarrel extends BaseEntityBlock implements MenuProvider {

	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	public static final BooleanProperty HAS_ASH = BooleanProperty.create("has_ash");

	private static final VoxelShape blockShape = Shapes.or(
		Block.box(3,0,3, 4,16,13),
		Block.box(3,0,3, 13,16,4),
		Block.box(12,0,3, 13,16,13),
		Block.box(3,0,12, 13,16,13),

		Block.box(3,0,3, 13,2,13)
	);
	private static final VoxelShape interactionShape = Block.box(3,0,3, 13,16,13);

	public BlockBurnBarrel() {
		super(Properties.copy(Blocks.FURNACE)
			.mapColor(MapColor.COLOR_GRAY)
			.sound(SoundType.METAL)
			.lightLevel(
				state -> {
					if (state.getValue(BlockBurnBarrel.LIT)){
						return 15;
					}
					return 0;
				}
			)
		);
		this.registerDefaultState(this.getStateDefinition().any()
			.setValue(LIT, false)
			.setValue(HAS_ASH, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
		builder.add(HAS_ASH);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.forestry.burn_barrel");
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
		return new TileBurnBarrel(blockPos, blockState);
	}
	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level,
							   BlockPos pos, CollisionContext context) {
		return blockShape;
	}

	@Override
	public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
		return interactionShape;
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
		return null;
	}
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos,
								 Player player, InteractionHand hand, BlockHitResult hit) {

		if (!level.isClientSide) {
			BlockEntity be = level.getBlockEntity(pos);
			if (be instanceof TileBurnBarrel burnBarrel) {
				burnBarrel.openGui((ServerPlayer) player, hand, pos);
			}
		}

		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> actual) {
		return level.isClientSide ? null : (lvl, pos, s, be) -> {
			if (be instanceof TileBurnBarrel barrel) {
				barrel.serverTick(lvl, pos, s);
			}
		};
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getBlock() != newState.getBlock()){
			BlockEntity b = level.getBlockEntity(pos);
			if (b instanceof TileBurnBarrel tb){
				Containers.dropContents(level, pos, tb.getInternalInventory());
			}
			level.removeBlockEntity(pos);
		}

		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	public PushReaction getPistonPushReaction(BlockState state) {
		return PushReaction.DESTROY;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) return;
		addParticlesAndSound(level, pos.getCenter().add(0, 0.55f, 0), random);
	}

	//Adapted from Jumbo Candles which was adapted from vanilla candles.
	private static void addParticlesAndSound(Level level, Vec3 offset, RandomSource random) {
		float f = random.nextFloat();
		if (f < 0.6F) {
			float randX = (random.nextFloat()-0.5f) / 10f;
			float randZ = (random.nextFloat()-0.5f) / 10f;
			level.addParticle(ParticleTypes.SMOKE, offset.x, offset.y, offset.z, randX, 0.05, randZ);
			if (f < 0.085F) {
				level.playLocalSound(offset.x + 0.5, offset.y + 0.5, offset.z + 0.5, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F, false);
			}
		}
		if (f < 0.1F)
			level.addParticle(ParticleTypes.LAVA, offset.x, offset.y, offset.z, 0.0, 0.0, 0.0);
	}
	/*

	private static IMachineProperties<?> createBurnBarrelProperties(FeatureTileType<TileBurnBarrel> teClass, String name) {
		return new MachineProperties.Builder<>(teClass, name)
			.setShape(Block.box(3, 0, 3, 13, 16, 13))
			.setServerTicker(TileBurnBarrel::serverTick)
			.create();

	}*/


}
