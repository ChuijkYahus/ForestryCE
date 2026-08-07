package forestry.core.platform.gui;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;

public interface IPagedInventory extends Container {
	void flipPage(ServerPlayer player, short page);
}
