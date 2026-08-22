package forestry.mail.letters;

import forestry.api.mail.EnumPostage;
import forestry.api.mail.IStamps;
import forestry.core.platform.item.TwoTintItem;
import net.minecraft.world.item.ItemStack;

public class ItemStamp extends TwoTintItem implements IStamps {
	private final EnumStampDefinition type;

	public ItemStamp(EnumStampDefinition type) {
		super(type);
		this.type = type;
	}

	@Override
	public EnumPostage getPostage(ItemStack stack) {
		return this.type.getPostage();
	}
}
