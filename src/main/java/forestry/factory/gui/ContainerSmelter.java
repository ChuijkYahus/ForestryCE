package forestry.factory.gui;

import forestry.core.gui.ContainerSocketed;
import forestry.core.gui.slots.SlotFiltered;
import forestry.core.gui.slots.SlotLocked;
import forestry.core.gui.slots.SlotOutput;
import forestry.core.network.packets.PacketItemStackDisplay;
import forestry.core.tiles.TileUtil;
import forestry.factory.features.FactoryMenuTypes;
import forestry.factory.tiles.TileCentrifuge;
import forestry.factory.tiles.TileSmelter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ContainerSmelter extends ContainerSocketed<TileSmelter> {
	private ItemStack oldCraftPreview = ItemStack.EMPTY;

	public static ContainerSmelter fromNetwork(int windowId, Inventory inv, FriendlyByteBuf data) {
		TileSmelter tile = TileUtil.getTile(inv.player.level(), data.readBlockPos(), TileSmelter.class);
		return new ContainerSmelter(windowId, inv, tile);
	}

	public ContainerSmelter(int windowId, Inventory player, TileSmelter tile) {
		super(windowId, FactoryMenuTypes.SMELTER.menuType(), player, tile, 8, 84);

		// Resources
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 3; x++) {
				this.addSlot(new SlotFiltered(this.tile,
					0 + y + x * 3, //Slot Index
					26 + x * 18, //x Pos
					21 + y * 18)); //Y Pos
			}
		}

		// Craft Preview display
		this.addSlot(new SlotLocked(this.tile.getCraftPreviewInventory(), 9, 99, 39));

		// Product Inventory
		this.addSlot(new SlotOutput(this.tile, 10, 143, 39));
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();

		Container craftPreviewInventory = this.tile.getCraftPreviewInventory();

		ItemStack newCraftPreview = craftPreviewInventory.getItem(0);
		if (!ItemStack.matches(this.oldCraftPreview, newCraftPreview)) {
            this.oldCraftPreview = newCraftPreview;

			PacketItemStackDisplay packet = new PacketItemStackDisplay(this.tile, newCraftPreview);
			sendPacketToListeners(packet);
		}
	}
}
