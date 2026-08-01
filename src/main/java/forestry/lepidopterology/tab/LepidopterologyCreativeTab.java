package forestry.lepidopterology.tab;

import forestry.api.ForestryConstants;
import forestry.api.genetics.ForestrySpeciesTypes;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.blocks.NaturalistChestBlockType;
import forestry.core.features.CoreBlocks;
import forestry.core.features.CoreItems;
import forestry.core.tab.ForestryCreativeTabs;
import forestry.core.utils.SpeciesUtil;
import forestry.modules.features.FeatureCreativeTab;
import forestry.modules.features.FeatureProvider;
import forestry.modules.features.IFeatureRegistry;
import forestry.modules.features.ModFeatureRegistry;
import forestry.storage.features.BackpackItems;
import net.minecraft.world.item.CreativeModeTab;
import forestry.api.lepidopterology.ForestryButterflySpecies;
import forestry.api.lepidopterology.genetics.ButterflyLifeStage;

/**
 * The lepidopterology creative tab. Ordering keys are built from tab ids rather than tab objects so
 * this module does not depend on the others' holder classes.
 */
@FeatureProvider
public class LepidopterologyCreativeTab {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.LEPIDOPTEROLOGY);

	public static final FeatureCreativeTab LEPIDOPTEROLOGY = REGISTRY.creativeTab("lepidopterology", tab -> {
		tab.icon(() -> SpeciesUtil.BUTTERFLY_TYPE.get().createStack(ForestryButterflySpecies.MONARCH, ButterflyLifeStage.BUTTERFLY));
		tab.displayItems(LepidopterologyCreativeTab::addLepidopterologyItems);
		tab.withTabsBefore(ForestryCreativeTabs.tabKey("arboriculture"));
		tab.withTabsAfter(ForestryCreativeTabs.tabKey("agriculture"));
	});

	static void addLepidopterologyItems(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output items) {
		// Genetics
		ForestryCreativeTabs.addGeneticBasics(items);
		items.accept(BackpackItems.LEPIDOPTERIST_BACKPACK);
		items.accept(CoreBlocks.NATURALIST_CHEST.get(NaturalistChestBlockType.LEPIDOPTERIST_CHEST));

		// Gear
		items.accept(CoreItems.SCOOP);

		// Specimens
		SpeciesUtil.addTypeToCreativeTab(items, ForestrySpeciesTypes.BUTTERFLY);
	}
}
