package forestry.factory.client;

import forestry.api.client.IClientModuleHandler;
import forestry.factory.features.FactoryMenuTypes;
import forestry.factory.gui.*;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class FactoryClientHandler implements IClientModuleHandler {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(FactoryClientHandler::setupClient);
	}

	private static void setupClient(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			MenuScreens.register(FactoryMenuTypes.BOTTLER.menuType(), GuiBottler::new);
			MenuScreens.register(FactoryMenuTypes.CARPENTER.menuType(), GuiCarpenter::new);
			MenuScreens.register(FactoryMenuTypes.CENTRIFUGE.menuType(), GuiCentrifuge::new);
			MenuScreens.register(FactoryMenuTypes.FABRICATOR.menuType(), GuiFabricator::new);
			MenuScreens.register(FactoryMenuTypes.FERMENTER.menuType(), GuiFermenter::new);
			MenuScreens.register(FactoryMenuTypes.MOISTENER.menuType(), GuiMoistener::new);
			MenuScreens.register(FactoryMenuTypes.RAINTANK.menuType(), GuiRaintank::new);
			MenuScreens.register(FactoryMenuTypes.SQUEEZER.menuType(), GuiSqueezer::new);
			MenuScreens.register(FactoryMenuTypes.STILL.menuType(), GuiStill::new);
		});
	}
}
