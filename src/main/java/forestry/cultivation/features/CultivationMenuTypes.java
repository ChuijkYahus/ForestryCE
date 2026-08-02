package forestry.cultivation.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.cultivation.gui.ContainerPlanter;
import forestry.core.platform.registration.FeatureMenuType;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

@FeatureProvider
public class CultivationMenuTypes {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CULTIVATION);

	public static final FeatureMenuType<ContainerPlanter> PLANTER = REGISTRY.menuType(ContainerPlanter::fromNetwork, "planter");
}
