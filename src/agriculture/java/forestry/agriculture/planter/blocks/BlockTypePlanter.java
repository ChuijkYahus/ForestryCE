package forestry.agriculture.planter.blocks;

import forestry.core.platform.block.IBlockType;
import forestry.core.platform.block.IMachineProperties;
import forestry.core.platform.block.MachineProperties;
import forestry.agriculture.features.CultivationTiles;
import forestry.agriculture.planter.tiles.TilePlanter;
import forestry.core.platform.registration.FeatureTileType;

import java.util.List;

public enum BlockTypePlanter implements IBlockType {
	ARBORETUM(CultivationTiles.ARBORETUM, "arboretum"),
	FARM_CROPS(CultivationTiles.CROPS, "farm_crops"),
	FARM_MUSHROOM(CultivationTiles.MUSHROOM, "farm_mushroom"),
	FARM_GOURD(CultivationTiles.GOURD, "farm_gourd"),
	FARM_NETHER(CultivationTiles.NETHER, "farm_nether"),
	FARM_ENDER(CultivationTiles.ENDER, "farm_ender"),
	PEAT_POG(CultivationTiles.BOG, "peat_bog");

	public static final List<BlockTypePlanter> VALUES = List.of(values());

	private final IMachineProperties<?> machineProperties;

	BlockTypePlanter(FeatureTileType<? extends TilePlanter> teClass, String name) {
		this.machineProperties = new MachineProperties.Builder<>(teClass, name)
			.setServerTicker(TilePlanter::serverTick)
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
