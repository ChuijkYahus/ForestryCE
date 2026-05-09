package forestry.compat.recipetest;

import dev.recipetest.api.RecipeTestExtension;
import dev.recipetest.api.TestContext;
import forestry.api.recipes.IFabricatorRecipe;
import forestry.factory.inventory.InventoryFabricator;
import forestry.factory.tiles.TileFabricator;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * L2 {@link RecipeTestExtension} for the fabricator. Same player-driven pattern as the carpenter:
 * the BE matches its currentRecipe against an {@code InventoryGhostCrafting} ghost matrix that
 * the GUI populates, plus an optional {@code plan} item in the dedicated plan slot. Without
 * seeding either, the BE never matches the recipe and the test TIMEOUTs.
 *
 * <p>Returns {@link InjectionDecision#FALL_THROUGH} after seeding so the L1 path still injects
 * crafting-grid materials into the fabricator's main-inventory slots (3-20), the molten input
 * fluid, and energy via the {@link ForestryFabricatorAdapter}. The extension is purely additive —
 * it adds the ghost-matrix and plan-slot seeding the L1 path can't do.
 */
public final class ForestryFabricatorExtension implements RecipeTestExtension<IFabricatorRecipe> {

	private static final Logger LOGGER = LogManager.getLogger("forestry/recipetest/fabricator");

	/** {@link InventoryFabricator#SLOT_PLAN} — the plan slot on the fabricator's main inventory. */
	private static final int SLOT_PLAN = InventoryFabricator.SLOT_PLAN;

	/** Side length of the ghost matrix grid (always 3 for vanilla {@code ShapedRecipePattern}). */
	private static final int GRID_SIDE = 3;

	private static final ResourceLocation RECIPE_TYPE = ResourceLocation.fromNamespaceAndPath("forestry", "fabricator");

	@Override
	public ResourceLocation recipeType() {
		return RECIPE_TYPE;
	}

	@Override
	public InjectionDecision injectInputs(TestContext ctx, RecipeHolder<IFabricatorRecipe> holder) {
		BlockEntity be = ctx.level().getBlockEntity(ctx.origin());
		if (!(be instanceof TileFabricator fabricator)) {
			LOGGER.warn("Fabricator recipe-test fired but BE at origin is {}, not TileFabricator; falling through", be);
			return InjectionDecision.FALL_THROUGH;
		}
		IFabricatorRecipe recipe = holder.value();
		seedGhostMatrix(fabricator.getCraftingInventory(), recipe.getCraftingGridRecipe());
		seedPlanSlot(fabricator, recipe.getPlan());
		seedMoltenTank(fabricator, recipe.getResultFluid());
		// Fall through so the L1 path injects general-inventory items (slots 3-20) and energy
		// via the ForestryFabricatorAdapter — the extension only handles what L1 can't:
		// ghost matrix, plan slot, and the molten tank (the moltenTank's FilteredTank rejects
		// the L1 IFluidHandler.fill path because forestry:glass / molten metals only pass the
		// FABRICATOR_SMELTING_OUTPUT filter; bypassing via setFluid is intentional).
		return InjectionDecision.FALL_THROUGH;
	}

	/**
	 * Place a representative {@link ItemStack} for each non-empty {@link Ingredient} in the
	 * recipe's grid pattern at the matching slot of the 3×3 ghost matrix.
	 */
	private static void seedGhostMatrix(Container ghost, ShapedRecipe grid) {
		ShapedRecipePattern pattern = grid.pattern;
		NonNullList<Ingredient> ingredients = pattern.ingredients();
		int width = pattern.width();
		// pattern.ingredients() is a row-major list sized width × height. Map (col, row) into
		// a 3×3 ghost matrix's row-major slot index.
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
	}

	/**
	 * Seed the plan slot of the fabricator's main inventory if the recipe requires a plan
	 * (e.g. circuit-board-plan recipes). Empty plan {@link Ingredient} → no-op.
	 */
	private static void seedPlanSlot(TileFabricator fabricator, Ingredient plan) {
		if (plan == null || plan.isEmpty()) {
			return;
		}
		ItemStack[] candidates = plan.getItems();
		if (candidates.length == 0) {
			return;
		}
		// TileFabricator implements WorldlyContainer (extends Container) via TilePowered's chain,
		// so setItem works directly against the fabricator's main inventory.
		fabricator.setItem(SLOT_PLAN, candidates[0].copy());
	}

	/**
	 * Pre-fill the fabricator's molten tank with the recipe's required fluid by bypassing the
	 * tank's filter. The {@code moltenTank} is a {@code FilteredTank} that only accepts fluids
	 * registered as outputs of {@code forestry:fabricator_smelting} recipes (you'd normally fill
	 * it by dropping glass/metal into the metal slot for the BE to smelt). The L1
	 * {@code IFluidHandler.fill} path goes through that filter and is rejected even when the
	 * fluid is otherwise valid for the recipe match. Direct {@code FluidTank.setFluid} bypasses
	 * the filter — it's the same path the BE itself uses when reading NBT on world load.
	 */
	private static void seedMoltenTank(TileFabricator fabricator, FluidStack molten) {
		if (molten == null || molten.isEmpty()) {
			return;
		}
		IFluidTank tank = fabricator.getTankManager().getTank(0);
		if (tank instanceof FluidTank ft) {
			ft.setFluid(molten.copy());
		} else {
			LOGGER.warn("Fabricator's tank 0 is not a FluidTank ({}); skipping molten seed", tank.getClass().getName());
		}
	}
}
