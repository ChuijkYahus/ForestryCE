package forestry.apiculture.bees;

import forestry.api.core.ItemGroups;
import forestry.core.platform.item.ItemOverlay;
import forestry.apiculture.bees.EnumPollenCluster;

public class ItemPollenCluster extends ItemOverlay {
	public ItemPollenCluster(EnumPollenCluster type) {
		super(ItemGroups.tabApiculture, type);
	}
}
