package forestry.factory.blocks;

import forestry.Forestry;
import forestry.core.blocks.BlockBase;
import forestry.core.blocks.IMachineProperties;
import forestry.core.data.models.ForestryBlockStateProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockFactoryPlain extends BlockBase<BlockTypeFactoryPlain> {



	public static final IntegerProperty TANK_PRODUCT_LEVEL = IntegerProperty.create("tank_product_fill_level", 0, 4);
	public static final IntegerProperty TANK_RESOURCE_LEVEL = IntegerProperty.create("tank_resource_fill_level", 0, 4);

	private static final ThreadLocal<BlockTypeFactoryPlain> cacheType = new ThreadLocal<>();

	private BlockFactoryPlain(BlockTypeFactoryPlain type) {
		super(type, Properties.of());
	}

	public static void setMachineType(BlockTypeFactoryPlain type){
		cacheType.set(type);
	}


	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {

		super.createBlockStateDefinition(builder);

		BlockTypeFactoryPlain tType = cacheType.get();

		if (tType != null) {

			Forestry.LOGGER.debug("Creating blockstates for: " + tType.name());
			ForestryBlockStateProvider.TankLayout layout = tType.getMachineProperties().getTankLayout();

			switch (layout) {
				case BOTH -> {
					builder.add(TANK_PRODUCT_LEVEL);
					builder.add(TANK_RESOURCE_LEVEL);
				}
				case RESOURCE -> builder.add(TANK_RESOURCE_LEVEL);
				case PRODUCT -> builder.add(TANK_PRODUCT_LEVEL);
			}
		}
    }

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter reader, BlockPos pos, CollisionContext context) {

		switch (this.blockType) {
			case FABRICATOR -> {
				return Shapes.block();
			}
			default -> {

				if (state.getValue(BlockBase.FACING) == Direction.SOUTH ||
					state.getValue(BlockBase.FACING) == Direction.NORTH) {
					return Shapes.or(
						Block.box(0, 0, 0, 16, 16, 4),
						Block.box(2, 2, 4, 14, 14, 12),
						Block.box(0, 0, 12, 16, 16, 16)
					);
				} else {
					return Shapes.or(
						Block.box(0, 0, 0, 4, 16, 16),
						Block.box(4, 2, 2, 12, 14, 14),
						Block.box(12, 0, 0, 16, 16, 16)
					);
				}
			}
		}
	}

	/**
	 * The registry thingy needs to use this instead of calling BlockFactoryPlain::new because otherwise blockstates and such don't generate. What a headache.
	 * @param type BlockTypeFactoryPlain - the type of factory block to make
	 * @return BlockFactoryPlain a new instance of this factory block
	 */
	public static BlockFactoryPlain create(BlockTypeFactoryPlain type) {
		setMachineType(type);
		return new BlockFactoryPlain(type);
	}
}
