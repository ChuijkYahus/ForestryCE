package forestry.apiculture.alveary;

import forestry.apiculture.alveary.multiblock.TileAlvearySwarmer;
import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.GuiForestryTitled;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import forestry.apiculture.alveary.ContainerAlvearySwarmer;

public class GuiAlvearySwarmer extends GuiForestryTitled<ContainerAlvearySwarmer> {
	private final TileAlvearySwarmer tile;

	public GuiAlvearySwarmer(ContainerAlvearySwarmer container, Inventory inventory, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/swarmer.png", container, inventory, title);
		this.tile = container.getTile();
	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
	}
}
