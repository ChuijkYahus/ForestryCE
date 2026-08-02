package forestry.apiculture.features;

import forestry.apiculture.items.EnumHoneyComb;
import forestry.apiculture.items.EnumPollenCluster;
import forestry.apiculture.items.EnumPropolis;
import forestry.core.platform.registration.FeatureItem;
import forestry.core.platform.registration.FeatureItemGroup;
import forestry.core.platform.registration.FeatureProvider;
import forestry.storage.features.CrateItems;
import forestry.storage.items.ItemCrated;

/**
 * Crates for apiculture products. Registered here rather than in {@link CrateItems} so the base
 * artifact does not name a bee item. The crates still register through base's registry, so their
 * ids and generated models are unchanged.
 */
@FeatureProvider
public class ApicultureCrates {
	public static final FeatureItem<ItemCrated> CRATED_POLLEN_CLUSTER_NORMAL = CrateItems.registerCrate(ApicultureItems.POLLEN_CLUSTER.get(EnumPollenCluster.NORMAL), "crated_pollen_cluster_normal");
	public static final FeatureItem<ItemCrated> CRATED_POLLEN_CLUSTER_CRYSTALLINE = CrateItems.registerCrate(ApicultureItems.POLLEN_CLUSTER.get(EnumPollenCluster.CRYSTALLINE), "crated_pollen_cluster_crystalline");
	public static final FeatureItem<ItemCrated> CRATED_PROPOLIS = CrateItems.registerCrate(ApicultureItems.PROPOLIS.get(EnumPropolis.NORMAL), "crated_propolis");
	public static final FeatureItem<ItemCrated> CRATED_ROYAL_JELLY = CrateItems.registerCrate(ApicultureItems.ROYAL_JELLY, "crated_royal_jelly");
	public static final FeatureItemGroup<ItemCrated, EnumHoneyComb> CRATED_BEE_COMBS = CrateItems.registry().itemGroup(comb -> new ItemCrated(() -> ApicultureItems.BEE_COMBS.get(comb).stack()), EnumHoneyComb.VALUES).identifier(comb -> "crated_" + (comb == EnumHoneyComb.SPONGE ? "spongy" : comb.getSerializedName()) + "_comb").create();

	static {
		CrateItems.addCrates(CRATED_BEE_COMBS.getFeatures());
	}
}
