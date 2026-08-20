package forestry.core.features;

import forestry.api.ForestryRegistries;
import forestry.api.core.FluidProduct;
import forestry.api.core.FluidProductType;
import forestry.api.core.Product;
import forestry.api.core.ProductType;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The built-in product and fluid product types. Both dispatch codecs treat their default type as the absent
 * {@code "type"} key, so these two ids only ever appear in JSON written by an addon that re-declares them.
 */
@FeatureProvider
public class CoreProductTypes {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CORE);

	public static final DeferredRegister<ProductType<?>> PRODUCT_TYPES = REGISTRY.getRegistry(ForestryRegistries.Keys.PRODUCT_TYPE);
	public static final DeferredRegister<FluidProductType<?>> FLUID_PRODUCT_TYPES = REGISTRY.getRegistry(ForestryRegistries.Keys.FLUID_PRODUCT_TYPE);

	public static final DeferredHolder<ProductType<?>, ProductType<Product>> ITEM = PRODUCT_TYPES.register("item", () -> Product.TYPE);
	public static final DeferredHolder<FluidProductType<?>, FluidProductType<FluidProduct>> FLUID = FLUID_PRODUCT_TYPES.register("fluid", () -> FluidProduct.TYPE);
}
