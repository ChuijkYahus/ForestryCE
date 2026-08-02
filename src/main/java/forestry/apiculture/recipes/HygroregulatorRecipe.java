package forestry.apiculture.recipes;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.recipes.IHygroregulatorRecipe;
import forestry.apiculture.features.ApicultureRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidStack;

// recipes used by Alveary Hygroregulator
public class HygroregulatorRecipe implements IHygroregulatorRecipe {
	private static final MapCodec<HygroregulatorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		ResourceLocation.CODEC.fieldOf("id").forGetter(HygroregulatorRecipe::getId),
		FluidStack.CODEC.fieldOf("liquid").forGetter(HygroregulatorRecipe::getInputFluid),
		Codec.INT.fieldOf("time").forGetter(HygroregulatorRecipe::getRetainTime),
		Codec.BYTE.fieldOf("humidity_steps").forGetter(HygroregulatorRecipe::getHumiditySteps),
		Codec.BYTE.fieldOf("temperature_steps").forGetter(HygroregulatorRecipe::getTemperatureSteps)
	).apply(instance, HygroregulatorRecipe::new));
	private static final StreamCodec<RegistryFriendlyByteBuf, HygroregulatorRecipe> STREAM_CODEC = StreamCodec.of(
		Serializer::toNetwork,
		Serializer::fromNetwork
	);

	private final ResourceLocation id;
	private final FluidStack liquid;
	private final byte humiditySteps;
	private final byte temperatureSteps;
	private final int retainTime;

	public HygroregulatorRecipe(ResourceLocation id, FluidStack liquid, int retainTime, byte humiditySteps, byte temperatureSteps) {
		Preconditions.checkNotNull(id, "Recipe identifier cannot be null");
		Preconditions.checkNotNull(liquid);
		this.id = id;
		this.liquid = liquid;
		this.retainTime = retainTime;
		this.humiditySteps = humiditySteps;
		this.temperatureSteps = temperatureSteps;
	}

	@Override
	public FluidStack getInputFluid() {
		return this.liquid;
	}

	@Override
	public int getRetainTime() {
		return this.retainTime;
	}

	@Override
	public byte getHumiditySteps() {
		return this.humiditySteps;
	}

	@Override
	public byte getTemperatureSteps() {
		return this.temperatureSteps;
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
		return ApicultureRecipeTypes.HYGROREGULATOR.serializer();
	}

	@Override
	public RecipeType<?> getType() {
		return ApicultureRecipeTypes.HYGROREGULATOR.type();
	}

	public static class Serializer implements RecipeSerializer<HygroregulatorRecipe> {
		@Override
		public MapCodec<HygroregulatorRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, HygroregulatorRecipe> streamCodec() {
			return STREAM_CODEC;
		}

		private static HygroregulatorRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
			ResourceLocation recipeId = ResourceLocation.STREAM_CODEC.decode(buffer);
			FluidStack liquid = FluidStack.STREAM_CODEC.decode(buffer);
			int retainTime = ByteBufCodecs.VAR_INT.decode(buffer);
			byte humiditySteps = ByteBufCodecs.BYTE.decode(buffer);
			byte temperatureSteps = ByteBufCodecs.BYTE.decode(buffer);

			return new HygroregulatorRecipe(recipeId, liquid, retainTime, humiditySteps, temperatureSteps);
		}

		private static void toNetwork(RegistryFriendlyByteBuf buffer, HygroregulatorRecipe recipe) {
			ResourceLocation.STREAM_CODEC.encode(buffer, recipe.id);
			FluidStack.STREAM_CODEC.encode(buffer, recipe.liquid);
			ByteBufCodecs.VAR_INT.encode(buffer, recipe.retainTime);
			ByteBufCodecs.BYTE.encode(buffer, recipe.humiditySteps);
			ByteBufCodecs.BYTE.encode(buffer, recipe.temperatureSteps);
		}
	}
}
