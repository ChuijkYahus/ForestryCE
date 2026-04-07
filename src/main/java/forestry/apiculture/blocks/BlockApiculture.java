package forestry.apiculture.blocks;

import forestry.core.blocks.BlockBase;
import net.minecraft.world.level.block.SoundType;

public class BlockApiculture extends BlockBase<BlockTypeApiculture> {
	public BlockApiculture(BlockTypeApiculture type) {
		super(Properties.of().sound(SoundType.WOOD), type);
	}
}
