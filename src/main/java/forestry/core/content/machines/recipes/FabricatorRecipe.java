package forestry.core.content.machines.recipes;

import com.google.common.base.Preconditions;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.core.machines.IFabricatorRecipe;
import forestry.core.content.machines.features.FactoryRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

public class FabricatorRecipe implements IFabricatorRecipe {

	private static final MapCodec<FabricatorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		ResourceLocation.CODEC.fieldOf("id").forGetter(FabricatorRecipe::getId),
		// "plan" is optional: many fabricator recipes (e.g. electron tubes) require no plan.
		// Ingredient.CODEC_NONEMPTY rejects empty ingredients at serialization, so we map
		// Ingredient.EMPTY to absent and back.
		Ingredient.CODEC_NONEMPTY.optionalFieldOf("plan").forGetter(r -> r.plan.isEmpty() ? java.util.Optional.<Ingredient>empty() : java.util.Optional.of(r.plan)),
		FluidStack.CODEC.fieldOf("molten").forGetter(FabricatorRecipe::getResultFluid),
		RecipeSerializer.SHAPED_RECIPE.codec().codec().fieldOf("recipe").forGetter(FabricatorRecipe::getCraftingGridRecipe)
	).apply(instance, (id, plan, molten, recipe) -> new FabricatorRecipe(id, plan.orElse(Ingredient.EMPTY), molten, recipe)));
	private static final StreamCodec<RegistryFriendlyByteBuf, FabricatorRecipe> STREAM_CODEC = StreamCodec.of(
		Serializer::toNetwork,
		Serializer::fromNetwork
	);

	private final ResourceLocation id;
	private final Ingredient plan;
	private final FluidStack resultFluid;
	private final ShapedRecipe recipe;

	public FabricatorRecipe(ResourceLocation id, Ingredient plan, FluidStack resultFluid, ShapedRecipe recipe) {
		Preconditions.checkNotNull(id, "Recipe identifier cannot be null");
		Preconditions.checkNotNull(plan);
		Preconditions.checkNotNull(resultFluid);

		this.id = id;
		this.plan = plan;
		this.resultFluid = resultFluid;
		this.recipe = recipe;
	}

	@Override
	public ResourceLocation getId() {
		return this.id;
	}

	@Override
	public Ingredient getPlan() {
		return this.plan;
	}

	@Override
	public FluidStack getResultFluid() {
		return this.resultFluid;
	}

	@Override
	public ShapedRecipe getCraftingGridRecipe() {
		return this.recipe;
	}

	@Override
	public boolean matches(Level level, FluidStack liquid, ItemStack stack, Container inventory) {
		return FluidStack.isSameFluidSameComponents(liquid, this.resultFluid) && liquid.getAmount() >= this.resultFluid.getAmount() && this.plan.test(stack) && this.recipe.matches(FakeCraftingInventory.of(inventory), level);
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider lookupProvider) {
		return this.recipe.getResultItem(lookupProvider);
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return FactoryRecipeTypes.FABRICATOR.serializer();
	}

	@Override
	public RecipeType<?> getType() {
		return FactoryRecipeTypes.FABRICATOR.type();
	}

	public static class Serializer implements RecipeSerializer<FabricatorRecipe> {
		@Override
		public MapCodec<FabricatorRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, FabricatorRecipe> streamCodec() {
			return STREAM_CODEC;
		}

		private static FabricatorRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
			ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buffer);
			Ingredient plan = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
			FluidStack molten = FluidStack.STREAM_CODEC.decode(buffer);
			ShapedRecipe shapedRecipe = RecipeSerializer.SHAPED_RECIPE.streamCodec().decode(buffer);

			return new FabricatorRecipe(id, plan, molten, shapedRecipe);
		}

		private static void toNetwork(RegistryFriendlyByteBuf buffer, FabricatorRecipe recipe) {
			ResourceLocation.STREAM_CODEC.encode(buffer, recipe.id);
			Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.plan);
			FluidStack.STREAM_CODEC.encode(buffer, recipe.resultFluid);
			RecipeSerializer.SHAPED_RECIPE.streamCodec().encode(buffer, recipe.recipe);
		}
	}
}
