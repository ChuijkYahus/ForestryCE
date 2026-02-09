package forestry.mail.network.packets;

import forestry.api.mail.IMailAddress;
import forestry.core.network.PacketIdClient;
import forestry.core.tiles.TileUtil;
import forestry.mail.MailAddress;
import forestry.mail.tiles.TileTrader;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record PacketTraderAddressResponse(BlockPos pos, IMailAddress address) implements CustomPacketPayload {
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return PacketIdClient.TRADING_ADDRESS_RESPONSE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketTraderAddressResponse msg) {
		buffer.writeBlockPos(msg.pos);
		buffer.writeUtf(msg.address.getName());
	}

	public static PacketTraderAddressResponse decode(RegistryFriendlyByteBuf buffer) {
		return new PacketTraderAddressResponse(buffer.readBlockPos(), new MailAddress(buffer.readUtf()));
	}

	public static void handle(PacketTraderAddressResponse msg, Player player) {
		TileUtil.actOnTile(player.level(), msg.pos, TileTrader.class, tile -> tile.setAddress(msg.address));
	}
}
