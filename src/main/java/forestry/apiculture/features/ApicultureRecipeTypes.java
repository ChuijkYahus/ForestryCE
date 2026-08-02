package forestry.apiculture.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.api.core.machines.IHygroregulatorRecipe;
import forestry.apiculture.recipes.HygroregulatorRecipe;
import forestry.modules.features.FeatureProvider;
import forestry.modules.features.FeatureRecipeType;
import forestry.modules.features.IFeatureRegistry;
import forestry.modules.features.ModFeatureRegistry;

/**
 * Apiculture's recipe types. The hygroregulator is an alveary component, so its recipe type is
 * registered here rather than with the factory machines.
 */
@FeatureProvider
public class ApicultureRecipeTypes {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.APICULTURE);

	public static final FeatureRecipeType<IHygroregulatorRecipe> HYGROREGULATOR = REGISTRY.recipeType("hygroregulator", HygroregulatorRecipe.Serializer::new);
}
