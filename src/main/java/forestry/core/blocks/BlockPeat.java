package forestry.core.blocks;

import forestry.core.features.CoreBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

/**
 * Mature form of Bog Earth.
 */
public class BlockPeat extends Block {

	public BlockPeat() {
        super(BlockBehaviour.Properties.of()
			.strength(0.5f)
			.sound(SoundType.MUDDY_MANGROVE_ROOTS));
	}

	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
		return new ItemStack(CoreBlocks.BOG_EARTH);
	}
}
