package forestry.lepidopterology.plugin;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import forestry.api.core.Product;
import forestry.api.lepidopterology.ForestryButterflyEffects;
import forestry.api.lepidopterology.ForestryCocoons;
import forestry.api.modules.ForestryModuleIds;
import forestry.api.plugin.IForestryPlugin;
import forestry.api.plugin.IGeneticRegistration;
import forestry.api.plugin.ILepidopterologyRegistration;
import forestry.core.features.CoreItems;
import forestry.core.items.definitions.EnumCraftingMaterial;
import forestry.lepidopterology.DummyButterflyEffect;
import forestry.lepidopterology.LepidopterologyFilterRule;
import forestry.lepidopterology.LepidopterologyFilterRuleType;
import forestry.lepidopterology.genetics.DefaultCocoon;
import forestry.plugin.DefaultButterflySpecies;

/**
 * Base Forestry's lepidopterology registrations. Split out of
 * {@code forestry.plugin.DefaultForestryPlugin} so the base artifact does not register butterfly
 * content.
 */
public class LepidopterologyForestryPlugin implements IForestryPlugin {
	@Override
	public void registerGenetics(IGeneticRegistration genetics) {
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
	public ResourceLocation id() {
		return ForestryModuleIds.LEPIDOPTEROLOGY;
	}
}
