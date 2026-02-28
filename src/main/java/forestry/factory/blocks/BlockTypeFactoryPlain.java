package forestry.factory.blocks;

import forestry.core.blocks.IBlockType;
import forestry.core.blocks.IMachineProperties;
import forestry.core.blocks.MachineProperties;
import forestry.core.data.models.ForestryBlockStateProvider;
import forestry.core.tiles.IForestryTicker;
import forestry.core.tiles.TileForestry;
import forestry.factory.features.FactoryTiles;
import forestry.factory.tiles.*;
import forestry.modules.features.FeatureTileType;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public enum BlockTypeFactoryPlain implements IBlockType {
	FABRICATOR(FactoryTiles.FABRICATOR, "fabricator", ForestryBlockStateProvider.TankLayout.NONE, TileFabricator::serverTick),
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
