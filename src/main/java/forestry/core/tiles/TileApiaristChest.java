package forestry.core.tiles;

import forestry.core.features.CoreTiles;
import forestry.core.utils.SpeciesUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class TileApiaristChest extends TileNaturalistChest {
	public TileApiaristChest(BlockPos pos, BlockState state) {
		super(CoreTiles.APIARIST_CHEST.tileType(), pos, state, SpeciesUtil.BEE_TYPE.get());
	}
}
