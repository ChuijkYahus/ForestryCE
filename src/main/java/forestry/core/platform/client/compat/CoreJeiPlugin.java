package forestry.core.platform.client.compat;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.features.CoreItems;
import forestry.core.platform.util.JeiUtil;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class CoreJeiPlugin implements IModPlugin {
	@Override
	public ResourceLocation getPluginUid() {
		return ForestryModuleIds.CORE;
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		JeiUtil.addDescription(registration, CoreItems.COMPOST);
		JeiUtil.addDescription(registration, CoreItems.MULCH);
		JeiUtil.addDescription(registration, CoreItems.FERTILIZER_COMPOUND);
	}
}
