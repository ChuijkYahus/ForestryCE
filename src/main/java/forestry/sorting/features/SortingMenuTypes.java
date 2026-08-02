package forestry.sorting.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.registration.FeatureMenuType;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import forestry.sorting.gui.ContainerGeneticFilter;

@FeatureProvider
public class SortingMenuTypes {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.SORTING);

	public static final FeatureMenuType<ContainerGeneticFilter> GENETIC_FILTER = REGISTRY.menuType(ContainerGeneticFilter::fromNetwork, "genetic_filter");
}
