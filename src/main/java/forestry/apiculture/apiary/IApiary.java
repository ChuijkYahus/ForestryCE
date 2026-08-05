package forestry.apiculture.apiary;

import forestry.api.apiculture.IBeeHousing;
import forestry.apiculture.apiary.IApiaryInventory;

public interface IApiary extends IBeeHousing {
	IApiaryInventory getApiaryInventory();
}
