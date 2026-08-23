package forestry.gametest;

import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.mail.features.MailItems;
import forestry.mail.letters.EnumStampDefinition;
import forestry.mail.postoffice.PostOffice;

/**
 * Conservation and persistence oracle for the post office stamp vault.
 *
 * <p>{@link PostOffice#getAnyStamp} used to mutate the vault without calling {@code setDirty}, and the
 * stamp collector is the one caller that never marks the data dirty afterwards. Withdrawing a stamp
 * and then reloading left it both in the vault and in the collector, which is a duplication path
 * rather than a cosmetic bug. {@link #withdrawingStampsMarksTheVaultDirty} is the pin for it.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class PostOfficeStampVaultTest {
	@GameTest(template = "empty")
	public static void withdrawingStampsMarksTheVaultDirty(GameTestHelper helper) {
		PostOffice office = new PostOffice();
		office.collectPostage(stamps(MailItems.STAMPS.stack(EnumStampDefinition.P_10, 4)));
		office.setDirty(false);

		ItemStack withdrawn = office.getAnyStamp(1);

		assertEquals(helper, withdrawn.getCount(), 1, "withdrawn stamp count");
		helper.assertTrue(office.isDirty(), "getAnyStamp changed the vault without marking it dirty, which loses the withdrawal on reload");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void collectedStampsSurviveASaveRoundTrip(GameTestHelper helper) {
		PostOffice office = new PostOffice();
		office.collectPostage(stamps(
			MailItems.STAMPS.stack(EnumStampDefinition.P_10, 4),
			MailItems.STAMPS.stack(EnumStampDefinition.P_1, 7)));

		PostOffice reloaded = new PostOffice(office.save(new CompoundTag(), helper.getLevel().registryAccess()));

		assertEquals(helper, reloaded.getAnyStamp(MailItems.STAMPS.item(EnumStampDefinition.P_10), 64).getCount(), 4, "reloaded 10n count");
		assertEquals(helper, reloaded.getAnyStamp(MailItems.STAMPS.item(EnumStampDefinition.P_1), 64).getCount(), 7, "reloaded 1n count");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void legacyOrdinalKeysMigrate(GameTestHelper helper) {
		// CPS<n> was indexed by EnumPostage.ordinal(), so CPS1 was P_1 and CPS7 was P_100. CPS0 was
		// never incremented and CPS8 (P_200) never had a stamp item
		CompoundTag legacy = new CompoundTag();
		legacy.putInt("CPS0", 0);
		legacy.putInt("CPS1", 5);
		legacy.putInt("CPS4", 2);
		legacy.putInt("CPS7", 1);
		legacy.putInt("CPS8", 9);

		PostOffice office = new PostOffice(legacy);

		assertEquals(helper, office.getAnyStamp(MailItems.STAMPS.item(EnumStampDefinition.P_1), 64).getCount(), 5, "migrated 1n count");
		assertEquals(helper, office.getAnyStamp(MailItems.STAMPS.item(EnumStampDefinition.P_10), 64).getCount(), 2, "migrated 10n count");
		assertEquals(helper, office.getAnyStamp(MailItems.STAMPS.item(EnumStampDefinition.P_100), 64).getCount(), 1, "migrated 100n count");
		helper.assertTrue(office.getAnyStamp(1).isEmpty(), "The vault held a stamp after every migrated denomination was drained");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void unfilteredWithdrawalTakesTheCheapestFirst(GameTestHelper helper) {
		PostOffice office = new PostOffice();
		office.collectPostage(stamps(
			MailItems.STAMPS.stack(EnumStampDefinition.P_100, 1),
			MailItems.STAMPS.stack(EnumStampDefinition.P_2, 1),
			MailItems.STAMPS.stack(EnumStampDefinition.P_20, 1)));

		helper.assertTrue(office.getAnyStamp(1).is(MailItems.STAMPS.item(EnumStampDefinition.P_2)), "First withdrawal was not the cheapest stamp");
		helper.assertTrue(office.getAnyStamp(1).is(MailItems.STAMPS.item(EnumStampDefinition.P_20)), "Second withdrawal was not the next cheapest stamp");
		helper.assertTrue(office.getAnyStamp(1).is(MailItems.STAMPS.item(EnumStampDefinition.P_100)), "Third withdrawal was not the dearest stamp");
		helper.assertTrue(office.getAnyStamp(1).isEmpty(), "The vault was not empty after every stamp was withdrawn");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void filteredWithdrawalMatchesTheExactItem(GameTestHelper helper) {
		PostOffice office = new PostOffice();
		office.collectPostage(stamps(MailItems.STAMPS.stack(EnumStampDefinition.P_5, 3)));

		helper.assertTrue(office.getAnyStamp(MailItems.STAMPS.item(EnumStampDefinition.P_10), 1).isEmpty(),
			"A filter the vault does not hold still yielded a stamp");
		assertEquals(helper, office.getAnyStamp(MailItems.STAMPS.item(EnumStampDefinition.P_5), 2).getCount(), 2, "filtered withdrawal count");
		helper.succeed();
	}

	private static NonNullList<ItemStack> stamps(ItemStack... stacks) {
		NonNullList<ItemStack> list = NonNullList.create();
		for (ItemStack stack : stacks) {
			list.add(stack);
		}
		return list;
	}

	private static void assertEquals(GameTestHelper helper, int actual, int expected, String what) {
		helper.assertTrue(actual == expected, what + " was " + actual + " instead of " + expected);
	}
}
