package forestry.lepidopterology.features;

import forestry.api.lepidopterology.genetics.ButterflyLifeStage;
import forestry.api.modules.ForestryModuleIds;
import forestry.lepidopterology.butterflies.ItemButterflyGE;
import forestry.core.platform.registration.FeatureItem;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

@FeatureProvider
public class LepidopterologyItems {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.LEPIDOPTEROLOGY);

	// registry names come from the life stage so the two cannot drift; ButterflyLifeStage
	// resolves its item form back out of the registry by the same id
	public static final FeatureItem<ItemButterflyGE> BUTTERFLY_GE = REGISTRY.item(() -> new ItemButterflyGE(ButterflyLifeStage.BUTTERFLY), ButterflyLifeStage.BUTTERFLY.itemId().getPath());
	public static final FeatureItem<ItemButterflyGE> SERUM_GE = REGISTRY.item(() -> new ItemButterflyGE(ButterflyLifeStage.SERUM), ButterflyLifeStage.SERUM.itemId().getPath());
	public static final FeatureItem<ItemButterflyGE> CATERPILLAR_GE = REGISTRY.item(() -> new ItemButterflyGE(ButterflyLifeStage.CATERPILLAR), ButterflyLifeStage.CATERPILLAR.itemId().getPath());
	public static final FeatureItem<ItemButterflyGE> COCOON_GE = REGISTRY.item(() -> new ItemButterflyGE(ButterflyLifeStage.COCOON), ButterflyLifeStage.COCOON.itemId().getPath());
}
