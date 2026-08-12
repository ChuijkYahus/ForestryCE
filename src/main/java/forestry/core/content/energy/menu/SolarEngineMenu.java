package forestry.core.content.energy.menu;

import forestry.core.platform.gui.ContainerSocketed;
import forestry.core.platform.network.packets.PacketGuiStream;
import forestry.core.platform.tile.TileUtil;
import forestry.core.content.energy.features.EnergyMenus;
import forestry.core.content.energy.tiles.SolarEngineTileEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class SolarEngineMenu extends ContainerSocketed<SolarEngineTileEntity> {
	public static SolarEngineMenu fromNetwork(int windowId, Inventory inv, FriendlyByteBuf extraData) {
		SolarEngineTileEntity tile = TileUtil.getTile(inv.player.level(), extraData.readBlockPos(), SolarEngineTileEntity.class);
		return new SolarEngineMenu(windowId, inv, tile);
	}

	public SolarEngineMenu(int id, Inventory player, SolarEngineTileEntity tile) {
		super(id, EnergyMenus.ENGINE_SOLAR.menuType(), player, tile, 18, 80);
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();
		PacketGuiStream packet = new PacketGuiStream(this.tile);
		sendPacketToListeners(packet);
	}
}
