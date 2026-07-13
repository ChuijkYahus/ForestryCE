package forestry.gametest;

import java.util.List;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.IForestryApi;
import forestry.api.genetics.ForestryTaxa;
import forestry.api.genetics.IGeneticManager;
import forestry.apiculture.genetics.TaxonManager;
import forestry.core.genetics.GeneticsReloadHandler;
import forestry.core.genetics.TaxonDefinition;

/**
 * Behavioral oracle for datapack taxa: proves that datapack taxa merge onto the built-in taxonomy with fixpoint parent
 * resolution (a child listed before its parent still resolves), that a taxon whose parent never resolves is skipped
 * rather than crashing, that built-in taxa survive the merge, and that an empty reload reverts to exactly the built-in
 * taxonomy.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class TaxonSystemTest {
	@GameTest(template = "empty")
	public static void datapackTaxaMergeWithFixpointAndSkip(GameTestHelper helper) {
		IGeneticManager gm = IForestryApi.INSTANCE.getGeneticManager();
		var original = TaxonManager.INSTANCE.getDefinitions().values();
		try {
			// child listed before its (datapack) parent — the fixpoint must still resolve both. orphan's parent never resolves.
			TaxonDefinition parent = new TaxonDefinition(ForestryTaxa.DOMAIN_EUKARYOTA, "gametest_kingdom");
			TaxonDefinition child = new TaxonDefinition("gametest_kingdom", "gametest_phylum");
			TaxonDefinition orphan = new TaxonDefinition("no_such_parent", "gametest_orphan");
			GeneticsReloadHandler.rebuildTaxa(List.of(child, parent, orphan));

			if (gm.getTaxonSafe("gametest_kingdom") == null || gm.getTaxonSafe("gametest_phylum") == null) {
				helper.fail("fixpoint parent resolution failed to register a child listed before its parent");
				return;
			}
			if (gm.getTaxonSafe("gametest_orphan") != null) {
				helper.fail("a taxon whose parent never resolves should be skipped, not registered");
				return;
			}
			if (gm.getTaxonSafe(ForestryTaxa.DOMAIN_EUKARYOTA) == null) {
				helper.fail("a built-in taxon was dropped by the datapack merge");
				return;
			}
			GeneticsReloadHandler.rebuildTaxa(List.of());
			if (gm.getTaxonSafe("gametest_kingdom") != null) {
				helper.fail("an empty reload should revert to exactly the built-in taxonomy");
				return;
			}
		} finally {
			GeneticsReloadHandler.rebuildTaxa(original);
		}
		helper.succeed();
	}
}
