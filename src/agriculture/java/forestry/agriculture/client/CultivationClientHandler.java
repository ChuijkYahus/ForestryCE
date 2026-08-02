package forestry.agriculture.client;

import forestry.api.client.IClientModuleHandler;
import forestry.agriculture.features.CultivationBlocks;
import forestry.agriculture.features.CultivationMenuTypes;
import forestry.agriculture.planter.gui.GuiPlanter;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class CultivationClientHandler implements IClientModuleHandler {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(CultivationClientHandler::onClientSetup);
		modBus.addListener(CultivationClientHandler::registerMenuScreens);
	}

	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			CultivationBlocks.MANAGED_PLANTER.getList().forEach(block -> ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutoutMipped()));
			CultivationBlocks.MANUAL_PLANTER.getList().forEach(block -> ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutoutMipped()));
		});
	}

	private static void registerMenuScreens(RegisterMenuScreensEvent event) {
		event.register(CultivationMenuTypes.PLANTER.menuType(), GuiPlanter::new);
	}
}
