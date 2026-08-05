package forestry.agriculture;

import forestry.api.client.IClientModuleHandler;
import forestry.api.modules.ForestryModule;
import forestry.api.modules.ForestryModuleIds;
import forestry.agriculture.features.CultivationTiles;
import forestry.agriculture.client.CultivationClientHandler;
import forestry.agriculture.planter.tiles.TilePlanter;
import forestry.modules.BlankForestryModule;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.List;
import java.util.function.Consumer;

@ForestryModule
public class ModuleCultivation extends BlankForestryModule {
	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		registerPlanterCapabilities(event, CultivationTiles.ARBORETUM.tileType());
		registerPlanterCapabilities(event, CultivationTiles.BOG.tileType());
		registerPlanterCapabilities(event, CultivationTiles.CROPS.tileType());
		registerPlanterCapabilities(event, CultivationTiles.ENDER.tileType());
		registerPlanterCapabilities(event, CultivationTiles.GOURD.tileType());
		registerPlanterCapabilities(event, CultivationTiles.MUSHROOM.tileType());
		registerPlanterCapabilities(event, CultivationTiles.NETHER.tileType());
	}

	private static <T extends TilePlanter> void registerPlanterCapabilities(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, (tile, side) -> tile.getEnergyHandler(side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (tile, side) -> tile.getFluidHandler(side));
	}

	@Override
	public ResourceLocation getId() {
		return ForestryModuleIds.CULTIVATION;
	}

	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(ModuleCultivation::registerCapabilities);
	}

	@Override
	public void registerClientHandler(Consumer<IClientModuleHandler> registrar) {
		registrar.accept(new CultivationClientHandler());
	}

	@Override
	public List<ResourceLocation> getModuleDependencies() {
		return List.of(ForestryModuleIds.CORE, ForestryModuleIds.FARMING);
	}
}
