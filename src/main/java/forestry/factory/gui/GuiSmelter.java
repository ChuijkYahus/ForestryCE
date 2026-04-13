package forestry.factory.gui;

import forestry.Forestry;
import forestry.core.config.Constants;
import forestry.core.gui.GuiForestryTitled;
import forestry.core.gui.widgets.SocketWidget;
import forestry.factory.tiles.TileCentrifuge;
import forestry.factory.tiles.TileSmelter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

public class GuiSmelter extends GuiForestryTitled<ContainerSmelter> {
	private final TileSmelter tile;

	public GuiSmelter(ContainerSmelter container, Inventory inventory, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/smelter.png", container, inventory, title);
		this.tile = container.getTile();
        this.widgetManager.add(new SocketWidget(this.widgetManager, 95, 21, this.tile, 0));
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseY, int mouseX) {
		super.renderBg(graphics, partialTicks, mouseY, mouseX);


		//Smelting progress bar
		int progress = this.tile.getProgressScaled(50);
		graphics.blit(this.textureFile,
			this.leftPos + 81, this.topPos + 39, //Where to draw thing
			176, 52, //Coordinates of thing to be drawn
			progress, 16); //Width and Height of thing to be drawn

			//Status Indicator. Kinda redundant but it's a nice visual indicator
		if (!this.tile.getErrorLogic().hasErrors()){
			graphics.blit(this.textureFile,
				this.leftPos + 96, this.topPos + 58, //Where to draw thing
				176, 68, //Coordinates of thing to be drawn
				14, 14); //Width and Height of thing to be drawn
		}

	}
	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
		addHintLedger("smelter");
		addPowerLedger(this.tile.getEnergyManager());
	}
}
