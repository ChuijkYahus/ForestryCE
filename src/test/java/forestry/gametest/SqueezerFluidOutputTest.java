package forestry.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.recipes.ISqueezerRecipe;
import forestry.core.config.Constants;
import forestry.core.fluids.ForestryFluids;
import forestry.core.utils.RecipeUtils;
import forestry.factory.features.FactoryRecipeTypes;
import forestry.factory.recipes.SqueezerRecipe;

/**
 * Behavioral oracle for "squeezer fluid output as a {@link SizedFluidIngredient}". Proves that a plain fluid output
 * serialises to the flat {@code {"fluid", "amount"}} form and round-trips (JSON and network), that a fluid tag resolves
 * at runtime to whatever fluid a loaded mod fills (an unfulfilled tag resolving to {@link FluidStack#EMPTY}), and that
 * the built-in squeezer recipes still resolve to their concrete fluid output through the migrated code path.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class SqueezerFluidOutputTest {
	/** A plain fluid output encodes to the flat fluid-ingredient JSON ({@code fluid}/{@code amount}) and round-trips. */
	@GameTest(template = "empty")
	public static void plainOutputRoundTripsThroughFlatJson(GameTestHelper helper) {
		RegistryOps<JsonElement> ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
		FluidStack stack = ForestryFluids.HONEY.getFluid(500);

		JsonElement json = SizedFluidIngredient.FLAT_CODEC.encodeStart(ops, SizedFluidIngredient.of(stack)).getOrThrow();
		JsonObject obj = json.getAsJsonObject();
		if (!obj.has("fluid") || !obj.has("amount")) {
			helper.fail("A plain fluid output should encode with \"fluid\" and \"amount\" keys: " + json);
			return;
		}

		SizedFluidIngredient decoded = SizedFluidIngredient.FLAT_CODEC.parse(ops, json).getOrThrow();
		if (!sameFluid(first(decoded), stack)) {
			helper.fail("Flat JSON round-trip changed the output: " + first(decoded));
			return;
		}

		helper.succeed();
	}

	/** The fluid-ingredient stream codec must round-trip the output unchanged. */
	@GameTest(template = "empty")
	public static void streamRoundTrip(GameTestHelper helper) {
		FluidStack stack = ForestryFluids.HONEY.getFluid(800);
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());

		SizedFluidIngredient.STREAM_CODEC.encode(buf, SizedFluidIngredient.of(stack));
		SizedFluidIngredient decoded = SizedFluidIngredient.STREAM_CODEC.decode(buf);
		if (!sameFluid(first(decoded), stack)) {
			helper.fail("Stream codec round-trip changed the output: " + first(decoded));
			return;
		}

		helper.succeed();
	}

	/**
	 * A tag output resolves to whatever fluid the tag holds (the mod-agnostic case the ingredient enables), and an
	 * unfulfilled tag resolves to {@link FluidStack#EMPTY} so {@code TileSqueezer} can refuse the recipe.
	 */
	@GameTest(template = "empty")
	public static void tagOutputResolvesAndEmptyTagYieldsEmpty(GameTestHelper helper) {
		// A populated vanilla tag: water resolves to a concrete fluid, amount applied.
		SqueezerRecipe waterRecipe = recipeWithOutput(new SizedFluidIngredient(
			net.neoforged.neoforge.fluids.crafting.FluidIngredient.tag(FluidTags.WATER), 500));
		FluidStack resolved = waterRecipe.getFluidOutput();
		if (resolved.isEmpty() || resolved.getAmount() != 500) {
			helper.fail("A populated fluid tag should resolve to a concrete fluid x500, got: " + resolved);
			return;
		}

		// A tag no fluid fills resolves to empty (rather than throwing or fabricating a fluid).
		TagKey<Fluid> missing = TagKey.create(Registries.FLUID, ForestryConstants.forestry("gametest_no_such_fluid"));
		SqueezerRecipe missingRecipe = recipeWithOutput(new SizedFluidIngredient(
			net.neoforged.neoforge.fluids.crafting.FluidIngredient.tag(missing), 500));
		if (!missingRecipe.getFluidOutput().isEmpty()) {
			helper.fail("An unfulfilled fluid tag should resolve to EMPTY, got: " + missingRecipe.getFluidOutput());
			return;
		}

		helper.succeed();
	}

	/** Every built-in squeezer recipe must resolve to a non-empty fluid, and honey_block must resolve to honey x800. */
	@GameTest(template = "empty")
	public static void squeezerRecipesResolveOutput(GameTestHelper helper) {
		RecipeManager manager = helper.getLevel().getRecipeManager();

		long count = RecipeUtils.getRecipes(manager, FactoryRecipeTypes.SQUEEZER).count();
		if (count == 0) {
			helper.fail("No squeezer recipes loaded");
			return;
		}
		boolean anyEmpty = RecipeUtils.getRecipes(manager, FactoryRecipeTypes.SQUEEZER)
			.anyMatch(recipe -> recipe.getFluidOutput().isEmpty());
		if (anyEmpty) {
			helper.fail("A built-in squeezer recipe resolved to an empty fluid output");
			return;
		}

		ISqueezerRecipe honeyBlock = RecipeUtils.getRecipes(manager, FactoryRecipeTypes.SQUEEZER)
			.filter(recipe -> recipe.getId().equals(ForestryConstants.forestry("squeezer/honey_block")))
			.findFirst()
			.orElse(null);
		if (honeyBlock == null) {
			helper.fail("Missing built-in squeezer/honey_block recipe");
			return;
		}
		FluidStack expected = ForestryFluids.HONEY.getFluid(Constants.FLUID_PER_HONEY_DROP * 8);
		if (!sameFluid(honeyBlock.getFluidOutput(), expected)) {
			helper.fail("honey_block output changed: " + honeyBlock.getFluidOutput() + ", expected " + expected);
			return;
		}

		helper.succeed();
	}

	private static SqueezerRecipe recipeWithOutput(SizedFluidIngredient output) {
		return new SqueezerRecipe(
			ForestryConstants.forestry("gametest/dynamic_output"), 20,
			java.util.List.of(net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.STONE)),
			output, net.minecraft.world.item.ItemStack.EMPTY, 0.0f);
	}

	private static FluidStack first(SizedFluidIngredient ingredient) {
		FluidStack[] fluids = ingredient.getFluids();
		return fluids.length == 0 ? FluidStack.EMPTY : fluids[0];
	}

	private static boolean sameFluid(FluidStack a, FluidStack b) {
		return FluidStack.isSameFluidSameComponents(a, b) && a.getAmount() == b.getAmount();
	}
}
