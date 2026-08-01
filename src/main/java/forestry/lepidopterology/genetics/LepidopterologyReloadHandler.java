package forestry.lepidopterology.genetics;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import forestry.Forestry;
import forestry.api.lepidopterology.genetics.IButterflySpecies;
import forestry.api.lepidopterology.genetics.IButterflySpeciesType;
import forestry.core.genetics.GeneticsReloadHandler;
import forestry.core.genetics.SpeciesType;
import forestry.core.utils.SpeciesUtil;
import forestry.lepidopterology.ButterflySpecies;
import forestry.lepidopterology.entities.EntityButterfly;

/**
 * Rebuilds lepidopterology's runtime genetics state from loaded data. Split out of
 * {@link GeneticsReloadHandler} so the base artifact does not name butterfly types.
 * <p>
 * <b>Ordering matters:</b> species must be rebuilt before {@link GeneticsReloadHandler#rebuildMutations}, which
 * resolves its species by id against the live map and then indexes the results by object identity.
 */
public final class LepidopterologyReloadHandler {
	/**
	 * Projects each butterfly definition into a {@link ButterflySpecies} (fail-soft: a bad definition is logged and
	 * dropped by {@link ButterflySpeciesProjector#project}) and swaps the resulting map into the live butterfly
	 * species type.
	 */
	@SuppressWarnings("unchecked")
	public static void rebuildButterflySpecies(Map<ResourceLocation, ButterflySpeciesDefinition> defs) {
		IButterflySpeciesType type = SpeciesUtil.BUTTERFLY_TYPE.get();
		ImmutableMap.Builder<ResourceLocation, IButterflySpecies> builder = ImmutableMap.builderWithExpectedSize(defs.size());
		for (Map.Entry<ResourceLocation, ButterflySpeciesDefinition> entry : defs.entrySet()) {
			ResourceLocation id = entry.getKey();
			ButterflySpecies species = ButterflySpeciesProjector.project(type, id, entry.getValue());
			if (species != null) {
				builder.put(id, species);
			}
		}
		ImmutableMap<ResourceLocation, IButterflySpecies> allSpecies = builder.build();
		((SpeciesType<IButterflySpecies, ?>) type).setSpecies(allSpecies);
		Forestry.LOGGER.info("Loaded {} butterfly species", allSpecies.size());

		// Any already-loaded EntityButterfly caches its resolved individual/species (see Individual's species
		// field); refresh those now so they pick up the fresh instances just swapped in above, otherwise a butterfly
		// that mates after this reload would look up mutations by an identity the (identity-keyed) MutationManager
		// no longer recognizes. No-op with a null server (e.g. the initial WorldLoader.load, before any world/entity
		// exists).
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server != null) {
			for (ServerLevel level : server.getAllLevels()) {
				for (EntityButterfly entity : level.getEntities(EntityTypeTest.forClass(EntityButterfly.class), e -> true)) {
					entity.refreshSpeciesFromReload();
				}
			}
		}
	}

	private LepidopterologyReloadHandler() {
	}
}
