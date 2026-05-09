package forestry.compat.recipetest;

import dev.recipetest.api.Layout;
import dev.recipetest.core.RecipeAdapter;
import forestry.api.recipes.ICarpenterRecipe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Recipe-test adapter for {@link ICarpenterRecipe}. Replaces the kit's old (now-removed)
 * carpenter compat adapter; living on the consumer-mod side honors the kit's mod-agnostic
 * thru-line.
 *
 * <p>Carpenter recipes wrap a {@link ShapedRecipe} (or shapeless equivalent) plus an input
 * {@link FluidStack}; the wrapped grid recipe carries pattern + ingredients, the outer carpenter
 * recipe carries the result item (with components — pulling from the inner grid would drop
 * any {@code custom_data} / lore / enchantments attached to the outer result).
 *
 * <p>The carpenter is a player-driven machine — the BE matches recipes against a separate
 * {@code InventoryGhostCrafting} that the GUI populates. The adapter does not seed the ghost
 * matrix; that's the spec's {@code lifecycle.preTickCommands} job (data-merge with
 * {@code CraftItems:[...]}).
 */
public final class ForestryCarpenterAdapter implements RecipeAdapter {

	@Override
	public boolean appliesTo(Recipe<?> recipe) {
		return recipe instanceof ICarpenterRecipe;
	}

	@Override
	public Layout defaultLayout() {
		return Layout.SHAPED3X3;
	}

	@Override
	public List<ItemStack> extractInputItems(Recipe<?> recipe) {
		ICarpenterRecipe carpenter = (ICarpenterRecipe) recipe;
		Recipe<?> grid = carpenter.getCraftingGridRecipe();
		List<ItemStack> result = new ArrayList<>();
		for (Ingredient ingredient : grid.getIngredients()) {
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
		ICarpenterRecipe carpenter = (ICarpenterRecipe) recipe;
		Recipe<?> grid = carpenter.getCraftingGridRecipe();
		if (!(grid instanceof ShapedRecipe shaped)) {
			return Collections.emptyList();
		}
		ShapedRecipePattern pattern = shaped.pattern;
		int width = pattern.width();
		List<int[]> positions = new ArrayList<>();
		NonNullList<Ingredient> ingredients = pattern.ingredients();
		// pattern.ingredients() is a row-major list sized width * height;
		// non-empty entries occupy (col, row) cells of the visible 3x3 / NxM grid.
		for (int i = 0; i < ingredients.size(); i++) {
			if (!ingredients.get(i).isEmpty()) {
				positions.add(new int[]{i % width, i / width});
			}
		}
		return positions;
	}

	@Override
	public List<FluidStack> extractInputFluids(Recipe<?> recipe) {
		ICarpenterRecipe carpenter = (ICarpenterRecipe) recipe;
		FluidStack input = carpenter.getInputFluid();
		return input == null || input.isEmpty() ? List.of() : List.of(input.copy());
	}

	@Override
	public ItemStack extractPrimaryOutput(Recipe<?> recipe, HolderLookup.Provider registries) {
		// Outer recipe.getResultItem returns the carpenter's own result ItemStack with components
		// (custom_data, lore, etc.); the wrapped grid recipe's result drops them. The actual
		// machine output keeps components, so the expected snapshot must too — pulling from the
		// inner grid recipe causes a phantom NBT mismatch in the diff.
		return recipe.getResultItem(registries).copy();
	}
}
