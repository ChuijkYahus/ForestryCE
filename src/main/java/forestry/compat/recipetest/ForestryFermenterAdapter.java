package forestry.compat.recipetest;

import dev.recipetest.api.Layout;
import dev.recipetest.core.RecipeAdapter;
import forestry.api.recipes.IFermenterRecipe;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Recipe-test adapter for {@link IFermenterRecipe}. Fermenter takes one item resource (slot 0)
 * plus an input fluid (tank 0) and produces an output fluid (tank 1). The fermentation rate is
 * modulated by a per-recipe {@code modifier} times the resource's fermentation value, but those
 * are runtime BE concerns — the adapter only extracts ingredient shape + expected output fluid.
 *
 * <p>Output amount is recipe-dependent on the BE's stored fuel and modifier multipliers; the
 * adapter returns the recipe's nominal output Fluid type as a 1-mB representative, leaving the
 * actual amount comparison to the L1 differ's tolerance settings.
 */
public final class ForestryFermenterAdapter implements RecipeAdapter {

	@Override
	public boolean appliesTo(Recipe<?> recipe) {
		return recipe instanceof IFermenterRecipe;
	}

	@Override
	public Layout defaultLayout() {
		return Layout.SHAPELESS;
	}

	@Override
	public List<ItemStack> extractInputItems(Recipe<?> recipe) {
		IFermenterRecipe fermenter = (IFermenterRecipe) recipe;
		Ingredient input = fermenter.getInputItem();
		if (input == null || input.isEmpty()) {
			return List.of();
		}
		ItemStack[] candidates = input.getItems();
		return candidates.length == 0 ? List.of(ItemStack.EMPTY) : List.of(candidates[0].copy());
	}

	@Override
	public List<int[]> extractInputPositions(Recipe<?> recipe) {
		return List.of();
	}

	@Override
	public List<FluidStack> extractInputFluids(Recipe<?> recipe) {
		IFermenterRecipe fermenter = (IFermenterRecipe) recipe;
		FluidStack input = fermenter.getInputFluid();
		if (input == null || input.isEmpty()) {
			return List.of();
		}
		// TileFermenter.workCycle drains 1 mB per fermentation tick, and total fermentation
		// = recipe.getFermentationValue(). The recipe's fluidResource.amount (typically 1)
		// is just the match threshold — the BE consumes fermentationValue mB total. Inject
		// enough to last a full fermentation so hasWork() doesn't bail mid-recipe.
		int totalAmount = Math.max(input.getAmount(), fermenter.getFermentationValue());
		return List.of(input.copyWithAmount(totalAmount));
	}

	@Override
	public ItemStack extractPrimaryOutput(Recipe<?> recipe, HolderLookup.Provider registries) {
		// Fermenter produces only fluids; getResultItem returns ItemStack.EMPTY.
		return recipe.getResultItem(registries).copy();
	}

	@Override
	public List<FluidStack> extractOutputFluids(Recipe<?> recipe) {
		IFermenterRecipe fermenter = (IFermenterRecipe) recipe;
		// TileFermenter.workCycle produces fermented * modifier * resourceMod mB per cycle, and
		// the total over a full fermentation = fermentationValue * modifier * resourceMod.
		// resourceMod is a per-item-type multiplier the runner can't see from the recipe alone,
		// so we estimate using fermentationValue * modifier and rely on the spec's fluidTolerance
		// to absorb the resourceMod variation across recipes (typical resourceMod is in [0.5, 2]).
		int amount = Math.max(1, Math.round(fermenter.getFermentationValue() * fermenter.getModifier()));
		return List.of(new FluidStack(fermenter.getOutput(), amount));
	}
}
