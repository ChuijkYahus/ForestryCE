package forestry.core.data.builder;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import forestry.core.recipes.IngredientStack;
import forestry.factory.features.FactoryRecipeTypes;
import forestry.factory.recipes.RecipeSerializers;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public class SmelterRecipeBuilder {

	private int temperature;
	private List<IngredientStack> ingredients = new ArrayList<>();
	private ItemStack output;
	private int processingTime;

	public SmelterRecipeBuilder setTemperature(int temp) {
		this.temperature = temp;
		return this;
	}

	public SmelterRecipeBuilder setProcessingTime(int time) {
		this.processingTime = time;
		return this;
	}

	public SmelterRecipeBuilder addIngredient(Ingredient input, int amount) {
		ingredients.add(new IngredientStack(input, amount));
		return this;
	}

	public SmelterRecipeBuilder addIngredient(Ingredient input) {
		return addIngredient(input, 1);
	}

	public SmelterRecipeBuilder setOutput(ItemStack output) {
		return setOutput(output, 1);
	}

	public SmelterRecipeBuilder setOutput(ItemStack output, int amount) {
		this.output = output;
		this.output.setCount(amount);
		return this;
	}

	public void build(Consumer<FinishedRecipe> consumer, ResourceLocation id) {
		consumer.accept(new Result(id, this.temperature, this.ingredients, this.output, this.processingTime));
	}

	private static class Result implements FinishedRecipe {
		private final ResourceLocation id;
		private final int temperature;
		private final List<IngredientStack> ingredients;
		private final ItemStack output;
		private final int processingTime;

		public Result(ResourceLocation id, int temperature, List<IngredientStack> ingredients, ItemStack output, int processingTime) {
			this.id = id;
			this.temperature = temperature;
			this.ingredients = ImmutableList.copyOf(ingredients); //This makes sense to me but idk if I need to do this.
			this.output = output;
			this.processingTime = processingTime;
		}

		@Override
		public void serializeRecipeData(JsonObject json) {
			json.addProperty("temperature", this.temperature);
			JsonArray inputsArray = new JsonArray();
			for (IngredientStack input : this.ingredients) {
				inputsArray.add(input.toJson());
			}
			json.add("inputs", inputsArray);
			json.add("output", RecipeSerializers.item(this.output));
			json.addProperty("processingTime", this.processingTime);
		}

		@Override
		public ResourceLocation getId() {
			return this.id;
		}

		@Override
		public RecipeSerializer<?> getType() {
			return FactoryRecipeTypes.SMELTER.serializer();
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
