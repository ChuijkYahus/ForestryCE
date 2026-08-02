package forestry.core.content.machines.recipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import javax.annotation.Nullable;
import java.util.Optional;

public class BottlerRecipe {
	@Nullable
	public static BottlerRecipe createEmptyingRecipe(ItemStack filled) {
		ItemStack empty = filled.copy();
		empty.setCount(1);
		Optional<IFluidHandlerItem> fluidHandlerCap = FluidUtil.getFluidHandler(empty);

		if (fluidHandlerCap.isEmpty()) {
			return null;
		}

		IFluidHandlerItem fluidHandler = fluidHandlerCap.orElseThrow();

		FluidStack drained = fluidHandler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
		if (!drained.isEmpty() && drained.getAmount() > 0) {
			return new BottlerRecipe(fluidHandler.getContainer(), drained, filled, false);
		}

		return null;
	}

	@Nullable
	public static BottlerRecipe createFillingRecipe(Fluid res, ItemStack empty) {
		ItemStack filled = empty.copy();
		filled.setCount(1);

		Optional<IFluidHandlerItem> fluidHandlerCap = FluidUtil.getFluidHandler(filled);
		if (fluidHandlerCap.isEmpty()) {
			return null;
		}

		IFluidHandlerItem fluidHandler = fluidHandlerCap.orElseThrow();

		int fillAmount = fluidHandler.fill(new FluidStack(res, Integer.MAX_VALUE), IFluidHandler.FluidAction.EXECUTE);
		if (fillAmount > 0) {
			return new BottlerRecipe(empty, new FluidStack(res, fillAmount), fluidHandler.getContainer(), true);
		}

		return null;
	}

	public final FluidStack fluid;
	public final ItemStack inputStack;
	public final ItemStack outputStack;
	public final boolean fillRecipe;

	public BottlerRecipe(ItemStack inputStack, FluidStack fluid, ItemStack outputStack, boolean fillRecipe) {
		this.fluid = fluid;
		this.inputStack = inputStack;
		this.outputStack = outputStack;
		this.fillRecipe = fillRecipe;
	}

	public boolean matchEmpty(ItemStack emptyCan, FluidStack resource) {
		return !emptyCan.isEmpty() && ItemStack.isSameItem(emptyCan, this.inputStack) && FluidStack.isSameFluidSameComponents(resource, this.fluid) && this.fillRecipe;
	}

	public boolean matchFilled(ItemStack filledCan) {
		return !this.outputStack.isEmpty() && !this.fillRecipe && ItemStack.isSameItem(this.outputStack, filledCan);
	}
}
