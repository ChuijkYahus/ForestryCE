package forestry.compat.recipetest;

import dev.recipetest.api.Layout;
import dev.recipetest.core.RecipeAdapter;
import forestry.api.recipes.ISqueezerRecipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Recipe-test adapter for {@link ISqueezerRecipe}. The squeezer takes a shapeless list of item
 * ingredients (slots 0-8 of the BE), produces a {@link FluidStack} into tank 0, and may produce
 * a probabilistic {@code remnant} item into slot 9.
 *
 * <p>The remnant is intentionally NOT returned from {@link #extractPrimaryOutput} because its
 * {@code chance} is &lt; 1 in many recipes — mid-run the actual remnant slot may be empty, and
 * a deterministic exact-mode comparison would FAIL. Distribution-mode validation for the remnant
 * channel is a future enhancement (would need an {@code RecipeTestExtension} for weights).
 */
public final class ForestrySqueezerAdapter implements RecipeAdapter {

	@Override
	public boolean appliesTo(Recipe<?> recipe) {
		return recipe instanceof ISqueezerRecipe;
	}

	@Override
	public Layout defaultLayout() {
		return Layout.SHAPELESS;
	}

	@Override
	public List<ItemStack> extractInputItems(Recipe<?> recipe) {
		ISqueezerRecipe squeezer = (ISqueezerRecipe) recipe;
		List<ItemStack> result = new ArrayList<>();
		for (Ingredient ingredient : squeezer.getInputs()) {
			if (ingredient.isEmpty()) {
				continue;
			}
			ItemStack[] candidates = ingredient.getItems();
			result.add(candidates.length == 0 ? ItemStack.EMPTY : candidates[0].copy());
		}
		return result;
	}

	@Override
	public List<int[]> extractInputPositions(Recipe<?> recipe) {
		return List.of();
	}

	@Override
	public ItemStack extractPrimaryOutput(Recipe<?> recipe, HolderLookup.Provider registries) {
		// Squeezer's primary output is fluid; getResultItem returns ItemStack.EMPTY.
		return recipe.getResultItem(registries).copy();
	}

	@Override
	public List<FluidStack> extractOutputFluids(Recipe<?> recipe) {
		ISqueezerRecipe squeezer = (ISqueezerRecipe) recipe;
		FluidStack output = squeezer.getFluidOutput();
		return output == null || output.isEmpty() ? List.of() : List.of(output.copy());
	}
}
