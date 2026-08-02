package forestry.factory.blocks;

import forestry.core.platform.block.BlockBase;

public class BlockFactoryPlain extends BlockBase<BlockTypeFactoryPlain> {
	public BlockFactoryPlain(BlockTypeFactoryPlain type) {
		super(Properties.of(), type);
	}
}
