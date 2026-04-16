package forestry.core.gui;

import forestry.core.config.Constants;
import forestry.core.gui.widgets.TankWidget;
import forestry.core.render.EnumTankLevel;
import forestry.core.tiles.TileAnalyzer;
import forestry.core.tiles.TileBurnBarrel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

// The block form of the analyzer
public class GuiBurnBarrel extends GuiForestryTitled<ContainerBurnBarrel> {
	private final TileBurnBarrel tile;

	public GuiBurnBarrel(ContainerBurnBarrel burnBarrel, Inventory inventory, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/burn_barrel.png", burnBarrel, inventory, title);
		this.tile = burnBarrel.tile;
		this.imageHeight = 202;
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseY, int mouseX) {
		super.renderBg(graphics, partialTicks, mouseY, mouseX);

		//Display the progress arrows
		if (this.tile.getAshProductionTimer() > 0){
			int ashProgress = this.tile.getValueScaled(
				this.tile.getAshProductionTimer(),
				TileBurnBarrel.ASH_PRODUCTION_TIME,
				18
			);
			graphics.blit(this.textureFile,
				this.leftPos + 68, this.topPos + 65, //Where to draw thing
				176, 14, //Coordinates of thing to be drawn
				38, ashProgress); //Width and Height of thing to be drawn
		}

		//Display the burning flame icon
		if (this.tile.getBurnTime() > 0){
			int burnTimeRemaining = this.tile.getValueScaled(
				this.tile.getBurnTime(),
				this.tile.getCurrentMaxBurnTime(),
				14
			);
			burnTimeRemaining = Math.min(burnTimeRemaining+1, 14);
			graphics.blit(this.textureFile,
				this.leftPos + 80, this.topPos + 67 + (14-burnTimeRemaining), //Where to draw thing
				176, 14-burnTimeRemaining, //Coordinates of thing to be drawn
				14, burnTimeRemaining); //Width and Height of thing to be drawn
		}

	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
		addHintLedger("burn_barrel");
	}
}
