package forestry.compat.recipetest;

import dev.recipetest.api.Layout;
import dev.recipetest.core.RecipeAdapter;
import forestry.api.recipes.IStillRecipe;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Recipe-test adapter for {@link IStillRecipe}. Pure fluid-to-fluid: input fluid in tank 0,
 * output fluid in tank 1. No item I/O (the still has a {@code SLOT_CAN} for fluid containers
 * but that's player UX, not recipe shape).
 */
public final class ForestryStillAdapter implements RecipeAdapter {

	@Override
	public boolean appliesTo(Recipe<?> recipe) {
		return recipe instanceof IStillRecipe;
	}

	@Override
	public Layout defaultLayout() {
		return Layout.SHAPELESS;
	}

	@Override
	public List<ItemStack> extractInputItems(Recipe<?> recipe) {
		return List.of();
	}

	@Override
	public List<int[]> extractInputPositions(Recipe<?> recipe) {
		return List.of();
	}

	@Override
	public List<FluidStack> extractInputFluids(Recipe<?> recipe) {
		IStillRecipe still = (IStillRecipe) recipe;
		FluidStack input = still.getInput();
		if (input == null || input.isEmpty()) {
			return List.of();
		}
		// TileStill.hasWork() drains cyclesPerUnit * input.getAmount() per execution cycle —
		// the recipe's nominal input amount is per-cycle, but the BE wants enough fluid in
		// the resource tank to complete a full cycle batch before it'll start. Inject the
		// full cycle's worth so hasWork() returns true on first tick.
		int cycles = still.getCyclesPerUnit();
		return List.of(input.copyWithAmount(input.getAmount() * cycles));
	}

	@Override
	public ItemStack extractPrimaryOutput(Recipe<?> recipe, HolderLookup.Provider registries) {
		return recipe.getResultItem(registries).copy();
	}

	@Override
	public List<FluidStack> extractOutputFluids(Recipe<?> recipe) {
		IStillRecipe still = (IStillRecipe) recipe;
		FluidStack output = still.getOutput();
		if (output == null || output.isEmpty()) {
			return List.of();
		}
		// Output amount mirrors the input scaling — TileStill.workCycle() fills
		// cyclesPerUnit * output.getAmount() into the product tank per execution.
		int cycles = still.getCyclesPerUnit();
		return List.of(output.copyWithAmount(output.getAmount() * cycles));
	}
}
