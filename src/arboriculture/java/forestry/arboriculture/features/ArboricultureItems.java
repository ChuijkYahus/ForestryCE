package forestry.arboriculture.features;

import forestry.api.arboriculture.genetics.TreeLifeStage;
import forestry.api.modules.ForestryModuleIds;
import forestry.arboriculture.wood.ForestryWoodType;
import forestry.arboriculture.wood.ItemForestryBoat;
import forestry.arboriculture.trees.TreeItem;
import forestry.arboriculture.trees.GrafterItem;
import forestry.core.platform.item.ItemForestry;
import forestry.core.platform.registration.*;

@FeatureProvider
public class ArboricultureItems {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.ARBORICULTURE);

	// registry names come from the life stage so the two cannot drift; TreeLifeStage resolves
	// its item form back out of the registry by the same id
	public static final FeatureItem<TreeItem> TREE_SAPLING = REGISTRY.item(() -> new TreeItem(TreeLifeStage.SAPLING), TreeLifeStage.SAPLING.itemId().getPath());
	public static final FeatureItem<TreeItem> TREE_POLLEN = REGISTRY.item(() -> new TreeItem(TreeLifeStage.POLLEN), TreeLifeStage.POLLEN.itemId().getPath());
	public static final FeatureItem<GrafterItem> GRAFTER = REGISTRY.item(() -> new GrafterItem(9), "grafter");
	public static final FeatureItem<GrafterItem> PROVEN_GRAFTER = REGISTRY.item(() -> new GrafterItem(149), "proven_grafter");
	// If you want to implement boats in your addon, look at ItemForestryBoat, ForestryBoat, ForestryChestBoat, and ForestryBoatRenderer
	public static final FeatureItemGroup<ItemForestryBoat, ForestryWoodType> BOAT = REGISTRY.itemGroup(type -> new ItemForestryBoat(type, false), ForestryWoodType.VALUES).identifier("boat", FeatureGroup.IdentifierType.SUFFIX).create();
	public static final FeatureItemGroup<ItemForestryBoat, ForestryWoodType> CHEST_BOAT = REGISTRY.itemGroup(type -> new ItemForestryBoat(type, true), ForestryWoodType.VALUES).identifier("chest_boat", FeatureGroup.IdentifierType.SUFFIX).create();

	// MISC
	public static final FeatureItem<ItemForestry> AMBER_SAPLING_FOSSIL = REGISTRY.item(ItemForestry::new, "amber_sapling_fossil");
}
