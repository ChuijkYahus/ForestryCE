package forestry.gametest;

import java.util.List;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.apiculture.genetics.IBeeSpecies;
import forestry.core.utils.SpeciesUtil;

/**
 * Behavioral oracle for the reloadable {@code allSpecies} map on {@link forestry.core.genetics.SpeciesType}: proves
 * that {@code getAllSpecies()}/{@code getSpeciesCount()} on the live bee type are safe to call without throwing (the
 * old {@code checkSpecies()} guard used to throw {@code IllegalStateException} before registration completed) and
 * stay consistent with each other. Real reload behavior (swapping the map at runtime via {@code setSpecies}) is
 * covered by Task 8.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class SpeciesReloadTest {
	@GameTest(template = "empty")
	public static void allSpeciesNeverThrowsAndCountMatches(GameTestHelper helper) {
		List<IBeeSpecies> allSpecies = SpeciesUtil.BEE_TYPE.get().getAllSpecies();
		if (allSpecies == null) {
			helper.fail("getAllSpecies() returned null for the bee species type");
			return;
		}
		int count = SpeciesUtil.BEE_TYPE.get().getSpeciesCount();
		if (count != allSpecies.size()) {
			helper.fail("getSpeciesCount() (" + count + ") did not match getAllSpecies().size() (" + allSpecies.size() + ")");
			return;
		}

		helper.succeed();
	}
}
