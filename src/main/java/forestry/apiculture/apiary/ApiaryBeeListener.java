package forestry.apiculture.apiary;

import forestry.api.apiculture.IBeeListener;
import forestry.apiculture.apiary.IApiaryInventory;
import forestry.apiculture.apiary.IApiary;

public class ApiaryBeeListener implements IBeeListener {
	private final IApiary apiary;

	public ApiaryBeeListener(IApiary apiary) {
		this.apiary = apiary;
	}

	@Override
	public void wearOutEquipment(int amount) {
		IApiaryInventory apiaryInventory = this.apiary.getApiaryInventory();
		apiaryInventory.wearOutFrames(this.apiary, amount);
	}
}
