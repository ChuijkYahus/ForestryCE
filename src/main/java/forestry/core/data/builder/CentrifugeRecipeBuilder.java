package forestry.core.data.builder;

import com.google.common.base.Preconditions;
import forestry.api.core.Product;
import forestry.factory.recipes.CentrifugeRecipe;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;

public class CentrifugeRecipeBuilder {
	private int processingTime;
	private Ingredient input;
	private final ArrayList<Product> outputs = new ArrayList<>();

	public CentrifugeRecipeBuilder setProcessingTime(int processingTime) {
		this.processingTime = processingTime;
		return this;
	}

	public CentrifugeRecipeBuilder setInput(Ingredient input) {
		this.input = input;
		return this;
	}

	public CentrifugeRecipeBuilder product(float chance, ItemStack stack) {
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		this.outputs.add(new Product(stack.getItem(), stack.getCount(), customData == null ? null : customData.copyTag(), chance));
		return this;
	}

	public void build(RecipeOutput output, ResourceLocation id) {
		Preconditions.checkState(!this.outputs.isEmpty(), "Empty centrifuge recipes are not allowed");
		output.accept(id, new CentrifugeRecipe(id, this.processingTime, this.input, this.outputs), null);
	}
}
