package forestry.factory.blocks;

import forestry.core.blocks.IBlockType;
import forestry.core.blocks.IMachineProperties;
import forestry.core.blocks.MachineProperties;
import forestry.core.tiles.IForestryTicker;
import forestry.core.tiles.TileBase;
import forestry.factory.features.FactoryTiles;
import forestry.factory.tiles.TileRaintank;
import forestry.modules.features.FeatureTileType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public enum BlockTypeFactoryRaintank implements IBlockType {

	RAINTANK(FactoryTiles.RAIN_TANK, "raintank", TileRaintank::serverTick);

	private final IMachineProperties<?> machineProperties;

	<T extends TileBase> BlockTypeFactoryRaintank(FeatureTileType<T> teClass, String name, @Nullable IForestryTicker<T> serverTicker) {
		VoxelShape container = Shapes.or(
			Block.box(0, 0, 0, 16, 2, 16),
			Block.box(0, 0, 0, 16, 16, 2),
			Block.box(0, 0, 14, 16, 16, 16),
			Block.box(0, 0, 0, 2, 16, 16),
			Block.box(14, 0, 0, 16, 16, 16)
		);

		this.machineProperties = new MachineProperties.Builder<>(teClass, name)
			.setServerTicker(serverTicker)
			.setShape(container)
			.create();
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
