package forestry.core.network.packets;

import forestry.core.gui.IContainerLiquidTanks;
import forestry.core.network.PacketIdServer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public record PacketPipetteClick(int slot) implements CustomPacketPayload {
	public static void handle(PacketPipetteClick msg, ServerPlayer player) {
		if (player.containerMenu instanceof IContainerLiquidTanks tanksMenu) {
			tanksMenu.handlePipetteClick(msg.slot(), player);
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return PacketIdServer.PIPETTE_CLICK;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketPipetteClick msg) {
		buffer.writeVarInt(msg.slot);
	}

	public static PacketPipetteClick decode(RegistryFriendlyByteBuf buffer) {
		return new PacketPipetteClick(buffer.readVarInt());
	}
}
