package forestry.factory.recipes;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.recipes.ICarpenterRecipe;
import forestry.factory.features.FactoryRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.Optional;

public class CarpenterRecipe implements ICarpenterRecipe {
	private static final MapCodec<CarpenterRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		ResourceLocation.CODEC.fieldOf("id").forGetter(CarpenterRecipe::getId),
		Codec.INT.fieldOf("time").forGetter(CarpenterRecipe::getPackagingTime),
		FluidStack.OPTIONAL_CODEC.optionalFieldOf("liquid", FluidStack.EMPTY).forGetter(CarpenterRecipe::getInputFluid),
		Ingredient.CODEC.fieldOf("box").forGetter(CarpenterRecipe::getBox),
		// only ShapedRecipe is supported on the network path; see PORTING.md
		RecipeSerializer.SHAPED_RECIPE.codec().codec()
			.xmap(sr -> (CraftingRecipe) sr, cr -> (ShapedRecipe) cr)
			.fieldOf("recipe")
			.forGetter(CarpenterRecipe::getCraftingGridRecipe),
		ItemStack.STRICT_CODEC.optionalFieldOf("result").forGetter(CarpenterRecipe::getOptionalResult)
	).apply(instance, (id, time, liquid, box, recipe, optResult) -> new CarpenterRecipe(id, time, liquid, box, recipe, optResult.orElse(null))));
	private static final StreamCodec<RegistryFriendlyByteBuf, CarpenterRecipe> STREAM_CODEC = StreamCodec.of(
		Serializer::toNetwork,
		Serializer::fromNetwork
	);

	private final ResourceLocation id;
	private final int packagingTime;
	private final FluidStack liquid;
	private final Ingredient box;
	private final CraftingRecipe recipe;
	@Nullable
	private final ItemStack result;

	public CarpenterRecipe(ResourceLocation id, int packagingTime, FluidStack liquid, Ingredient box, CraftingRecipe recipe, @Nullable ItemStack result) {
		Preconditions.checkNotNull(id, "Recipe identifier cannot be null");
		Preconditions.checkNotNull(box);
		Preconditions.checkNotNull(recipe);

		this.id = id;
		this.packagingTime = packagingTime;
		this.liquid = liquid;
		this.box = box;
		this.recipe = recipe;
		this.result = result;
	}

	@Override
	public int getPackagingTime() {
		return this.packagingTime;
	}

	@Override
	public Ingredient getBox() {
		return this.box;
	}

	@Override
	public FluidStack getInputFluid() {
		return this.liquid;
	}

	@Override
	public CraftingRecipe getCraftingGridRecipe() {
		return this.recipe;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider lookupProvider) {
		return this.result != null ? this.result : this.recipe.getResultItem(lookupProvider);
	}

	Optional<ItemStack> getOptionalResult() {
		return Optional.ofNullable(this.result);
	}

	@Override
	public boolean matches(FluidStack fluid, ItemStack boxStack, Container craftingInventory, Level level) {
		FluidStack liquid = this.liquid;
		if (!liquid.isEmpty()) {
			if (fluid.isEmpty() || !fluid.containsFluid(liquid)) {
				return false;
			}
		}

		Ingredient box = this.box;
		if (!box.isEmpty() && !box.test(boxStack)) {
			return false;
		}

		return this.recipe.matches(FakeCraftingInventory.of(craftingInventory), level);
	}

	@Override
	public ResourceLocation getId() {
		return this.id;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return FactoryRecipeTypes.CARPENTER.serializer();
	}

	@Override
	public RecipeType<?> getType() {
		return FactoryRecipeTypes.CARPENTER.type();
	}

	public static class Serializer implements RecipeSerializer<CarpenterRecipe> {
		@Override
		public MapCodec<CarpenterRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, CarpenterRecipe> streamCodec() {
			return STREAM_CODEC;
		}

		private static CarpenterRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
			ResourceLocation recipeId = ResourceLocation.STREAM_CODEC.decode(buffer);
			int packagingTime = ByteBufCodecs.VAR_INT.decode(buffer);
			FluidStack liquid = FluidStack.OPTIONAL_STREAM_CODEC.decode(buffer);
			Ingredient box = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
			ShapedRecipe internal = RecipeSerializer.SHAPED_RECIPE.streamCodec().decode(buffer);
			ItemStack result = buffer.readBoolean() ? ItemStack.STREAM_CODEC.decode(buffer) : null;

			return new CarpenterRecipe(recipeId, packagingTime, liquid, box, internal, result);
		}

		private static void toNetwork(RegistryFriendlyByteBuf buffer, CarpenterRecipe recipe) {
			ResourceLocation.STREAM_CODEC.encode(buffer, recipe.id);
			ByteBufCodecs.VAR_INT.encode(buffer, recipe.packagingTime);
			FluidStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.liquid);
			Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.box);
			RecipeSerializer.SHAPED_RECIPE.streamCodec().encode(buffer, (ShapedRecipe) recipe.recipe);

			boolean hasResult = recipe.result != null;
			buffer.writeBoolean(hasResult);
			if (hasResult) {
				ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
			}
		}
	}
}
