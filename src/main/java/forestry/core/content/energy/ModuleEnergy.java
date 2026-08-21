package forestry.core.content.energy;

import forestry.api.client.IClientModuleHandler;
import forestry.api.core.machines.fuels.EngineBronzeFuel;
import forestry.api.core.machines.fuels.EngineCopperFuel;
import forestry.api.core.machines.fuels.FuelManager;
import forestry.api.modules.ForestryModule;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.config.Constants;
import forestry.core.features.CoreItems;
import forestry.core.platform.fluids.ForestryFluids;
import forestry.core.platform.util.datastructures.FluidMap;
import forestry.core.platform.util.datastructures.ItemStackMap;
import forestry.core.content.energy.client.EnergyClientHandler;
import forestry.core.content.energy.features.EnergyTiles;
import forestry.modules.BlankForestryModule;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForgeMod;

import java.util.function.Consumer;

@ForestryModule
public class ModuleEnergy extends BlankForestryModule {
	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, EnergyTiles.CLOCKWORK_ENGINE.tileType(), (tile, side) -> tile.getEnergyHandler(side));
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, EnergyTiles.BIOGAS_ENGINE.tileType(), (tile, side) -> tile.getEnergyHandler(side));
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, EnergyTiles.PEAT_ENGINE.tileType(), (tile, side) -> tile.getEnergyHandler(side));
		// Deviation from 1.20.1: capabilities are registered here instead of overriding getCapability on the tile.
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, EnergyTiles.SOLAR_ENGINE.tileType(), (tile, side) -> tile.getEnergyHandler(side));
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, EnergyTiles.COMBUSTION_ENGINE.tileType(), (tile, side) -> tile.getEnergyHandler(side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, EnergyTiles.BIOGAS_ENGINE.tileType(), (tile, side) -> tile.getFluidHandler(side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, EnergyTiles.COMBUSTION_ENGINE.tileType(), (tile, side) -> tile.getFluidHandler(side));
	}

	@Override
	public ResourceLocation getId() {
		return ForestryModuleIds.ENERGY;
	}

	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(ModuleEnergy::registerCapabilities);
	}

	@Override
	public void setupApi() {
		FuelManager.biogasEngineFuel = new FluidMap<>();
		FuelManager.peatEngineFuel = new ItemStackMap<>();
		FuelManager.combustionEngineFuel = new FluidMap<>();
		FuelManager.combustionEngineCoolant = new FluidMap<>();

		// Biogas Engine
		Fluid biomass = ForestryFluids.BIOMASS.getFluid();
		FuelManager.biogasEngineFuel.put(biomass, new EngineBronzeFuel(biomass,
			Constants.ENGINE_FUEL_VALUE_BIOMASS, Constants.ENGINE_CYCLE_DURATION_BIOMASS, 1));

		Fluid milk = NeoForgeMod.MILK.get();
		FuelManager.biogasEngineFuel.put(milk, new EngineBronzeFuel(milk,
			Constants.ENGINE_FUEL_VALUE_MILK, Constants.ENGINE_CYCLE_DURATION_MILK, 3));

		Fluid seedOil = ForestryFluids.SEED_OIL.getFluid();
		FuelManager.biogasEngineFuel.put(seedOil, new EngineBronzeFuel(seedOil,
			Constants.ENGINE_FUEL_VALUE_SEED_OIL, Constants.ENGINE_CYCLE_DURATION_SEED_OIL, 1));

		Fluid honey = ForestryFluids.HONEY.getFluid();
		FuelManager.biogasEngineFuel.put(honey, new EngineBronzeFuel(honey,
			Constants.ENGINE_FUEL_VALUE_HONEY, Constants.ENGINE_CYCLE_DURATION_HONEY, 1));

		Fluid juice = ForestryFluids.JUICE.getFluid();
		FuelManager.biogasEngineFuel.put(juice, new EngineBronzeFuel(juice,
			Constants.ENGINE_FUEL_VALUE_JUICE, Constants.ENGINE_CYCLE_DURATION_JUICE, 2));

		// Combustion Engine
		// Deviation from 1.20.1: the duration constant is named ENGINE_CYCLE_DURATION_ETHANOL here, not
		// ENGINE_FUEL_DURATION_ETHANOL. Same value. The commented-out biodiesel entry was left behind
		Fluid ethanol = ForestryFluids.BIO_ETHANOL.getFluid();
		FuelManager.combustionEngineFuel.put(ethanol, new EngineBronzeFuel(ethanol,
			Constants.ENGINE_FUEL_VALUE_ETHANOL, Constants.ENGINE_CYCLE_DURATION_ETHANOL, 1));

		// Coolant
		Fluid water = Fluids.WATER.getSource();
		FuelManager.combustionEngineCoolant.put(water, new EngineBronzeFuel(water,
			0, Constants.ENGINE_COOLANT_VALUE_WATER, 0));

		Fluid crushedIce = ForestryFluids.ICE.getFluid();
		FuelManager.combustionEngineCoolant.put(crushedIce, new EngineBronzeFuel(crushedIce,
			0, Constants.ENGINE_COOLANT_VALUE_CRUSHED_ICE, 20));

		// Peat Engine
		ItemStack peat = CoreItems.PEAT.stack();
		FuelManager.peatEngineFuel.put(peat, new EngineCopperFuel(peat, Constants.ENGINE_COPPER_FUEL_VALUE_PEAT, Constants.ENGINE_COPPER_CYCLE_DURATION_PEAT));

		ItemStack bituminousPeat = CoreItems.BITUMINOUS_PEAT.stack();
		FuelManager.peatEngineFuel.put(bituminousPeat, new EngineCopperFuel(bituminousPeat, Constants.ENGINE_COPPER_FUEL_VALUE_BITUMINOUS_PEAT, Constants.ENGINE_COPPER_CYCLE_DURATION_BITUMINOUS_PEAT));
	}

	@Override
	public void registerClientHandler(Consumer<IClientModuleHandler> registrar) {
		registrar.accept(new EnergyClientHandler());
	}
}
