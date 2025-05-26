package forestry.mail.network.packets;

import com.mojang.authlib.GameProfile;
import forestry.api.mail.IMailAddress;
import forestry.api.modules.IForestryPacketClient;
import forestry.core.network.PacketIdClient;
import forestry.mail.MailAddress;
import forestry.mail.carriers.PostalCarriers;
import forestry.mail.gui.ILetterInfoReceiver;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record PacketLetterInfoResponsePlayer(IMailAddress address) implements IForestryPacketClient {
	@Override
	public ResourceLocation id() {
		return PacketIdClient.LETTER_INFO_RESPONSE_PLAYER;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		GameProfile profile = this.address.getPlayerProfile();
		buffer.writeUUID(profile.getId());
		buffer.writeUtf(profile.getName());
	}

	public static PacketLetterInfoResponsePlayer decode(FriendlyByteBuf buffer) {
		return new PacketLetterInfoResponsePlayer(new MailAddress(new GameProfile(buffer.readUUID(), buffer.readUtf())));
	}

	public static void handle(PacketLetterInfoResponsePlayer msg, Player player) {
		if (player.containerMenu instanceof ILetterInfoReceiver receiver) {
			receiver.handleLetterInfoUpdate(PostalCarriers.PLAYER.get(), msg.address, null);
		}
	}
}
