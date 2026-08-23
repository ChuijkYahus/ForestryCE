package forestry.mail.letters;

import forestry.api.ForestryDataMaps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The only reader of the postage data map. Every place that used to test for {@code IStamps} and call
 * {@code getPostage} goes through here instead.
 */
public class PostageUtil {
	private PostageUtil() {
	}

	/**
	 * @param item The item to read the postage of
	 * @return The postage the item is worth, or zero when it is not a stamp
	 */
	public static int getPostage(Item item) {
		Integer postage = item.builtInRegistryHolder().getData(ForestryDataMaps.POSTAGE);
		return postage == null ? 0 : postage;
	}

	/**
	 * @param stack The stack to read the postage of
	 * @return The postage one item of the stack is worth, or zero when it is not a stamp
	 */
	public static int getPostage(ItemStack stack) {
		return stack.isEmpty() ? 0 : getPostage(stack.getItem());
	}

	/**
	 * @param stack The stack to test
	 * @return Whether the stack is worth postage
	 */
	public static boolean isStamp(ItemStack stack) {
		return getPostage(stack) > 0;
	}

	/**
	 * @param stacks The stacks to add up
	 * @return The total postage of every stack, counting stack size
	 */
	public static int sumPostage(Iterable<ItemStack> stacks) {
		int posted = 0;

		for (ItemStack stack : stacks) {
			posted += getPostage(stack) * stack.getCount();
		}

		return posted;
	}
}
