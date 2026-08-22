package forestry.mail.carriers.players;

import forestry.api.ForestryConstants;
import forestry.api.client.IForestryClientApi;
import forestry.api.mail.IMailAddress;
import forestry.api.mail.IPostOffice;
import forestry.api.mail.IPostalCarrier;
import forestry.api.mail.IPostalState;
import forestry.core.platform.util.NetworkUtil;
import forestry.core.platform.util.PlayerUtil;
import forestry.mail.letters.MailAddress;
import forestry.mail.network.packets.PacketPOBoxInfoResponse;
import forestry.mail.letters.EnumDeliveryState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class PlayerPostalCarrier implements IPostalCarrier {
	private final ResourceLocation iconID;

	public PlayerPostalCarrier() {
		this.iconID = ForestryConstants.forestry("mail/carrier.player");
	}

	@Override
	public String getTranslationKey() {
		return "for.gui.addressee.player";
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public TextureAtlasSprite getSprite() {
		return IForestryClientApi.INSTANCE.getTextureManager().getSprite(this.iconID);
	}

	@Override
	public IPostalState deliverLetter(ServerLevel level, IPostOffice office, IMailAddress recipient, ItemStack letterStack, boolean doDeliver) {
		POBox pobox = POBoxRegistry.getOrCreate(level).getOrCreatePOBox(recipient);

		if (!pobox.storeLetter(letterStack.copy())) {
			return EnumDeliveryState.MAILBOX_FULL;
		} else {
			Player player = PlayerUtil.getPlayer(level, recipient.getPlayerProfile());
			if (player instanceof ServerPlayer) {
				NetworkUtil.sendToPlayer(new PacketPOBoxInfoResponse(pobox.getPOBoxInfo(), false), (ServerPlayer) player);
			}
		}

		return EnumDeliveryState.OK;
	}

	@Override
	public IMailAddress getRecipient(MinecraftServer minecraftServer, String recipientName) {
		return minecraftServer.getProfileCache().get(recipientName).map(MailAddress::new).orElse(MailAddress.INVALID);
	}

	@Override
	public String toString() {
		return "player";
	}
}
