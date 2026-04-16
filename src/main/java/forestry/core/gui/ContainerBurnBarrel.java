package forestry.core.gui;

import forestry.core.blocks.BlockBurnBarrel;
import forestry.core.features.CoreMenuTypes;
import forestry.core.gui.slots.SlotFiltered;
import forestry.core.gui.slots.SlotLiquidIn;
import forestry.core.gui.slots.SlotOutput;
import forestry.core.gui.slots.SlotWorking;
import forestry.core.inventory.InventoryAnalyzer;
import forestry.core.inventory.InventoryBurnBarrel;
import forestry.core.network.packets.PacketGuiStream;
import forestry.core.tiles.TileAnalyzer;
import forestry.core.tiles.TileBurnBarrel;
import forestry.core.tiles.TileUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SimpleContainerData;

public class ContainerBurnBarrel extends ContainerTile<TileBurnBarrel> {
	public static ContainerBurnBarrel fromNetwork(int windowId, Inventory playerInv, FriendlyByteBuf extraData) {
		TileBurnBarrel burnBarrel = TileUtil.getTile(playerInv.player.level(), extraData.readBlockPos(), TileBurnBarrel.class);
		return new ContainerBurnBarrel(windowId, playerInv, burnBarrel);
	}

	int lastAshProgress = 0;
	int lastBurnTime = 0;
	int lastMaxBurnTime = 0;

	private static int[] inputXCoords = {70, 88, 70, 88};
	private static int[] inputYCoords = {24, 24, 42, 42};
	private static int[] outputXCoords = {61, 79, 97};

	public ContainerBurnBarrel(int windowId, Inventory player, TileBurnBarrel tile) {
		super(windowId, CoreMenuTypes.BURN_BARREL.menuType(), player, tile, 8, 120);
		addDataSlots(new SimpleContainerData(8));

		// Input buffer
		for (int i = 0; i < inputXCoords.length; i++){
			this.addSlot(new SlotFiltered(tile,
				InventoryBurnBarrel.SLOT_INPUT_1 + i,
				inputXCoords[i],
				inputYCoords[i]));
			//sure it's probably better to just use math but that's why it's static.
		}

		// Output buffer
		for (int i = 0; i < outputXCoords.length; i++){
			this.addSlot(new SlotFiltered(tile,
				InventoryBurnBarrel.SLOT_OUTPUT_1 + i,
				outputXCoords[i],
				90));
		}

	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();

		int currentAshProgress = this.tile.getAshProductionTimer();
		int currentBurnTime = this.tile.getBurnTime();
		int currentMaxBurnTime = this.tile.getCurrentMaxBurnTime();

		if (currentAshProgress != this.lastAshProgress
			|| currentBurnTime != this.lastBurnTime
			|| currentMaxBurnTime != this.lastMaxBurnTime){
			this.lastAshProgress = currentAshProgress;
			this.lastBurnTime = currentBurnTime;
			this.lastMaxBurnTime = currentMaxBurnTime;
			PacketGuiStream packet = new PacketGuiStream(this.tile);
			sendPacketToListeners(packet);
		}
	}
}
