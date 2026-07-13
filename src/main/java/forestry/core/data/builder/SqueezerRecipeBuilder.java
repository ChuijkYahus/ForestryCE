package forestry.core.data.builder;

import forestry.factory.recipes.SqueezerRecipe;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.List;

public class SqueezerRecipeBuilder {
	private int processingTime;
	private List<Ingredient> resources;
	private SizedFluidIngredient fluidOutput;
	private ItemStack remnants = ItemStack.EMPTY;
	private float remnantsChance;

	public SqueezerRecipeBuilder setProcessingTime(int processingTime) {
		this.processingTime = processingTime;
		return this;
	}

	public SqueezerRecipeBuilder setResources(List<Ingredient> resources) {
		this.resources = resources;
		return this;
	}

	public SqueezerRecipeBuilder setFluidOutput(FluidStack fluidOutput) {
		this.fluidOutput = SizedFluidIngredient.of(fluidOutput);
		return this;
	}

	/** Sets a fluid ingredient output, e.g. a fluid tag resolved at runtime to whatever a loaded mod fills. */
	public SqueezerRecipeBuilder setFluidOutput(SizedFluidIngredient fluidOutput) {
		this.fluidOutput = fluidOutput;
		return this;
	}

	public SqueezerRecipeBuilder setRemnants(ItemStack remnants) {
		this.remnants = remnants;
		return this;
	}

	public SqueezerRecipeBuilder setRemnantsChance(float remnantsChance) {
		this.remnantsChance = remnantsChance;
		return this;
	}

	public void build(RecipeOutput output, ResourceLocation id) {
		output.accept(id, new SqueezerRecipe(id, this.processingTime, this.resources, this.fluidOutput, this.remnants, this.remnantsChance), null);
	}
}
