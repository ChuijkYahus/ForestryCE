package forestry.core.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.apiculture.blocks.BlockBeeHive;
import forestry.apiculture.blocks.BlockHiveType;
import forestry.apiculture.blocks.NaturalistChestBlockType;
import forestry.core.blocks.*;
import forestry.core.items.ItemBlockForestry;
import forestry.core.items.ItemBlockTesr;
import forestry.modules.features.*;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

@FeatureProvider
public class CoreBlocks {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CORE);

	public static final FeatureBlockGroup<BlockCore, BlockTypeCoreTesr> BASE = REGISTRY.blockGroup(BlockCore::new, BlockTypeCoreTesr.values()).item(ItemBlockTesr::new).create();
	public static final FeatureBlock<BlockBogEarth, ItemBlockForestry<?>> BOG_EARTH = REGISTRY.block(BlockBogEarth::new, ItemBlockForestry::new, "bog_earth");
	public static final FeatureBlock<BlockPeat, ItemBlockForestry<?>> PEAT = REGISTRY.block(BlockPeat::new, "peat");
	public static final FeatureBlock<BlockHumus, ItemBlockForestry<?>> HUMUS = REGISTRY.block(BlockHumus::new, ItemBlockForestry::new, "humus");
	public static final FeatureBlockGroup<BlockResourceStorage, EnumResourceType> RESOURCE_STORAGE = REGISTRY.blockGroup(BlockResourceStorage::new, EnumResourceType.values()).item(ItemBlockForestry::new).identifier("resource_storage").create();
	public static final FeatureBlock<Block, BlockItem> APATITE_ORE = REGISTRY.block(() -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.COAL_ORE), UniformInt.of(0, 4)), ItemBlockForestry::new, "apatite_ore");
	public static final FeatureBlock<Block, BlockItem> DEEPSLATE_APATITE_ORE = REGISTRY.block(() -> new DropExperienceBlock(BlockBehaviour.Properties.copy(APATITE_ORE.block()).mapColor(MapColor.DEEPSLATE).strength(4.5f, 3.0f).sound(SoundType.DEEPSLATE), UniformInt.of(0, 4)), ItemBlockForestry::new, "deepslate_apatite_ore");
	public static final FeatureBlock<Block, BlockItem> TIN_ORE = REGISTRY.block(() -> new Block(BlockBehaviour.Properties.copy(Blocks.COPPER_ORE)), ItemBlockForestry::new, "tin_ore");
	public static final FeatureBlock<Block, BlockItem> DEEPSLATE_TIN_ORE = REGISTRY.block(() -> new Block(BlockBehaviour.Properties.copy(TIN_ORE.block()).mapColor(MapColor.DEEPSLATE).strength(4.5f, 3.0f).sound(SoundType.DEEPSLATE)), ItemBlockForestry::new, "deepslate_tin_ore");
	public static final FeatureBlock<Block, BlockItem> RAW_TIN_BLOCK = REGISTRY.block(() -> new Block(BlockBehaviour.Properties.copy(Blocks.RAW_COPPER_BLOCK)), ItemBlockForestry::new, "raw_tin_block");

	/* Block Sets */
	//TODO: Helper method?

	public static final FeatureBlock<Block, BlockItem> ASH_BRICKS = REGISTRY.block(() -> new Block(BlockBehaviour.Properties.copy(Blocks.MUD_BRICKS)), ItemBlockForestry::new, "ash_bricks");

	public static final FeatureBlock<StairBlock, BlockItem> ASH_BRICK_STAIRS = REGISTRY.block(() -> new StairBlock(Blocks.MUD_BRICKS::defaultBlockState, BlockBehaviour.Properties.copy(Blocks.MUD_BRICK_STAIRS)), ItemBlockForestry::new, "ash_brick_stairs");

	public static final FeatureBlock<SlabBlock, BlockItem> ASH_BRICK_SLAB = REGISTRY.block(() -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.MUD_BRICK_SLAB)), ItemBlockForestry::new, "ash_brick_slab");

	public static final FeatureBlock<WallBlock, BlockItem> ASH_BRICK_WALL = REGISTRY.block(() -> new WallBlock(BlockBehaviour.Properties.copy(Blocks.MUD_BRICK_WALL)), ItemBlockForestry::new, "ash_brick_wall");
	//TODO: Chiseled Ash Bricks?

	public static final FeatureBlockGroup<BlockMetalPlating, BlockTypeMetalPlating> METAL_PLATING = REGISTRY.blockGroup(BlockMetalPlating::new, BlockTypeMetalPlating.values()).itemWithType((block, type) -> new ItemBlockForestry<>(block, new Item.Properties())).identifier("metal_plating").create();
	public static final FeatureBlockGroup<BlockTesr<NaturalistChestBlockType>, NaturalistChestBlockType> NATURALIST_CHEST = REGISTRY.blockGroup(type -> {
		return new BlockTesr<>(type, Block.Properties.of().sound(SoundType.WOOD));
	}, NaturalistChestBlockType.values()).item(ItemBlockTesr::new).create();
}
