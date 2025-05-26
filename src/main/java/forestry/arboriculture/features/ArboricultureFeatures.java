package forestry.arboriculture.features;

import forestry.api.ForestryConstants;
import forestry.api.modules.ForestryModuleIds;
import forestry.arboriculture.worldgen.TreeDecorator;
import forestry.arboriculture.worldgen.feature.ForestryTreeFeature;
import forestry.modules.features.FeatureProvider;
import forestry.modules.features.IFeatureRegistry;
import forestry.modules.features.ModFeatureRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@FeatureProvider
public class ArboricultureFeatures {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.APICULTURE);

	public static final DeferredRegister<Feature<?>> FEATURES = REGISTRY.getRegistry(Registries.FEATURE);

	public static final RegistryObject<TreeDecorator> TREE_DECORATOR = FEATURES.register("tree", TreeDecorator::new);
	public static final ResourceKey<ConfiguredFeature<?, ?>> CONFIGURED_TREE = ResourceKey.create(Registries.CONFIGURED_FEATURE, ForestryConstants.forestry("tree"));
	public static final ResourceKey<PlacedFeature> PLACED_TREE = ResourceKey.create(Registries.PLACED_FEATURE, ForestryConstants.forestry("tree"));

	public static final RegistryObject<ForestryTreeFeature> CUSTOM_TREE = FEATURES.register("custom_tree", ForestryTreeFeature::new);
}
