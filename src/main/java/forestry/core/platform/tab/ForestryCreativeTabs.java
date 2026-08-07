package forestry.core.platform.tab;

import forestry.api.ForestryConstants;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.block.NaturalistChestBlockType;
import forestry.core.platform.block.BlockTypeCoreTesr;
import forestry.core.features.CoreBlocks;
import forestry.core.features.CoreItems;
import forestry.core.features.FluidsItems;
import forestry.core.platform.fluids.ForestryFluids;
import forestry.core.platform.item.EnumContainerType;
import forestry.core.platform.item.FluidHandlerItemForestry;
import forestry.core.content.energy.features.EnergyBlocks;
import forestry.core.content.machines.blocks.BlockTypeFactoryPlain;
import forestry.core.content.machines.blocks.BlockTypeFactoryTesr;
import forestry.core.content.machines.features.FactoryBlocks;
import forestry.core.platform.registration.*;
import forestry.core.content.sorting.features.SortingBlocks;
import forestry.core.content.backpacks.features.BackpackItems;
import forestry.core.content.backpacks.features.CrateItems;
import forestry.core.content.backpacks.items.ItemCrated;
import forestry.core.content.worktable.features.WorktableBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

@FeatureProvider
public class ForestryCreativeTabs {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CORE);

	public static final FeatureCreativeTab FORESTRY = REGISTRY.creativeTab(ForestryConstants.MOD_ID, tab -> {
		tab.icon(CoreItems.PORTABLE_ALYZER::stack);
		tab.displayItems(ForestryCreativeTabs::addForestryItems);
		tab.withTabsBefore(CreativeModeTabs.SPAWN_EGGS);
		tab.withTabsAfter(tabKey("storage"), tabKey("apiculture"), tabKey("arboriculture"), tabKey("lepidopterology"));
	});
	public static final FeatureCreativeTab STORAGE = REGISTRY.creativeTab("storage", tab -> {
		tab.icon(BackpackItems.MINER_BACKPACK::stack);
		tab.displayItems(ForestryCreativeTabs::addStorageItems);
		tab.withTabsBefore(tabKey("agriculture"));
		tab.withTabsAfter(tabKey("mail"));
	});

	/**
	 * Builds a creative tab ordering key from its id. Lets a tab reference tabs owned by other
	 * modules without naming their holder classes, so the ordering survives the module split.
	 */
	public static ResourceKey<CreativeModeTab> tabKey(String path) {
		return ResourceKey.create(Registries.CREATIVE_MODE_TAB, ForestryConstants.forestry(path));
	}

	private static void addForestryItems(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output items) {
		// Genetics tools
		addGeneticBasics(items);
		items.accept(CoreItems.FORESTERS_MANUAL);
		items.accept(CoreItems.SCOOP);
		items.accept(CoreItems.SPECTACLES);
		items.accept(SortingBlocks.FILTER);

		// Storages
		items.accept(BackpackItems.APIARIST_BACKPACK);
		items.accept(BackpackItems.ARBORIST_BACKPACK);
		items.accept(BackpackItems.LEPIDOPTERIST_BACKPACK);
		CoreBlocks.NATURALIST_CHEST.getItems().forEach(items::accept);

		// Machine tools
		items.accept(CoreItems.WRENCH);
		items.accept(CoreItems.PIPETTE);
		items.accept(CoreItems.SOLDERING_IRON);
		items.accept(WorktableBlocks.WORKTABLE);
		// Engines
		EnergyBlocks.ENGINES.getItems().forEach(items::accept);
		// Machines
		FactoryBlocks.TESR.getItems().forEach(items::accept);
		// Circuit boards
		items.accept(FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.FABRICATOR));
		CoreItems.CIRCUITBOARDS.getItems().forEach(items::accept);
		CoreItems.ELECTRON_TUBES.getItems().forEach(items::accept);

		// Ores
		items.accept(CoreBlocks.APATITE_ORE);
		items.accept(CoreBlocks.DEEPSLATE_APATITE_ORE);
		items.accept(CoreBlocks.TIN_ORE);
		items.accept(CoreBlocks.DEEPSLATE_TIN_ORE);
		// Raw Ores
		items.accept(CoreItems.APATITE);
		items.accept(CoreItems.RAW_TIN);
		items.accept(CoreItems.AMBER);
		// Processed ores
		items.accept(CoreItems.FERTILIZER_COMPOUND);
		items.accept(CoreItems.INGOT_TIN);
		items.accept(CoreItems.INGOT_BRONZE);
		// Block forms
		items.accept(CoreBlocks.RAW_TIN_BLOCK);
		CoreBlocks.RESOURCE_STORAGE.getItems().forEach(items::accept);
		// Gears
		items.accept(CoreItems.GEAR_COPPER);
		items.accept(CoreItems.GEAR_TIN);
		items.accept(CoreItems.GEAR_BRONZE);
		// Casings
		items.accept(CoreItems.STURDY_CASING);
		items.accept(CoreItems.HARDENED_CASING);
		items.accept(CoreItems.IMPREGNATED_CASING);
		items.accept(CoreItems.FLEXIBLE_CASING);

		items.accept(CoreItems.FERTILIZER_COMPOUND);
		items.accept(CoreItems.CARTON);
		items.accept(CoreItems.SURVIVALISTS_PICKAXE);
		items.accept(CoreItems.SURVIVALISTS_SHOVEL);
		items.accept(CoreItems.SURVIVALISTS_AXE);
		items.accept(CoreItems.SURVIVALISTS_SWORD);
		items.accept(CoreItems.SURVIVALISTS_HOE);
		items.accept(CoreItems.PICKAXE_KIT);
		items.accept(CoreItems.SHOVEL_KIT);
		items.accept(CoreItems.AXE_KIT);
		items.accept(CoreItems.SWORD_KIT);
		items.accept(CoreItems.HOE_KIT);
		items.accept(CoreItems.GEAR_TIN);
		items.accept(CoreItems.GEAR_COPPER);
		items.accept(CoreItems.GEAR_BRONZE);
		items.accept(CoreItems.SOLDERING_IRON);
		items.accept(CoreItems.SPECTACLES);
		items.accept(CoreItems.ASH);
		items.accept(CoreItems.PEAT);
		items.accept(CoreItems.BITUMINOUS_PEAT);
		items.accept(CoreItems.BEESWAX);
		items.accept(CoreItems.REFRACTORY_WAX);
		// todo merge more items into crafting materials
		CoreItems.CRAFTING_MATERIALS.getItems().forEach(items::accept);

		// Escritoire output — research notes ship players hint about mutations. Worth
		// surfacing in creative so they can be inspected without needing the workflow.
		items.accept(CoreItems.RESEARCH_NOTE);
	}

	private static void addStorageItems(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output items) {
		// Genetics backpacks
		items.accept(BackpackItems.APIARIST_BACKPACK);
		items.accept(BackpackItems.ARBORIST_BACKPACK);
		items.accept(BackpackItems.LEPIDOPTERIST_BACKPACK);

		// T1
		items.accept(BackpackItems.MINER_BACKPACK);
		items.accept(BackpackItems.DIGGER_BACKPACK);
		items.accept(BackpackItems.FORESTER_BACKPACK);
		items.accept(BackpackItems.HUNTER_BACKPACK);
		items.accept(BackpackItems.ADVENTURER_BACKPACK);
		items.accept(BackpackItems.BUILDER_BACKPACK);

		// T2
		items.accept(BackpackItems.MINER_BACKPACK_T_2);
		items.accept(BackpackItems.DIGGER_BACKPACK_T_2);
		items.accept(BackpackItems.FORESTER_BACKPACK_T_2);
		items.accept(BackpackItems.HUNTER_BACKPACK_T_2);
		items.accept(BackpackItems.ADVENTURER_BACKPACK_T_2);
		items.accept(BackpackItems.BUILDER_BACKPACK_T_2);

		// Packing machines
		items.accept(FactoryBlocks.TESR.get(BlockTypeFactoryTesr.BOTTLER));
		items.accept(FactoryBlocks.TESR.get(BlockTypeFactoryTesr.CARPENTER));

		// Misc gear
		items.accept(CoreItems.PIPETTE);
		items.accept(CoreBlocks.NATURALIST_CHEST.get(NaturalistChestBlockType.APIARIST_CHEST));
		items.accept(CoreBlocks.NATURALIST_CHEST.get(NaturalistChestBlockType.ARBORIST_CHEST));
		items.accept(CoreBlocks.NATURALIST_CHEST.get(NaturalistChestBlockType.LEPIDOPTERIST_CHEST));
		items.accept(SortingBlocks.FILTER);

		// Empty containers
		items.accept(CoreItems.CARTON);
		FluidsItems.CONTAINERS.getItems().forEach(items::accept);
		items.accept(CrateItems.CRATE);

		// Filled cartons
		items.accept(CoreItems.PICKAXE_KIT);
		items.accept(CoreItems.SHOVEL_KIT);
		items.accept(CoreItems.AXE_KIT);
		items.accept(CoreItems.SWORD_KIT);
		items.accept(CoreItems.HOE_KIT);

		// Filled containers
		for (EnumContainerType type : EnumContainerType.values()) {
			for (Fluid fluid : BuiltInRegistries.FLUID) {
				if (fluid instanceof FlowingFluid flowing && flowing.getSource() != fluid) {
					continue;
				}

				ItemStack itemStack = FluidsItems.CONTAINERS.stack(type);

				IFluidHandlerItem fluidHandler = new FluidHandlerItemForestry(itemStack, type);
				if (fluidHandler.fill(new FluidStack(fluid, FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE) == FluidType.BUCKET_VOLUME) {
					ItemStack filled = fluidHandler.getContainer();
					items.accept(filled);
				}
			}
		}

		// Filled buckets
		for (ForestryFluids type : ForestryFluids.values()) {
			items.accept(type.getBucket());
		}

		// Filled crates
		for (FeatureItem<ItemCrated> crate : CrateItems.getCrates()) {
			items.accept(crate);
		}
	}

	public static void addGeneticBasics(CreativeModeTab.Output items) {
		items.accept(CoreItems.PORTABLE_ALYZER);
		items.accept(CoreItems.HONEY_DROP);
		items.accept(CoreItems.HONEYDEW);
		items.accept(CoreBlocks.BASE.get(BlockTypeCoreTesr.ESCRITOIRE));
		items.accept(CoreBlocks.BASE.get(BlockTypeCoreTesr.ANALYZER));
	}
}
