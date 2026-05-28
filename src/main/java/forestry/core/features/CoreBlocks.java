package forestry.core.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.apiculture.blocks.NaturalistChestBlockType;
import forestry.core.blocks.*;
import forestry.core.items.ItemBlockForestry;
import forestry.core.items.ItemBlockTesr;
import forestry.core.items.ItemProperties;
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

	public static final FeatureBlock<BlockBurnBarrel, ItemBlockForestry<?>> BURN_BARREL = REGISTRY.block(BlockBurnBarrel::new, ItemBlockForestry::new, "burn_barrel");

	public static final FeatureBlock<BlockBogEarth, ItemBlockForestry<?>> BOG_EARTH = REGISTRY.block(BlockBogEarth::new, ItemBlockForestry::new, "bog_earth");
	public static final FeatureBlock<BlockPeat, ItemBlockForestry<?>> PEAT = REGISTRY.block(BlockPeat::new, "peat");
	public static final FeatureBlock<BlockHumus, ItemBlockForestry<?>> HUMUS = REGISTRY.block(BlockHumus::new, ItemBlockForestry::new, "humus");
	public static final FeatureBlockGroup<BlockResourceStorage, EnumResourceType> RESOURCE_STORAGE = REGISTRY.blockGroup(BlockResourceStorage::new, EnumResourceType.values()).item(ItemBlockForestry::new).identifier("resource_storage").create();
	public static final FeatureBlock<Block, BlockItem> APATITE_ORE = REGISTRY.block(() -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.COAL_ORE), UniformInt.of(0, 4)), ItemBlockForestry::new, "apatite_ore");
	public static final FeatureBlock<Block, BlockItem> DEEPSLATE_APATITE_ORE = REGISTRY.block(() -> new DropExperienceBlock(BlockBehaviour.Properties.copy(APATITE_ORE.block()).mapColor(MapColor.DEEPSLATE).strength(4.5f, 3.0f).sound(SoundType.DEEPSLATE), UniformInt.of(0, 4)), ItemBlockForestry::new, "deepslate_apatite_ore");
	public static final FeatureBlock<Block, BlockItem> TIN_ORE = REGISTRY.block(() -> new Block(BlockBehaviour.Properties.copy(Blocks.COPPER_ORE)), ItemBlockForestry::new, "tin_ore");
	public static final FeatureBlock<Block, BlockItem> DEEPSLATE_TIN_ORE = REGISTRY.block(() -> new Block(BlockBehaviour.Properties.copy(TIN_ORE.block()).mapColor(MapColor.DEEPSLATE).strength(4.5f, 3.0f).sound(SoundType.DEEPSLATE)), ItemBlockForestry::new, "deepslate_tin_ore");
	public static final FeatureBlock<Block, BlockItem> RAW_TIN_BLOCK = REGISTRY.block(() -> new Block(BlockBehaviour.Properties.copy(Blocks.RAW_COPPER_BLOCK).mapColor(MapColor.METAL)), ItemBlockForestry::new, "raw_tin_block");

	public static final FeatureBlock<Block, BlockItem> TURF_BLOCK = REGISTRY.block(() -> new Block(BlockBehaviour.Properties.copy(Blocks.GRASS_BLOCK)), ItemBlockForestry::new, "turf_block");

	public static final FeatureBlock<Block, BlockItem> TURF = REGISTRY.block(() -> new CarpetBlock(BlockBehaviour.Properties.copy(Blocks.GRASS_BLOCK)), ItemBlockForestry::new, "turf");


	//public static final FeatureBlock<DecorativeLogPileBlock, BlockItem> DECORATIVE_LOG_PILE = REGISTRY.block(DecorativeLogPileBlock::new, (block) -> new ItemBlockForestry<>(block, new ItemProperties().burnTime(1200)), "decorative_log_pile");

	public static final FeatureBlock<SheetBlock, BlockItem> PLYWOOD_SHEET = REGISTRY.block(SheetBlock::new, (block) ->
		new ItemBlockForestry<>(block, new ItemProperties().burnTime(50)), "plywood");


	public static final FeatureBlock<RotatedPillarBlock, BlockItem> PLYWOOD_BLOCK = REGISTRY.block(
		() -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)),
		(block) -> new ItemBlockForestry<>(block, new ItemProperties().burnTime(300)),
		"plywood_block");

	//TODO: needs to have less hardness and stuff to make it 'softer'
	public static final FeatureBlock<Block, BlockItem> CORK = REGISTRY.block(() -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).sound(SoundType.CHERRY_WOOD)), ItemBlockForestry::new, "cork");

		//() -> new BlockSheet(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)), ItemBlockForestry::new, "plywood");

	//public static final FeatureBlock<Block, BlockItem> PLYWOOD_BLOCK = REGISTRY.block(() -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)), ItemBlockForestry::new, "plywood_block");

	/* Block Sets */
	//TODO: Helper method?

	public static final FeatureBlock<Block, BlockItem> ASH_BRICKS = REGISTRY.block(() -> new Block(BlockBehaviour.Properties.copy(Blocks.MUD_BRICKS).mapColor(MapColor.COLOR_LIGHT_GRAY)), ItemBlockForestry::new, "ash_bricks");

	public static final FeatureBlock<StairBlock, BlockItem> ASH_BRICK_STAIRS = REGISTRY.block(() -> new StairBlock(Blocks.MUD_BRICKS::defaultBlockState, BlockBehaviour.Properties.copy(Blocks.MUD_BRICK_STAIRS).mapColor(MapColor.COLOR_LIGHT_GRAY)), ItemBlockForestry::new, "ash_brick_stairs");

	public static final FeatureBlock<SlabBlock, BlockItem> ASH_BRICK_SLAB = REGISTRY.block(() -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.MUD_BRICK_SLAB).mapColor(MapColor.COLOR_LIGHT_GRAY)), ItemBlockForestry::new, "ash_brick_slab");

	public static final FeatureBlock<WallBlock, BlockItem> ASH_BRICK_WALL = REGISTRY.block(() -> new WallBlock(BlockBehaviour.Properties.copy(Blocks.MUD_BRICK_WALL).mapColor(MapColor.COLOR_LIGHT_GRAY)), ItemBlockForestry::new, "ash_brick_wall");
	//TODO: Chiseled Ash Bricks?


	public static final FeatureBlock<Block, BlockItem> HARDENED_WAX_BLOCK = REGISTRY.block(() -> new Block(BlockBehaviour.Properties.copy(Blocks.MUD_BRICKS).sound(SoundType.MUD).mapColor(MapColor.COLOR_YELLOW)), ItemBlockForestry::new, "hardened_wax_block");
	public static final FeatureBlock<Block, BlockItem> WAX_BRICKS = REGISTRY.block(() -> new Block(BlockBehaviour.Properties.copy(Blocks.MUD_BRICKS).sound(SoundType.MUD).mapColor(MapColor.COLOR_YELLOW)), ItemBlockForestry::new, "wax_bricks");
	public static final FeatureBlock<StairBlock, BlockItem> WAX_BRICK_STAIRS = REGISTRY.block(() -> new StairBlock(Blocks.MUD_BRICKS::defaultBlockState, BlockBehaviour.Properties.copy(Blocks.MUD_BRICK_STAIRS).sound(SoundType.MUD).mapColor(MapColor.COLOR_YELLOW)), ItemBlockForestry::new, "wax_brick_stairs");
	public static final FeatureBlock<SlabBlock, BlockItem> WAX_BRICK_SLAB = REGISTRY.block(() -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.MUD_BRICK_SLAB).sound(SoundType.MUD).mapColor(MapColor.COLOR_YELLOW)), ItemBlockForestry::new, "wax_brick_slab");
	public static final FeatureBlock<WallBlock, BlockItem> WAX_BRICK_WALL = REGISTRY.block(() -> new WallBlock(BlockBehaviour.Properties.copy(Blocks.MUD_BRICK_WALL).sound(SoundType.MUD).mapColor(MapColor.COLOR_YELLOW)), ItemBlockForestry::new, "wax_brick_wall");


	public static final FeatureBlock<Block, BlockItem> HARDENED_REFRACTORY_WAX_BLOCK = REGISTRY.block(() -> new Block(BlockBehaviour.Properties.copy(Blocks.MUD_BRICKS).sound(SoundType.MUD).mapColor(MapColor.COLOR_RED)), ItemBlockForestry::new, "hardened_refractory_wax_block");

	public static final FeatureBlock<Block, BlockItem> REFRACTORY_WAX_BRICKS = REGISTRY.block(() -> new Block(BlockBehaviour.Properties.copy(Blocks.MUD_BRICKS).sound(SoundType.MUD).mapColor(MapColor.COLOR_RED)), ItemBlockForestry::new, "refractory_wax_bricks");
	public static final FeatureBlock<StairBlock, BlockItem> REFRACTORY_WAX_BRICK_STAIRS = REGISTRY.block(() -> new StairBlock(Blocks.MUD_BRICKS::defaultBlockState, BlockBehaviour.Properties.copy(Blocks.MUD_BRICK_STAIRS).sound(SoundType.MUD).mapColor(MapColor.COLOR_RED)), ItemBlockForestry::new, "refractory_wax_brick_stairs");
	public static final FeatureBlock<SlabBlock, BlockItem> REFRACTORY_WAX_BRICK_SLAB = REGISTRY.block(() -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.MUD_BRICK_SLAB).sound(SoundType.MUD).mapColor(MapColor.COLOR_RED)), ItemBlockForestry::new, "refractory_wax_brick_slab");
	public static final FeatureBlock<WallBlock, BlockItem> REFRACTORY_WAX_BRICK_WALL = REGISTRY.block(() -> new WallBlock(BlockBehaviour.Properties.copy(Blocks.MUD_BRICK_WALL).sound(SoundType.MUD).mapColor(MapColor.COLOR_RED)), ItemBlockForestry::new, "refractory_wax_brick_wall");

	public static final FeatureBlockGroup<BlockMetalPlating, BlockTypeMetalPlating> METAL_PLATING = REGISTRY.blockGroup(BlockMetalPlating::new, BlockTypeMetalPlating.values()).itemWithType((block, type) -> new ItemBlockForestry<>(block, new Item.Properties())).identifier("metal_plating").create();

	public static final FeatureBlockGroup<BlockJumboCandle, BlockTypeJumboCandle> JUMBO_CANDLES = REGISTRY.blockGroup(BlockJumboCandle::new, BlockTypeJumboCandle.values()).itemWithType((block, type) -> new ItemBlockForestry<>(block, new Item.Properties())).identifier("jumbo_candle").create();

	public static final FeatureBlockGroup<BlockBigCandle, BlockTypeBigCandle> BIG_CANDLES = REGISTRY.blockGroup(BlockBigCandle::new, BlockTypeBigCandle.values()).itemWithType((block, type) -> new ItemBlockForestry<>(block, new Item.Properties())).identifier("big_candle").create();

	public static final FeatureBlockGroup<BlockTesr<NaturalistChestBlockType>, NaturalistChestBlockType> NATURALIST_CHEST = REGISTRY.blockGroup(type -> {
		return new BlockTesr<>(type, Block.Properties.of().sound(SoundType.WOOD));
	}, NaturalistChestBlockType.values()).item(ItemBlockTesr::new).create();
}
