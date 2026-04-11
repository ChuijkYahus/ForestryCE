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
        this.widgetManager.add(new SocketWidget(this.widgetManager, 99, 21, this.tile, 0));
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseY, int mouseX) {
		super.renderBg(graphics, partialTicks, mouseY, mouseX);


		//Smelting progress bar
		int progress = this.tile.getProgressScaled(49);
		graphics.blit(this.textureFile,
			this.leftPos + 86, this.topPos + 39, //Where to draw thing
			176, 52, //Coordinates of thing to be drawn
			progress, 16); //Width and Height of thing to be drawn


		//Heat Bar
		int heatScaled = this.tile.getHeatScaled(52);
		if (heatScaled > 0) {
			graphics.blit(this.textureFile,
				this.leftPos + 12, this.topPos + 21 + 52 - heatScaled, //Where to draw thing
				176, 52 - heatScaled, //Coordinates of thing to be drawn
				4, heatScaled); //Width and Height of thing to be drawn
		}


		int meltingPointScaled = this.tile.getMeltingPointScaled(52);
		if (meltingPointScaled > 0) {
			graphics.blit(this.textureFile,
				this.leftPos + 9, this.topPos + 19 + 52 - meltingPointScaled, //Where to draw thing
				180, 0, //Coordinates of thing to be drawn
				10, 5); //Width and Height of thing to be drawn
		}

			//Status Indicator. Kinda redundant but it's a nice visual indicator
		if (this.tile.getHeat() >= this.tile.getMeltingPoint()
			&& this.tile.getMeltingPoint() > 0){
			graphics.blit(this.textureFile,
				this.leftPos + 100, this.topPos + 58, //Where to draw thing
				176, 68, //Coordinates of thing to be drawn
				14, 14); //Width and Height of thing to be drawn
		}

	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);

		this.renderTooltip(guiGraphics, mouseX, mouseY);

		boolean mouseOverHeatingBar = (
			mouseX >= this.leftPos + 11
				&& mouseX <= this.leftPos + 17
			&& mouseY >= this.topPos + 20
			&& mouseY <= this.topPos + 74
		);

		if (mouseOverHeatingBar) {

			List<Component> messages;
			if(this.tile.getMeltingPoint() > 0)
				messages = List.of(
					Component.translatable("for.gui.smelter.heat", tile.getHeat(), TileSmelter.MAX_HEAT),
					Component.translatable("for.gui.smelter.requiredHeat", tile.getMeltingPoint()));
			else
				messages = List.of(Component.translatable("for.gui.smelter.heat", tile.getHeat(), TileSmelter.MAX_HEAT));

			guiGraphics.renderTooltip(
				this.font,
				messages,
				Optional.empty(),
				mouseX,
				mouseY
			);
		}
	}
	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
		addHintLedger("smelter");
		addPowerLedger(this.tile.getEnergyManager());
	}
}
