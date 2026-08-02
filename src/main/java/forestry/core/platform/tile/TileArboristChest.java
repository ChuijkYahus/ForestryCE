package forestry.core.platform.tile;

import forestry.core.features.CoreTiles;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import forestry.core.platform.tile.TileNaturalistChest;

public class TileArboristChest extends TileNaturalistChest {
	public TileArboristChest(BlockPos pos, BlockState state) {
		super(CoreTiles.ARBORIST_CHEST.tileType(), pos, state, SpeciesUtil.TREE_TYPE.get());
	}
}
