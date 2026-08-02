package forestry.core.content.backpacks.inventory;

import forestry.core.platform.gui.IPagedInventory;
import forestry.core.content.backpacks.gui.ContainerNaturalistBackpack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PagedBackpackInventory extends BackpackInventory implements IPagedInventory {
	private final ResourceLocation typeId;

	public PagedBackpackInventory(Player player, int size, ItemStack itemstack, ResourceLocation typeId) {
		super(player, size, itemstack);
		this.typeId = typeId;
	}

	@Override
	public void flipPage(ServerPlayer player, short page) {
		ItemStack backpack = getParent();
		SimpleMenuProvider provider = new SimpleMenuProvider((windowId, playerInv, p) -> ContainerNaturalistBackpack.makeContainer(windowId, p, backpack, page, this.typeId), backpack.getHoverName());
		player.openMenu(provider, buffer -> {
			buffer.writeByte(page);
			buffer.writeResourceLocation(this.typeId);
		});
	}
}
