package forestry.core.data.builder;

import forestry.core.content.machines.recipes.FabricatorRecipe;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.fluids.FluidStack;

public class FabricatorRecipeBuilder {
	private static final Criterion<ImpossibleTrigger.TriggerInstance> IMPOSSIBLE = new Criterion<>(CriteriaTriggers.IMPOSSIBLE, new ImpossibleTrigger.TriggerInstance());

	private Ingredient plan;
	private FluidStack molten;
	private ShapedRecipe recipe;

	public FabricatorRecipeBuilder setPlan(Ingredient plan) {
		this.plan = plan;
		return this;
	}

	public FabricatorRecipeBuilder setMolten(FluidStack molten) {
		this.molten = molten;
		return this;
	}

	public FabricatorRecipeBuilder recipe(ShapedRecipeBuilder recipe) {
		RecipeCapture capture = new RecipeCapture();
		recipe.unlockedBy("impossible", IMPOSSIBLE).save(capture, ResourceLocation.withDefaultNamespace("forestry_fabricator"));
		this.recipe = capture.recipe(ResourceLocation.withDefaultNamespace("forestry_fabricator"), ShapedRecipe.class);
		return this;
	}

	public void build(RecipeOutput output, ResourceLocation id) {
		output.accept(id, new FabricatorRecipe(id, this.plan, this.molten, this.recipe), null);
	}
}
