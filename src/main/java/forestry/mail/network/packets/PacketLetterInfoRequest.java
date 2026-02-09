package forestry.mail.network.packets;

import forestry.api.mail.IPostalCarrier;
import forestry.core.network.PacketIdServer;
import forestry.mail.carriers.PostalCarriers;
import forestry.mail.gui.ContainerLetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record PacketLetterInfoRequest(String recipientName,
									  IPostalCarrier addressType) implements CustomPacketPayload {
	public static void handle(PacketLetterInfoRequest msg, ServerPlayer player) {
		if (player.containerMenu instanceof ContainerLetter containerLetter) {
			containerLetter.handleRequestLetterInfo(player, msg.recipientName(), msg.addressType());
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return PacketIdServer.LETTER_INFO_REQUEST;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketLetterInfoRequest msg) {
		buffer.writeUtf(msg.recipientName);
		buffer.writeUtf(PostalCarriers.REGISTRY.get().getKey(msg.addressType).toString());
	}

	public static PacketLetterInfoRequest decode(RegistryFriendlyByteBuf buffer) {
		return new PacketLetterInfoRequest(buffer.readUtf(), PostalCarriers.REGISTRY.get().getValue(ResourceLocation.tryParse(buffer.readUtf())));
	}
}
