package forestry.core.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.modules.features.FeatureProvider;
import forestry.modules.features.IFeatureRegistry;
import forestry.modules.features.ModFeatureRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@FeatureProvider
public class CorePaintings {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CORE);
	private static final DeferredRegister<PaintingVariant> PAINTINGS = REGISTRY.getRegistry(Registries.PAINTING_VARIANT);

	public static final DeferredHolder<PaintingVariant, PaintingVariant> MOUSETREE = PAINTINGS.register("mousetree", () -> new PaintingVariant(48, 48));
	public static final DeferredHolder<PaintingVariant, PaintingVariant> WASPHOL = PAINTINGS.register("wasphol", () -> new PaintingVariant(32, 32));
	public static final DeferredHolder<PaintingVariant, PaintingVariant> CAGE = PAINTINGS.register("cage", () -> new PaintingVariant(32, 32));
	public static final DeferredHolder<PaintingVariant, PaintingVariant> LEWIS = PAINTINGS.register("lewis", () -> new PaintingVariant(32, 48));
	public static final DeferredHolder<PaintingVariant, PaintingVariant> SITEBEE = PAINTINGS.register("site_bee", () -> new PaintingVariant(32, 32));
	public static final DeferredHolder<PaintingVariant, PaintingVariant> ALEXBLOOME = PAINTINGS.register("alex_bloome", () -> new PaintingVariant(64, 32));
	public static final DeferredHolder<PaintingVariant, PaintingVariant> SUSPICIOUS_LOOKING_TREE = PAINTINGS.register("suspicious_looking_tree", () -> new PaintingVariant(32, 48));
	public static final DeferredHolder<PaintingVariant, PaintingVariant> WISDOM = PAINTINGS.register("wisdom", () -> new PaintingVariant(32, 32));
	public static final DeferredHolder<PaintingVariant, PaintingVariant> MYSTICAL_TREE = PAINTINGS.register("mystical_tree", () -> new PaintingVariant(32, 32));
	public static final DeferredHolder<PaintingVariant, PaintingVariant> DEKU = PAINTINGS.register("deku", () -> new PaintingVariant(64, 32));
}
