package forestry.core.content.tools;

import forestry.api.core.IToolPipette;
import forestry.core.features.CoreDataComponents;
import forestry.core.platform.fluids.PipetteContents;
import forestry.core.platform.item.ITintedItem;
import forestry.core.platform.util.RenderUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;

import java.util.List;
import forestry.core.platform.item.ItemForestry;

public class ItemPipette extends ItemForestry implements IToolPipette, ITintedItem {
	public ItemPipette() {
		super(new Properties().stacksTo(1));
	}

	@Override
	public boolean canPipette(ItemStack itemstack) {
		PipetteContents contained = PipetteContents.create(itemstack);
		return contained == null || !contained.isFull();
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);

		PipetteContents contained = PipetteContents.create(itemstack);
		if (contained != null) {
			contained.addTooltip(list);
		}
	}

	public IFluidHandlerItem createFluidHandler(ItemStack stack) {
		return new FluidHandlerItemStack(CoreDataComponents.FLUID_CONTENT, stack, FluidType.BUCKET_VOLUME);
	}

	@Override
	public int getColorFromItemStack(ItemStack stack, int tintIndex) {
		if (tintIndex == 1) {
			PipetteContents contents = PipetteContents.create(stack);

			if (contents != null) {
				return RenderUtil.getFluidColor(contents.getContents().getFluid());
			}
		}
		return 0xffffff;
	}
}
