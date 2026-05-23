package forestry.factory.recipes;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.recipes.IStillRecipe;
import forestry.factory.features.FactoryRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidStack;

public class StillRecipe implements IStillRecipe {
	private static final MapCodec<StillRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		ResourceLocation.CODEC.fieldOf("id").forGetter(StillRecipe::getId),
		Codec.INT.fieldOf("time").forGetter(StillRecipe::getCyclesPerUnit),
		FluidStack.CODEC.fieldOf("input").forGetter(StillRecipe::getInput),
		FluidStack.CODEC.fieldOf("output").forGetter(StillRecipe::getOutput)
	).apply(instance, StillRecipe::new));
	private static final StreamCodec<RegistryFriendlyByteBuf, StillRecipe> STREAM_CODEC = StreamCodec.of(
		Serializer::toNetwork,
		Serializer::fromNetwork
	);

	private final ResourceLocation id;
	private final int timePerUnit;
	private final FluidStack input;
	private final FluidStack output;

	public StillRecipe(ResourceLocation id, int timePerUnit, FluidStack input, FluidStack output) {
		Preconditions.checkNotNull(id, "Recipe identifier cannot be null");
		Preconditions.checkNotNull(input, "Still recipes need an input. Input was null.");
		Preconditions.checkNotNull(output, "Still recipes need an output. Output was null.");

		this.id = id;
		this.timePerUnit = timePerUnit;
		this.input = input;
		this.output = output;
	}

	@Override
	public int getCyclesPerUnit() {
		return this.timePerUnit;
	}

	@Override
	public FluidStack getInput() {
		return this.input;
	}

	@Override
	public FluidStack getOutput() {
		return this.output;
	}

	@Override
	public boolean matches(FluidStack input) {
		return FluidStack.isSameFluidSameComponents(input, this.input) && input.getAmount() >= this.input.getAmount();
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
		return FactoryRecipeTypes.STILL.serializer();
	}

	@Override
	public RecipeType<?> getType() {
		return FactoryRecipeTypes.STILL.type();
	}

	public static class Serializer implements RecipeSerializer<StillRecipe> {
		@Override
		public MapCodec<StillRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, StillRecipe> streamCodec() {
			return STREAM_CODEC;
		}

		private static StillRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
			ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buffer);
			int timePerUnit = ByteBufCodecs.VAR_INT.decode(buffer);
			FluidStack input = FluidStack.STREAM_CODEC.decode(buffer);
			FluidStack output = FluidStack.STREAM_CODEC.decode(buffer);

			return new StillRecipe(id, timePerUnit, input, output);
		}

		private static void toNetwork(RegistryFriendlyByteBuf buffer, StillRecipe recipe) {
			ResourceLocation.STREAM_CODEC.encode(buffer, recipe.id);
			ByteBufCodecs.VAR_INT.encode(buffer, recipe.timePerUnit);
			FluidStack.STREAM_CODEC.encode(buffer, recipe.input);
			FluidStack.STREAM_CODEC.encode(buffer, recipe.output);
		}
	}
}
