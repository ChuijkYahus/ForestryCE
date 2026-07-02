package forestry.core.genetics;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import forestry.Forestry;
import forestry.api.IForestryApi;
import forestry.api.apiculture.genetics.IBeeSpecies;
import forestry.api.apiculture.genetics.IBeeSpeciesType;
import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.arboriculture.genetics.ITreeSpeciesType;
import forestry.api.genetics.IMutation;
import forestry.api.genetics.ISpecies;
import forestry.api.genetics.ISpeciesType;
import forestry.apiculture.BeeSpecies;
import forestry.apiculture.genetics.BeeSpeciesDefinition;
import forestry.apiculture.genetics.BeeSpeciesProjector;
import forestry.arboriculture.TreeSpecies;
import forestry.arboriculture.genetics.TreeSpeciesDefinition;
import forestry.arboriculture.genetics.TreeSpeciesProjector;
import forestry.core.features.GeneticsRecipeTypes;
import forestry.core.genetics.mutations.Mutation;
import forestry.core.genetics.mutations.MutationConditionTypes;
import forestry.core.genetics.mutations.MutationRecipe;
import forestry.core.utils.SpeciesUtil;
import forestry.modules.features.FeatureRecipeType;

/**
 * Rebuilds runtime genetics state from loaded data.
 * <p>
 * {@link #rebuildSpecies} projects the datapack-loaded (or, on the client, sync-packet-delivered) bee species
 * definitions into the live {@code BeeSpeciesType}: fires on server datapack (re)load (via {@code BeeSpeciesManager},
 * itself driven by {@code AddReloadListenerEvent}) and on client sync (via {@code BeeSpeciesSyncPacket}).
 * <p>
 * {@link #rebuildMutations} rebuilds each species type's {@link MutationManager} from the loaded
 * {@link MutationRecipe}s. Fires on server datapack (re)load (via {@code AddReloadListenerEvent}) and on client
 * recipe sync (via {@code RecipesUpdatedEvent}), so the runtime mutation index always mirrors the active recipes for
 * both gameplay and JEI/analyzer display.
 * <p>
 * <b>Ordering matters:</b> mutation recipes resolve their species by looking them up (by id) in the live
 * {@code allSpecies} map at rebuild time, and the resulting {@link IMutation}s are indexed by species <em>object
 * identity</em> ({@link MutationManager} uses an {@code IdentityHashMap}). Species must therefore always be rebuilt
 * before mutations - every call site in this codebase does {@code rebuildSpecies} then {@code rebuildMutations}, in
 * that order, on the same thread/executor.
 * <p>
 * Safe to run with zero definitions/recipes: species/mutation indexes are simply left/set empty.
 */
public final class GeneticsReloadHandler {
	/**
	 * Projects each definition into a {@link BeeSpecies} (fail-soft: a bad definition is logged and dropped by
	 * {@link BeeSpeciesProjector#project}) and swaps the resulting map into the live bee species type.
	 */
	@SuppressWarnings("unchecked")
	public static void rebuildSpecies(Map<ResourceLocation, BeeSpeciesDefinition> defs) {
		IBeeSpeciesType type = SpeciesUtil.BEE_TYPE.get();
		ImmutableMap.Builder<ResourceLocation, IBeeSpecies> builder = ImmutableMap.builderWithExpectedSize(defs.size());
		for (Map.Entry<ResourceLocation, BeeSpeciesDefinition> entry : defs.entrySet()) {
			ResourceLocation id = entry.getKey();
			BeeSpecies species = BeeSpeciesProjector.project(type, id, entry.getValue());
			if (species != null) {
				builder.put(id, species);
			}
		}
		ImmutableMap<ResourceLocation, IBeeSpecies> allSpecies = builder.build();
		((SpeciesType<IBeeSpecies, ?>) type).setSpecies(allSpecies);
		Forestry.LOGGER.info("Loaded {} bee species", allSpecies.size());
	}

	/**
	 * Projects each tree definition into a {@link TreeSpecies} (fail-soft: a bad/binding-less definition is logged and
	 * dropped by {@link TreeSpeciesProjector#project}) and swaps the resulting map into the live tree species type.
	 */
	@SuppressWarnings("unchecked")
	public static void rebuildTreeSpecies(Map<ResourceLocation, TreeSpeciesDefinition> defs) {
		ITreeSpeciesType type = SpeciesUtil.TREE_TYPE.get();
		ImmutableMap.Builder<ResourceLocation, ITreeSpecies> builder = ImmutableMap.builderWithExpectedSize(defs.size());
		for (Map.Entry<ResourceLocation, TreeSpeciesDefinition> entry : defs.entrySet()) {
			ResourceLocation id = entry.getKey();
			TreeSpecies species = TreeSpeciesProjector.project(type, id, entry.getValue());
			if (species != null) {
				builder.put(id, species);
			}
		}
		ImmutableMap<ResourceLocation, ITreeSpecies> allSpecies = builder.build();
		((SpeciesType<ITreeSpecies, ?>) type).setSpecies(allSpecies);
		Forestry.LOGGER.info("Loaded {} tree species", allSpecies.size());
	}

	public static void rebuildMutations(RecipeManager recipeManager) {
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

	private GeneticsReloadHandler() {
	}
}
