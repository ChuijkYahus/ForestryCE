package forestry.apiculture.bees.genetics;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import net.minecraft.resources.ResourceLocation;

import forestry.Forestry;
import forestry.api.apiculture.IFlowerType;
import forestry.api.apiculture.genetics.IBeeEffect;
import forestry.api.apiculture.genetics.IBeeSpecies;
import forestry.api.apiculture.genetics.IBeeSpeciesType;
import forestry.apiculture.bees.BeeSpecies;
import forestry.core.engine.genetics.GeneticsReloadHandler;
import forestry.core.engine.genetics.SpeciesType;
import forestry.core.platform.util.SpeciesUtil;

/**
 * Rebuilds apiculture's runtime genetics state from loaded data. Split out of
 * {@link GeneticsReloadHandler} so the base artifact does not name bee types.
 * <p>
 * {@link #rebuildSpecies} projects the datapack-loaded (or, on the client, sync-packet-delivered) bee species
 * definitions into the live {@code BeeSpeciesType}: fires on server datapack (re)load (via {@code BeeSpeciesManager},
 * itself driven by {@code AddReloadListenerEvent}) and on client sync (via {@code BeeSpeciesSyncPacket}).
 * <p>
 * <b>Ordering matters:</b> species must be rebuilt before {@link GeneticsReloadHandler#rebuildMutations}, which
 * resolves its species by id against the live map and then indexes the results by object identity.
 */
public final class ApicultureReloadHandler {
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
	 * Installs the effective bee-effect map into the live bee species type: the code-registered base overlaid by the
	 * datapack-loaded (or sync-delivered) effects, datapack winning on id. Mirrors {@link #rebuildFlowerTypes}.
	 */
	public static void rebuildBeeEffects(Map<ResourceLocation, IBeeEffect> dataDefinitions) {
		BeeSpeciesType type = (BeeSpeciesType) SpeciesUtil.BEE_TYPE.get();
		Map<ResourceLocation, IBeeEffect> effective = new LinkedHashMap<>(type.getCodeBeeEffects());
		effective.putAll(dataDefinitions);
		type.setBeeEffects(ImmutableMap.copyOf(effective));
	}

	private ApicultureReloadHandler() {
	}
}
