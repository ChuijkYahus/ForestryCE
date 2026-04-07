package forestry.core.data.builder;

import forestry.factory.recipes.FabricatorSmeltingRecipe;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

public class FabricatorSmeltingRecipeBuilder {

	private int meltingPoint;
	private Ingredient resource;
	private FluidStack product;

	public FabricatorSmeltingRecipeBuilder setMeltingPoint(int meltingPoint) {
		this.meltingPoint = meltingPoint;
		return this;
	}

	public FabricatorSmeltingRecipeBuilder setResource(Ingredient resource) {
		this.resource = resource;
		return this;
	}

	public FabricatorSmeltingRecipeBuilder setProduct(FluidStack product) {
		this.product = product;
		return this;
	}

	public void build(RecipeOutput output, ResourceLocation id) {
		output.accept(id, new FabricatorSmeltingRecipe(id, this.resource, this.product, this.meltingPoint), null);
	}
}
