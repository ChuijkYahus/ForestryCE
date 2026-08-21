package forestry.apiculture.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.apiculture.alveary.BlockAlveary;
import forestry.apiculture.hives.BlockBeeHive;
import forestry.apiculture.hives.BlockHiveType;
import forestry.apiculture.bees.BlockHoneyComb;
import forestry.apiculture.apiary.BlockTypeApiculture;
import forestry.apiculture.bees.BlockWax;
import forestry.apiculture.bees.EnumHoneyComb;
import forestry.apiculture.bees.ItemBlockHoneyComb;
import forestry.core.platform.block.BlockBase;
import forestry.core.platform.fluids.ForestryFluids;
import forestry.core.platform.item.ItemBlockForestry;
import forestry.core.platform.registration.FeatureBlock;
import forestry.core.platform.registration.FeatureBlockGroup;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.FeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

import java.util.List;

@FeatureProvider
public class ApicultureBlocks {
	private static final FeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.APICULTURE);

	public static final FeatureBlockGroup<BlockBase<BlockTypeApiculture>, BlockTypeApiculture> BASE = REGISTRY.blockGroup(
			(BlockTypeApiculture type) -> new BlockBase<>(Block.Properties.of().sound(SoundType.WOOD).strength(2.0f), type),
			List.of(BlockTypeApiculture.values()))
		.item(ItemBlockForestry::new)
		.create();

	public static final FeatureBlockGroup<BlockBeeHive, BlockHiveType> BEEHIVE = REGISTRY.blockGroup(BlockBeeHive::new, List.of(BlockHiveType.values())).item((block, properties) -> new ItemBlockForestry<>(block, properties)).identifier(type -> switch (type) {
		case DESERT -> "modest_hive";
		case JUNGLE -> "tropical_hive";
		case END -> "ender_hive";
		case SNOW -> "wintry_hive";
		case SWAMP -> "marshy_hive";
		default -> type.getSerializedName() + "_hive";
	}).create();

	public static final FeatureBlockGroup<BlockHoneyComb, EnumHoneyComb> BEE_COMB = REGISTRY.blockGroup(BlockHoneyComb::new, List.of(EnumHoneyComb.VALUES)).item((block, properties) -> new ItemBlockHoneyComb(block)).identifier(type -> (type == EnumHoneyComb.SPONGE ? "spongy" : type.getSerializedName()) + "_comb_block").create();
	public static final FeatureBlockGroup<BlockAlveary, BlockAlveary.Type> ALVEARY = REGISTRY.blockGroup(BlockAlveary::new, BlockAlveary.Type.DEFAULT_VALUES).item((block, properties) -> new ItemBlockForestry<>(block, properties)).identifier(type -> switch (type.getSerializedName()) {
		case "plain" -> "alveary_block";
		case "hygro" -> "alveary_hygroregulator";
		case "stabiliser" -> "alveary_stabilizer";
		default -> "alveary_" + type.getSerializedName();
	}).create();

	// Deviation from 1.20.1: the refractory id follows this tree's <qualifier>_<material>_block naming,
	// so "wax_block_refractory" became "refractory_wax_block", matching the refractory_wax item
	public static final FeatureBlock<BlockWax, BlockItem> WAX_BLOCK = REGISTRY.block(properties -> new BlockWax(properties, true, () -> ForestryFluids.WAX.getFluid()), () -> Block.Properties.ofFullCopy(Blocks.HONEYCOMB_BLOCK).sound(SoundType.HONEY_BLOCK).mapColor(MapColor.COLOR_YELLOW).ignitedByLava(), ItemBlockForestry::new, "wax_block");
	public static final FeatureBlock<BlockWax, BlockItem> REFRACTORY_WAX_BLOCK = REGISTRY.block(properties -> new BlockWax(properties, false, null), () -> Block.Properties.ofFullCopy(Blocks.HONEYCOMB_BLOCK).sound(SoundType.HONEY_BLOCK).mapColor(MapColor.COLOR_RED).ignitedByLava(), ItemBlockForestry::new, "refractory_wax_block");
}
