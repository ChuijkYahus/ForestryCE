package forestry.compat.recipetest;

import dev.recipetest.api.RecipeTestExtension;
import dev.recipetest.api.TestContext;
import forestry.api.recipes.ICarpenterRecipe;
import forestry.factory.inventory.InventoryCarpenter;
import forestry.factory.tiles.TileCarpenter;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * L2 {@link RecipeTestExtension} for the carpenter. The carpenter is a player-driven machine —
 * the BE matches its currentRecipe against an {@code InventoryGhostCrafting} ghost matrix that
 * the GUI populates, NOT against the visible item slots. Without seeding the ghost matrix the
 * BE never matches the recipe and the test TIMEOUTs.
 *
 * <p>Previously the spec did this via a hardcoded {@code lifecycle.preTickCommands} data-merge
 * but that only worked for one specific recipe pattern. This extension reads the recipe's grid
 * pattern at runtime and writes the appropriate ItemStacks into the ghost matrix slots, so all
 * 200+ carpenter recipes can run without per-recipe spec authoring.
 *
 * <p>Returns {@link InjectionDecision#FALL_THROUGH} after seeding so the L1 path still injects
 * recipe ingredients into the carpenter's general inventory (slots 12-29) and the input fluid
 * via the {@link ForestryCarpenterAdapter}. The extension is purely additive — it adds the
 * ghost-matrix seeding the L1 path can't do.
 */
public final class ForestryCarpenterExtension implements RecipeTestExtension<ICarpenterRecipe> {

	private static final Logger LOGGER = LogManager.getLogger("forestry/recipetest/carpenter");

	/** {@link InventoryCarpenter#SLOT_BOX} — the box slot on the carpenter's main inventory. */
	private static final int SLOT_BOX = InventoryCarpenter.SLOT_BOX;

	/** Side length of the ghost matrix grid (always 3 for vanilla {@code ShapedRecipePattern}). */
	private static final int GRID_SIDE = 3;

	private static final ResourceLocation RECIPE_TYPE = ResourceLocation.fromNamespaceAndPath("forestry", "carpenter");

	@Override
	public ResourceLocation recipeType() {
		return RECIPE_TYPE;
	}

	@Override
	public InjectionDecision injectInputs(TestContext ctx, RecipeHolder<ICarpenterRecipe> holder) {
		BlockEntity be = ctx.level().getBlockEntity(ctx.origin());
		if (!(be instanceof TileCarpenter carpenter)) {
			LOGGER.warn("Carpenter recipe-test fired but BE at origin is {}, not TileCarpenter; falling through", be);
			return InjectionDecision.FALL_THROUGH;
		}
		ICarpenterRecipe recipe = holder.value();
		seedGhostMatrix(carpenter.getCraftingInventory(), recipe.getCraftingGridRecipe());
		seedBoxSlot(carpenter, recipe.getBox());
		// Fall through so the L1 path injects general-inventory items, fluid, and energy via the
		// ForestryCarpenterAdapter — the extension only handles what L1 can't (ghost matrix + box).
		return InjectionDecision.FALL_THROUGH;
	}

	/**
	 * Place a representative {@link ItemStack} for each non-empty {@link Ingredient} in the
	 * recipe's grid pattern at the matching slot of the 3×3 ghost matrix.
	 */
	private static void seedGhostMatrix(Container ghost, Recipe<?> grid) {
		if (grid instanceof ShapedRecipe shaped) {
			ShapedRecipePattern pattern = shaped.pattern;
			NonNullList<Ingredient> ingredients = pattern.ingredients();
			int width = pattern.width();
			// pattern.ingredients() is a row-major list sized width × height. For a 2×3 recipe
			// the pattern indices land at (col, row) of a width-by-height grid; we map them
			// into a 3×3 ghost matrix's row-major slot index.
			for (int i = 0; i < ingredients.size(); i++) {
				Ingredient ing = ingredients.get(i);
				if (ing.isEmpty()) {
					continue;
				}
				ItemStack[] candidates = ing.getItems();
				if (candidates.length == 0) {
					continue;
				}
				int col = i % width;
				int row = i / width;
				int slot = row * GRID_SIDE + col;
				if (slot >= 0 && slot < ghost.getContainerSize()) {
					ghost.setItem(slot, candidates[0].copy());
				}
			}
		} else {
			// Shapeless / other CraftingRecipe types — pack ingredients left-to-right starting
			// at slot 0. The carpenter's recipe-matching tolerates any ghost-matrix arrangement
			// for shapeless recipes, so packing is fine.
			int slot = 0;
			for (Ingredient ing : grid.getIngredients()) {
				if (slot >= ghost.getContainerSize()) {
					break;
				}
				if (!ing.isEmpty()) {
					ItemStack[] candidates = ing.getItems();
					if (candidates.length > 0) {
						ghost.setItem(slot, candidates[0].copy());
					}
				}
				slot++;
			}
		}
	}

	/**
	 * Seed the box slot of the carpenter's main inventory if the recipe requires a box
	 * (e.g. carton recipes). Empty box {@link Ingredient} → no-op.
	 */
	private static void seedBoxSlot(TileCarpenter carpenter, Ingredient box) {
		if (box == null || box.isEmpty()) {
			return;
		}
		ItemStack[] candidates = box.getItems();
		if (candidates.length == 0) {
			return;
		}
		// TileCarpenter implements WorldlyContainer (extends Container), so setItem works
		// directly against the carpenter's main inventory.
		carpenter.setItem(SLOT_BOX, candidates[0].copy());
	}
}
