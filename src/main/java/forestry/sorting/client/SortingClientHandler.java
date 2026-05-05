package forestry.sorting.client;

import forestry.api.client.IClientModuleHandler;
import forestry.sorting.features.SortingMenuTypes;
import forestry.sorting.gui.GuiGeneticFilter;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class SortingClientHandler implements IClientModuleHandler {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(SortingClientHandler::registerMenuScreens);
	}

	private static void registerMenuScreens(RegisterMenuScreensEvent event) {
		event.register(SortingMenuTypes.GENETIC_FILTER.menuType(), GuiGeneticFilter::new);
	}
}
