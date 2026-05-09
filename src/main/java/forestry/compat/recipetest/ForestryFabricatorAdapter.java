package forestry.compat.recipetest;

import dev.recipetest.api.Layout;
import dev.recipetest.core.RecipeAdapter;
import forestry.api.recipes.IFabricatorRecipe;
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
 * Recipe-test adapter for {@link IFabricatorRecipe}. Fabricator wraps a {@link ShapedRecipe} (the
 * crafting grid pattern), takes a {@link FluidStack} of molten metal (tank 0), and optionally
 * requires a {@code plan} {@link Ingredient} in slot 1 — many recipes leave plan empty
 * ({@code Ingredient.EMPTY}), in which case an empty plan slot satisfies the match.
 *
 * <p>Like the carpenter, the fabricator is a player-driven machine: the BE matches recipes
 * against a separate {@code InventoryGhostCrafting}, not the visible item slots. The adapter
 * does not seed the ghost matrix; that's the spec's {@code lifecycle.preTickCommands} job.
 */
public final class ForestryFabricatorAdapter implements RecipeAdapter {

	@Override
	public boolean appliesTo(Recipe<?> recipe) {
		return recipe instanceof IFabricatorRecipe;
	}

	@Override
	public Layout defaultLayout() {
		return Layout.SHAPED3X3;
	}

	@Override
	public List<ItemStack> extractInputItems(Recipe<?> recipe) {
		IFabricatorRecipe fabricator = (IFabricatorRecipe) recipe;
		ShapedRecipe grid = fabricator.getCraftingGridRecipe();
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
		IFabricatorRecipe fabricator = (IFabricatorRecipe) recipe;
		ShapedRecipe grid = fabricator.getCraftingGridRecipe();
		ShapedRecipePattern pattern = grid.pattern;
		int width = pattern.width();
		List<int[]> positions = new ArrayList<>();
		NonNullList<Ingredient> ingredients = pattern.ingredients();
		for (int i = 0; i < ingredients.size(); i++) {
			if (!ingredients.get(i).isEmpty()) {
				positions.add(new int[]{i % width, i / width});
			}
		}
		return positions.isEmpty() ? Collections.emptyList() : positions;
	}

	// extractInputFluids intentionally NOT overridden — the fabricator's moltenTank is a
	// FilteredTank that rejects the L1 IFluidHandler.fill path for fluids not registered as
	// outputs of forestry:fabricator_smelting recipes (and even then often misses the runtime
	// state). The companion ForestryFabricatorExtension fills the tank directly via
	// FluidTank.setFluid (bypassing the filter) before this adapter's L1 phase runs, so there
	// is nothing for the adapter to extract for the L1 fluid-injection path.

	@Override
	public ItemStack extractPrimaryOutput(Recipe<?> recipe, HolderLookup.Provider registries) {
		IFabricatorRecipe fabricator = (IFabricatorRecipe) recipe;
		// Fabricator's result lives on the wrapped ShapedRecipe — the outer IFabricatorRecipe
		// doesn't carry a separate result with components.
		return fabricator.getCraftingGridRecipe().getResultItem(registries).copy();
	}
}
