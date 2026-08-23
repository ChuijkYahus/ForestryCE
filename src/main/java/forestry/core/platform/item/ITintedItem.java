package forestry.core.platform.item;

import net.minecraft.world.item.ItemStack;

/**
 * Gives an item the ability to be colored.
 * <p>
 * <b>NOTE:</b> Items are NOT automatically registered to be tinted. You will have to register in {@code RegisterColorHandlersEvent.Item} yourself.
 */
public interface ITintedItem {
	/**
	 * Defines the color of the texture sprite with the given index in the model file of the item.
	 *
	 * @param stack     The stack that contains this item
	 * @param tintIndex The index of the texture sprite in the model
	 * @return The color that the sprite with the given index should have
	 */
	int getColorFromItemStack(ItemStack stack, int tintIndex);
}
