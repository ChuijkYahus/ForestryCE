package forestry.core.blocks;

import forestry.core.features.CoreTiles;
import forestry.core.tiles.TileAnalyzer;
import forestry.core.tiles.TileEscritoire;
import forestry.modules.features.FeatureTileType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public enum BlockTypeCoreTesr implements IBlockType {
	ANALYZER(createAnalyzerProperties(CoreTiles.ANALYZER, "analyzer")),
	ESCRITOIRE(createEscritoireProperties(CoreTiles.ESCRITOIRE, "escritoire"));

	private final IMachineProperties<?> machineProperties;

	//I, Spearkiller, do sorrowfully declare that the following code is AI-written.
	private static final EnumMap<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);
	static {
		VoxelShape south = makeBaseShape();
		SHAPES.put(Direction.SOUTH, south);
		SHAPES.put(Direction.WEST,  rotateY(south));
		SHAPES.put(Direction.NORTH, rotateY(SHAPES.get(Direction.WEST)));
		SHAPES.put(Direction.EAST,  rotateY(SHAPES.get(Direction.NORTH)));
	}

	private static IMachineProperties<? extends TileAnalyzer> createAnalyzerProperties(FeatureTileType<TileAnalyzer> teClass, String name) {
		return new MachineProperties.Builder<>(teClass, name)
			.setShape(Shapes::block)
			.setServerTicker(TileAnalyzer::serverTick)
			.create();
	}

	//I, Spearkiller, do sorrowfully declare that the following code is AI-written.
	private static VoxelShape makeBaseShape() {
		VoxelShape legs = Shapes.or(
			Block.box(14, 0, 12, 15, 10, 13),
			Block.box(14, 0, 1, 15, 10, 2),
			Block.box(1, 0, 12, 2, 10, 13),
			Block.box(1, 0, 1, 2, 10, 2)
		);

		VoxelShape tabletop = Block.box(0, 10, 0, 16, 14, 14);
		VoxelShape backLip  = Block.box(0, 14, 0, 16, 18, 4);

		VoxelShape sides = Shapes.or(
			Block.box(0, 14, 4, 1, 17, 12),
			Block.box(15, 14, 4, 16, 17, 12)
		);

		return Shapes.or(legs, tabletop, backLip, sides);
	}

	//I, Spearkiller, do sorrowfully declare that the following code is AI-written.
	private static VoxelShape rotateY(VoxelShape shape) {
		VoxelShape[] result = new VoxelShape[1];
		result[0] = Shapes.empty();

		shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
			double rMinX = 1.0 - maxZ;
			double rMaxX = 1.0 - minZ;
			double rMinZ = minX;
			double rMaxZ = maxX;

			VoxelShape rotated = Block.box(
				rMinX * 16.0, minY * 16.0, rMinZ * 16.0,
				rMaxX * 16.0, maxY * 16.0, rMaxZ * 16.0
			);

			result[0] = Shapes.or(result[0], rotated);
		});

		return result[0];
	}

	private static MachineProperties<? extends TileEscritoire> createEscritoireProperties(FeatureTileType<TileEscritoire> teClass, String name) {

		//I, Spearkiller, do sorrowfully declare that the following code is AI-written.
		return new MachineProperties.Builder<>(teClass, name)
			.setShape((state, level, pos, context) ->
				SHAPES.getOrDefault(
					state.getValue(BlockBase.FACING),
					SHAPES.get(Direction.SOUTH)
				)
			)
			.create();
	}

	BlockTypeCoreTesr(IMachineProperties<?> machineProperties) {
		this.machineProperties = machineProperties;
	}

	@Override
	public IMachineProperties<?> getMachineProperties() {
		return this.machineProperties;
	}

	@Override
	public String getSerializedName() {
		return getMachineProperties().getSerializedName();
	}
}
