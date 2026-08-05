package forestry.agriculture.multifarm.tiles;

import forestry.core.platform.fluids.ITankManager;
import forestry.core.platform.tile.ILiquidTankTile;
import forestry.agriculture.features.FarmingTiles;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class TileFarmValve extends TileFarm implements ILiquidTankTile {
	public TileFarmValve(BlockPos pos, BlockState state) {
		super(FarmingTiles.VALVE.tileType(), pos, state);
	}

	@Override
	public ITankManager getTankManager() {
		return getMultiblockLogic().getController().getTankManager();
	}

	public IFluidHandler getFluidHandler(net.minecraft.core.Direction facing) {
		return getTankManager();
	}
}
