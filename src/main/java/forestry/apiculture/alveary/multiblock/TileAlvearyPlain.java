package forestry.apiculture.alveary.multiblock;

import forestry.apiculture.alveary.BlockAlveary;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class TileAlvearyPlain extends TileAlveary {

	public TileAlvearyPlain(BlockPos pos, BlockState state) {
		super(BlockAlveary.Type.PLAIN, pos, state);
	}

	@Override
	public boolean allowsAutomation() {
		return true;
	}

	@Override
	public String patternTypeId() {
		return AlvearyPattern.PLAIN;
	}

}
