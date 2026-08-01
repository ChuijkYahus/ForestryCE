package forestry.energy.features;

import forestry.api.core.IMenuTypeProvider;
import forestry.api.modules.ForestryModuleIds;
import forestry.energy.menu.BiogasEngineMenu;
import forestry.energy.menu.CombustionEngineMenu;
import forestry.energy.menu.PeatEngineMenu;
import forestry.energy.menu.SolarEngineMenu;
import forestry.modules.features.FeatureMenuType;
import forestry.modules.features.FeatureProvider;
import forestry.modules.features.IFeatureRegistry;
import forestry.modules.features.ModFeatureRegistry;
import net.minecraft.world.inventory.AbstractContainerMenu;

@FeatureProvider
public class EnergyMenus {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.ENERGY);

	public static final FeatureMenuType<BiogasEngineMenu> ENGINE_BIOGAS = REGISTRY.menuType(BiogasEngineMenu::fromNetwork, "engine_biogas");
	public static final FeatureMenuType<PeatEngineMenu> ENGINE_PEAT = REGISTRY.menuType(PeatEngineMenu::fromNetwork, "engine_peat");
	public static final FeatureMenuType<CombustionEngineMenu> ENGINE_COMBUSTION = REGISTRY.menuType(CombustionEngineMenu::fromNetwork, "engine_combustion");
	public static final FeatureMenuType<SolarEngineMenu> ENGINE_SOLAR = REGISTRY.menuType(SolarEngineMenu::fromNetwork, "engine_solar");
}
