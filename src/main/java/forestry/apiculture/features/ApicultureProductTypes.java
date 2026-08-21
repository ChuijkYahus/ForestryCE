package forestry.apiculture.features;

import forestry.api.ForestryRegistries;
import forestry.api.core.ProductType;
import forestry.api.modules.ForestryModuleIds;
import forestry.apiculture.bees.genetics.FireworkProduct;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Apiculture's product types. Core owns the registry and its dispatch codec; the module that owns a product
 * registers it.
 */
@FeatureProvider
public class ApicultureProductTypes {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.APICULTURE);

	public static final DeferredRegister<ProductType<?>> PRODUCT_TYPES = REGISTRY.getRegistry(ForestryRegistries.Keys.PRODUCT_TYPE);

	public static final DeferredHolder<ProductType<?>, ProductType<FireworkProduct>> FIREWORK = PRODUCT_TYPES.register("firework", () -> FireworkProduct.TYPE);
}
