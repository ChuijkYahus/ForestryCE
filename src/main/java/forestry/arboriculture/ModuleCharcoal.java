package forestry.arboriculture;

import forestry.api.modules.ForestryModule;
import forestry.api.modules.ForestryModuleIds;
import forestry.modules.BlankForestryModule;
import net.minecraft.resources.ResourceLocation;

// todo: merge into arboriculture in 1.21
@ForestryModule
public class ModuleCharcoal extends BlankForestryModule {
	@Override
	public ResourceLocation getId() {
		return ForestryModuleIds.CHARCOAL;
	}
}
