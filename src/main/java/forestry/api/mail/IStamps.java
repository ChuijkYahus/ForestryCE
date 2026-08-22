package forestry.api.mail;

import net.minecraft.world.item.ItemStack;

// todo replace with capability
@Deprecated
public interface IStamps {
	EnumPostage getPostage(ItemStack itemstack);
}
