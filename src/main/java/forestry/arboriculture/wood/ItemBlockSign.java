package forestry.arboriculture.wood;

import forestry.api.arboriculture.WoodBlockKind;
import forestry.arboriculture.wood.ForestryWoodType;
import forestry.arboriculture.wood.WoodHelper;
import forestry.arboriculture.wood.BlockForestryStandingSign;
import forestry.arboriculture.features.ArboricultureBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignItem;

public class ItemBlockSign extends SignItem {
	private final ForestryWoodType type;

	public ItemBlockSign(BlockForestryStandingSign block, Item.Properties properties) {
		super(properties, block, ArboricultureBlocks.WALL_SIGN.get(block.getWoodType()).block());

		this.type = block.getWoodType();
	}

	@Override
	public Component getName(ItemStack itemstack) {
		// todo use vanilla names and data generation instead of this
		return WoodHelper.getDisplayName(WoodBlockKind.SIGN, false, this.type);
	}
}
