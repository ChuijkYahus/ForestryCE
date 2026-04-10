package forestry.farming;

import forestry.api.client.IClientModuleHandler;
import forestry.api.modules.ForestryModule;
import forestry.api.modules.ForestryModuleIds;
import forestry.farming.client.FarmingClientHandler;
import forestry.farming.features.FarmingTiles;
import forestry.modules.BlankForestryModule;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.function.Consumer;

@ForestryModule
public class ModuleFarming extends BlankForestryModule {
	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, FarmingTiles.GEARBOX.tileType(), (tile, side) -> tile.getEnergyHandler(side));
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FarmingTiles.HATCH.tileType(), (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FarmingTiles.VALVE.tileType(), (tile, side) -> tile.getFluidHandler(side));
	}

	@Override
	public ResourceLocation getId() {
		return ForestryModuleIds.FARMING;
	}

	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(ModuleFarming::registerCapabilities);
	}

	@Override
	public void registerClientHandler(Consumer<IClientModuleHandler> registrar) {
		registrar.accept(new FarmingClientHandler());
	}
}
