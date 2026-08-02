package forestry.lepidopterology.proxy;

import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import forestry.core.models.ClientManager;
import forestry.api.ForestryConstants;
import forestry.api.client.IClientModuleHandler;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.render.ForestryModelLayers;
import forestry.lepidopterology.features.LepidopterologyEntities;
import forestry.lepidopterology.features.LepidopterologyItems;
import forestry.lepidopterology.items.ItemButterflyGE;
import forestry.lepidopterology.render.ButterflyEntityRenderer;
import forestry.lepidopterology.render.ButterflyItemModel;
import forestry.lepidopterology.render.ButterflyModel;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.bus.api.IEventBus;

public class LepidopterologyClientHandler implements IClientModuleHandler {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(LepidopterologyClientHandler::registerItemColors);
		modBus.addListener(LepidopterologyClientHandler::setupRenderers);
		modBus.addListener(LepidopterologyClientHandler::setupLayers);
		modBus.addListener(LepidopterologyClientHandler::registerModelLoaders);
		modBus.addListener(LepidopterologyClientHandler::registerItemProperties);
	}

	@SuppressWarnings("deprecation")
	private static void registerItemProperties(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			ItemPropertyFunction itemPropertyFunction = (stack, clientLevel, holder, idk) -> ItemButterflyGE.getAge(stack);
			ItemProperties.register(LepidopterologyItems.COCOON_GE.get(), ForestryConstants.forestry("age"), itemPropertyFunction);
		});
	}

	public static void setupRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(LepidopterologyEntities.BUTTERFLY.entityType(), ButterflyEntityRenderer::new);
	}

	public static void setupLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(ForestryModelLayers.BUTTERFLY_LAYER, ButterflyModel::createLayer);
	}

	public static void registerModelLoaders(ModelEvent.RegisterGeometryLoaders event) {
		event.register(ForestryConstants.forestry("butterfly_ge"), new ButterflyItemModel.Loader());
	}

	private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		event.register(ClientManager.FORESTRY_ITEM_COLOR, LepidopterologyItems.CATERPILLAR_GE.item());
		event.register(ClientManager.FORESTRY_ITEM_COLOR, LepidopterologyItems.SERUM_GE.item());
	}

}
