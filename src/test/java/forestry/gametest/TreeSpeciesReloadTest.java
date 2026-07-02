package forestry.gametest;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.arboriculture.ForestryTreeSpecies;
import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.arboriculture.genetics.ITreeSpeciesType;
import forestry.api.core.HumidityType;
import forestry.api.core.TemperatureType;
import forestry.api.genetics.alleles.ForestryAlleles;
import forestry.api.genetics.alleles.TreeChromosomes;
import forestry.arboriculture.genetics.TreeSpeciesDefinition;
import forestry.core.genetics.GeneticsReloadHandler;
import forestry.core.genetics.SpeciesType;
import forestry.core.utils.SpeciesUtil;

/**
 * Note: {@code TreeSpeciesProvider.buildDefinitions()} (Task 8) doesn't exist yet, so this test builds a single
 * definition inline from the live oak species (mirroring {@code TreeSpeciesProjectorTest}). Task 8 will expand this
 * to the full round-trip over every code-registered tree species.
 * <p>
 * {@code rebuildRepopulatesSpecies} mutates the live {@code TREE_TYPE}'s species map down to a single (oak-only)
 * entry and restores it in a {@code finally} block (mirroring {@code SpeciesFallbackTest}), so other GameTests
 * running later in the same server session - notably {@code MutationRecipeTest}'s tree mutation assertions - still
 * see the full built-in tree species set.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class TreeSpeciesReloadTest {
	@GameTest(template = "empty")
	@SuppressWarnings("unchecked")
	public static void rebuildRepopulatesSpecies(GameTestHelper helper) {
		ITreeSpeciesType type = SpeciesUtil.TREE_TYPE.get();
		ITreeSpecies oak = type.getSpecies(ForestryTreeSpecies.OAK);

		// Snapshot the live, full species map before mutating it, so it can be restored afterwards.
		ImmutableMap<ResourceLocation, ITreeSpecies> snapshot = ImmutableMap.copyOf(
			type.getAllSpeciesIds().stream().collect(java.util.stream.Collectors.toMap(id -> id, type::getSpecies))
		);

		TreeSpeciesDefinition def = new TreeSpeciesDefinition(
			oak.getGenusName(),
			oak.getSpeciesName(),
			oak.isDominant(),
			false,
			false,
			0,
			oak.getAuthority(),
			oak.getEscritoireColor(),
			TemperatureType.NORMAL,
			HumidityType.NORMAL,
			oak.getRarity(),
			Map.of(TreeChromosomes.HEIGHT.id(), ForestryAlleles.HEIGHT_AVERAGE)
		);

		Map<ResourceLocation, TreeSpeciesDefinition> defs = Map.of(ForestryTreeSpecies.OAK, def);

		try {
			GeneticsReloadHandler.rebuildTreeSpecies(defs);

			if (type.getSpeciesSafe(ForestryTreeSpecies.OAK) == null) {
				helper.fail("Expected rebuildTreeSpecies to repopulate oak from the projected definitions map");
				return;
			}
		} finally {
			// Restore the live state so later tests in this same server session (e.g. MutationRecipeTest's tree
			// mutation assertions) still see the full built-in tree species set, and re-pair the mutation index with
			// the restored species (rebuildMutations rebuilds all species types, mirroring SpeciesFallbackTest).
			((SpeciesType<ITreeSpecies, ?>) type).setSpecies(snapshot);
			GeneticsReloadHandler.rebuildMutations(helper.getLevel().getServer().getRecipeManager());
		}

		helper.succeed();
	}
}
