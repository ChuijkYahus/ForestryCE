package forestry.energy.menu;

import forestry.core.gui.ContainerLiquidTanks;
import forestry.core.gui.ContainerLiquidTanksSocketed;
import forestry.core.gui.slots.SlotLiquidIn;
import forestry.core.network.packets.PacketGuiStream;
import forestry.core.tiles.TileUtil;
import forestry.energy.features.EnergyMenus;
import forestry.energy.inventory.InventoryEngineCombustion;
import forestry.energy.tiles.CombustionEngineTileEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class CombustionEngineMenu extends ContainerLiquidTanksSocketed<CombustionEngineTileEntity> {
	public static CombustionEngineMenu fromNetwork(int windowId, Inventory inv, FriendlyByteBuf extraData) {
		CombustionEngineTileEntity tile = TileUtil.getTile(inv.player.level(), extraData.readBlockPos(), CombustionEngineTileEntity.class);
		return new CombustionEngineMenu(windowId, inv, tile);
	}

	public CombustionEngineMenu(int windowId, Inventory player, CombustionEngineTileEntity engine) {
		super(windowId, EnergyMenus.ENGINE_COMBUSTION.menuType(), player, engine, 8, 84);

		this.addSlot(new SlotLiquidIn(engine, InventoryEngineCombustion.SLOT_CAN, 134, 40));
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();
		PacketGuiStream packet = new PacketGuiStream(this.tile);
		sendPacketToListeners(packet);
	}
}
