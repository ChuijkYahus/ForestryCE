package forestry.core.content.machines.recipes;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.core.machines.IMoistenerRecipe;
import forestry.core.content.machines.features.FactoryRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public class MoistenerRecipe implements IMoistenerRecipe {
	private static final MapCodec<MoistenerRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		ResourceLocation.CODEC.fieldOf("id").forGetter(MoistenerRecipe::getId),
		Ingredient.CODEC_NONEMPTY.fieldOf("resource").forGetter(MoistenerRecipe::getInput),
		ItemStack.STRICT_CODEC.fieldOf("product").forGetter(MoistenerRecipe::getProduct),
		Codec.INT.fieldOf("time").forGetter(MoistenerRecipe::getTimePerItem)
	).apply(instance, MoistenerRecipe::new));
	private static final StreamCodec<RegistryFriendlyByteBuf, MoistenerRecipe> STREAM_CODEC = StreamCodec.of(
		Serializer::toNetwork,
		Serializer::fromNetwork
	);

	private final ResourceLocation id;
	private final int timePerItem;
	private final Ingredient resource;
	private final ItemStack product;

	public MoistenerRecipe(ResourceLocation id, Ingredient resource, ItemStack product, int timePerItem) {
		Preconditions.checkNotNull(id, "Recipe identifier cannot be null");
		Preconditions.checkNotNull(resource, "Resource cannot be null");
		Preconditions.checkNotNull(product, "Product cannot be null");

		this.id = id;
		this.timePerItem = timePerItem;
		this.resource = resource;
		this.product = product;
	}

	@Override
	public int getTimePerItem() {
		return timePerItem;
	}

	@Override
	public Ingredient getInput() {
		return resource;
	}

	@Override
	public ItemStack getProduct() {
		return product;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider lookupProvider) {
		return this.product;
	}

	@Override
	public ResourceLocation getId() {
		return id;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return FactoryRecipeTypes.MOISTENER.serializer();
	}

	@Override
	public RecipeType<?> getType() {
		return FactoryRecipeTypes.MOISTENER.type();
	}

	public static class Serializer implements RecipeSerializer<MoistenerRecipe> {
		@Override
		public MapCodec<MoistenerRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, MoistenerRecipe> streamCodec() {
			return STREAM_CODEC;
		}

		private static MoistenerRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
			ResourceLocation recipeId = ResourceLocation.STREAM_CODEC.decode(buffer);
			Ingredient resource = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
			ItemStack product = ItemStack.STREAM_CODEC.decode(buffer);
			int timePerItem = ByteBufCodecs.VAR_INT.decode(buffer);

			return new MoistenerRecipe(recipeId, resource, product, timePerItem);
		}

		private static void toNetwork(RegistryFriendlyByteBuf buffer, MoistenerRecipe recipe) {
			ResourceLocation.STREAM_CODEC.encode(buffer, recipe.id);
			Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.resource);
			ItemStack.STREAM_CODEC.encode(buffer, recipe.product);
			ByteBufCodecs.VAR_INT.encode(buffer, recipe.timePerItem);
		}
	}
}
