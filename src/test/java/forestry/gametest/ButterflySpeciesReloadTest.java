package forestry.gametest;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.genetics.alleles.ButterflyChromosomes;
import forestry.api.genetics.alleles.ForestryAlleles;
import forestry.api.lepidopterology.ForestryButterflySpecies;
import forestry.api.lepidopterology.genetics.IButterflySpecies;
import forestry.api.lepidopterology.genetics.IButterflySpeciesType;
import forestry.core.genetics.GeneticsReloadHandler;
import forestry.core.genetics.SpeciesType;
import forestry.core.utils.SpeciesUtil;
import forestry.lepidopterology.genetics.ButterflySpeciesDefinition;

/**
 * Note: {@code ButterflySpeciesProvider.buildDefinitions()} (Task 8) doesn't exist yet, so {@code rebuildRepopulates}
 * builds a single definition inline from the live Monarch species (mirroring {@code ButterflySpeciesProjectorTest}).
 * Task 8 will expand this to the full round-trip over every code-registered butterfly species.
 * <p>
 * Mutates the live {@code BUTTERFLY_TYPE}'s species map and restores it (plus rebuilds mutations) in a
 * {@code finally} block (mirroring {@code TreeSpeciesReloadTest}), so other GameTests running later in the same
 * server session - notably {@code MutationRecipeTest}'s butterfly mutation assertions - still see the full built-in
 * butterfly species set.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class ButterflySpeciesReloadTest {
	@GameTest(template = "empty")
	@SuppressWarnings("unchecked")
	public static void rebuildRepopulates(GameTestHelper helper) {
		IButterflySpeciesType type = SpeciesUtil.BUTTERFLY_TYPE.get();
		IButterflySpecies monarch = type.getSpecies(ForestryButterflySpecies.MONARCH);

		// Snapshot the live, full species map before mutating it, so it can be restored afterwards.
		ImmutableMap<ResourceLocation, IButterflySpecies> snapshot = ImmutableMap.copyOf(
			type.getAllSpeciesIds().stream().collect(java.util.stream.Collectors.toMap(id -> id, type::getSpecies))
		);

		ButterflySpeciesDefinition def = new ButterflySpeciesDefinition(
			monarch.getGenusName(),
			monarch.getSpeciesName(),
			monarch.isDominant(),
			false,
			monarch.isSecret(),
			0,
			monarch.getAuthority(),
			-1,
			monarch.getTemperature(),
			monarch.getHumidity(),
			monarch.isNocturnal(),
			monarch.isMoth(),
			monarch.getRarity(),
			monarch.getFlightDistance(),
			monarch.getSerumColor(),
			Optional.ofNullable(monarch.getSpawnBiomes()),
			monarch.getButterflyLoot(),
			monarch.getCaterpillarProducts(),
			Map.of(ButterflyChromosomes.SIZE.id(), ForestryAlleles.SIZE_AVERAGE)
		);

		Map<ResourceLocation, ButterflySpeciesDefinition> defs = Map.of(ForestryButterflySpecies.MONARCH, def);

		try {
			GeneticsReloadHandler.rebuildButterflySpecies(defs);

			if (type.getAllSpeciesIds().isEmpty()) {
				helper.fail("Expected rebuildButterflySpecies to repopulate the species map from the projected definitions");
				return;
			}
			if (type.getSpeciesSafe(ForestryButterflySpecies.MONARCH) == null) {
				helper.fail("Expected rebuildButterflySpecies to repopulate Monarch from the projected definitions map");
				return;
			}
		} finally {
			// Restore the live state so later tests in this same server session (e.g. MutationRecipeTest's
			// butterfly mutation assertions) still see the full built-in butterfly species set, and re-pair the
			// mutation index with the restored species (rebuildMutations rebuilds all species types, mirroring
			// TreeSpeciesReloadTest).
			((SpeciesType<IButterflySpecies, ?>) type).setSpecies(snapshot);
			GeneticsReloadHandler.rebuildMutations(helper.getLevel().getServer().getRecipeManager());
		}

		helper.succeed();
	}
}
