package forestry.sorting.network.packets;

import forestry.api.ForestryCapabilities;
import forestry.api.core.genetics.filter.IFilterRuleType;
import forestry.core.network.PacketIdClient;
import forestry.core.tiles.TileUtil;
import forestry.sorting.AlleleFilter;
import forestry.sorting.FilterLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record PacketGuiFilterUpdate(BlockPos pos, IFilterRuleType[] filterRules,
									AlleleFilter[][] genomeFilter) implements CustomPacketPayload {
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return PacketIdClient.GUI_UPDATE_FILTER;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketGuiFilterUpdate msg) {
		buffer.writeBlockPos(msg.pos);
		FilterLogic.writeFilterRules(buffer, msg.filterRules);
		FilterLogic.writeGenomeFilters(buffer, msg.genomeFilter);
	}

	public static PacketGuiFilterUpdate decode(RegistryFriendlyByteBuf buffer) {
		return new PacketGuiFilterUpdate(buffer.readBlockPos(), FilterLogic.readFilterRules(buffer), FilterLogic.readGenomeFilters(buffer));
	}

	public static void handle(PacketGuiFilterUpdate msg, Player player) {
		TileUtil.getInterface(player.level(), msg.pos(), ForestryCapabilities.FILTER_LOGIC, null).ifPresent(l -> {
			if (l instanceof FilterLogic logic) {
				logic.readGuiUpdatePacket(msg);
			}
		});
	}
}
