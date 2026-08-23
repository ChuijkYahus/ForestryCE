package forestry.agriculture.client;

import forestry.api.client.IClientModuleHandler;
import forestry.agriculture.features.MinifarmBlocks;
import forestry.agriculture.features.MinifarmMenuTypes;
import forestry.agriculture.minifarm.gui.MinifarmScreen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class MinifarmClientHandler implements IClientModuleHandler {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(MinifarmClientHandler::onClientSetup);
		modBus.addListener(MinifarmClientHandler::registerMenuScreens);
	}

	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			MinifarmBlocks.MANAGED_PLANTER.getList().forEach(block -> ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutoutMipped()));
			MinifarmBlocks.MANUAL_PLANTER.getList().forEach(block -> ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutoutMipped()));
		});
	}

	private static void registerMenuScreens(RegisterMenuScreensEvent event) {
		event.register(MinifarmMenuTypes.PLANTER.menuType(), MinifarmScreen::new);
	}
}
