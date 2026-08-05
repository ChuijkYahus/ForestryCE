package forestry.agriculture.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.agriculture.multifarm.blocks.EnumFarmBlockType;
import forestry.agriculture.multifarm.blocks.EnumFarmMaterial;
import forestry.agriculture.multifarm.blocks.FarmBlock;
import forestry.agriculture.multifarm.items.ItemBlockFarm;
import forestry.core.platform.registration.FeatureBlockTable;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

@FeatureProvider
public class FarmingBlocks {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.FARMING);

	public static final FeatureBlockTable<FarmBlock, EnumFarmBlockType, EnumFarmMaterial> FARM = REGISTRY.blockTable(FarmBlock::create, EnumFarmBlockType.values(), EnumFarmMaterial.values()).item(ItemBlockFarm::new).identifier((part, material) -> {
		String mat = switch (material) {
			case SANDSTONE_CHISELED -> "chiseled_sandstone";
			case BRICK_NETHER -> "nether_brick";
			case BRICK_CHISELED -> "chiseled_stone_brick";
			case QUARTZ_CHISELED -> "chiseled_quartz";
			case QUARTZ_LINES -> "quartz_pillar";
			default -> material.getSerializedName();
		};
		String partName = (part == EnumFarmBlockType.PLAIN && material == EnumFarmMaterial.STONE_BRICK) ? "block" : part.getSerializedName();
		return mat + "_farm_" + partName;
	}).create();
}
