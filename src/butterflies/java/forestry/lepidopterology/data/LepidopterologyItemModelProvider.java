package forestry.lepidopterology.data;

import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import forestry.api.ForestryConstants;
import forestry.lepidopterology.features.LepidopterologyItems;

/**
 * Generates the item models for the butterflies jar.
 */
public class LepidopterologyItemModelProvider extends ItemModelProvider {
	public LepidopterologyItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
		super(output, ForestryConstants.MOD_ID, existingFileHelper);
	}

	@Override
	protected void registerModels() {
		withExistingParent(LepidopterologyItems.CATERPILLAR_GE.getName(), mcLoc("item/generated"))
			.texture("layer0", ForestryConstants.forestry("item/caterpillar.body2"))
			.texture("layer1", ForestryConstants.forestry("item/caterpillar.body"));
		withExistingParent(LepidopterologyItems.SERUM_GE.getName(), mcLoc("item/generated"))
			.texture("layer0", ForestryConstants.forestry("item/liquids/jar.bottle"))
			.texture("layer1", ForestryConstants.forestry("item/liquids/jar.contents"));
	}
}
