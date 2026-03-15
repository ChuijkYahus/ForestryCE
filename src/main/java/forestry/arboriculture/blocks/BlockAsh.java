package forestry.arboriculture.blocks;

import forestry.core.advancements.AdvancementHelper;
import forestry.core.data.ForestryAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;

import javax.annotation.Nullable;

public class BlockAsh extends FallingBlock {
	public static final IntegerProperty AMOUNT = IntegerProperty.create("amount", 0, 63);
	private static final ResourceLocation BREAK_ASH_BLOCK = new ResourceLocation("forestry:break_ash_block");

	public BlockAsh() {
		super(Block.Properties.of().sound(SoundType.SAND).strength(0.6F));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AMOUNT);
	}

	@Override
	public PushReaction getPistonPushReaction(BlockState state) {
		return PushReaction.DESTROY;
	}


	@Override
	public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {

		super.playerDestroy(level, player, pos, state, blockEntity, tool);
		//Using a field to not create a new object every time someone breaks an ash block.
		//Performant? Maybe.
		if (state.getValue(AMOUNT) > 0)
			AdvancementHelper.tryUnlock(player, BREAK_ASH_BLOCK);

	}

	@Override
	public int getDustColor(BlockState state, BlockGetter reader, BlockPos pos) {
		return state.getMapColor(reader, pos).col;
	}


}
