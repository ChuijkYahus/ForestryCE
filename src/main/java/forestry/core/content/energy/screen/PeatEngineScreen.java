package forestry.core.content.energy.screen;

import forestry.core.platform.config.Constants;
import forestry.core.content.energy.menu.PeatEngineMenu;
import forestry.core.content.energy.tiles.PeatEngineBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PeatEngineScreen extends EngineScreen<PeatEngineMenu, PeatEngineBlockEntity> {
	public PeatEngineScreen(PeatEngineMenu menu, Inventory inv, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/peatengine.png", menu, inv, title, menu.getTile());
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
		super.renderBg(graphics, partialTicks, mouseX, mouseY);

		if (this.engine.isBurning()) {
			int progress = this.engine.getBurnTimeRemainingScaled(12);
			graphics.blit(this.textureFile, this.leftPos + 45, this.topPos + 27 + 12 - progress, 176, 12 - progress, 14, progress + 2);
		}
	}
}
