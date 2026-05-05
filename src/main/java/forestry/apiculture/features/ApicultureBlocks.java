package forestry.apiculture.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.apiculture.blocks.*;
import forestry.apiculture.items.EnumHoneyComb;
import forestry.apiculture.items.ItemBlockHoneyComb;
import forestry.core.blocks.BlockBase;
import forestry.core.items.ItemBlockForestry;
import forestry.modules.features.FeatureBlockGroup;
import forestry.modules.features.FeatureProvider;
import forestry.modules.features.FeatureRegistry;
import forestry.modules.features.ModFeatureRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;

import java.util.List;

@FeatureProvider
public class ApicultureBlocks {
	private static final FeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.APICULTURE);

	public static final FeatureBlockGroup<BlockBase<BlockTypeApiculture>, BlockTypeApiculture> BASE = REGISTRY.<BlockBase<BlockTypeApiculture>, BlockTypeApiculture>blockGroup(BlockTypeApiculture.values())
		.block(BlockBase::new, props -> props.sound(SoundType.WOOD).strength(2.0f))
		.item(ItemBlockForestry::new)
		.create();

	public static final FeatureBlockGroup<BlockBeeHive, BlockHiveType> BEEHIVE = REGISTRY.blockGroup(BlockBeeHive::new, List.of(BlockHiveType.values())).itemWithType((block, type) -> new ItemBlockForestry<>(block, new Item.Properties())).identifier("beehive").create();

	public static final FeatureBlockGroup<BlockHoneyComb, EnumHoneyComb> BEE_COMB = REGISTRY.blockGroup(BlockHoneyComb::new, List.of(EnumHoneyComb.VALUES)).item(ItemBlockHoneyComb::new).identifier("block_bee_comb").create();
	public static final FeatureBlockGroup<BlockAlveary, BlockAlveary.Type> ALVEARY = REGISTRY.blockGroup(BlockAlveary::new, BlockAlveary.Type.DEFAULT_VALUES).item(blockAlveary -> new ItemBlockForestry<>(blockAlveary, new Item.Properties())).identifier("alveary").create();
}
