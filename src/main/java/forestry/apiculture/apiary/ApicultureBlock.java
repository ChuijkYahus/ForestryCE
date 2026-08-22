package forestry.apiculture.apiary;

import forestry.core.platform.block.BlockBase;
import net.minecraft.world.level.block.SoundType;

public class ApicultureBlock extends BlockBase<ApicultureBlockType> {
	public ApicultureBlock(ApicultureBlockType type) {
		super(Properties.of().sound(SoundType.WOOD), type);
	}
}
