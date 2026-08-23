package forestry.gametest;

import java.util.List;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.mail.features.MailItems;
import forestry.mail.letters.EnumStampDefinition;
import forestry.mail.letters.PostageSelector;
import forestry.mail.letters.PostageUtil;

/**
 * Behavior lock for the change-making solver lifted out of {@code TradeStation}. The three passes are
 * preserved from the original, greedy and admittedly not optimal, so these assert what it does rather
 * than what an optimal solver would do.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class PostageSelectorTest {
	@GameTest(template = "empty")
	public static void heldDenominationsSortCheapestFirstAndIgnoreNonStamps(GameTestHelper helper) {
		List<PostageSelector.Denomination> held = PostageSelector.heldDenominations(List.of(
			MailItems.STAMPS.stack(EnumStampDefinition.P_20, 2),
			new ItemStack(Items.PAPER, 64),
			MailItems.STAMPS.stack(EnumStampDefinition.P_1, 3),
			MailItems.STAMPS.stack(EnumStampDefinition.P_1, 4),
			ItemStack.EMPTY));

		assertEquals(helper, held.size(), 2, "denomination count");
		assertEquals(helper, held.get(0).postage(), 1, "cheapest denomination postage");
		// Two stacks of the same stamp merge into one denomination
		assertEquals(helper, held.get(0).available(), 7, "cheapest denomination count");
		assertEquals(helper, held.get(1).postage(), 20, "dearest denomination postage");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void virtualDenominationsAreForestryStampsOnly(GameTestHelper helper) {
		List<PostageSelector.Denomination> virtual = PostageSelector.virtualDenominations();

		assertEquals(helper, virtual.size(), EnumStampDefinition.VALUES.length, "virtual denomination count");
		for (PostageSelector.Denomination denomination : virtual) {
			helper.assertTrue(isForestryStamp(denomination), "A virtual trade station offered a stamp Forestry does not ship: " + denomination.item());
			assertEquals(helper, denomination.available(), 99, "virtual supply of " + denomination.item());
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void exactChangeUsesTheLargestStampsThatFit(GameTestHelper helper) {
		// 27 from 20 + 5 + 1 + 1, taken largest first
		List<ItemStack> selected = PostageSelector.select(PostageSelector.heldDenominations(List.of(
			MailItems.STAMPS.stack(EnumStampDefinition.P_1, 9),
			MailItems.STAMPS.stack(EnumStampDefinition.P_5, 9),
			MailItems.STAMPS.stack(EnumStampDefinition.P_20, 9))), 27);

		assertEquals(helper, PostageUtil.sumPostage(selected), 27, "selected postage");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void noExactChangeOverpaysWithTheSmallestCoveringStamp(GameTestHelper helper) {
		// 3 required, only 20n stamps held, so one 20n overpays rather than failing
		List<ItemStack> selected = PostageSelector.select(PostageSelector.heldDenominations(List.of(
			MailItems.STAMPS.stack(EnumStampDefinition.P_20, 2),
			MailItems.STAMPS.stack(EnumStampDefinition.P_100, 2))), 3);

		assertEquals(helper, selected.size(), 1, "selected stack count");
		helper.assertTrue(selected.get(0).is(MailItems.STAMPS.item(EnumStampDefinition.P_20)), "Overpayment did not use the smallest covering stamp");
		assertEquals(helper, selected.get(0).getCount(), 1, "selected stamp count");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void noCoveringStampCombinesSmallerOnes(GameTestHelper helper) {
		// 7 required, only 2n stamps held, so four of them combine and overpay by one
		List<ItemStack> selected = PostageSelector.select(PostageSelector.heldDenominations(List.of(
			MailItems.STAMPS.stack(EnumStampDefinition.P_2, 9))), 7);

		helper.assertTrue(PostageUtil.sumPostage(selected) >= 7, "Combined stamps did not cover the required postage");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void anEmptyStationSelectsNothing(GameTestHelper helper) {
		helper.assertTrue(PostageSelector.select(PostageSelector.heldDenominations(List.of()), 5).isEmpty(),
			"A station holding no stamps still selected postage");
		helper.succeed();
	}

	private static boolean isForestryStamp(PostageSelector.Denomination denomination) {
		for (EnumStampDefinition stamp : EnumStampDefinition.VALUES) {
			if (MailItems.STAMPS.item(stamp) == denomination.item()) {
				return true;
			}
		}
		return false;
	}

	private static void assertEquals(GameTestHelper helper, int actual, int expected, String what) {
		helper.assertTrue(actual == expected, what + " was " + actual + " instead of " + expected);
	}
}
