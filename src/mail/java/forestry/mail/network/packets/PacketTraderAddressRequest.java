package forestry.mail.network.packets;

import forestry.mail.network.MailPacketIds;

import forestry.core.platform.tile.TileUtil;
import forestry.mail.tradestation.TradeStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public record PacketTraderAddressRequest(BlockPos pos, String addressName) implements CustomPacketPayload {
	public PacketTraderAddressRequest(TradeStationBlockEntity tile, String addressName) {
		this(tile.getBlockPos(), addressName);
	}

	public static void handle(PacketTraderAddressRequest msg, ServerPlayer player) {
		TileUtil.actOnTile(player.level(), msg.pos(), TradeStationBlockEntity.class, tile -> {
			if (tile.handleSetAddressRequest(msg.addressName())) {
				player.openMenu(tile, msg.pos());
			}
		});
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return MailPacketIds.TRADING_ADDRESS_REQUEST;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketTraderAddressRequest msg) {
		buffer.writeBlockPos(msg.pos);
		buffer.writeUtf(msg.addressName);
	}

	public static PacketTraderAddressRequest decode(RegistryFriendlyByteBuf buffer) {
		return new PacketTraderAddressRequest(buffer.readBlockPos(), buffer.readUtf());
	}
}
