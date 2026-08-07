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

		// Blocks and items that deliberately share one descriptionId across many registered variants
		// (farm materials, pollinated vs default leaves, letter sizes and states). Auto-generation
		// would otherwise pick a name from whichever variant it visits first, which is both wrong
		// (ex. "Farm Hatch Quartz Lines") and nondeterministic across datagen runs. Name them
		// explicitly. Content-jar keys are named here too, because base writes every key
		lang.add("block." + ForestryConstants.MOD_ID + ".farm_plain", "Farm Block");
		lang.add("block." + ForestryConstants.MOD_ID + ".farm_gearbox", "Farm Gearbox");
		lang.add("block." + ForestryConstants.MOD_ID + ".farm_hatch", "Farm Hatch");
		lang.add("block." + ForestryConstants.MOD_ID + ".farm_valve", "Farm Valve");
		lang.add("block." + ForestryConstants.MOD_ID + ".farm_control", "Farm Control");
		lang.add("block." + ForestryConstants.MOD_ID + ".leaves", "Leaves");
		lang.add("item." + ForestryConstants.MOD_ID + ".letter", "Letter");

		// The denomination is part of the id, so a generated name reads "Stamp 1n"
		lang.add("item." + ForestryConstants.MOD_ID + ".stamp_1n", "Stamp (1n)");
		lang.add("item." + ForestryConstants.MOD_ID + ".stamp_2n", "Stamp (2n)");
		lang.add("item." + ForestryConstants.MOD_ID + ".stamp_5n", "Stamp (5n)");
		lang.add("item." + ForestryConstants.MOD_ID + ".stamp_10n", "Stamp (10n)");
		lang.add("item." + ForestryConstants.MOD_ID + ".stamp_20n", "Stamp (20n)");
		lang.add("item." + ForestryConstants.MOD_ID + ".stamp_50n", "Stamp (50n)");
		lang.add("item." + ForestryConstants.MOD_ID + ".stamp_100n", "Stamp (100n)");
	}
}
