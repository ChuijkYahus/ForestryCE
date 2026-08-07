package forestry.apiculture.apiary;

import forestry.api.apiculture.IBeeModifier;
import forestry.api.core.genetics.IGenome;

public class ApiaryBeeModifier implements IBeeModifier {
	@Override
	public float modifyProductionSpeed(IGenome genome, float currentSpeed) {
		return 0.1f * currentSpeed;
	}
}
