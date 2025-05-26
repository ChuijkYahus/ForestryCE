package forestry.sorting.network.packets;

import forestry.api.ForestryCapabilities;
import forestry.api.IForestryApi;
import forestry.api.genetics.filter.IFilterRuleType;
import forestry.api.modules.IForestryPacketServer;
import forestry.core.network.PacketIdServer;
import forestry.core.tiles.TileUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

public record PacketFilterChangeRule(BlockPos pos, Direction facing,
									 IFilterRuleType rule) implements IForestryPacketServer {
	@Override
	public ResourceLocation id() {
		return PacketIdServer.FILTER_CHANGE_RULE;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeBlockPos(this.pos);
		buffer.writeShort(this.facing.get3DDataValue());
		buffer.writeShort(IForestryApi.INSTANCE.getFilterManager().getId(this.rule));
	}

	public static PacketFilterChangeRule decode(FriendlyByteBuf buffer) {
		return new PacketFilterChangeRule(buffer.readBlockPos(), Direction.VALUES[buffer.readShort()], Objects.requireNonNull(IForestryApi.INSTANCE.getFilterManager().getRule(buffer.readShort())));
	}

	public static void handle(PacketFilterChangeRule msg, ServerPlayer player) {
		TileUtil.getInterface(player.level(), msg.pos(), ForestryCapabilities.FILTER_LOGIC, null).ifPresent(logic -> {
			if (logic.setRule(msg.facing(), msg.rule())) {
				logic.getNetworkHandler().sendToPlayers(logic, player.serverLevel(), player);
			}
		});
	}
}
