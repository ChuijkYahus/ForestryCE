package forestry.worktable.network.packets;

import forestry.core.network.PacketIdClient;
import forestry.core.tiles.TileUtil;
import forestry.worktable.recipes.RecipeMemory;
import forestry.worktable.tiles.WorktableTile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record PacketWorktableMemoryUpdate(BlockPos pos, RecipeMemory memory) implements CustomPacketPayload {
	public PacketWorktableMemoryUpdate(WorktableTile worktable) {
		this(worktable.getBlockPos(), worktable.getMemory());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return PacketIdClient.WORKTABLE_MEMORY_UPDATE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketWorktableMemoryUpdate msg) {
		buffer.writeBlockPos(msg.pos);
		msg.memory.writeData(buffer);
	}

	public static PacketWorktableMemoryUpdate decode(RegistryFriendlyByteBuf buffer) {
		return new PacketWorktableMemoryUpdate(buffer.readBlockPos(), new RecipeMemory(buffer));
	}

	public static void handle(PacketWorktableMemoryUpdate msg, Player player) {
		WorktableTile tile = TileUtil.getTile(player.level(), msg.pos, WorktableTile.class);
		if (tile != null) {
			tile.getMemory().copy(msg.memory);
		}
	}
}
