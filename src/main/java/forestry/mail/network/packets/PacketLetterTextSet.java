package forestry.mail.network.packets;

import forestry.core.network.PacketIdServer;
import forestry.mail.gui.LetterMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public record PacketLetterTextSet(String string) implements CustomPacketPayload {
	public static void handle(PacketLetterTextSet msg, ServerPlayer player) {
		if (player.containerMenu instanceof LetterMenu letterMenu) {
			letterMenu.handleSetText(msg.string());
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return PacketIdServer.LETTER_TEXT_SET;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketLetterTextSet msg) {
		buffer.writeUtf(msg.string);
	}

	public static PacketLetterTextSet decode(RegistryFriendlyByteBuf buffer) {
		return new PacketLetterTextSet(buffer.readUtf());
	}
}
