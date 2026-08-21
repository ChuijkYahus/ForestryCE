package forestry.core.content.machines.blocks;

import forestry.core.platform.block.IBlockType;
import forestry.core.platform.block.IMachineProperties;
import forestry.core.platform.block.MachineProperties;
import forestry.core.platform.tile.IForestryTicker;
import forestry.core.platform.tile.TileForestry;
import forestry.core.content.machines.features.FactoryTiles;
import forestry.core.content.machines.tiles.TileFabricator;
import forestry.core.content.machines.tiles.TileRaintank;
import forestry.core.content.machines.tiles.TileSmelter;
import forestry.core.platform.registration.FeatureTileType;

public enum BlockTypeFactoryPlain implements IBlockType {
	FABRICATOR(FactoryTiles.FABRICATOR, "thermionic_fabricator", TileFabricator::serverTick),
	RAINTANK(FactoryTiles.RAIN_TANK, "raintank", TileRaintank::serverTick),
	// Deviation from 1.20.1: membership of BlockTypeFactoryTesr tracks having a classic Forestry
	// machine model and its 64x32 renderer textures. The smelter is a ForestryCE addition with
	// neither, so it keeps its own 1.20.1 block model and stays plain
	SMELTER(FactoryTiles.SMELTER, "smelter", TileSmelter::serverTick);

	private final IMachineProperties<?> machineProperties;

	<T extends TileForestry> BlockTypeFactoryPlain(FeatureTileType<T> teClass, String name, IForestryTicker<T> serverTicker) {
		this.machineProperties = new MachineProperties.Builder<>(teClass, name)
			.setServerTicker(serverTicker)
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
