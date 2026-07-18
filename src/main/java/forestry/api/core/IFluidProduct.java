package forestry.api.core;

import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A fluid analog of {@link IProduct}: some fluid output that a machine (currently the Squeezer) produces.
 * Serialized via the dispatch codec in {@code forestry.core.FluidProductTypes}.
 *
 * @see FluidProduct The default fixed-fluid implementation used by Forestry's own recipes.
 */
public interface IFluidProduct {
	/**
	 * The representative stack for this product. Used for display identity (a {@link FluidStack} carries data
	 * components, e.g. liquid potion effects) and as the MAXIMAL amount this product can produce. A machine reserves
	 * space for this amount before it begins processing. Returning {@link FluidStack#EMPTY} signals the product is not
	 * currently producible (e.g. an addon tag product whose tag no loaded mod fills), and the machine refuses the
	 * recipe.
	 *
	 * @return A new representative stack, or {@link FluidStack#EMPTY} if not currently producible.
	 */
	FluidStack createFluidStack();

	/**
	 * The actual fluid produced in one work cycle. May be a smaller amount than {@link #createFluidStack()}, or
	 * {@link FluidStack#EMPTY} if this product's own probability roll failed. Any per-cycle randomness (variable
	 * amount, chance of nothing) is folded in here; the machine does not roll it separately.
	 *
	 * @param random The random source. If no randomness is desired, this defaults to {@link #createFluidStack()}.
	 * @return A new stack for this cycle, possibly empty or smaller than the representative stack.
	 */
	default FluidStack createRandomFluidStack(RandomSource random) {
		return createFluidStack();
	}

	/**
	 * The type of this product, used to (de)serialize it via the dispatch codec in
	 * {@code forestry.core.FluidProductTypes}. Plain {@link FluidProduct} instances return {@link FluidProduct#TYPE},
	 * which the dispatch codec treats as the default: it serializes without a {@code "type"} key.
	 *
	 * @return The type of this product.
	 */
	FluidProductType<?> type();
}
