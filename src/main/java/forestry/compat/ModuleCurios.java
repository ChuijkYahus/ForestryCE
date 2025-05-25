package forestry.compat;

import java.util.function.Consumer;

import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.fml.ModList;

import forestry.api.client.IClientModuleHandler;
import forestry.api.modules.ForestryModule;
import forestry.api.modules.ForestryModuleIds;
import forestry.api.modules.IForestryModule;
import forestry.compat.curios.client.CuriosClientHandler;

@ForestryModule
public class ModuleCurios implements IForestryModule {
	@Override
	public ResourceLocation getId() {
		return ForestryModuleIds.CURIOS;
	}

	@Override
	public void registerClientHandler(Consumer<IClientModuleHandler> registrar) {
		if (ModList.get().isLoaded("curios")) {
			registrar.accept(new CuriosClientHandler());
		}
	}
}
