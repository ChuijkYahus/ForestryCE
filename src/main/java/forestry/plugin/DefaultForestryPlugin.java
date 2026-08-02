package forestry.plugin;

import forestry.api.apiculture.ForestryActivityTypes;
import forestry.api.apiculture.ForestryBeeEffects;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.genetics.alleles.BeeChromosomes;
import forestry.apiculture.genetics.BeeSpeciesType;
import forestry.farming.plugin.DefaultFarms;
import forestry.api.ForestryConstants;
import forestry.api.apiculture.*;
import forestry.api.arboriculture.ForestryFruits;
import forestry.api.arboriculture.ForestryTreeSpecies;
import forestry.api.arboriculture.genetics.TreeLifeStage;
import forestry.api.circuits.ForestryCircuitLayouts;
import forestry.api.circuits.ForestryCircuitSocketTypes;
import forestry.api.client.plugin.IClientRegistration;
import forestry.api.core.ForestryError;
import forestry.api.core.IError;
import forestry.api.core.Product;
import forestry.api.farming.ForestryFarmTypes;
import forestry.api.genetics.ForestrySpeciesTypes;
import forestry.api.genetics.alleles.ButterflyChromosomes;
import forestry.api.genetics.alleles.ForestryAlleles;
import forestry.api.genetics.alleles.TreeChromosomes;
import forestry.api.lepidopterology.ForestryButterflySpecies;
import forestry.api.lepidopterology.genetics.ButterflyLifeStage;
import forestry.api.plugin.*;
import forestry.arboriculture.genetics.TreeSpeciesType;
import forestry.core.features.CoreItems;
import forestry.core.items.definitions.EnumCraftingMaterial;
import forestry.core.items.definitions.EnumElectronTube;
import forestry.factory.circuits.CircuitMachineUpgrade;
import forestry.farming.circuits.CircuitFarmLogic;
import forestry.lepidopterology.DummyButterflyEffect;
import forestry.lepidopterology.LepidopterologyFilterRule;
import forestry.lepidopterology.LepidopterologyFilterRuleType;
import forestry.lepidopterology.genetics.ButterflySpeciesType;
import forestry.lepidopterology.genetics.DefaultCocoon;
import forestry.apiculture.client.plugin.ApicultureClientRegistration;
import forestry.arboriculture.client.plugin.ArboricultureClientRegistration;
import forestry.lepidopterology.client.plugin.LepidopterologyClientRegistration;
import forestry.sorting.DefaultFilterRuleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Consumer;
import forestry.api.lepidopterology.ForestryButterflyEffects;
import forestry.api.lepidopterology.ForestryCocoons;
import forestry.api.apiculture.ForestryFlowerTypes;

public class DefaultForestryPlugin implements IForestryPlugin {
	public static final ResourceLocation ID = ForestryConstants.forestry("default");

	@Override
	public void registerGenetics(IGeneticRegistration genetics) {

		// Bee type
		genetics.registerSpeciesType(ForestrySpeciesTypes.BEE, BeeSpeciesType::new)
			.setKaryotype(karyotype -> {
				karyotype.setSpecies(BeeChromosomes.SPECIES, ForestryBeeSpecies.FOREST);
				karyotype.set(BeeChromosomes.SPEED, ForestryAlleles.SPEED_SLOWEST);
				karyotype.set(BeeChromosomes.LIFESPAN, ForestryAlleles.LIFESPAN_SHORTER);
				karyotype.set(BeeChromosomes.FERTILITY, ForestryAlleles.FERTILITY_2);
				karyotype.set(BeeChromosomes.TEMPERATURE_TOLERANCE, ForestryAlleles.TOLERANCE_NONE)
					.setWeaklyInherited(true);
				karyotype.set(BeeChromosomes.HUMIDITY_TOLERANCE, ForestryAlleles.TOLERANCE_NONE)
					.setWeaklyInherited(true);
				karyotype.set(BeeChromosomes.ACTIVITY, ForestryActivityTypes.DIURNAL)
					.setWeaklyInherited(true);
				karyotype.set(BeeChromosomes.CAVE_DWELLING, false)
					.setWeaklyInherited(true);
				karyotype.set(BeeChromosomes.TOLERATES_RAIN, false)
					.setWeaklyInherited(true);
				karyotype.set(BeeChromosomes.FLOWER_TYPE, ForestryFlowerTypes.VANILLA);
				karyotype.set(BeeChromosomes.TERRITORY, ForestryAlleles.TERRITORY_AVERAGE);
				karyotype.set(BeeChromosomes.EFFECT, ForestryBeeEffects.NONE);
				karyotype.set(BeeChromosomes.POLLINATION, ForestryAlleles.POLLINATION_SLOWEST);
			})
			.addStages(BeeLifeStage.DRONE, BeeLifeStage.PRINCESS, BeeLifeStage.QUEEN, BeeLifeStage.LARVAE)
			.setDefaultStage(BeeLifeStage.DRONE);

		// Tree type
		genetics.registerSpeciesType(ForestrySpeciesTypes.TREE, TreeSpeciesType::new)
			.setKaryotype(karyotype -> {
				karyotype.setSpecies(TreeChromosomes.SPECIES, ForestryTreeSpecies.OAK);
				karyotype.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_SMALL);
				karyotype.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_LOWER);
				karyotype.set(TreeChromosomes.FRUIT, ForestryFruits.NONE);
				karyotype.set(TreeChromosomes.YIELD, ForestryAlleles.YIELD_LOWEST);
				karyotype.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOWEST);
				karyotype.set(TreeChromosomes.EFFECT, ForestryConstants.forestry("tree_effect_none"));
				karyotype.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_AVERAGE);
				karyotype.set(TreeChromosomes.GIRTH, ForestryAlleles.GIRTH_1);
				karyotype.set(TreeChromosomes.FIREPROOF, false);
			})
			.addStages(TreeLifeStage.SAPLING, TreeLifeStage.POLLEN)
			.setDefaultStage(TreeLifeStage.SAPLING);

		// Butterfly type
		genetics.registerSpeciesType(ForestrySpeciesTypes.BUTTERFLY, ButterflySpeciesType::new)
			.setKaryotype(karyotype -> {
				karyotype.setSpecies(ButterflyChromosomes.SPECIES, ForestryButterflySpecies.MONARCH);
				karyotype.set(ButterflyChromosomes.SIZE, ForestryAlleles.SIZE_SMALL);
				karyotype.set(ButterflyChromosomes.SPEED, ForestryAlleles.SPEED_SLOWEST);
				karyotype.set(ButterflyChromosomes.LIFESPAN, ForestryAlleles.LIFESPAN_SHORTER);
				karyotype.set(ButterflyChromosomes.METABOLISM, ForestryAlleles.METABOLISM_SLOWER);
				karyotype.set(ButterflyChromosomes.FERTILITY, ForestryAlleles.FERTILITY_3);
				karyotype.set(ButterflyChromosomes.TEMPERATURE_TOLERANCE, ForestryAlleles.TOLERANCE_NONE)
					.setWeaklyInherited(true);
				karyotype.set(ButterflyChromosomes.HUMIDITY_TOLERANCE, ForestryAlleles.TOLERANCE_NONE)
					.setWeaklyInherited(true);
				karyotype.set(ButterflyChromosomes.NEVER_SLEEPS, false)
					.setWeaklyInherited(true);
				karyotype.set(ButterflyChromosomes.TOLERATES_RAIN, false)
					.setWeaklyInherited(true);
				karyotype.set(ButterflyChromosomes.FIREPROOF, false);
				karyotype.set(ButterflyChromosomes.FLOWER_TYPE, ForestryFlowerTypes.VANILLA);
				karyotype.set(ButterflyChromosomes.EFFECT, ForestryButterflyEffects.NONE);
				karyotype.set(ButterflyChromosomes.COCOON, ForestryCocoons.DEFAULT);
			})
			.addStages(ButterflyLifeStage.BUTTERFLY, ButterflyLifeStage.SERUM, ButterflyLifeStage.CATERPILLAR, ButterflyLifeStage.COCOON)
			.setDefaultStage(ButterflyLifeStage.BUTTERFLY)
			.addResearchMaterials(map -> map.put(Items.GLASS_BOTTLE, 0.9f));

		// Taxonomy is no longer registered in code here: base Forestry's whole taxonomy ships as datapack JSON
		// (generated by TaxonProvider from ForestryTaxonomy) and is merged into the live taxonomy on datapack
		// (re)load by TaxonManager, before species are projected.

		// Filter rules for the Genetic Filter
		genetics.registerFilterRuleTypes(DefaultFilterRuleType.values());
		genetics.registerFilterRuleTypes(LepidopterologyFilterRuleType.values());
		LepidopterologyFilterRule.init();
	}







	@Override
	public void registerLepidopterology(ILepidopterologyRegistration lepidopterology) {
		DefaultButterflySpecies.register(lepidopterology);

		lepidopterology.registerCocoon(ForestryCocoons.DEFAULT, new DefaultCocoon("default", List.of(
			Product.of(Items.STRING, 2, 1f),
			Product.of(Items.STRING, 1, 0.75f),
			Product.of(Items.STRING, 3, 0.25f)
		)));

		lepidopterology.registerCocoon(ForestryCocoons.SILK, new DefaultCocoon("silk", List.of(
			Product.of(CoreItems.CRAFTING_MATERIALS.item(EnumCraftingMaterial.SILK_WISP), 3, 0.75f),
			Product.of(CoreItems.CRAFTING_MATERIALS.item(EnumCraftingMaterial.SILK_WISP), 2, 0.25f)
		)));

		lepidopterology.registerEffect(ForestryButterflyEffects.NONE, new DummyButterflyEffect());
	}

	@Override
	public void registerCircuits(ICircuitRegistration circuits) {
		// Layouts
		circuits.registerLayout(ForestryCircuitLayouts.MANAGED_FARM, ForestryCircuitSocketTypes.FARM);
		circuits.registerLayout(ForestryCircuitLayouts.MANUAL_FARM, ForestryCircuitSocketTypes.FARM);
		circuits.registerLayout(ForestryCircuitLayouts.MACHINE_UPGRADE, ForestryCircuitSocketTypes.MACHINE);

		// Managed Farms
		registerFarmCircuit(circuits, EnumElectronTube.COPPER, ForestryFarmTypes.ARBOREAL, false);
		registerFarmCircuit(circuits, EnumElectronTube.TIN, ForestryFarmTypes.PEAT, false);
		registerFarmCircuit(circuits, EnumElectronTube.BRONZE, ForestryFarmTypes.CROPS, false);
		registerFarmCircuit(circuits, EnumElectronTube.IRON, ForestryFarmTypes.ENDER, false);
		registerFarmCircuit(circuits, EnumElectronTube.BLAZE, ForestryFarmTypes.INFERNAL, false);
		registerFarmCircuit(circuits, EnumElectronTube.OBSIDIAN, ForestryFarmTypes.GOURD, false);
		registerFarmCircuit(circuits, EnumElectronTube.APATITE, ForestryFarmTypes.SHROOM, false);

		// Manual Farms
		registerFarmCircuit(circuits, EnumElectronTube.COPPER, ForestryFarmTypes.ORCHARD, true);
		registerFarmCircuit(circuits, EnumElectronTube.TIN, ForestryFarmTypes.PEAT, true);
		registerFarmCircuit(circuits, EnumElectronTube.BRONZE, ForestryFarmTypes.CROPS, true);
		registerFarmCircuit(circuits, EnumElectronTube.IRON, ForestryFarmTypes.ENDER, true);
		registerFarmCircuit(circuits, EnumElectronTube.GOLD, ForestryFarmTypes.SUCCULENTES, true);
		registerFarmCircuit(circuits, EnumElectronTube.DIAMOND, ForestryFarmTypes.POALES, true);
		registerFarmCircuit(circuits, EnumElectronTube.OBSIDIAN, ForestryFarmTypes.GOURD, true);
		registerFarmCircuit(circuits, EnumElectronTube.APATITE, ForestryFarmTypes.SHROOM, true);
		registerFarmCircuit(circuits, EnumElectronTube.LAPIS, ForestryFarmTypes.COCOA, true);

		// Factory
		circuits.registerCircuit(ForestryCircuitLayouts.MACHINE_UPGRADE, CoreItems.ELECTRON_TUBES.stack(EnumElectronTube.EMERALD, 1), new CircuitMachineUpgrade("machine.speed.boost.1", 0.125f, 0.05f, 1.0f));
		circuits.registerCircuit(ForestryCircuitLayouts.MACHINE_UPGRADE, CoreItems.ELECTRON_TUBES.stack(EnumElectronTube.BLAZE, 1), new CircuitMachineUpgrade("machine.speed.boost.2", 0.250f, 0.10f, 1.0f));
		circuits.registerCircuit(ForestryCircuitLayouts.MACHINE_UPGRADE, CoreItems.ELECTRON_TUBES.stack(EnumElectronTube.GOLD, 1), new CircuitMachineUpgrade("machine.efficiency.1", 0, -0.10f, 1.0f));
		circuits.registerCircuit(ForestryCircuitLayouts.MACHINE_UPGRADE, CoreItems.ELECTRON_TUBES.stack(EnumElectronTube.AMBER, 1), new CircuitMachineUpgrade("machine.fortune.1", 0, 0.05f, 1.25f));
	}

	private static void registerFarmCircuit(ICircuitRegistration circuits, EnumElectronTube tube, ResourceLocation typeId, boolean manual) {
		String id = manual ? "farm.manual." + typeId.getPath() : "farm.managed." + typeId.getPath();
		circuits.registerCircuit(manual ? ForestryCircuitLayouts.MANUAL_FARM : ForestryCircuitLayouts.MANAGED_FARM, CoreItems.ELECTRON_TUBES.stack(tube, 1), new CircuitFarmLogic(id, typeId, manual));
	}

	@Override
	public void registerErrors(IErrorRegistration errors) {
		for (IError error : ForestryError.values()) {
			errors.registerError(error);
		}
	}

	@Override
	public void registerFarming(IFarmingRegistration farming) {
		DefaultFarms.registerFarmTypes(farming);

		farming.registerFertilizer(CoreItems.FERTILIZER_COMPOUND.get(), 500);
	}



	@Override
	public void registerClient(Consumer<Consumer<IClientRegistration>> registrar) {
		registrar.accept(new ApicultureClientRegistration());
		registrar.accept(new ArboricultureClientRegistration());
		registrar.accept(new LepidopterologyClientRegistration());
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
