package forestry.api.mail;

import net.minecraft.world.item.ItemStack;

public interface IStamps {

	EnumPostage getPostage(ItemStack itemstack);

}
