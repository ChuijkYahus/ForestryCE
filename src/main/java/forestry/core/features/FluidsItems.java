package forestry.core.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.items.ItemFluidContainerForestry;
import forestry.core.items.definitions.EnumContainerType;
import forestry.core.platform.registration.FeatureItemGroup;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

@FeatureProvider
public class FluidsItems {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.FLUIDS);

	public static final FeatureItemGroup<ItemFluidContainerForestry, EnumContainerType> CONTAINERS = REGISTRY.itemGroup(ItemFluidContainerForestry::new, EnumContainerType.values()).identifier(type -> switch (type) {
		case CAPSULE -> "wax_capsule";
		case REFRACTORY -> "refractory_capsule";
		default -> type.getSerializedName();
	}).create();
}
