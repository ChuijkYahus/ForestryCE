package forestry.energy.screen;

import forestry.core.config.Constants;
import forestry.core.config.ForestryConfig;
import forestry.energy.menu.SolarEngineMenu;
import forestry.energy.tiles.SolarEngineTileEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class SolarEngineScreen extends EngineScreen<SolarEngineMenu, SolarEngineTileEntity> {
	public SolarEngineScreen(SolarEngineMenu menu, Inventory inv, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/solarengine.png", menu, inv, title, menu.getTile());
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
		super.renderBg(graphics, partialTicks, mouseX, mouseY);
		this.textLayout.startPage(graphics);
		this.textLayout.newLine();
		this.textLayout.newLine();
		Component component;
		int active = menu.getTile().activePanels;
		int total = menu.getTile().arraySize;
		if (active < total) {
			if (active == 0) {
				component = Component.literal(ChatFormatting.DARK_RED + "0/" + total);
			} else {
				component = Component.literal(ChatFormatting.GOLD + "" + active + "/" + total);
			}
		} else {
			component = Component.literal(ChatFormatting.GREEN + "" + active);
		}
		this.textLayout.drawLine(graphics, Component.translatable("for.gui.solar_array_size").append(component), 44);
		this.textLayout.newLine();
		if (active == 0) {
			component = Component.literal(ChatFormatting.DARK_RED + "???");
		} else {
			if (menu.getTile().getLevel().dimension().location().toString().equals("twilightforest:twilight_forest")) {
				int value = ForestryConfig.SERVER.twilightSolarRF.get() * 100 / ForestryConfig.SERVER.solarRF.get();
				component = Component.literal(getColor(value, 100) + "" + value + "%");
			} else {
				int dark = menu.getTile().darkening;
				if (dark < 7) {
					int value = 100 >> dark;
					component = Component.literal(getColor(value, 100) + "" + value + "%");
				} else
					component = Component.literal(ChatFormatting.DARK_RED + "0%");
			}
		}
		this.textLayout.drawLine(graphics, Component.translatable("for.gui.solar_efficiency").append(component), 44);
		if (menu.getTile().darkening < 7 && menu.getTile().activePanels > 0) {
			graphics.blit(this.textureFile, this.leftPos + 17, this.topPos + 38, 176, 0, 16, 16);
		}
	}

	private ChatFormatting getColor(int i, int y) {
		if (i > y) {
			return ChatFormatting.BLUE;
		}
		if (i == y) {
			return ChatFormatting.GREEN;
		}
		if (i <= 0) {
			return ChatFormatting.DARK_RED;
		}
		return ChatFormatting.GOLD;
	}
}
