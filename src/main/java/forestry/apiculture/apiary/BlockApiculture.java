package forestry.apiculture.apiary;

import forestry.core.platform.block.BlockBase;
import net.minecraft.world.level.block.SoundType;
import forestry.apiculture.apiary.BlockTypeApiculture;

public class BlockApiculture extends BlockBase<BlockTypeApiculture> {
	public BlockApiculture(BlockTypeApiculture type) {
		super(Properties.of().sound(SoundType.WOOD), type);
	}
}
