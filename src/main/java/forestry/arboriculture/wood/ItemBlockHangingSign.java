package forestry.arboriculture.wood;

import forestry.api.arboriculture.WoodBlockKind;
import forestry.arboriculture.wood.ForestryWoodType;
import forestry.arboriculture.wood.WoodHelper;
import forestry.arboriculture.wood.BlockForestryHangingSign;
import forestry.arboriculture.features.ArboricultureBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemBlockHangingSign extends HangingSignItem {
	private final ForestryWoodType type;

	public ItemBlockHangingSign(BlockForestryHangingSign block, Item.Properties properties) {
		super(block, ArboricultureBlocks.WALL_HANGING_SIGN.get(block.getWoodType()).block(), properties);

		this.type = block.getWoodType();
	}

	@Override
	public Component getName(ItemStack itemstack) {
		// todo use vanilla names and data generation instead of this
		return WoodHelper.getDisplayName(WoodBlockKind.HANGING_SIGN, false, this.type);
	}
}
