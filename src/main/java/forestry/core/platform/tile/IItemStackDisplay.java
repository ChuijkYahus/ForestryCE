package forestry.core.platform.tile;

import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface IItemStackDisplay {
	@OnlyIn(Dist.CLIENT)
	void handleItemStackForDisplay(ItemStack itemStack);
}
