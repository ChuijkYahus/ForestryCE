package forestry.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.arboriculture.WoodBlockKind;
import forestry.apiimpl.fake.FakeFarmingManager;
import forestry.apiimpl.fake.FakeHiveManager;
import forestry.apiimpl.fake.FakeTreeManager;

/**
 * The no-op managers are unreachable while every module ships in one jar, so this constructs them
 * directly. What it protects is the contract a defensive addon relies on under D7: non-null returns,
 * empty collections and isLoaded() false.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class NoOpManagerTest {
	@GameTest(template = "empty")
	public static void noOpsReportNotLoaded(GameTestHelper helper) {
		if (FakeFarmingManager.INSTANCE.isLoaded() || FakeHiveManager.INSTANCE.isLoaded() || FakeTreeManager.INSTANCE.isLoaded()) {
			helper.fail("A no-op manager reported itself loaded");
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void noOpFarmingManagerDegrades(GameTestHelper helper) {
		ResourceLocation any = ForestryConstants.forestry("wheat");

		if (FakeFarmingManager.INSTANCE.getFarmType(any) != null) {
			helper.fail("No-op farming manager returned a farm type");
			return;
		}
		if (!FakeFarmingManager.INSTANCE.getFarmables(any).isEmpty()) {
			helper.fail("No-op farming manager returned farmables");
			return;
		}
		if (FakeFarmingManager.INSTANCE.getFertilizeValue(new ItemStack(Items.BONE_MEAL)) != 0) {
			helper.fail("No-op farming manager valued a fertilizer");
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void noOpHiveManagerDegrades(GameTestHelper helper) {
		if (!FakeHiveManager.INSTANCE.getHives().isEmpty()
				|| !FakeHiveManager.INSTANCE.getCommonVillageHives().isEmpty()
				|| !FakeHiveManager.INSTANCE.getRareVillageHives().isEmpty()
				|| !FakeHiveManager.INSTANCE.getDrops(ForestryConstants.forestry("forest")).isEmpty()) {
			helper.fail("No-op hive manager returned a non-empty registry");
			return;
		}
		if (FakeHiveManager.INSTANCE.getSwarmingMaterialChance(Items.SLIME_BALL) != 0.0f) {
			helper.fail("No-op hive manager gave an item a swarming chance");
			return;
		}
		// The three create* methods promise non-null. An addon that calls them on a partial install
		// and gets null is the failure this whole phase exists to prevent.
		if (FakeHiveManager.INSTANCE.createBeekeepingLogic(null) == null
				|| FakeHiveManager.INSTANCE.createBeeHousingModifier(null) == null
				|| FakeHiveManager.INSTANCE.createBeeHousingListener(null) == null) {
			helper.fail("No-op hive manager returned null from a non-null factory method");
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void noOpTreeManagerDegrades(GameTestHelper helper) {
		if (FakeTreeManager.INSTANCE.getRefractoryWaxed(Blocks.OAK_PLANKS) != null) {
			helper.fail("No-op tree manager waxed a block");
			return;
		}
		if (!FakeTreeManager.INSTANCE.getRegisteredWoodTypes().isEmpty()) {
			helper.fail("No-op tree manager returned wood types");
			return;
		}
		if (FakeTreeManager.INSTANCE.getCharcoalManager() == null
				|| !FakeTreeManager.INSTANCE.getCharcoalManager().getWalls().isEmpty()) {
			helper.fail("No-op tree manager returned a null or non-empty charcoal manager");
			return;
		}
		// Vanilla resolves an undefined TagKey as empty rather than erroring, so the empty tag the
		// no-op hands back is safe to query and matches nothing.
		TagKey<Block> logs = FakeTreeManager.INSTANCE.getLogBlockTag(null, false);
		if (logs == null || Blocks.OAK_LOG.defaultBlockState().is(logs)) {
			helper.fail("No-op tree manager returned a null or matching log tag");
			return;
		}
		if (!FakeTreeManager.INSTANCE.getStack(null, WoodBlockKind.PLANKS, false).isEmpty()) {
			helper.fail("No-op tree manager returned a non-empty stack");
			return;
		}
		if (!FakeTreeManager.INSTANCE.getBlock(null, WoodBlockKind.PLANKS, false).isAir()) {
			helper.fail("No-op tree manager returned a non-air block state");
			return;
		}
		helper.succeed();
	}
}
