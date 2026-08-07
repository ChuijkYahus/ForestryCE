package forestry.apiculture.bees;

import forestry.apiculture.bees.BlockHoneyComb;
import forestry.core.platform.item.ItemBlockForestry;
import forestry.core.platform.item.IColoredItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import forestry.apiculture.bees.EnumHoneyComb;

public class ItemBlockHoneyComb extends ItemBlockForestry<BlockHoneyComb> implements IColoredItem {
	public ItemBlockHoneyComb(BlockHoneyComb block) {
		super(block, new Item.Properties());
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
