package forestry.factory.recipes;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.core.machines.IFermenterRecipe;
import forestry.factory.features.FactoryRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

public class FermenterRecipe implements IFermenterRecipe {
	private static final MapCodec<FermenterRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		ResourceLocation.CODEC.fieldOf("id").forGetter(FermenterRecipe::getId),
		Ingredient.CODEC_NONEMPTY.fieldOf("resource").forGetter(FermenterRecipe::getInputItem),
		Codec.INT.fieldOf("fermentationValue").forGetter(FermenterRecipe::getFermentationValue),
		Codec.FLOAT.fieldOf("modifier").forGetter(FermenterRecipe::getModifier),
		BuiltInRegistries.FLUID.byNameCodec().fieldOf("output").forGetter(FermenterRecipe::getOutput),
		FluidStack.CODEC.fieldOf("fluidResource").forGetter(FermenterRecipe::getInputFluid)
	).apply(instance, (id, resource, value, mod, out, fluidRes) -> new FermenterRecipe(id, resource, value, mod, out, fluidRes)));
	private static final StreamCodec<RegistryFriendlyByteBuf, FermenterRecipe> STREAM_CODEC = StreamCodec.of(
		Serializer::toNetwork,
		Serializer::fromNetwork
	);

	private final ResourceLocation id;
	private final Ingredient resource;
	private final int fermentationValue;
	private final float modifier;
	private final Fluid output;
	private final FluidStack fluidResource;

	public FermenterRecipe(ResourceLocation id, Ingredient resource, int fermentationValue, float modifier, Fluid output, FluidStack fluidResource) {
		Preconditions.checkNotNull(id, "Recipe identifier cannot be null");
		Preconditions.checkNotNull(resource, "Fermenter Resource cannot be null!");
		Preconditions.checkArgument(!resource.isEmpty(), "Fermenter Resource item cannot be empty!");
		Preconditions.checkNotNull(output, "Fermenter Output cannot be null!");
		Preconditions.checkNotNull(fluidResource, "Fermenter Liquid cannot be null!");

		this.id = id;
		this.resource = resource;
		this.fermentationValue = fermentationValue;
		this.modifier = modifier;
		this.output = output;
		this.fluidResource = fluidResource;
	}

	public FermenterRecipe(ResourceLocation id, int fermentationValue, float modifier, Fluid output, FluidStack fluidResource) {
		Preconditions.checkNotNull(id, "Recipe identifier cannot be null");
		Preconditions.checkNotNull(output, "Fermenter output cannot be null!");
		Preconditions.checkNotNull(fluidResource, "Fermenter liquid cannot be null!");

		this.id = id;
		this.resource = Ingredient.EMPTY;
		this.fermentationValue = fermentationValue;
		this.modifier = modifier;
		this.output = output;
		this.fluidResource = fluidResource;
	}

	@Override
	public Ingredient getInputItem() {
		return this.resource;
	}

	@Override
	public FluidStack getInputFluid() {
		return this.fluidResource;
	}

	@Override
	public int getFermentationValue() {
		return this.fermentationValue;
	}

	@Override
	public float getModifier() {
		return this.modifier;
	}

	@Override
	public Fluid getOutput() {
		return this.output;
	}

	@Override
	public boolean matches(ItemStack inputItem, FluidStack inputFluid) {
		return this.resource.test(inputItem) && FluidStack.isSameFluidSameComponents(this.fluidResource, inputFluid);
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
		return FactoryRecipeTypes.FERMENTER.serializer();
	}

	@Override
	public RecipeType<?> getType() {
		return FactoryRecipeTypes.FERMENTER.type();
	}

	public static class Serializer implements RecipeSerializer<FermenterRecipe> {
		@Override
		public MapCodec<FermenterRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, FermenterRecipe> streamCodec() {
			return STREAM_CODEC;
		}

		private static FermenterRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
			ResourceLocation recipeId = ResourceLocation.STREAM_CODEC.decode(buffer);
			Ingredient resource = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
			int fermentationValue = ByteBufCodecs.VAR_INT.decode(buffer);
			float modifier = ByteBufCodecs.FLOAT.decode(buffer);
			Fluid output = ByteBufCodecs.registry(Registries.FLUID).decode(buffer);
			FluidStack fluidResource = FluidStack.STREAM_CODEC.decode(buffer);

			return new FermenterRecipe(recipeId, resource, fermentationValue, modifier, output, fluidResource);
		}

		private static void toNetwork(RegistryFriendlyByteBuf buffer, FermenterRecipe recipe) {
			ResourceLocation.STREAM_CODEC.encode(buffer, recipe.id);
			Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.resource);
			ByteBufCodecs.VAR_INT.encode(buffer, recipe.fermentationValue);
			ByteBufCodecs.FLOAT.encode(buffer, recipe.modifier);
			ByteBufCodecs.registry(Registries.FLUID).encode(buffer, recipe.output);
			FluidStack.STREAM_CODEC.encode(buffer, recipe.fluidResource);
		}
	}
}
