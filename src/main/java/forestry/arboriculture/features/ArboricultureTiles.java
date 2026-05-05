package forestry.arboriculture.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.arboriculture.tiles.TileFruitPod;
import forestry.arboriculture.tiles.TileLeaves;
import forestry.arboriculture.tiles.TileSapling;
import forestry.modules.features.FeatureProvider;
import forestry.modules.features.FeatureTileType;
import forestry.modules.features.IFeatureRegistry;
import forestry.modules.features.ModFeatureRegistry;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;

import java.util.stream.Stream;

@FeatureProvider
public class ArboricultureTiles {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.ARBORICULTURE);

	public static final FeatureTileType<TileSapling> SAPLING = REGISTRY.tile(TileSapling::new, "sapling", ArboricultureBlocks.SAPLING_GE::collect);
	public static final FeatureTileType<TileLeaves> LEAVES = REGISTRY.tile(TileLeaves::new, "leaves", ArboricultureBlocks.LEAVES::collect);
	public static final FeatureTileType<TileFruitPod> PODS = REGISTRY.tile(TileFruitPod::new, "pods", ArboricultureBlocks.PODS::getList);

	public static final FeatureTileType<SignBlockEntity> SIGN = REGISTRY.tile((pos, state) -> new SignBlockEntity(ArboricultureTiles.SIGN.tileType(), pos, state), "sign", () -> Stream.concat(ArboricultureBlocks.SIGN.getList().stream(), ArboricultureBlocks.WALL_SIGN.getList().stream()).toList());
	// In 1.21, HangingSignBlockEntity's only public constructor is (BlockPos, BlockState) and is hardcoded to vanilla BlockEntityType.HANGING_SIGN internally. Register the Forestry tile type as BlockEntityType<HangingSignBlockEntity> so block-side ticker/newBlockEntity wiring is type-correct; runtime registration of the BE type to Forestry's hanging sign blocks is handled via BlockEntityType.Builder.of(...).
	public static final FeatureTileType<HangingSignBlockEntity> HANGING_SIGN = REGISTRY.tile(HangingSignBlockEntity::new, "hanging_sign", () -> Stream.concat(ArboricultureBlocks.HANGING_SIGN.getList().stream(), ArboricultureBlocks.WALL_HANGING_SIGN.getList().stream()).toList());
}
