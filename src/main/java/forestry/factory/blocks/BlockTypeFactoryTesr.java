package forestry.factory.blocks;

import forestry.core.blocks.BlockBase;
import forestry.core.blocks.IBlockType;
import forestry.core.blocks.IMachineProperties;
import forestry.core.blocks.MachineProperties;
import forestry.core.config.Constants;
import forestry.core.tiles.IForestryTicker;
import forestry.core.tiles.TileBase;
import forestry.core.tiles.TileMill;
import forestry.factory.features.FactoryTiles;
import forestry.factory.tiles.*;
import forestry.modules.features.FeatureTileType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public enum BlockTypeFactoryTesr implements IBlockType {

	RAINMAKER(FactoryTiles.RAINMAKER, "rainmaker", Constants.TEXTURE_PATH_BLOCK + "/rainmaker_");

	private final IMachineProperties<?> machineProperties;

	<T extends TileMill> BlockTypeFactoryTesr(FeatureTileType<T> teClass, String name, String renderMillTexture) {
		final VoxelShape pedestal = Block.box(0D, 0D, 0D, 16, 1, 16);
		final VoxelShape column = Block.box(5D, 1D, 4D, 11, 16, 12);
		final VoxelShape extension = Block.box(1D, 8D, 7D, 15, 10, 9);

		this.machineProperties = new MachineProperties.Builder<>(teClass, name)
			.setShape(() -> Shapes.or(pedestal, column, extension))
			.setClientTicker(TileMill::clientTick)
			.setServerTicker(TileMill::serverTick)
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
