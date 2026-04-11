package forestry.factory.recipes;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import forestry.Forestry;
import forestry.api.recipes.ISmelterRecipe;
import forestry.api.recipes.ISqueezerRecipe;
import forestry.core.recipes.IngredientStack;
import forestry.factory.features.FactoryRecipeTypes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class SmelterRecipe implements ISmelterRecipe {
	private final ResourceLocation id;
	private final int temperature;
	private final List<IngredientStack> inputs;
	private final ItemStack output;

	private final int processingTime;

	public SmelterRecipe(ResourceLocation id, int temperature, List<IngredientStack> inputs, ItemStack output, int processTime) {
		Preconditions.checkNotNull(id, "Recipe identifier cannot be null");
		Preconditions.checkNotNull(inputs);
		Preconditions.checkArgument(!inputs.isEmpty());
		Preconditions.checkNotNull(output);

		this.id = id;
		this.temperature = temperature;
		this.inputs = inputs;
		this.output = output;
		this.processingTime = processTime;
	}

	@Override
	public List<IngredientStack> getInputs() {
		return this.inputs;
	}

	@Override
	public ItemStack getOutput() { return this.output; }

	@Override
	public int getTemperature() {
		return this.temperature;
	}

	@Override
	public int getProcessingTime() {
		return this.processingTime;
	}

	@Override
	public ItemStack getResultItem(RegistryAccess registryAccess) {
		return output;
	}

	@Override
	public ResourceLocation getId() {
		return this.id;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return FactoryRecipeTypes.SMELTER.serializer();
	}

	@Override
	public RecipeType<?> getType() {
		return FactoryRecipeTypes.SMELTER.type();
	}

	public boolean matches(int temp, List<IngredientStack> in, ItemStack out){
		return (
			this.temperature == temp
			&& in.equals(this.inputs)
			&& out.equals(output)
			);
	}

	/**
	 * Helper method to see if a smelter has enough contents to fulfil a certain recipe.
	 * Currently only used for the Smelter
	 * @param recipe The Smelter recipe to check against.
	 * @param contents The contents of the given Smelter
	 * @return A boolean, if the contents of the Smelter are sufficient enough to alloy a given recipe.
	 */
	public static boolean canAlloy(ISmelterRecipe recipe, List<ItemStack> contents){
		//Forestry.LOGGER.info(recipe.toString());
		//Loop through all required inputs for a given recipe
		for(IngredientStack s: recipe.getInputs()) {
			//Forestry.LOGGER.info(s.toString());
			int found = 0;
			//Loop through all the current contents and see if one of the contents matches what is required by the recipe
			for (ItemStack i : contents) {
				//Forestry.LOGGER.info(i.toString());
				if (s.getIngredient().test(i)) { //Note we need to check if there are enough too.
					//Forestry.LOGGER.info("found an matching ingredient");
					found += i.getCount();
					if (found >= s.getCount()) break;
				}
				//Forestry.LOGGER.info("--------------");
			}
			if (found < s.getCount()) return false;
		}
		return true;
	}

	public static class Serializer implements RecipeSerializer<SmelterRecipe> {
		@Override
		public SmelterRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
			int temperature = GsonHelper.getAsInt(json, "temperature");
			ArrayList<IngredientStack> ingredients = new ArrayList<>();
			for (JsonElement element : GsonHelper.getAsJsonArray(json, "inputs")) {
				ingredients.add(IngredientStack.fromJson(element.getAsJsonObject()));
			}
			ItemStack out = RecipeSerializers.item(GsonHelper.getAsJsonObject(json, "output"));
			int processTime = GsonHelper.getAsInt(json, "processingTime");
			return new SmelterRecipe(recipeId, temperature, ingredients, out, processTime);
		}

		@Override
		public SmelterRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
			int processingTime = buffer.readVarInt();
			List<IngredientStack> resources = new ArrayList<>();
			//Read the size of the list of ingredients. I miss scanners.
			int count = buffer.readVarInt();
			for(int i = 0; i < count; i++) {
				resources.add(IngredientStack.fromNetwork(buffer));
			}
			ItemStack out = buffer.readItem();
			int processTime = buffer.readVarInt();

			return new SmelterRecipe(recipeId, processingTime, resources, out, processTime);
		}

		@Override
		public void toNetwork(FriendlyByteBuf buffer, SmelterRecipe recipe) {
			buffer.writeVarInt(recipe.temperature);
			//Can't store just the list of ingredients. Have to also store the size of the list. whack.
			buffer.writeVarInt(recipe.getInputs().size());
			for (IngredientStack i: recipe.getInputs()){
				i.toNetwork(buffer);
			}
			buffer.writeItem(recipe.output);
			buffer.writeVarInt(recipe.processingTime);
		}
	}

	public String toString(){
		return this.id.toString() + "= \n[ "
			+ this.inputs
			+ " ]: "+ this.output;
	}
}
