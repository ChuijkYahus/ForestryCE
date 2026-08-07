package forestry.core.platform.network.packets;

import forestry.core.platform.network.PacketIdClient;
import forestry.core.platform.recipes.RecipeManagers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public class RecipeCachePacket implements CustomPacketPayload {
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return PacketIdClient.RECIPE_CACHE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, RecipeCachePacket msg) {
	}

	public static RecipeCachePacket decode(RegistryFriendlyByteBuf buffer) {
		return new RecipeCachePacket();
	}

	public static void handle(RecipeCachePacket msg, Player player) {
		RecipeManagers.invalidateCaches();
	}
}
