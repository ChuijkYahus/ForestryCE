package forestry.core.items;

import forestry.core.utils.ItemTooltipUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class ItemBlockForestry<B extends Block> extends BlockItem {
	private final int burnTime;

	public ItemBlockForestry(B block, Item.Properties builder) {
		super(block, builder);

		if (builder instanceof ItemProperties properties) {
			this.burnTime = properties.burnTime;
		} else {
			// 0 = "not a fuel". NeoForge 1.21 IItemStackExtension#getBurnTime throws on
			// negative returns, so the legacy -1 sentinel is no longer valid.
			this.burnTime = 0;
		}
	}

	public ItemBlockForestry(B block) {
		this(block, new Item.Properties());
	}

	@Override
	public B getBlock() {
		//noinspection unchecked
		return (B) super.getBlock();
	}

	@Override
	public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
		return this.burnTime;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag advanced) {
		super.appendHoverText(stack, context, tooltip, advanced);
		ItemTooltipUtil.addInformation(stack, tooltip);
	}
}
