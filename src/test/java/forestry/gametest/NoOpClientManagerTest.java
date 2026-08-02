package forestry.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.client.arboriculture.ILeafSprite;
import forestry.api.genetics.ILifeStage;
import forestry.apiimpl.client.fake.FakeBeeClientManager;
import forestry.apiimpl.client.fake.FakeButterflyClientManager;
import forestry.apiimpl.client.fake.FakeClientHelper;
import forestry.apiimpl.client.fake.FakeTreeClientManager;

/**
 * Constructs the client no-ops on a dedicated server. That is the point of the test as much as the
 * assertions are: if one of these ever reaches for a net.minecraft.client type, this fails with
 * NoClassDefFoundError rather than crashing a player's client after the jars are split.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class NoOpClientManagerTest {
	@GameTest(template = "empty")
	public static void noOpClientManagersReportNotLoaded(GameTestHelper helper) {
		if (FakeBeeClientManager.INSTANCE.isLoaded()
				|| FakeTreeClientManager.INSTANCE.isLoaded()
				|| FakeButterflyClientManager.INSTANCE.isLoaded()) {
			helper.fail("A no-op client manager reported itself loaded");
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void noOpClientManagersReturnNonNull(GameTestHelper helper) {
		ILifeStage stage = forestry.api.apiculture.genetics.BeeLifeStage.DRONE;

		if (FakeBeeClientManager.INSTANCE.getDefaultModelLocation(stage) == null
				|| FakeBeeClientManager.INSTANCE.getModelLocation(stage, ForestryConstants.forestry("forest")) == null
				|| !FakeBeeClientManager.INSTANCE.getAllModelLocations(stage).isEmpty()) {
			helper.fail("No-op bee client manager broke its contract");
			return;
		}
		if (FakeTreeClientManager.INSTANCE.getLeafSprite(null) == null
				|| FakeTreeClientManager.INSTANCE.getTint(null) == null
				|| FakeTreeClientManager.INSTANCE.getDefaultSaplingModels() == null
				|| !FakeTreeClientManager.INSTANCE.getAllLeafSprites().isEmpty()
				|| !FakeTreeClientManager.INSTANCE.getAllSaplingModels().isEmpty()) {
			helper.fail("No-op tree client manager broke its contract");
			return;
		}
		if (FakeButterflyClientManager.INSTANCE.getDefaultTextures() == null
				|| !FakeButterflyClientManager.INSTANCE.getAllTextures().isEmpty()) {
			helper.fail("No-op butterfly client manager broke its contract");
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void noOpClientHelperReturnsNonNull(GameTestHelper helper) {
		if (FakeClientHelper.INSTANCE.createNoneTint() == null
				|| FakeClientHelper.INSTANCE.createBiomeTint() == null
				|| FakeClientHelper.INSTANCE.createLeafSprite(ForestryConstants.forestry("leaves")) == null) {
			helper.fail("No-op client helper returned null");
			return;
		}
		if (ILeafSprite.MISSING.get(false, false) == null || ILeafSprite.MISSING.getParticle() == null) {
			helper.fail("ILeafSprite.MISSING returned null");
			return;
		}
		helper.succeed();
	}
}
