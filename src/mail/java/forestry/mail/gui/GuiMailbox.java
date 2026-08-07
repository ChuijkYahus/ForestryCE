package forestry.mail.gui;

import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.GuiForestry;
import forestry.mail.postoffice.TileMailbox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GuiMailbox extends GuiForestry<ContainerMailbox> {
	private final TileMailbox tile;

	public GuiMailbox(ContainerMailbox container, Inventory inv, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/mailbox.png", container, inv, title);
		this.tile = container.getTile();
		this.imageWidth = 230;
		this.imageHeight = 227;
	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
		addHintLedger("mailbox");
	}
}
