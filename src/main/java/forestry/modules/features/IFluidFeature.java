package forestry.modules.features;

import forestry.core.fluids.BlockForestryFluid;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;

public interface IFluidFeature extends IModFeature {
	IBlockFeature<BlockForestryFluid, BlockItem> fluidBlock();

	FluidProperties properties();

	FlowingFluid fluid();

	FlowingFluid flowing();

	default FluidStack fluidStack(int amount) {
		return new FluidStack(fluid(), amount);
	}

	default FluidStack fluidStack() {
		return fluidStack(FluidType.BUCKET_VOLUME);
	}
}
