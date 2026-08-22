package forestry.apiculture.bees;

import forestry.core.platform.item.ItemBlockForestry;
import forestry.core.platform.item.IColoredItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemBlockHoneyComb extends ItemBlockForestry<BlockHoneyComb> implements IColoredItem {
	@Deprecated
	public ItemBlockHoneyComb(BlockHoneyComb block) {
		this(block, new Item.Properties());
	}

	public ItemBlockHoneyComb(BlockHoneyComb block, Item.Properties properties) {
		super(block, properties);
	}

	@Override
	public int getColorFromItemStack(ItemStack stack, int tintIndex) {
		EnumHoneyComb honeyComb = getBlock().getType();
		if (tintIndex == 1) {
			return honeyComb.primaryColor;
		} else {
			return honeyComb.secondaryColor;
		}
	}
}
