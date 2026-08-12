package forestry.core.content.energy.client;

import forestry.core.content.energy.features.EnergyBlocks;
import forestry.core.content.energy.features.EnergyMenus;
import forestry.core.content.energy.screen.BiogasEngineScreen;
import forestry.core.content.energy.screen.PeatEngineScreen;
import forestry.core.content.energy.screen.SolarEngineScreen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class EnergyClientHandler implements forestry.api.client.IClientModuleHandler {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(EnergyClientHandler::registerMenuScreens);
		modBus.addListener(EnergyClientHandler::onClientSetup);
	}

	// Deviation from 1.20.1: the solar panel's cutout render layer was registered from CoreClientHandler
	// there. That file is outside the energy slice here, and the energy module already owns a client
	// handler, so the registration lives with the block it belongs to.
	private static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(EnergyBlocks.SOLAR_PANEL.block(), RenderType.cutout()));
	}

	private static void registerMenuScreens(RegisterMenuScreensEvent event) {
		event.register(EnergyMenus.ENGINE_BIOGAS.menuType(), BiogasEngineScreen::new);
		event.register(EnergyMenus.ENGINE_PEAT.menuType(), PeatEngineScreen::new);
		event.register(EnergyMenus.ENGINE_SOLAR.menuType(), SolarEngineScreen::new);
	}
}
