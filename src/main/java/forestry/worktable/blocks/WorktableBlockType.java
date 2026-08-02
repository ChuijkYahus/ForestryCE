package forestry.worktable.blocks;

import forestry.core.platform.block.IBlockType;
import forestry.core.platform.block.IMachineProperties;
import forestry.core.platform.block.MachineProperties;
import forestry.core.platform.tile.TileForestry;
import forestry.core.platform.registration.FeatureTileType;
import forestry.worktable.features.WorktableTiles;

public enum WorktableBlockType implements IBlockType {
	WORKTABLE(WorktableTiles.WORKTABLE, "worktable");

	private final IMachineProperties<?> machineProperties;

	WorktableBlockType(FeatureTileType<? extends TileForestry> tileType, String name) {
		this.machineProperties = new MachineProperties.Builder<>(tileType, name).create();
	}

	@Override
	public IMachineProperties<?> getMachineProperties() {
		return this.machineProperties;
	}

	@Override
	public String getSerializedName() {
		return this.machineProperties.getSerializedName();
	}
}
