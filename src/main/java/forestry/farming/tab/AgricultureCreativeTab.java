package forestry.farming.tab;

import forestry.api.ForestryConstants;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.blocks.NaturalistChestBlockType;
import forestry.core.features.CoreBlocks;
import forestry.core.features.CoreItems;
import forestry.core.platform.tab.ForestryCreativeTabs;
import forestry.core.platform.util.SpeciesUtil;
import forestry.core.platform.registration.FeatureCreativeTab;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import forestry.storage.features.BackpackItems;
import net.minecraft.world.item.CreativeModeTab;
import forestry.cultivation.blocks.BlockTypePlanter;
import forestry.cultivation.features.CultivationBlocks;
import forestry.energy.features.EnergyBlocks;
import forestry.factory.blocks.BlockTypeFactoryPlain;
import forestry.factory.blocks.BlockTypeFactoryTesr;
import forestry.factory.features.FactoryBlocks;
import forestry.farming.blocks.EnumFarmBlockType;
import forestry.farming.blocks.EnumFarmMaterial;
import forestry.farming.features.FarmingBlocks;

/**
 * The farming creative tab. Ordering keys are built from tab ids rather than tab objects so
 * this module does not depend on the others' holder classes.
 */
@FeatureProvider
public class AgricultureCreativeTab {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.FARMING);

	public static final FeatureCreativeTab AGRICULTURE = REGISTRY.creativeTab("agriculture", tab -> {
		tab.icon(() -> CultivationBlocks.MANAGED_PLANTER.stack(BlockTypePlanter.ARBORETUM));
		tab.displayItems(AgricultureCreativeTab::addAgricultureItems);
		tab.withTabsBefore(ForestryCreativeTabs.tabKey("lepidopterology"));
		tab.withTabsAfter(ForestryCreativeTabs.tabKey("storage"));
	});

	static void addAgricultureItems(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output items) {
		// Machine tools
		items.accept(CoreItems.WRENCH);
		items.accept(CoreItems.PIPETTE);
		items.accept(CoreItems.SOLDERING_IRON);

		// Circuit boards
		CoreItems.CIRCUITBOARDS.getItems().forEach(items::accept);
		CoreItems.ELECTRON_TUBES.getItems().forEach(items::accept);

		// Engines
		EnergyBlocks.ENGINES.getItems().forEach(items::accept);
		// Machines
		items.accept(FactoryBlocks.TESR.get(BlockTypeFactoryTesr.CARPENTER));
		items.accept(FactoryBlocks.TESR.get(BlockTypeFactoryTesr.CENTRIFUGE));
		items.accept(FactoryBlocks.TESR.get(BlockTypeFactoryTesr.FERMENTER));
		items.accept(FactoryBlocks.TESR.get(BlockTypeFactoryTesr.MOISTENER));
		items.accept(FactoryBlocks.TESR.get(BlockTypeFactoryTesr.SQUEEZER));
		items.accept(FactoryBlocks.TESR.get(BlockTypeFactoryTesr.STILL));
		items.accept(FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.RAINTANK));

		// Rainmaker
		items.accept(FactoryBlocks.TESR.get(BlockTypeFactoryTesr.RAINMAKER));
		items.accept(CoreItems.IODINE_CHARGE);
		items.accept(CoreItems.DISSIPATION_CHARGE);

		// Misc items
		items.accept(CoreItems.PEAT);
		items.accept(CoreItems.BITUMINOUS_PEAT);
		items.accept(CoreBlocks.HUMUS);
		items.accept(CoreBlocks.BOG_EARTH);
		items.accept(CoreItems.COMPOST);
		items.accept(CoreItems.MOULDY_WHEAT);
		items.accept(CoreItems.DECAYING_WHEAT);
		items.accept(CoreItems.MULCH);

		// Multi farm
		for (EnumFarmMaterial material : EnumFarmMaterial.values()) {
			for (EnumFarmBlockType type : EnumFarmBlockType.values()) {
				items.accept(FarmingBlocks.FARM.stack(type, material));
			}
		}

		// Single farm (boo)
		for (BlockTypePlanter type : BlockTypePlanter.values()) {
			items.accept(CultivationBlocks.MANAGED_PLANTER.stack(type));
			items.accept(CultivationBlocks.MANUAL_PLANTER.stack(type));
		}
	}
}
