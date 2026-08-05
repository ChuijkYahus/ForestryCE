package forestry.agriculture.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.agriculture.planter.blocks.BlockTypePlanter;
import forestry.agriculture.planter.tiles.*;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.FeatureTileType;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.List;
import java.util.function.Supplier;

@FeatureProvider
public class CultivationTiles {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CULTIVATION);

	public static final FeatureTileType<TileArboretum> ARBORETUM = createTile("arboretum", () -> BlockTypePlanter.ARBORETUM, TileArboretum::new);
	public static final FeatureTileType<TileBog> BOG = createTile("bog", () -> BlockTypePlanter.PEAT_POG, TileBog::new);
	public static final FeatureTileType<TileFarmCrops> CROPS = createTile("crops", () -> BlockTypePlanter.FARM_CROPS, TileFarmCrops::new);
	public static final FeatureTileType<TileFarmEnder> ENDER = createTile("ender", () -> BlockTypePlanter.FARM_ENDER, TileFarmEnder::new);
	public static final FeatureTileType<TileFarmGourd> GOURD = createTile("gourd", () -> BlockTypePlanter.FARM_GOURD, TileFarmGourd::new);
	public static final FeatureTileType<TileFarmMushroom> MUSHROOM = createTile("mushroom", () -> BlockTypePlanter.FARM_MUSHROOM, TileFarmMushroom::new);
	public static final FeatureTileType<TileFarmNether> NETHER = createTile("nether", () -> BlockTypePlanter.FARM_NETHER, TileFarmNether::new);

	private static <T extends BlockEntity> FeatureTileType<T> createTile(String id, Supplier<BlockTypePlanter> type, BlockEntityType.BlockEntitySupplier<T> supplier) {
		return REGISTRY.tile(supplier, id, () -> {
			BlockTypePlanter t = type.get();
			return List.of(CultivationBlocks.MANAGED_PLANTER.get(t).block(), CultivationBlocks.MANUAL_PLANTER.get(t).block());
		});
	}
}
