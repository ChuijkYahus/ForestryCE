package forestry.factory.network.packets;

import forestry.core.network.PacketIdClient;
import forestry.core.tiles.TileUtil;
import forestry.core.utils.NetworkUtil;
import forestry.factory.tiles.TileCarpenter;
import forestry.factory.tiles.TileFabricator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public record PacketRecipeTransferUpdate(BlockPos pos,
										 NonNullList<ItemStack> craftingInventory) implements CustomPacketPayload {
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return PacketIdClient.RECIPE_TRANSFER_UPDATE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketRecipeTransferUpdate msg) {
		buffer.writeBlockPos(msg.pos);
		NetworkUtil.writeItemStacks(buffer, msg.craftingInventory);
	}

	public static PacketRecipeTransferUpdate decode(RegistryFriendlyByteBuf buffer) {
		return new PacketRecipeTransferUpdate(buffer.readBlockPos(), NetworkUtil.readItemStacks(buffer));
	}

	public static void handle(PacketRecipeTransferUpdate msg, Player player) {
		BlockEntity tile = TileUtil.getTile(player.level(), msg.pos);
		if (tile instanceof TileCarpenter carpenter) {
			int index = 0;
			for (ItemStack stack : msg.craftingInventory) {
				carpenter.getCraftingInventory().setItem(index, stack);
				index++;
			}
		} else if (tile instanceof TileFabricator fabricator) {
			int index = 0;
			for (ItemStack stack : msg.craftingInventory) {
				fabricator.getCraftingInventory().setItem(index, stack);
				index++;
			}
		}
	}
}
