package forestry.factory.recipes;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.recipes.IFabricatorSmeltingRecipe;
import forestry.factory.features.FactoryRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidStack;

public class FabricatorSmeltingRecipe implements IFabricatorSmeltingRecipe {
	private static final MapCodec<FabricatorSmeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		ResourceLocation.CODEC.fieldOf("id").forGetter(FabricatorSmeltingRecipe::getId),
		Ingredient.CODEC_NONEMPTY.fieldOf("resource").forGetter(FabricatorSmeltingRecipe::getInput),
		FluidStack.CODEC.fieldOf("product").forGetter(FabricatorSmeltingRecipe::getResultFluid),
		Codec.INT.fieldOf("melting").forGetter(FabricatorSmeltingRecipe::getMeltingPoint)
	).apply(instance, FabricatorSmeltingRecipe::new));
	private static final StreamCodec<RegistryFriendlyByteBuf, FabricatorSmeltingRecipe> STREAM_CODEC = StreamCodec.of(
		Serializer::toNetwork,
		Serializer::fromNetwork
	);

	private final ResourceLocation id;
	private final Ingredient resource;
	private final FluidStack product;
	private final int meltingPoint;

	public FabricatorSmeltingRecipe(ResourceLocation id, Ingredient resource, FluidStack molten, int meltingPoint) {
		Preconditions.checkNotNull(id, "Recipe identifier cannot be null");
		Preconditions.checkNotNull(resource);
		Preconditions.checkArgument(!resource.isEmpty());
		Preconditions.checkNotNull(molten);

		this.id = id;
		this.resource = resource;
		this.product = molten;
		this.meltingPoint = meltingPoint;
	}

	@Override
	public Ingredient getInput() {
		return this.resource;
	}

	@Override
	public FluidStack getResultFluid() {
		return this.product;
	}

	@Override
	public int getMeltingPoint() {
		return this.meltingPoint;
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
		return FactoryRecipeTypes.FABRICATOR_SMELTING.serializer();
	}

	@Override
	public RecipeType<?> getType() {
		return FactoryRecipeTypes.FABRICATOR_SMELTING.type();
	}

	public static class Serializer implements RecipeSerializer<FabricatorSmeltingRecipe> {
		@Override
		public MapCodec<FabricatorSmeltingRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, FabricatorSmeltingRecipe> streamCodec() {
			return STREAM_CODEC;
		}

		private static FabricatorSmeltingRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
			ResourceLocation recipeId = ResourceLocation.STREAM_CODEC.decode(buffer);
			Ingredient resource = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
			FluidStack product = FluidStack.STREAM_CODEC.decode(buffer);
			int meltingPoint = ByteBufCodecs.VAR_INT.decode(buffer);

			return new FabricatorSmeltingRecipe(recipeId, resource, product, meltingPoint);
		}

		private static void toNetwork(RegistryFriendlyByteBuf buffer, FabricatorSmeltingRecipe recipe) {
			ResourceLocation.STREAM_CODEC.encode(buffer, recipe.id);
			Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.resource);
			FluidStack.STREAM_CODEC.encode(buffer, recipe.product);
			ByteBufCodecs.VAR_INT.encode(buffer, recipe.meltingPoint);
		}
	}
}
