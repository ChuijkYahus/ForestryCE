package forestry.core.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockMetalPlating extends Block {

	public BlockMetalPlating(BlockTypeMetalPlating type) {
		super(BlockBehaviour.Properties.copy(Blocks.WAXED_COPPER_BLOCK)
			.mapColor(BlockTypeMetalPlating.getMapColour().get(type)));
	}
}
