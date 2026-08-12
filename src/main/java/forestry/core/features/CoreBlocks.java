package forestry.core.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.block.NaturalistChestBlockType;
import forestry.core.platform.block.*;
import forestry.core.content.burnbarrel.BlockBurnBarrel;
import forestry.core.content.soil.*;
import forestry.core.content.lighting.BlockWaterloggableTorch;
import forestry.core.content.lighting.BlockWaterloggableWallTorch;
import forestry.core.content.resources.*;
import forestry.core.platform.item.ItemBlockForestry;
import forestry.core.platform.item.ItemBlockTesr;
import forestry.core.platform.registration.*;
import java.util.List;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;

@FeatureProvider
public class CoreBlocks {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CORE);

	public static final FeatureBlockGroup<BlockCore, BlockTypeCoreTesr> BASE = REGISTRY.blockGroup(BlockCore::new, List.of(BlockTypeCoreTesr.values())).item(ItemBlockTesr::new).create();

	// Deviation from 1.20.1: the barrel built its own properties inside its constructor. Here the registry owns them.
	// Blocks.FURNACE is safe to ofFullCopy, it sets no dropsLike, so the copy cannot steal a vanilla loot table id
	public static final FeatureBlock<BlockBurnBarrel, ItemBlockForestry<?>> BURN_BARREL = REGISTRY.block(BlockBurnBarrel::new, () -> Properties.ofFullCopy(Blocks.FURNACE)
		.mapColor(MapColor.COLOR_GRAY)
		.sound(SoundType.METAL)
		.lightLevel(state -> state.getValue(BlockBurnBarrel.LIT) ? 15 : 0), ItemBlockForestry::new, "burn_barrel");

	public static final FeatureBlock<BlockBogEarth, ItemBlockForestry<?>> BOG_EARTH = REGISTRY.block(BlockBogEarth::new, ItemBlockForestry::new, "bog_earth");
	public static final FeatureBlock<Block, ItemBlockForestry<?>> PEAT = REGISTRY.block(Block::new, () -> Properties.of().strength(0.5f).sound(SoundType.GRAVEL), null, "peat");
	public static final FeatureBlock<BlockHumus, ItemBlockForestry<?>> HUMUS = REGISTRY.block(BlockHumus::new, ItemBlockForestry::new, "humus");
	public static final FeatureBlockGroup<BlockResourceStorage, EnumResourceType> RESOURCE_STORAGE = REGISTRY.blockGroup(BlockResourceStorage::new, List.of(EnumResourceType.values())).item(ItemBlockForestry::new).identifier("block", FeatureGroup.IdentifierType.SUFFIX).create();
	public static final FeatureBlock<Block, BlockItem> APATITE_ORE = REGISTRY.block((properties) -> new DropExperienceBlock(UniformInt.of(0, 4), properties), () -> Properties.ofFullCopy(Blocks.COAL_ORE), ItemBlockForestry::new, "apatite_ore");
	public static final FeatureBlock<Block, BlockItem> DEEPSLATE_APATITE_ORE = REGISTRY.block((properties) -> new DropExperienceBlock(UniformInt.of(0, 4), properties), () -> Properties.ofFullCopy(APATITE_ORE.block()).mapColor(MapColor.DEEPSLATE).strength(4.5f, 3.0f).sound(SoundType.DEEPSLATE), ItemBlockForestry::new, "deepslate_apatite_ore");
	public static final FeatureBlock<Block, BlockItem> TIN_ORE = REGISTRY.block(Block::new, () -> Properties.ofFullCopy(Blocks.COPPER_ORE), ItemBlockForestry::new, "tin_ore");
	public static final FeatureBlock<Block, BlockItem> DEEPSLATE_TIN_ORE = REGISTRY.block(Block::new, () -> Properties.ofFullCopy(TIN_ORE.block()).mapColor(MapColor.DEEPSLATE).strength(4.5f, 3.0f).sound(SoundType.DEEPSLATE), ItemBlockForestry::new, "deepslate_tin_ore");
	public static final FeatureBlock<Block, BlockItem> RAW_TIN_BLOCK = REGISTRY.block(Block::new, () -> Properties.ofFullCopy(Blocks.RAW_COPPER_BLOCK), ItemBlockForestry::new, "raw_tin_block");

	/* Lighting */
	// Deviation from 1.20.1: block properties now come from the registry's Supplier<Properties> overload
	// (Properties.copy was renamed ofFullCopy), and block items take a BiFunction<B, Item.Properties, I>.
	public static final FeatureBlock<ChainBlock, BlockItem> TIN_CHAIN = REGISTRY.block(ChainBlock::new, () -> Properties.ofFullCopy(Blocks.CHAIN), ItemBlockForestry::new, "tin_chain");
	public static final FeatureBlock<LanternBlock, BlockItem> PHOSPHOR_LANTERN = REGISTRY.block(LanternBlock::new, () -> Properties.ofFullCopy(Blocks.SOUL_LANTERN).lightLevel(state -> 13), ItemBlockForestry::new, "phosphor_lantern");
	// The torch's item is registered separately in CoreItems as a StandingAndWallBlockItem covering both blocks
	public static final FeatureBlock<BlockWaterloggableTorch, ItemBlockForestry<?>> PHOSPHOR_TORCH = REGISTRY.block(BlockWaterloggableTorch::new, () -> Properties.ofFullCopy(Blocks.SOUL_TORCH).lightLevel(state -> 13), null, "phosphor_torch");
	// Deviation from 1.20.1: copies SOUL_TORCH rather than SOUL_WALL_TORCH. 1.20.1's Properties.copy left the loot
	// table alone, but 1.21.1's ofFullCopy also copies `drops`, and vanilla's soul wall torch sets dropsLike(SOUL_TORCH).
	// Copying it would give this block the minecraft:blocks/soul_torch loot table, so it would drop a vanilla soul
	// torch in game and make the loot datagen write forestry's table into the minecraft namespace. Vanilla's soul wall
	// torch properties are otherwise identical to soul torch's, so copying the standing torch reproduces 1.20.1 exactly.
	public static final FeatureBlock<BlockWaterloggableWallTorch, ItemBlockForestry<?>> PHOSPHOR_WALL_TORCH = REGISTRY.block(BlockWaterloggableWallTorch::new, () -> Properties.ofFullCopy(Blocks.SOUL_TORCH).lightLevel(state -> 13), null, "phosphor_wall_torch");

	public static final FeatureBlockGroup<BlockTesr<NaturalistChestBlockType>, NaturalistChestBlockType> NATURALIST_CHEST = REGISTRY.blockGroup(type -> new BlockTesr<>(type, Properties.of().sound(SoundType.WOOD)), List.of(NaturalistChestBlockType.values())).item(ItemBlockTesr::new).create();
}
