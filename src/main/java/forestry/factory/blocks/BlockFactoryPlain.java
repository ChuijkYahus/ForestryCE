package forestry.factory.blocks;

import forestry.core.blocks.BlockBase;

public class BlockFactoryPlain extends BlockBase<BlockTypeFactoryPlain> {
	public BlockFactoryPlain(BlockTypeFactoryPlain type) {
		super(Properties.of(), type);
	}
}
