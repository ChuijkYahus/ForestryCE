package forestry.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.ForestryDataMaps;
import forestry.mail.features.MailItems;
import forestry.mail.letters.EnumStampDefinition;

/**
 * Guard for the postage data map. The data map is what lets another mod declare a stamp with one JSON
 * file and no dependency on the mail jar, so a data map that fails to register, or a generated file
 * that fails to load, silently turns every Forestry stamp into a worthless item.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class PostageDataMapTest {
	@GameTest(template = "empty")
	public static void everyForestryStampCarriesItsPostage(GameTestHelper helper) {
		List<String> broken = new ArrayList<>();

		for (EnumStampDefinition stamp : EnumStampDefinition.VALUES) {
			Item item = MailItems.STAMPS.item(stamp);
			Integer postage = item.builtInRegistryHolder().getData(ForestryDataMaps.POSTAGE);
			int expected = stamp.getPostage().getValue();

			if (postage == null) {
				broken.add(stamp.getSerializedName() + " has no postage entry at all");
			} else if (postage != expected) {
				broken.add(stamp.getSerializedName() + " is worth " + postage + " instead of " + expected);
			}
		}

		if (!broken.isEmpty()) {
			helper.fail(broken.size() + " stamp(s) did not resolve their postage:\n  " + String.join("\n  ", broken));
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void nonStampsCarryNoPostage(GameTestHelper helper) {
		helper.assertTrue(Items.PAPER.builtInRegistryHolder().getData(ForestryDataMaps.POSTAGE) == null,
			"Paper resolved a postage value, so the data map is matching items it should not");
		helper.succeed();
	}
}
