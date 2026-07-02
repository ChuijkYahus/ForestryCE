package forestry.gametest;

import java.util.Map;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.arboriculture.ForestryTreeSpecies;
import forestry.api.arboriculture.genetics.ITreeSpeciesType;
import forestry.api.core.HumidityType;
import forestry.api.core.TemperatureType;
import forestry.api.genetics.IGenome;
import forestry.api.genetics.alleles.ForestryAlleles;
import forestry.api.genetics.alleles.TreeChromosomes;
import forestry.arboriculture.TreeSpecies;
import forestry.arboriculture.genetics.TreeSpeciesDefinition;
import forestry.arboriculture.genetics.TreeSpeciesProjector;
import forestry.core.utils.SpeciesUtil;

@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class TreeSpeciesProjectorTest {
	@GameTest(template = "empty")
	public static void projectsMatchingTreeSpecies(GameTestHelper helper) {
		ITreeSpeciesType type = SpeciesUtil.TREE_TYPE.get();
		// Read the genus/species off the real oak so the definition is faithful.
		var oak = type.getSpecies(ForestryTreeSpecies.OAK);

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
			Map.of(TreeChromosomes.HEIGHT.id(), ForestryAlleles.HEIGHT_LARGE)
		);

		// Project against the real oak id so the code-side bindings are found.
		TreeSpecies projected = TreeSpeciesProjector.project(type, ForestryTreeSpecies.OAK, def);
		if (projected == null) {
			helper.fail("Projection returned null for a valid definition with registered bindings");
			return;
		}
		if (projected.getGenerator() != oak.getGenerator()) {
			helper.fail("Expected the projected species to reuse oak's code-side generator binding");
			return;
		}
		IGenome genome = projected.getDefaultGenome();
		if (genome.getActiveValue(TreeChromosomes.HEIGHT) != ForestryAlleles.HEIGHT_LARGE.value()) {
			helper.fail("Expected default genome HEIGHT override to apply");
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void missingBindingsSkips(GameTestHelper helper) {
		ITreeSpeciesType type = SpeciesUtil.TREE_TYPE.get();
		TreeSpeciesDefinition def = new TreeSpeciesDefinition(
			"Quercus", "phantom", false, false, false, 0, "Sengir", -1,
			TemperatureType.NORMAL, HumidityType.NORMAL, 0.0f, Map.of()
		);
		TreeSpecies projected = TreeSpeciesProjector.project(type, ForestryConstants.forestry("phantom_tree_no_bindings"), def);
		if (projected != null) {
			helper.fail("Expected projection to skip (null) a species id with no registered bindings");
			return;
		}
		helper.succeed();
	}
}
