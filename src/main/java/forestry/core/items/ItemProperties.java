package forestry.core.items;

import net.minecraft.world.item.Item;

public class ItemProperties extends Item.Properties {
	// 0 = "not a fuel". NeoForge 1.21 IItemStackExtension#getBurnTime throws on
	// negative returns, so the legacy -1 sentinel is no longer valid.
	public int burnTime = 0;

	public ItemProperties burnTime(int burnTime) {
		this.burnTime = burnTime;
		return this;
	}
}
