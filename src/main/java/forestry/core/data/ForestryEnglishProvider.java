package forestry.core.data;

import forestry.api.ForestryConstants;
import forestry.core.features.CoreItems;

import thedarkcolour.modkit.data.MKEnglishProvider;

public class ForestryEnglishProvider {
	// todo flesh this out more in 1.21 and change IDs of everything over for autogen lang
	public static void addTranslations(MKEnglishProvider lang) {
		// fertility - new allele-foundation key scheme: allele.forestry.fertility.<value> (value = String.valueOf(int))
		lang.add("allele." + ForestryConstants.MOD_ID + ".fertility.0", "Infertile");
		for (int i = 1; i <= 10; ++i) {
			lang.add("allele." + ForestryConstants.MOD_ID + ".fertility." + i, String.valueOf(i));
		}

		lang.add(CoreItems.SURVIVALISTS_PICKAXE.item(), "Survivalist's Pickaxe");
		lang.add(CoreItems.SURVIVALISTS_SHOVEL.item(), "Survivalist's Shovel");
		lang.add(CoreItems.SURVIVALISTS_AXE.item(), "Survivalist's Axe");
		lang.add(CoreItems.SURVIVALISTS_SWORD.item(), "Survivalist's Sword");
		lang.add(CoreItems.SURVIVALISTS_HOE.item(), "Survivalist's Hoe");

		// autogen would render the possessive in these as "Survivalists", so name them explicitly
		lang.add(CoreItems.BROKEN_SURVIVALISTS_PICKAXE.item(), "Broken Survivalist's Pickaxe");
		lang.add(CoreItems.BROKEN_SURVIVALISTS_SHOVEL.item(), "Broken Survivalist's Shovel");
		lang.add(CoreItems.BROKEN_SURVIVALISTS_AXE.item(), "Broken Survivalist's Axe");
		lang.add(CoreItems.BROKEN_SURVIVALISTS_SWORD.item(), "Broken Survivalist's Sword");
		lang.add(CoreItems.BROKEN_SURVIVALISTS_HOE.item(), "Broken Survivalist's Hoe");

		// Blocks that deliberately share one descriptionId across many registered variants (farm
		// materials, pollinated vs default leaves). Auto-generation would otherwise pick a name from
		// whichever variant it visits first, which is both wrong (e.g. "Farm Hatch Quartz Lines") and
		// nondeterministic across datagen runs. Name them explicitly.
		// The letter sizes and states share a descriptionId the same way, and mail names it in
		// MailData, since mail is the jar that ships the items
		lang.add("block." + ForestryConstants.MOD_ID + ".farm_plain", "Farm Block");
		lang.add("block." + ForestryConstants.MOD_ID + ".farm_gearbox", "Farm Gearbox");
		lang.add("block." + ForestryConstants.MOD_ID + ".farm_hatch", "Farm Hatch");
		lang.add("block." + ForestryConstants.MOD_ID + ".farm_valve", "Farm Valve");
		lang.add("block." + ForestryConstants.MOD_ID + ".farm_control", "Farm Control");
		lang.add("block." + ForestryConstants.MOD_ID + ".leaves", "Leaves");
	}
}
