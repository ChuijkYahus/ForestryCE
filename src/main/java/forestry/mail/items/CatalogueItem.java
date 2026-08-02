package forestry.mail.items;

import forestry.core.platform.item.WithScreenItem;
import forestry.mail.gui.ContainerCatalogue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class CatalogueItem extends WithScreenItem {
	public CatalogueItem() {
		super(new Item.Properties());
	}

	@Nullable
	@Override
	public AbstractContainerMenu getContainer(int containerId, Player player, ItemStack heldItem) {
		return new ContainerCatalogue(containerId, player.getInventory());
	}
}
