package forestry.energy.screen;

import forestry.core.config.Constants;
import forestry.core.gui.widgets.SocketWidget;
import forestry.core.render.ColourProperties;
import forestry.energy.menu.SolarEngineMenu;
import forestry.energy.tiles.SolarEngineTileEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

public class SolarEngineScreen extends EngineScreen<SolarEngineMenu, SolarEngineTileEntity> {
	/** Position of the sun/moon decal in the GUI texture, immediately right of the 196px wide background. */
	private static final int DECAL_U = 196;
	private static final int DECAL_SUN_V = 0;
	private static final int DECAL_MOON_V = 16;
	private static final int DECAL_SIZE = 16;

	public SolarEngineScreen(SolarEngineMenu menu, Inventory inv, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/solar_engine.png", menu, inv, title, menu.getTile());

		this.widgetManager.add(new SocketWidget(this.widgetManager, 14, 50, menu.getTile(), 0));
		this.imageWidth = 196;
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
		super.renderBg(graphics, partialTicks, mouseX, mouseY);

		int active = this.engine.getActivePanelCount();
		int total = this.engine.getPanelCount();
		int textColour = ColourProperties.INSTANCE.get("gui.screen");

		graphics.drawString(this.font, Component.translatable("for.gui.engine.tin.total_panel_count", total), this.leftPos + 48, this.topPos + 21, textColour, false);

		if (active < total) {
			Component obscured = Component.translatable("for.gui.engine.tin.obscured_panel_count", total - active).withStyle(ChatFormatting.RED);
			graphics.drawString(this.font, obscured, this.leftPos + 48, this.topPos + 32, textColour, false);
		}

		// How much of a panel's rated output the current sky is delivering.
		double insolation = SolarEngineTileEntity.insolation(this.engine.getLevel(), this.engine.getSkyDarken()) * 100.0;
		Component insolationLine = Component.translatable("for.gui.engine.tin.insolation",
			Component.literal(formatPercent(insolation)).withStyle(percentColour(insolation)));
		graphics.drawString(this.font, insolationLine, this.leftPos + 48, this.topPos + 47, textColour, false);

		// How much the size of the array multiplies that output by.
		double multiplier = SolarEngineTileEntity.calculateMult(this.engine.getLevel(), active);
		Component multiplierLine = Component.translatable("for.gui.engine.tin.efficiency",
			Component.literal(String.format(Locale.ROOT, "%.2f", multiplier)).withStyle(multiplier > 1.0 ? ChatFormatting.GREEN : ChatFormatting.GOLD));
		graphics.drawString(this.font, multiplierLine, this.leftPos + 48, this.topPos + 58, textColour, false);

		// Sun/moon decal. The sun fills in from the left in proportion to how many panels are lit.
		if (insolation > 0.0) {
			if (total > 0) {
				int progress = (int) Math.ceil((float) active / total * DECAL_SIZE - 0.05f);
				graphics.blit(this.textureFile, this.leftPos + 14, this.topPos + 23, DECAL_U, DECAL_SUN_V, progress, DECAL_SIZE);
			}
		} else {
			graphics.blit(this.textureFile, this.leftPos + 14, this.topPos + 23, DECAL_U, DECAL_MOON_V, DECAL_SIZE, DECAL_SIZE);
		}
	}

	/**
	 * Formats a percentage to at most two decimal places, dropping trailing zeros so a full
	 * strength sky reads "100%" while a nearly dark one still reads "0.78%" instead of "0%".
	 */
	private static String formatPercent(double percent) {
		String formatted = String.format(Locale.ROOT, "%.2f", percent);
		if (formatted.indexOf('.') >= 0) {
			formatted = formatted.replaceAll("0+$", "").replaceAll("\\.$", "");
		}
		return formatted + "%";
	}

	private static ChatFormatting percentColour(double percent) {
		if (percent > 100.0) {
			return ChatFormatting.BLUE;
		}
		if (percent == 100.0) {
			return ChatFormatting.GREEN;
		}
		if (percent <= 0.0) {
			return ChatFormatting.DARK_RED;
		}
		return ChatFormatting.GOLD;
	}
}
