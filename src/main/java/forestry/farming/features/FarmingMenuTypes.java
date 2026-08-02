package forestry.farming.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.farming.gui.ContainerFarm;
import forestry.core.platform.registration.FeatureMenuType;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

@FeatureProvider
public class FarmingMenuTypes {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.FARMING);

	public static final FeatureMenuType<ContainerFarm> FARM = REGISTRY.menuType(ContainerFarm::fromNetwork, "farm");
}
