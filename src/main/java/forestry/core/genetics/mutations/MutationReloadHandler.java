package forestry.core.genetics.mutations;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import forestry.Forestry;
import forestry.api.IForestryApi;
import forestry.api.genetics.IMutation;
import forestry.api.genetics.ISpecies;
import forestry.api.genetics.ISpeciesType;
import forestry.core.features.GeneticsRecipeTypes;
import forestry.core.genetics.MutationManager;
import forestry.core.genetics.SpeciesType;
import forestry.modules.features.FeatureRecipeType;

/**
 * Rebuilds each species type's {@link MutationManager} from the loaded {@link MutationRecipe}s. Fires on server
 * datapack (re)load (via {@code AddReloadListenerEvent}) and on client recipe sync (via {@code RecipesUpdatedEvent}),
 * so the runtime mutation index always mirrors the active recipes for both gameplay and JEI/analyzer display.
 * <p>
 * Safe to run with zero mutation recipes: each species type simply gets an empty index.
 */
public final class MutationReloadHandler {
	public static void rebuild(RecipeManager recipeManager) {
		MutationConditionTypes.registerBuiltins(); // idempotent safety net
		for (ISpeciesType<?, ?> type : IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypes()) {
			rebuildOne(type, recipeManager);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static <S extends ISpecies<?>> void rebuildOne(ISpeciesType<S, ?> type, RecipeManager rm) {
		FeatureRecipeType<MutationRecipe> featureType = GeneticsRecipeTypes.forType(type.id());
		if (featureType == null) {
			return; // species type without a mutation recipe type (e.g. third-party)
		}
		// build id -> species lookup once
		Map<ResourceLocation, S> lookup = new HashMap<>();
		for (S species : type.getAllSpecies()) {
			lookup.put(species.id(), species);
		}
		ImmutableList.Builder<IMutation<S>> builder = ImmutableList.builder();
		for (RecipeHolder<MutationRecipe> holder : rm.getAllRecipesFor(featureType.type())) {
			Mutation<S> mutation = holder.value().toMutation(type, lookup::get);
			if (mutation != null) {
				builder.add(mutation);
			} else {
				Forestry.LOGGER.warn("Skipping mutation recipe {} (unknown or mismatched species)", holder.id());
			}
		}
		ImmutableList<IMutation<S>> mutations = builder.build();
		((SpeciesType<S, ?>) type).setMutations(new MutationManager<>(mutations));
		Forestry.LOGGER.debug("Loaded {} {} mutation recipes", mutations.size(), type.id());
	}

	private MutationReloadHandler() {
	}
}
