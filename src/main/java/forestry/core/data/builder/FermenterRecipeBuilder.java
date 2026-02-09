package forestry.core.data.builder;

import com.google.gson.JsonObject;
import forestry.core.utils.ModUtil;
import forestry.factory.features.FactoryRecipeTypes;
import forestry.factory.recipes.RecipeSerializers;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.function.Consumer;

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

	public void build(Consumer<FinishedRecipe> consumer, ResourceLocation id) {
		consumer.accept(new Result(id, this.resource, this.fermentationValue, this.modifier, this.output, this.fluidResource));
	}

	private static class Result implements FinishedRecipe {
		private final ResourceLocation id;
		private final Ingredient resource;
		private final int fermentationValue;
		private final float modifier;
		private final Fluid output;
		private final FluidStack fluidResource;

		public Result(ResourceLocation id, Ingredient resource, int fermentationValue, float modifier, Fluid output, FluidStack fluidResource) {
			this.id = id;
			this.resource = resource;
			this.fermentationValue = fermentationValue;
			this.modifier = modifier;
			this.output = output;
			this.fluidResource = fluidResource;
		}

		@Override
		public void serializeRecipeData(JsonObject json) {
			json.add("resource", this.resource.toJson());
			json.addProperty("fermentationValue", this.fermentationValue);
			json.addProperty("modifier", this.modifier);
			json.addProperty("output", ModUtil.getRegistryName(this.output).toString());
			json.add("fluidResource", RecipeSerializers.serializeFluid(this.fluidResource));
		}

		@Override
		public ResourceLocation getId() {
			return this.id;
		}

		@Override
		public RecipeSerializer<?> getType() {
			return FactoryRecipeTypes.FERMENTER.serializer();
		}

		@Nullable
		@Override
		public JsonObject serializeAdvancement() {
			return null;
		}

		@Nullable
		@Override
		public ResourceLocation getAdvancementId() {
			return null;
		}
	}
}
