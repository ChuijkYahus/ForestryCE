package forestry.energy.menu;

import forestry.core.gui.ContainerTile;
import forestry.core.network.packets.PacketGuiStream;
import forestry.core.tiles.TileUtil;
import forestry.energy.features.EnergyMenus;
import forestry.energy.tiles.SolarEngineTileEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class SolarEngineMenu extends ContainerTile<SolarEngineTileEntity> {
	public static SolarEngineMenu fromNetwork(int windowId, Inventory inv, FriendlyByteBuf extraData) {
		SolarEngineTileEntity tile = TileUtil.getTile(inv.player.level(), extraData.readBlockPos(), SolarEngineTileEntity.class);
		return new SolarEngineMenu(windowId, inv, tile);
	}

	public SolarEngineMenu(int id, Inventory player, SolarEngineTileEntity tile) {
		super(id, EnergyMenus.ENGINE_SOLAR.menuType(), player, tile, 8, 84);
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();
		PacketGuiStream packet = new PacketGuiStream(this.tile);
		sendPacketToListeners(packet);
	}
}
