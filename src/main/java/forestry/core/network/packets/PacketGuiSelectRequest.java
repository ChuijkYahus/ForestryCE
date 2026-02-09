package forestry.core.network.packets;

import forestry.core.gui.IGuiSelectable;
import forestry.core.network.PacketIdServer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

public record PacketGuiSelectRequest(int primaryIndex, int secondaryIndex) implements CustomPacketPayload {
	public static void handle(PacketGuiSelectRequest msg, ServerPlayer player) {
		AbstractContainerMenu container = player.containerMenu;

		if (container instanceof IGuiSelectable guiSelectable) {
			guiSelectable.handleSelectionRequest(player, msg.primaryIndex(), msg.secondaryIndex());
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return PacketIdServer.GUI_SELECTION_REQUEST;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketGuiSelectRequest msg) {
		buffer.writeVarInt(msg.primaryIndex);
		buffer.writeVarInt(msg.secondaryIndex);
	}

	public static PacketGuiSelectRequest decode(RegistryFriendlyByteBuf buffer) {
		return new PacketGuiSelectRequest(buffer.readVarInt(), buffer.readVarInt());
	}
}
