package forestry.factory.recipes;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.recipes.ISqueezerRecipe;
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

import java.util.List;

public class SqueezerRecipe implements ISqueezerRecipe {
	private static final MapCodec<SqueezerRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		ResourceLocation.CODEC.fieldOf("id").forGetter(SqueezerRecipe::getId),
		Codec.INT.fieldOf("time").forGetter(SqueezerRecipe::getProcessingTime),
		Ingredient.CODEC_NONEMPTY.listOf().fieldOf("resources").forGetter(SqueezerRecipe::getInputs),
		FluidStack.CODEC.fieldOf("output").forGetter(SqueezerRecipe::getFluidOutput),
		ItemStack.STRICT_CODEC.fieldOf("remnant").forGetter(SqueezerRecipe::getRemnants),
		Codec.FLOAT.fieldOf("chance").forGetter(SqueezerRecipe::getRemnantsChance)
	).apply(instance, SqueezerRecipe::new));
	private static final StreamCodec<RegistryFriendlyByteBuf, SqueezerRecipe> STREAM_CODEC = StreamCodec.of(
		Serializer::toNetwork,
		Serializer::fromNetwork
	);

	private final ResourceLocation id;
	private final int processingTime;
	private final List<Ingredient> resources;
	private final FluidStack fluidOutput;
	private final ItemStack remnants;
	private final float remnantsChance;

	public SqueezerRecipe(ResourceLocation id, int processingTime, List<Ingredient> resources, FluidStack fluidOutput, ItemStack remnants, float remnantsChance) {
		Preconditions.checkNotNull(id, "Recipe identifier cannot be null");
		Preconditions.checkNotNull(resources);
		Preconditions.checkArgument(!resources.isEmpty());
		Preconditions.checkNotNull(fluidOutput);
		Preconditions.checkNotNull(remnants);

		this.id = id;
		this.processingTime = processingTime;
		this.resources = resources;
		this.fluidOutput = fluidOutput;
		this.remnants = remnants;
		this.remnantsChance = remnantsChance;
	}

	@Override
	public List<Ingredient> getInputs() {
		return this.resources;
	}

	@Override
	public ItemStack getRemnants() {
		return this.remnants;
	}

	@Override
	public float getRemnantsChance() {
		return this.remnantsChance;
	}

	@Override
	public FluidStack getFluidOutput() {
		return this.fluidOutput;
	}

	@Override
	public int getProcessingTime() {
		return this.processingTime;
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
		return FactoryRecipeTypes.SQUEEZER.serializer();
	}

	@Override
	public RecipeType<?> getType() {
		return FactoryRecipeTypes.SQUEEZER.type();
	}

	public static class Serializer implements RecipeSerializer<SqueezerRecipe> {
		@Override
		public MapCodec<SqueezerRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, SqueezerRecipe> streamCodec() {
			return STREAM_CODEC;
		}

		private static SqueezerRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
			ResourceLocation recipeId = ResourceLocation.STREAM_CODEC.decode(buffer);
			int processingTime = ByteBufCodecs.VAR_INT.decode(buffer);
			List<Ingredient> resources = Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer);
			FluidStack fluidOutput = FluidStack.STREAM_CODEC.decode(buffer);
			ItemStack remnants = ItemStack.STREAM_CODEC.decode(buffer);
			float remnantsChance = ByteBufCodecs.FLOAT.decode(buffer);

			return new SqueezerRecipe(recipeId, processingTime, resources, fluidOutput, remnants, remnantsChance);
		}

		private static void toNetwork(RegistryFriendlyByteBuf buffer, SqueezerRecipe recipe) {
			ResourceLocation.STREAM_CODEC.encode(buffer, recipe.id);
			ByteBufCodecs.VAR_INT.encode(buffer, recipe.processingTime);
			Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, recipe.resources);
			FluidStack.STREAM_CODEC.encode(buffer, recipe.fluidOutput);
			ItemStack.STREAM_CODEC.encode(buffer, recipe.remnants);
			ByteBufCodecs.FLOAT.encode(buffer, recipe.remnantsChance);
		}
	}
}
