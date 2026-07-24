package forestry.energy;

import forestry.api.client.IClientModuleHandler;
import forestry.api.fuels.EngineBronzeFuel;
import forestry.api.fuels.EngineCopperFuel;
import forestry.api.fuels.FuelManager;
import forestry.api.modules.ForestryModule;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.config.Constants;
import forestry.core.features.CoreItems;
import forestry.core.fluids.ForestryFluids;
import forestry.core.utils.datastructures.FluidMap;
import forestry.core.utils.datastructures.ItemStackMap;
import forestry.energy.client.EnergyClientHandler;
import forestry.modules.BlankForestryModule;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fluids.FluidStack;

import java.util.function.Consumer;

@ForestryModule
public class ModuleEnergy extends BlankForestryModule {
	@Override
	public ResourceLocation getId() {
		return ForestryModuleIds.ENERGY;
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

		Fluid milk = ForgeMod.MILK.get();
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
		Fluid ethanol = ForestryFluids.BIO_ETHANOL.getFluid();
		FuelManager.combustionEngineFuel.put(ethanol, new EngineBronzeFuel(ethanol,
			Constants.ENGINE_FUEL_VALUE_ETHANOL, Constants.ENGINE_FUEL_DURATION_ETHANOL, 1));
		/*FuelManager.combustionEngineFuel.put(seedOil, new EngineBronzeFuel(seedOil,
			Constants.ENGINE_FUEL_VALUE_BIODIESEL, Constants.ENGINE_FUEL_DURATION_BIODIESEL, 1));*/

		// Coolant
		Fluid water = Fluids.WATER.getSource();
		FuelManager.combustionEngineCoolant.put(water, new EngineBronzeFuel(water,
			0,Constants.ENGINE_COOLANT_VALUE_WATER,0));
		Fluid crushedIce = ForestryFluids.ICE.getFluid();
		FuelManager.combustionEngineCoolant.put(crushedIce, new EngineBronzeFuel(crushedIce,
			0,Constants.ENGINE_COOLANT_VALUE_CRUSHED_ICE, 20));

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
