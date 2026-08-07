package forestry.core.platform.block;

import net.minecraft.world.level.block.SoundType;

public class BlockCore extends BlockBase<BlockTypeCoreTesr> {
	public BlockCore(BlockTypeCoreTesr blockType) {
		super(Properties.of().sound(SoundType.WOOD).noOcclusion(), blockType);
	}
}
