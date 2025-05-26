package forestry.core.data.builder;

import com.google.gson.JsonObject;
import forestry.factory.features.FactoryRecipeTypes;
import forestry.factory.recipes.RecipeSerializers;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class StillRecipeBuilder {

	private int timePerUnit;
	private FluidStack input;
	private FluidStack output;

	public StillRecipeBuilder setTimePerUnit(int timePerUnit) {
		this.timePerUnit = timePerUnit;
		return this;
	}

	public StillRecipeBuilder setInput(FluidStack input) {
		this.input = input;
		return this;
	}

	public StillRecipeBuilder setOutput(FluidStack output) {
		this.output = output;
		return this;
	}

	public void build(Consumer<FinishedRecipe> consumer, ResourceLocation id) {
		consumer.accept(new Result(id, this.timePerUnit, this.input, this.output));
	}

	private static class Result implements FinishedRecipe {
		private final ResourceLocation id;
		private final int timePerUnit;
		private final FluidStack input;
		private final FluidStack output;

		public Result(ResourceLocation id, int timePerUnit, FluidStack input, FluidStack output) {
			this.id = id;
			this.timePerUnit = timePerUnit;
			this.input = input;
			this.output = output;
		}

		@Override
		public void serializeRecipeData(JsonObject json) {
			json.addProperty("time", this.timePerUnit);
			json.add("input", RecipeSerializers.serializeFluid(this.input));
			json.add("output", RecipeSerializers.serializeFluid(this.output));
		}

		@Override
		public ResourceLocation getId() {
			return this.id;
		}

		@Override
		public RecipeSerializer<?> getType() {
			return FactoryRecipeTypes.STILL.serializer();
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
