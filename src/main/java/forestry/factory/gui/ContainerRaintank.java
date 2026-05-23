package forestry.factory.gui;

import forestry.core.gui.ContainerLiquidTanks;
import forestry.core.gui.slots.SlotEmptyLiquidContainerIn;
import forestry.core.gui.slots.SlotOutput;
import forestry.core.tiles.TileUtil;
import forestry.factory.features.FactoryMenuTypes;
import forestry.factory.inventory.InventoryRaintank;
import forestry.factory.tiles.TileRaintank;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.SimpleContainerData;

import java.util.ArrayList;
import java.util.List;

public class ContainerRaintank extends ContainerLiquidTanks<TileRaintank> {
	private final List<ContainerListener> trackedListeners = new ArrayList<>();

	@Override
	public void addSlotListener(ContainerListener listener) {
		super.addSlotListener(listener);
		if (!this.trackedListeners.contains(listener)) {
			this.trackedListeners.add(listener);
		}
	}

	@Override
	public void removeSlotListener(ContainerListener listener) {
		super.removeSlotListener(listener);
		this.trackedListeners.remove(listener);
	}

	public static ContainerRaintank fromNetwork(int windowId, Inventory inv, FriendlyByteBuf data) {
		TileRaintank tile = TileUtil.getTile(inv.player.level(), data.readBlockPos(), TileRaintank.class);
		return new ContainerRaintank(windowId, inv, tile);
	}

	public ContainerRaintank(int windowId, Inventory player, TileRaintank tile) {
		super(windowId, FactoryMenuTypes.RAINTANK.menuType(), player, tile, 8, 84);
		addDataSlots(new SimpleContainerData(1));

		this.addSlot(new SlotEmptyLiquidContainerIn(this.tile, InventoryRaintank.SLOT_RESOURCE, 116, 19));
		this.addSlot(new SlotOutput(this.tile, InventoryRaintank.SLOT_PRODUCT, 116, 55));
	}

	@Override
	public void setData(int messageId, int data) {
		super.setData(messageId, data);

        this.tile.getGUINetworkData(messageId, data);
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();

		for (ContainerListener crafter : this.trackedListeners) {
            this.tile.sendGUINetworkData(this, crafter);
		}
	}
}
