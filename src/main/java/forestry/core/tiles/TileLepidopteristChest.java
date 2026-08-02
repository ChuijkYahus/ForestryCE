package forestry.core.tiles;

import forestry.core.features.CoreTiles;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class TileLepidopteristChest extends TileNaturalistChest {
	public TileLepidopteristChest(BlockPos pos, BlockState state) {
		super(CoreTiles.LEPIDOPTERIST_CHEST.tileType(), pos, state, SpeciesUtil.BUTTERFLY_TYPE.get());
	}
}
