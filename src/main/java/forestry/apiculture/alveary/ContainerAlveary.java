package forestry.apiculture.alveary;

import forestry.api.ForestryConstants;
import forestry.apiculture.features.ApicultureMenuTypes;
import forestry.apiculture.alveary.multiblock.TileAlveary;
import forestry.core.platform.advancements.AdvancementHelper;
import forestry.core.platform.gui.ContainerTile;
import forestry.core.platform.network.packets.PacketGuiStream;
import forestry.core.platform.tile.TileUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import forestry.apiculture.bees.ContainerBeeHelper;

public class ContainerAlveary extends ContainerTile<TileAlveary> {
	private static final ResourceLocation OPEN_ALVEARY_UI = ForestryConstants.forestry("get_alveary");

	public static ContainerAlveary fromNetwork(int windowId, Inventory inv, FriendlyByteBuf data) {
		TileAlveary tile = TileUtil.getTile(inv.player.level(), data.readBlockPos(), TileAlveary.class);
		return new ContainerAlveary(windowId, inv, tile);
	}

	public ContainerAlveary(int windowid, Inventory playerInv, TileAlveary tile) {
		super(windowid, ApicultureMenuTypes.ALVEARY.menuType(), playerInv, tile, 8, 108);
		ContainerBeeHelper.addSlots(this, tile, false);

		Player player = playerInv.player;
		if (player != null) {
			AdvancementHelper.tryUnlock(player, OPEN_ALVEARY_UI);
		}

		tile.getBeekeepingLogic().clearCachedValues();
	}

	private int beeProgress = -1;

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();

		int beeProgress = this.tile.getBeekeepingLogic().getBeeProgressPercent();
		if (this.beeProgress != beeProgress) {
			this.beeProgress = beeProgress;
			PacketGuiStream packet = new PacketGuiStream(this.tile);
			sendPacketToListeners(packet);
		}
	}
}
