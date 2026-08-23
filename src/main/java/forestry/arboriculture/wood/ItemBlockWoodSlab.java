package forestry.arboriculture.wood;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

import javax.annotation.Nullable;

public class ItemBlockWoodSlab extends BlockItem {
	public ItemBlockWoodSlab(BlockForestrySlab block) {
		super(block, new Item.Properties());
	}

	@Override
	public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
		BlockForestrySlab forestrySlab = (BlockForestrySlab) getBlock();

		if (forestrySlab.isFireproof()) {
			return 0;
		} else {
			return 150;
		}
	}
}
