package forestry.core.content.energy.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.content.energy.menu.BiogasEngineMenu;
import forestry.core.content.energy.menu.PeatEngineMenu;
import forestry.core.platform.registration.FeatureMenuType;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

@FeatureProvider
public class EnergyMenus {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.ENERGY);

	public static final FeatureMenuType<BiogasEngineMenu> ENGINE_BIOGAS = REGISTRY.menuType(BiogasEngineMenu::fromNetwork, "biogas_engine");
	public static final FeatureMenuType<PeatEngineMenu> ENGINE_PEAT = REGISTRY.menuType(PeatEngineMenu::fromNetwork, "peat_engine");
}
