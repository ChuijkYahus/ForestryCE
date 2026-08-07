package forestry.core.platform.particles;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

@FeatureProvider
public class CoreParticles {
	private static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = ModFeatureRegistry.get(ForestryModuleIds.APICULTURE).getRegistry(Registries.PARTICLE_TYPE);

	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> REFRACTORY_WAX = PARTICLE_TYPES.register("refractory_wax", () -> new SimpleParticleType(true));
}
