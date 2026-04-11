package forestry.api.recipes;

import forestry.core.recipes.IngredientStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

public interface ISmelterRecipe extends IForestryRecipe {

	/**
	 * @return How hot the smelter has to be to process this recipe
	 */
	int getTemperature();

	/**
	 * @return How long this recipe takes to process
	 */
	int getProcessingTime();


	/**
	 * @return item stacks representing the required resources for one process.
	 */
	List<IngredientStack> getInputs();

	/**
	 * @return the result
	 */
	ItemStack getOutput();

	boolean matches(int temp, List<IngredientStack> in, ItemStack out);
}
