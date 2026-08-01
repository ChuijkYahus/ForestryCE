package forestry.arboriculture.genetics;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import net.minecraft.resources.ResourceLocation;

import forestry.Forestry;
import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.arboriculture.genetics.ITreeSpeciesType;
import forestry.arboriculture.TreeSpecies;
import forestry.core.genetics.GeneticsReloadHandler;
import forestry.core.genetics.SpeciesType;
import forestry.core.utils.SpeciesUtil;

/**
 * Rebuilds arboriculture's runtime genetics state from loaded data. Split out of
 * {@link GeneticsReloadHandler} so the base artifact does not name tree types.
 * <p>
 * <b>Ordering matters:</b> species must be rebuilt before {@link GeneticsReloadHandler#rebuildMutations}, which
 * resolves its species by id against the live map and then indexes the results by object identity.
 */
public final class ArboricultureReloadHandler {
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

	private ArboricultureReloadHandler() {
	}
}
