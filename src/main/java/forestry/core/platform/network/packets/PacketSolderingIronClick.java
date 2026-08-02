package forestry.core.platform.network.packets;

import forestry.core.platform.gui.IContainerSocketed;
import forestry.core.platform.network.PacketIdServer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record PacketSolderingIronClick(int slot) implements CustomPacketPayload {
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return PacketIdServer.SOLDERING_IRON_CLICK;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketSolderingIronClick msg) {
		buffer.writeVarInt(msg.slot);
	}

	public static PacketSolderingIronClick decode(RegistryFriendlyByteBuf buffer) {
		return new PacketSolderingIronClick(buffer.readVarInt());
	}

	public static void handle(PacketSolderingIronClick msg, ServerPlayer player) {
		if (player.containerMenu instanceof IContainerSocketed socketMenu) {
			ItemStack itemstack = player.containerMenu.getCarried();

			socketMenu.handleSolderingIronClickServer(msg.slot(), player, itemstack);
		}
	}
}
