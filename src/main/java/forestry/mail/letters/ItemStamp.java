package forestry.mail.letters;

import forestry.api.core.ItemGroups;
import forestry.api.mail.EnumPostage;
import forestry.api.mail.IStamps;
import forestry.core.platform.item.ItemOverlay;
import net.minecraft.world.item.ItemStack;
import forestry.mail.letters.EnumStampDefinition;

public class ItemStamp extends ItemOverlay implements IStamps {
	private final EnumStampDefinition def;

	public ItemStamp(EnumStampDefinition def) {
		super(ItemGroups.tabForestry, def);
		this.def = def;
	}

	@Override
	public EnumPostage getPostage(ItemStack itemstack) {
		return this.def.getPostage();
	}
}
