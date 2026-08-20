package forestry.core.content.machines.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.api.core.machines.*;
import forestry.core.content.machines.recipes.*;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.FeatureRecipeType;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

@FeatureProvider
public class FactoryRecipeTypes {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.FACTORY);

	public static final FeatureRecipeType<ICarpenterRecipe> CARPENTER = REGISTRY.recipeType("carpenter", CarpenterRecipe.Serializer::new);
	public static final FeatureRecipeType<ICentrifugeRecipe> CENTRIFUGE = REGISTRY.recipeType("centrifuge", CentrifugeRecipe.Serializer::new);
	public static final FeatureRecipeType<IFabricatorRecipe> FABRICATOR = REGISTRY.recipeType("fabricator", FabricatorRecipe.Serializer::new);
	public static final FeatureRecipeType<IFabricatorSmeltingRecipe> FABRICATOR_SMELTING = REGISTRY.recipeType("fabricator_smelting", FabricatorSmeltingRecipe.Serializer::new);
	public static final FeatureRecipeType<IFermenterRecipe> FERMENTER = REGISTRY.recipeType("fermenter", FermenterRecipe.Serializer::new);
	public static final FeatureRecipeType<IMoistenerRecipe> MOISTENER = REGISTRY.recipeType("moistener", MoistenerRecipe.Serializer::new);
	public static final FeatureRecipeType<ISmelterRecipe> SMELTER = REGISTRY.recipeType("smelter", SmelterRecipe.Serializer::new);
	public static final FeatureRecipeType<ISqueezerRecipe> SQUEEZER = REGISTRY.recipeType("squeezer", SqueezerRecipe.Serializer::new);
	public static final FeatureRecipeType<ISqueezerContainerRecipe> SQUEEZER_CONTAINER = REGISTRY.recipeType("squeezer_container", SqueezerContainerRecipe.Serializer::new);
	public static final FeatureRecipeType<IStillRecipe> STILL = REGISTRY.recipeType("still", StillRecipe.Serializer::new);
}
