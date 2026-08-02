package forestry.core.data.builder;

import forestry.core.content.machines.recipes.FermenterRecipe;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

public class FermenterRecipeBuilder {
	private Ingredient resource;
	private int fermentationValue;
	private float modifier = 1.0f;
	private Fluid output;
	private FluidStack fluidResource;

	public FermenterRecipeBuilder setResource(Ingredient resource) {
		this.resource = resource;
		return this;
	}

	public FermenterRecipeBuilder setFermentationValue(int fermentationValue) {
		this.fermentationValue = fermentationValue;
		return this;
	}

	public FermenterRecipeBuilder setModifier(float modifier) {
		this.modifier = modifier;
		return this;
	}

	public FermenterRecipeBuilder setOutput(Fluid output) {
		this.output = output;
		return this;
	}

	public FermenterRecipeBuilder setFluidResource(FluidStack fluidResource) {
		this.fluidResource = fluidResource;
		return this;
	}

	public void build(RecipeOutput output, ResourceLocation id) {
		output.accept(id, new FermenterRecipe(id, this.resource, this.fermentationValue, this.modifier, this.output, this.fluidResource), null);
	}
}
