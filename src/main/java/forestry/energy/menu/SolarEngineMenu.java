package forestry.energy.menu;

import forestry.core.gui.ContainerSocketed;
import forestry.core.gui.slots.SlotFiltered;
import forestry.core.gui.slots.SlotOutput;
import forestry.core.network.packets.PacketGuiStream;
import forestry.core.tiles.TileUtil;
import forestry.energy.features.EnergyMenus;
import forestry.energy.tiles.PeatEngineBlockEntity;
import forestry.energy.tiles.SolarEngineBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class SolarEngineMenu extends ContainerSocketed<SolarEngineBlockEntity> {
	public static SolarEngineMenu fromNetwork(int windowId, Inventory inv, FriendlyByteBuf extraData) {
		SolarEngineBlockEntity tile = TileUtil.getTile(inv.player.level(), extraData.readBlockPos(), SolarEngineBlockEntity.class);
		return new SolarEngineMenu(windowId, inv, tile);
	}

	public SolarEngineMenu(int id, Inventory player, SolarEngineBlockEntity tile) {
		super(id, EnergyMenus.ENGINE_SOLAR.menuType(), player, tile, 8, 80);
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();
		PacketGuiStream packet = new PacketGuiStream(this.tile);
		sendPacketToListeners(packet);
	}
}
