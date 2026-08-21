package forestry.core.content.machines.blocks;

import forestry.core.data.models.ForestryBlockStateProvider;
import forestry.core.platform.block.IBlockType;
import forestry.core.platform.block.IMachineProperties;
import forestry.core.platform.block.MachineProperties;
import forestry.core.platform.tile.IForestryTicker;
import forestry.core.platform.tile.TileForestry;
import forestry.core.content.machines.features.FactoryTiles;
import forestry.core.content.machines.tiles.*;
import forestry.core.platform.registration.FeatureTileType;

public enum BlockTypeFactoryPlain implements IBlockType {
	FABRICATOR(FactoryTiles.FABRICATOR, "thermionic_fabricator", ForestryBlockStateProvider.TankLayout.NONE, TileFabricator::serverTick),
	// Deviation from 1.20.1: membership of BlockTypeFactoryTesr tracks having a classic Forestry
	// machine model and its 64x32 renderer textures. The smelter is a ForestryCE addition with
	// neither, so it keeps its own 1.20.1 block model and stays plain
	SMELTER(FactoryTiles.SMELTER, "smelter", ForestryBlockStateProvider.TankLayout.NONE, TileSmelter::serverTick),
	BOTTLER(FactoryTiles.BOTTLER, "bottler", ForestryBlockStateProvider.TankLayout.RESOURCE, TileBottler::serverTick),
	CARPENTER(FactoryTiles.CARPENTER, "carpenter", ForestryBlockStateProvider.TankLayout.RESOURCE, TileCarpenter::serverTick),
	CENTRIFUGE(FactoryTiles.CENTRIFUGE, "centrifuge", ForestryBlockStateProvider.TankLayout.NONE, TileCentrifuge::serverTick),
	FERMENTER(FactoryTiles.FERMENTER, "fermenter", ForestryBlockStateProvider.TankLayout.BOTH, TileFermenter::serverTick),
	MOISTENER(FactoryTiles.MOISTENER, "moistener", ForestryBlockStateProvider.TankLayout.RESOURCE, TileMoistener::serverTick),
	SQUEEZER(FactoryTiles.SQUEEZER, "squeezer", ForestryBlockStateProvider.TankLayout.PRODUCT, TileSqueezer::serverTick),
	STILL(FactoryTiles.STILL, "still", ForestryBlockStateProvider.TankLayout.BOTH, TileStill::serverTick);

	private final IMachineProperties<?> machineProperties;

	<T extends TileForestry> BlockTypeFactoryPlain(FeatureTileType<T> teClass, String name, ForestryBlockStateProvider.TankLayout layout, IForestryTicker<T> serverTicker) {
		this.machineProperties = new MachineProperties.Builder<>(teClass, name)
			.setServerTicker(serverTicker)
			.setTankLayout(layout)
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
