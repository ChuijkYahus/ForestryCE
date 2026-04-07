package forestry.core;

import forestry.api.modules.ForestryModule;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.features.FluidsItems;
import forestry.core.items.ItemFluidContainerForestry;
import forestry.modules.BlankForestryModule;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@ForestryModule
public class ModuleFluids extends BlankForestryModule {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(ModuleFluids::registerCapabilities);
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> ((ItemFluidContainerForestry) stack.getItem()).createFluidHandler(stack), FluidsItems.CONTAINERS.itemArray());
	}

	@Override
	public ResourceLocation getId() {
		return ForestryModuleIds.FLUIDS;
	}
}
