package forestry.storage.features;

import forestry.api.core.ItemGroups;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.modules.ForestryModuleIds;
import forestry.api.core.backpacks.EnumBackpackType;
import forestry.core.platform.registration.FeatureItem;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import forestry.storage.ModuleStorage;

@FeatureProvider
public class BackpackItems {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.STORAGE);

	public static final FeatureItem<?> APIARIST_BACKPACK = REGISTRY.naturalistBackpack(ModuleStorage.APIARIST, ForestrySpeciesTypes.BEE, ItemGroups.tabApiculture, "apiarists_backpack");
	public static final FeatureItem<?> ARBORIST_BACKPACK = REGISTRY.naturalistBackpack(ModuleStorage.ARBORIST, ForestrySpeciesTypes.TREE, ItemGroups.tabArboriculture, "arborists_backpack");
	public static final FeatureItem<?> LEPIDOPTERIST_BACKPACK = REGISTRY.naturalistBackpack(ModuleStorage.LEPIDOPTERIST, ForestrySpeciesTypes.BUTTERFLY, ItemGroups.tabLepidopterology, "lepidopterists_backpack");

	public static final FeatureItem<?> MINER_BACKPACK = REGISTRY.backpack(ModuleStorage.MINER, EnumBackpackType.NORMAL, "miner_backpack");
	public static final FeatureItem<?> MINER_BACKPACK_T_2 = REGISTRY.backpack(ModuleStorage.MINER, EnumBackpackType.WOVEN, "woven_miner_backpack");
	public static final FeatureItem<?> DIGGER_BACKPACK = REGISTRY.backpack(ModuleStorage.DIGGER, EnumBackpackType.NORMAL, "digger_backpack");
	public static final FeatureItem<?> DIGGER_BACKPACK_T_2 = REGISTRY.backpack(ModuleStorage.DIGGER, EnumBackpackType.WOVEN, "woven_digger_backpack");
	public static final FeatureItem<?> FORESTER_BACKPACK = REGISTRY.backpack(ModuleStorage.FORESTER, EnumBackpackType.NORMAL, "forester_backpack");
	public static final FeatureItem<?> FORESTER_BACKPACK_T_2 = REGISTRY.backpack(ModuleStorage.FORESTER, EnumBackpackType.WOVEN, "woven_forester_backpack");
	public static final FeatureItem<?> HUNTER_BACKPACK = REGISTRY.backpack(ModuleStorage.HUNTER, EnumBackpackType.NORMAL, "hunter_backpack");
	public static final FeatureItem<?> HUNTER_BACKPACK_T_2 = REGISTRY.backpack(ModuleStorage.HUNTER, EnumBackpackType.WOVEN, "woven_hunter_backpack");
	public static final FeatureItem<?> ADVENTURER_BACKPACK = REGISTRY.backpack(ModuleStorage.ADVENTURER, EnumBackpackType.NORMAL, "adventurer_backpack");
	public static final FeatureItem<?> ADVENTURER_BACKPACK_T_2 = REGISTRY.backpack(ModuleStorage.ADVENTURER, EnumBackpackType.WOVEN, "woven_adventurer_backpack");
	public static final FeatureItem<?> BUILDER_BACKPACK = REGISTRY.backpack(ModuleStorage.BUILDER, EnumBackpackType.NORMAL, "builder_backpack");
	public static final FeatureItem<?> BUILDER_BACKPACK_T_2 = REGISTRY.backpack(ModuleStorage.BUILDER, EnumBackpackType.WOVEN, "woven_builder_backpack");
}
