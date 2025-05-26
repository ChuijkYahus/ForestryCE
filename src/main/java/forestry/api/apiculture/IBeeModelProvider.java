package forestry.api.apiculture;

import forestry.api.apiculture.genetics.BeeLifeStage;
import net.minecraft.client.resources.model.ModelResourceLocation;

public interface IBeeModelProvider {

	ModelResourceLocation getModel(BeeLifeStage type);
}
