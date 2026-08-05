package forestry.energy.screen;

import forestry.Forestry;
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
		this.imageWidth = 196;
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
		super.renderBg(graphics, partialTicks, mouseX, mouseY);

		//player.displayClientMessage(
		// Component.literal(ChatFormatting.GREEN+"Solar Array Status: "+activePanels+"/"+array.size()+ChatFormatting.WHITE+" |
		// "+ChatFormatting.DARK_RED+"Current Output: "+(isRedstoneActivated()?currentOutput:0)+"/"+activePanels*ForestryConfig.SERVER.solarRF.get()/1000+"RF/t"),true);

		Component d1 = Component.translatable("for.gui.engine.tin.total_panel_count", this.engine.getPanelCount());
		graphics.drawString(this.font, d1, this.leftPos + 48, this.topPos + 27, ColourProperties.INSTANCE.get("gui.screen"), false);

		if (this.engine.getActivePanelCount() < this.engine.getPanelCount()){
			Component d2 = Component.translatable("for.gui.engine.tin.obscured_panel_count", (this.engine.getPanelCount()-this.engine.getActivePanelCount()));
			graphics.drawString(this.font, ChatFormatting.RED + d2.getString(), this.leftPos + 48, this.topPos + 37, ColourProperties.INSTANCE.get("gui.screen"), false);
		}

		double numMult = SolarEngineBlockEntity.calculateMult(this.engine.getActivePanelCount(), this.engine);
		String mult = String.format(Locale.ROOT, "%.2f", numMult);

		Component d3 = Component.translatable("for.gui.engine.tin.efficiency", mult);

		graphics.drawString(this.font, d3, this.leftPos + 48, this.topPos + 56, ColourProperties.INSTANCE.get("gui.screen"), false);


		float total = (float) this.engine.getActivePanelCount() /this.engine.getPanelCount();

		//Sun/Moon decal
		boolean isTwilight = this.engine.getLevel().dimension().location().toString().equals("twilightforest:twilight_forest");
		if (this.engine.getCacheSkyDarkness() <= 7) {
			if (this.engine.getPanelCount() > 0) {
				int progress = (int) Math.ceil((total * 16 - 0.05));
				graphics.blit(this.textureFile,
					this.leftPos + 14, this.topPos + 23, //Where to draw thing
					196, 0, //Coordinates of thing to be drawn
					progress, 16); //Width and Height of thing to be drawn
			}
		}
		else {
			graphics.blit(this.textureFile,
				this.leftPos + 14, this.topPos + 23, //Where to draw thing
				196, 16, //Coordinates of thing to be drawn
				16, 16); //Width and Height of thing to be drawn
		}

	}
}
