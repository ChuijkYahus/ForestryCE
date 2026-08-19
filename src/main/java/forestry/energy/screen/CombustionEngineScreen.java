package forestry.energy.screen;

import forestry.core.config.Constants;
import forestry.core.gui.widgets.SocketWidget;
import forestry.core.gui.widgets.TankWidget;
import forestry.energy.menu.CombustionEngineMenu;
import forestry.energy.tiles.CombustionEngineBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CombustionEngineScreen extends EngineScreen<CombustionEngineMenu, CombustionEngineBlockEntity> {
	public CombustionEngineScreen(CombustionEngineMenu menu, Inventory inv, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/fuelengine.png", menu, inv, title, menu.getTile());

		this.widgetManager.add(new TankWidget(this.widgetManager, 53, 19, 0));
		this.widgetManager.add(new TankWidget(this.widgetManager, 107, 19, 1));

		this.widgetManager.add(new BiogasSlot(this.widgetManager, 80, 53, 2));
		this.widgetManager.add(new BiogasSlot(this.widgetManager, 80, 27, 3));

		this.widgetManager.add(new SocketWidget(this.widgetManager, 26, 40, menu.getTile(), 0));
	}
}
