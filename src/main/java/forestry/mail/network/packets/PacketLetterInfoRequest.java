package forestry.mail.network.packets;

import forestry.api.ForestryRegistries;
import forestry.api.mail.IPostalCarrier;
import forestry.core.network.PacketIdServer;
import forestry.mail.gui.LetterMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record PacketLetterInfoRequest(String recipientName,
									  IPostalCarrier addressType) implements CustomPacketPayload {
	public static void handle(PacketLetterInfoRequest msg, ServerPlayer player) {
		if (player.containerMenu instanceof LetterMenu menu) {
			menu.handleRequestLetterInfo(player, msg.recipientName(), msg.addressType());
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return PacketIdServer.LETTER_INFO_REQUEST;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketLetterInfoRequest msg) {
		buffer.writeUtf(msg.recipientName);
		buffer.writeUtf(ForestryRegistries.POSTAL_CARRIER.getKey(msg.addressType).toString());
	}

	public static PacketLetterInfoRequest decode(RegistryFriendlyByteBuf buffer) {
		return new PacketLetterInfoRequest(buffer.readUtf(), ForestryRegistries.POSTAL_CARRIER.get(ResourceLocation.tryParse(buffer.readUtf())));
	}
}
