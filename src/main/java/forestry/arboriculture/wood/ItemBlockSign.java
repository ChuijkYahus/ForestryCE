package forestry.arboriculture.wood;

import forestry.arboriculture.features.ArboricultureBlocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SignItem;

public class ItemBlockSign extends SignItem {
	public ItemBlockSign(BlockForestryStandingSign block, Item.Properties properties) {
		super(properties, block, ArboricultureBlocks.WALL_SIGN.get(block.getWoodType()).block());
	}
}
