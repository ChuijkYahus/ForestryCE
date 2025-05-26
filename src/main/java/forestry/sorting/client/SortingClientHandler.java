package forestry.sorting.client;

import forestry.api.client.IClientModuleHandler;
import forestry.sorting.features.SortingMenuTypes;
import forestry.sorting.gui.GuiGeneticFilter;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class SortingClientHandler implements IClientModuleHandler {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(SortingClientHandler::onClientSetup);
	}

	private static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> MenuScreens.register(SortingMenuTypes.GENETIC_FILTER.menuType(), GuiGeneticFilter::new));
	}
}
