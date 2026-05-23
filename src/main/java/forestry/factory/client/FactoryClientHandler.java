package forestry.factory.client;

import forestry.api.client.IClientModuleHandler;
import forestry.factory.features.FactoryMenuTypes;
import forestry.factory.gui.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class FactoryClientHandler implements IClientModuleHandler {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(FactoryClientHandler::registerMenuScreens);
	}

	private static void registerMenuScreens(RegisterMenuScreensEvent event) {
		event.register(FactoryMenuTypes.BOTTLER.menuType(), GuiBottler::new);
		event.register(FactoryMenuTypes.CARPENTER.menuType(), GuiCarpenter::new);
		event.register(FactoryMenuTypes.CENTRIFUGE.menuType(), GuiCentrifuge::new);
		event.register(FactoryMenuTypes.FABRICATOR.menuType(), GuiFabricator::new);
		event.register(FactoryMenuTypes.FERMENTER.menuType(), GuiFermenter::new);
		event.register(FactoryMenuTypes.MOISTENER.menuType(), GuiMoistener::new);
		event.register(FactoryMenuTypes.RAINTANK.menuType(), GuiRaintank::new);
		event.register(FactoryMenuTypes.SQUEEZER.menuType(), GuiSqueezer::new);
		event.register(FactoryMenuTypes.STILL.menuType(), GuiStill::new);
	}
}
