package forestry.storage.inventory;

import com.google.common.base.Preconditions;
import forestry.api.storage.IBackpackDefinition;
import forestry.core.inventory.ItemInventory;
import forestry.storage.items.BackpackItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class BackpackInventory extends ItemInventory {
	private final IBackpackDefinition backpackDefinition;

	public BackpackInventory(Player player, int size, ItemStack parent) {
		super(player, size, parent);

		Item item = parent.getItem();
		Preconditions.checkArgument(item instanceof BackpackItem, "Parent must be a backpack.");

		this.backpackDefinition = ((BackpackItem) item).getDefinition();
	}

	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		return this.backpackDefinition.getFilter().test(stack);
	}
}
