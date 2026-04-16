package forestry.core.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.circuits.ContainerSolderingIron;
import forestry.core.gui.*;
import forestry.modules.features.FeatureMenuType;
import forestry.modules.features.FeatureProvider;
import forestry.modules.features.IFeatureRegistry;
import forestry.modules.features.ModFeatureRegistry;

@FeatureProvider
public class CoreMenuTypes {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CORE);

	public static final FeatureMenuType<ContainerAlyzer> ALYZER = REGISTRY.menuType(ContainerAlyzer::fromNetwork, "alyzer");
	public static final FeatureMenuType<ContainerAnalyzer> ANALYZER = REGISTRY.menuType(ContainerAnalyzer::fromNetwork, "analyzer");
	public static final FeatureMenuType<ContainerEscritoire> ESCRITOIRE = REGISTRY.menuType(ContainerEscritoire::fromNetwork, "escritoire");
	public static final FeatureMenuType<ContainerBurnBarrel> BURN_BARREL = REGISTRY.menuType(ContainerBurnBarrel::fromNetwork, "burn_barrel");
	public static final FeatureMenuType<ContainerNaturalistInventory> NATURALIST_INVENTORY = REGISTRY.menuType(ContainerNaturalistInventory::fromNetwork, "naturalist_inventory");
	public static final FeatureMenuType<ContainerSolderingIron> SOLDERING_IRON = REGISTRY.menuType(ContainerSolderingIron::fromNetwork, "soldering_iron");
}
