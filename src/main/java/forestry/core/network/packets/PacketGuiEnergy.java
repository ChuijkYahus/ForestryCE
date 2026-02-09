package forestry.core.network.packets;

import forestry.core.gui.ContainerTile;
import forestry.core.network.PacketIdClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record PacketGuiEnergy(int windowId, int value) implements CustomPacketPayload {
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return PacketIdClient.GUI_ENERGY;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketGuiEnergy msg) {
		buffer.writeVarInt(msg.windowId);
		buffer.writeVarInt(msg.value);
	}

	public static PacketGuiEnergy decode(RegistryFriendlyByteBuf buffer) {
		return new PacketGuiEnergy(buffer.readVarInt(), buffer.readVarInt());
	}

	public static void handle(PacketGuiEnergy msg, Player player) {
		if (player.containerMenu.containerId == msg.windowId && player.containerMenu instanceof ContainerTile<?> menu) {
			menu.onGuiEnergy(msg.value);
		}
	}
}
