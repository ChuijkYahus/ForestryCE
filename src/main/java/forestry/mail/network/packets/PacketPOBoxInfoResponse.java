package forestry.mail.network.packets;

import forestry.core.config.ForestryConfig;
import forestry.core.network.PacketIdClient;
import forestry.mail.carriers.players.POBoxInfo;
import forestry.mail.gui.ToastMailboxInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record PacketPOBoxInfoResponse(int playerLetters, int tradeLetters,
									  boolean silent) implements CustomPacketPayload {
	public PacketPOBoxInfoResponse(POBoxInfo info, boolean silent) {
		this(info.playerLetters(), info.tradeLetters(), silent);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return PacketIdClient.POBOX_INFO_RESPONSE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketPOBoxInfoResponse msg) {
		buffer.writeInt(msg.playerLetters);
		buffer.writeInt(msg.tradeLetters);
		buffer.writeBoolean(msg.silent);
	}

	public static PacketPOBoxInfoResponse decode(RegistryFriendlyByteBuf buffer) {
		return new PacketPOBoxInfoResponse(buffer.readInt(), buffer.readInt(), buffer.readBoolean());
	}

	public static void handle(PacketPOBoxInfoResponse msg, Player player) {
		POBoxInfo poBox = new POBoxInfo(msg.playerLetters, msg.tradeLetters);
		if (player.equals(Minecraft.getInstance().player) && ForestryConfig.CLIENT.mailAlertsEnabled.get()) {
			ToastMailboxInfo.addOrUpdate(Minecraft.getInstance().getToasts(), poBox, msg.silent);
		}
	}
}
