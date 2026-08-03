package forestry.energy.screen;

import forestry.core.config.Constants;
import forestry.core.gui.widgets.SocketWidget;
import forestry.core.render.ColourProperties;
import forestry.energy.menu.CombustionEngineMenu;
import forestry.energy.menu.SolarEngineMenu;
import forestry.energy.tiles.SolarEngineBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

public class SolarEngineScreen extends EngineScreen<SolarEngineMenu, SolarEngineBlockEntity>{
	public SolarEngineScreen(SolarEngineMenu menu, Inventory inv, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/solar_engine.png", menu, inv, title, menu.getTile());

		this.widgetManager.add(new SocketWidget(this.widgetManager, 13,49, menu.getTile(), 0));
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
		super.renderBg(graphics, partialTicks, mouseX, mouseY);

		//player.displayClientMessage(
		// Component.literal(ChatFormatting.GREEN+"Solar Array Status: "+activePanels+"/"+array.size()+ChatFormatting.WHITE+" |
		// "+ChatFormatting.DARK_RED+"Current Output: "+(isRedstoneActivated()?currentOutput:0)+"/"+activePanels*ForestryConfig.SERVER.solarRF.get()/1000+"RF/t"),true);

		Component d1 = Component.translatable("for.gui.engine.tin.active_panel_count", this.engine.getActivePanelCount());
		graphics.drawString(this.font, d1, this.leftPos + 44, this.topPos + 22, ColourProperties.INSTANCE.get("gui.screen"), false);

		Component d2 = Component.translatable("for.gui.engine.tin.total_panel_count", this.engine.getPanelCount());
		graphics.drawString(this.font, d2, this.leftPos + 44, this.topPos + 32, ColourProperties.INSTANCE.get("gui.screen"), false);

		String mult = String.format(Locale.ROOT, "%.2f", SolarEngineBlockEntity.calculateMult(this.engine.getActivePanelCount(), this.engine))+'%';
		Component d3 = Component.translatable("for.gui.engine.tin.multiplier");
		graphics.drawString(this.font, d3, this.leftPos + 44, this.topPos + 52, ColourProperties.INSTANCE.get("gui.screen"), false);
		graphics.drawString(this.font, Component.literal(ChatFormatting.GREEN+mult), this.leftPos + 44, this.topPos + 62, ColourProperties.INSTANCE.get("gui.screen"), false);


		float total = (float) this.engine.getActivePanelCount() /this.engine.getPanelCount();

		//Sun decal
		if (this.engine.getPanelCount() > 0) {
			int progress = (int) Math.ceil((total * 16 - 0.05));
			graphics.blit(this.textureFile,
				this.leftPos + 14, this.topPos + 23, //Where to draw thing
				176, 0, //Coordinates of thing to be drawn
				progress, 16); //Width and Height of thing to be drawn
		}

	}
}
