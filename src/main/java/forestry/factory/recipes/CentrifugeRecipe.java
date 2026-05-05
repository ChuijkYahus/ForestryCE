package forestry.factory.recipes;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import forestry.api.core.Product;
import forestry.api.recipes.ICentrifugeRecipe;
import forestry.factory.features.FactoryRecipeTypes;

public class CentrifugeRecipe implements ICentrifugeRecipe {
	private static final MapCodec<CentrifugeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		ResourceLocation.CODEC.fieldOf("id").forGetter(CentrifugeRecipe::getId),
		Codec.INT.fieldOf("time").forGetter(CentrifugeRecipe::getProcessingTime),
		Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(CentrifugeRecipe::getInput),
		Product.CODEC.listOf().fieldOf("products").forGetter(CentrifugeRecipe::getAllProducts)
	).apply(instance, CentrifugeRecipe::new));
	private static final StreamCodec<RegistryFriendlyByteBuf, CentrifugeRecipe> STREAM_CODEC = StreamCodec.of(
		Serializer::toNetwork,
		Serializer::fromNetwork
	);

	private final ResourceLocation id;
	private final int processingTime;
	private final Ingredient input;
	private final List<Product> products;

	public CentrifugeRecipe(ResourceLocation id, int processingTime, Ingredient input, List<Product> products) {
		Preconditions.checkNotNull(id, "Recipe identifier cannot be null");

		this.id = id;
		this.processingTime = processingTime;
		this.input = input;
		this.products = products;
	}

	@Override
	public Ingredient getInput() {
		return this.input;
	}

	@Override
	public int getProcessingTime() {
		return this.processingTime;
	}

	@Override
	public List<ItemStack> getProducts(RandomSource random, double outputMult) {
		ArrayList<ItemStack> products = new ArrayList<>();

		for (Product entry : this.products) {
			double probability = entry.chance() * outputMult;

			if (probability >= 1.0) {
				products.add(entry.createStack());
			} else if (random.nextFloat() < probability) {
				products.add(entry.createStack());
			}
		}

		return products;
	}

	@Override
	public List<Product> getAllProducts() {
		return this.products;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider lookupProvider) {
		return ItemStack.EMPTY;
	}

	@Override
	public ResourceLocation getId() {
		return this.id;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return FactoryRecipeTypes.CENTRIFUGE.serializer();
	}

	@Override
	public RecipeType<?> getType() {
		return FactoryRecipeTypes.CENTRIFUGE.type();
	}

	public static class Serializer implements RecipeSerializer<CentrifugeRecipe> {
		@Override
		public MapCodec<CentrifugeRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, CentrifugeRecipe> streamCodec() {
			return STREAM_CODEC;
		}

		private static CentrifugeRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
			ResourceLocation recipeId = ResourceLocation.STREAM_CODEC.decode(buffer);
			int processingTime = ByteBufCodecs.VAR_INT.decode(buffer);
			Ingredient input = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
			int count = ByteBufCodecs.VAR_INT.decode(buffer);
			List<Product> products = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				products.add(Product.fromNetwork(buffer));
			}

			return new CentrifugeRecipe(recipeId, processingTime, input, products);
		}

		private static void toNetwork(RegistryFriendlyByteBuf buffer, CentrifugeRecipe recipe) {
			ResourceLocation.STREAM_CODEC.encode(buffer, recipe.id);
			ByteBufCodecs.VAR_INT.encode(buffer, recipe.processingTime);
			Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input);
			ByteBufCodecs.VAR_INT.encode(buffer, recipe.products.size());
			for (Product product : recipe.products) {
				Product.toNetwork(buffer, product);
			}
		}
	}
}
