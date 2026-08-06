package forestry.core.data;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import forestry.core.platform.registration.FeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

/**
 * Which registry ids a set of modules registered. Read out of the live registries, so it cannot drift
 * from what the code registers. Used to scope automatic name generation to one jar.
 */
public final class JarModules {
	private JarModules() {
	}

	/**
	 * @param moduleIds The modules a jar owns. Ex. {@code Set.of(ForestryModuleIds.MAIL)}
	 * @return Every id those modules registered, in any registry
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static Set<ResourceLocation> ownedIds(Set<ResourceLocation> moduleIds) {
		Set<ResourceLocation> ids = new HashSet<>();
		for (ModFeatureRegistry modRegistry : ModFeatureRegistry.getRegistries().values()) {
			for (Map.Entry<ResourceLocation, FeatureRegistry> module : modRegistry.getModules().entrySet()) {
				if (!moduleIds.contains(module.getKey())) {
					continue;
				}
				for (Map.Entry<ResourceKey, DeferredRegister> registry : module.getValue().getRegistries().entrySet()) {
					for (DeferredHolder<?, ?> holder : (Collection<DeferredHolder<?, ?>>) registry.getValue().getEntries()) {
						ids.add(holder.getId());
					}
				}
			}
		}
		// A module named here that registered nothing is a typo or a rename, and it would silently scope
		// a jar's generation to less than it owns
		if (ids.isEmpty()) {
			throw new IllegalStateException("No registered ids for modules: " + moduleIds);
		}
		return ids;
	}
}
