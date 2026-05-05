package forestry.core.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.modules.features.FeatureProvider;
import forestry.modules.features.IFeatureRegistry;
import forestry.modules.features.ModFeatureRegistry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Forestry-side {@link DataComponentType} registrations.
 *
 * <p>Registered here because the 1.21 NeoForge {@code FluidHandlerItemStack} /
 * {@code FluidHandlerItemStackSimple} constructors now require a
 * {@code Supplier<DataComponentType<SimpleFluidContent>>} for fluid storage on item stacks
 * (replacing the implicit NBT storage used in 1.20).
 */
@FeatureProvider
public class CoreDataComponents {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CORE);
	private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = REGISTRY.getRegistry(Registries.DATA_COMPONENT_TYPE);

	/**
	 * Generic fluid content component used by Forestry fluid-holding items (pipettes, capsules, cans, etc.).
	 */
	@SuppressWarnings("unchecked")
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID_CONTENT =
			(DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>>) (DeferredHolder<?, ?>) DATA_COMPONENT_TYPES.register(
					"fluid_content",
					() -> DataComponentType.<SimpleFluidContent>builder()
							.persistent(SimpleFluidContent.CODEC)
							.networkSynchronized(SimpleFluidContent.STREAM_CODEC)
							.build());
}
