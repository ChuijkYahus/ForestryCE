package forestry.arboriculture.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.arboriculture.entities.ForestryBoat;
import forestry.arboriculture.entities.ForestryChestBoat;
import forestry.modules.features.FeatureEntityType;
import forestry.modules.features.FeatureProvider;
import forestry.modules.features.ModFeatureRegistry;
import net.minecraft.world.entity.MobCategory;

@FeatureProvider
public class ArboricultureEntities {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.ARBORICULTURE);

	public static final FeatureEntityType<ForestryBoat> BOAT = REGISTRY.entity(ForestryBoat::new, MobCategory.MISC, "boat", builder -> builder.sized(1.375F, 0.5625F).clientTrackingRange(10));
	public static final FeatureEntityType<ForestryBoat> CHEST_BOAT = REGISTRY.entity(ForestryChestBoat::new, MobCategory.MISC, "chest_boat", builder -> builder.sized(1.375F, 0.5625F).clientTrackingRange(10));
}
