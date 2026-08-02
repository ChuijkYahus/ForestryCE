package forestry.mail.gui;

import forestry.api.mail.IMailAddress;
import forestry.core.platform.gui.ContainerTile;
import forestry.core.platform.tile.TileUtil;
import forestry.mail.features.MailMenuTypes;
import forestry.mail.tiles.TileTrader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public class ContainerTradeName extends ContainerTile<TileTrader> {
	public static ContainerTradeName fromNetwork(int windowId, Inventory inv, FriendlyByteBuf data) {
		TileTrader tile = TileUtil.getTile(inv.player.level(), data.readBlockPos(), TileTrader.class);
		return new ContainerTradeName(windowId, inv.player, tile);
	}

	public ContainerTradeName(int windowId, Player player, TileTrader tile) {
		super(windowId, MailMenuTypes.TRADE_NAME.menuType(), tile, player);
	}

	public IMailAddress getAddress() {
		return this.tile.getAddress();
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();

		if (this.tile.isLinked() && this.player != null) {
            this.tile.openGui(this.player, InteractionHand.MAIN_HAND, this.tile.getBlockPos());
		}
	}
}
