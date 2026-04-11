package forestry.core.recipes;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * A custom class to handle stacks of ingredients, for machines that process multiple things at once.
 */
public class IngredientStack {

	private final Ingredient ingredient;
	private final int count;

	public IngredientStack(Ingredient i, int c){
		this.ingredient = i;
		this.count = c;
	}

	public Ingredient getIngredient(){
		return this.ingredient;
	}

	public int getCount(){
		return this.count;
	}

	public JsonObject toJson() {
		JsonObject obj = new JsonObject();
		obj.add("ingredient", ingredient.toJson());
		obj.addProperty("count", count);
		return obj;
	}

	public static IngredientStack fromJson(JsonObject obj){
		return new IngredientStack(
			Ingredient.fromJson(obj.get("ingredient")),
			obj.get("count").getAsInt()
		);
	}

	public void toNetwork(FriendlyByteBuf buf) {
		this.ingredient.toNetwork(buf);
		buf.writeVarInt(this.count);
	}

	public static IngredientStack fromNetwork(FriendlyByteBuf buf) {
		Ingredient ingredient = Ingredient.fromNetwork(buf);
		int count = buf.readVarInt();
		return new IngredientStack(ingredient, count);
	}

	public String toString(){
		return "[ " + this.ingredient + " * " + this.count + " ]";
	}

}
