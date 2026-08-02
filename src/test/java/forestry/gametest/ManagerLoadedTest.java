package forestry.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.IForestryApi;

/**
 * Asserts that the managers installed in a full install report themselves loaded. The no-op
 * implementations report false; see NoOpManagerTest for the other side of the contract.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class ManagerLoadedTest {
	@GameTest(template = "empty")
	public static void serverManagersAreLoaded(GameTestHelper helper) {
		if (!IForestryApi.INSTANCE.getFarmingManager().isLoaded()) {
			helper.fail("IFarmingManager reports not loaded in a full install");
			return;
		}
		if (!IForestryApi.INSTANCE.getHiveManager().isLoaded()) {
			helper.fail("IHiveManager reports not loaded in a full install");
			return;
		}
		if (!IForestryApi.INSTANCE.getTreeManager().isLoaded()) {
			helper.fail("ITreeManager reports not loaded in a full install");
			return;
		}
		helper.succeed();
	}
}
