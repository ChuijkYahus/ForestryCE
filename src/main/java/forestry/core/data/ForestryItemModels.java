package forestry.core.data;

import forestry.api.arboriculture.ForestryTreeSpecies;
import forestry.api.arboriculture.ITreeSpecies;
import forestry.apiculture.features.ApicultureItems;
import forestry.arboriculture.ForestryWoodType;
import forestry.arboriculture.features.ArboricultureItems;
import forestry.core.features.CoreItems;
import forestry.core.items.ItemFruit;
import forestry.core.items.definitions.EnumCraftingMaterial;
import forestry.core.utils.SpeciesUtil;
import forestry.mail.features.MailItems;
import forestry.mail.items.LetterItem;
import forestry.storage.features.CrateItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import thedarkcolour.modkit.data.MKItemModelProvider;

import java.util.List;
import java.util.Set;

public class ForestryItemModels {
	// Tier 2: parent-only models (`withExistingParent`). id -> parent model path.
	// All bee combs share the hand-authored `item/bee_combs` template.
	private static final List<String> BEE_COMB_ITEMS = List.of(
		"bee_comb_darkened", "bee_comb_irradiated", "bee_comb_omega", "bee_comb_reddened",
		"cocoa_comb", "dripping_comb", "frozen_comb", "honey_comb", "kaolin_comb",
		"mellow_comb", "mossy_comb", "mysterious_comb", "parched_comb", "powdery_comb",
		"sculken_comb", "silky_comb", "simmering_comb", "spongy_comb", "stringy_comb",
		"vintage_comb", "wheaten_comb"
	);

	// Tier 3: two-layer `item/generated` models. Every item in a family shares the same texture pair,
	// so only the id varies. (Textures keep their existing `<name>.0`/`<name>.1` sprite-sheet names.)
	private static final List<String> ELECTRON_TUBES = List.of(
		"amber_electron_tube", "apatine_electron_tube", "blazing_electron_tube", "bronze_electron_tube",
		"copper_electron_tube", "diamantine_electron_tube", "emerald_electron_tube", "ender_electron_tube",
		"golden_electron_tube", "iron_electron_tube", "lapis_electron_tube", "obsidian_electron_tube",
		"tin_electron_tube", "electron_tube_orchid", "electron_tube_rubber"
	);
	private static final List<String> CIRCUIT_BOARDS = List.of(
		"basic_circuit_board", "enhanced_circuit_board", "intricate_circuit_board", "refined_circuit_board"
	);
	private static final List<String> POLLEN_ITEMS = List.of(
		"pollen_cluster", "crystalline_pollen_cluster", "tree_pollen"
	);
	private static final List<String> STAMPS = List.of(
		"stamp_1n", "stamp_2n", "stamp_5n", "stamp_10n", "stamp_20n", "stamp_50n", "stamp_100n"
	);

	// Builds a layered `item/generated` model referencing the given (already-existing) textures in order.
	private static void layered(MKItemModelProvider models, String id, String... textures) {
		var builder = models.getBuilder(id).parent(new ModelFile.UncheckedModelFile("item/generated"));
		for (int i = 0; i < textures.length; i++) {
			builder.texture("layer" + i, models.modLoc(textures[i]));
		}
	}

	public static void addModels(MKItemModelProvider models) {
		models.generic2d(CoreItems.AMBER);
		models.generic2d(ApicultureItems.AMBER_DRONE);
		models.generic2d(ArboricultureItems.AMBER_SAPLING);
		models.generic2d(ApicultureItems.AMBROSIA);
		models.generic2d(CoreItems.APATITE);
		models.generic2d(ApicultureItems.APIARIST_HELMET);
		models.generic2d(ApicultureItems.APIARIST_LEGS);
		models.generic2d(ApicultureItems.APIARIST_CHEST);
		models.generic2d(ApicultureItems.APIARIST_BOOTS);
		models.generic2d(CoreItems.ASH);
		models.generic2d(ApicultureItems.SMOKER);
		models.generic2d(CoreItems.BEESWAX);
		models.generic2d(CoreItems.BITUMINOUS_PEAT);
		models.generic2d(MailItems.CATALOGUE);
		models.generic2d(CoreItems.COMPOST);
		models.generic2d(CrateItems.CRATE);
		models.generic2d(CoreItems.DECAYING_WHEAT);
		models.generic2d(CoreItems.DISSIPATION_CHARGE);
		models.generic2d(CoreItems.FERTILIZER_COMPOUND);
		models.generic2d(CoreItems.FLEXIBLE_CASING);
		models.generic2d(CoreItems.FORESTERS_MANUAL);
		models.generic2d(ArboricultureItems.GRAFTER);
		models.generic2d(CoreItems.HARDENED_CASING);
		models.generic2d(CoreItems.IMPREGNATED_CASING);
		models.generic2d(ApicultureItems.FRAME_IMPREGNATED);
		models.generic2d(CoreItems.IODINE_CHARGE);
		models.generic2d(CoreItems.MOULDY_WHEAT);
		models.generic2d(CoreItems.MULCH);
		models.generic2d(CoreItems.PEAT);
		models.generic2d(CoreItems.PORTABLE_ALYZER);
		models.generic2d(ApicultureItems.FRAME_PROVEN);
		models.generic2d(ArboricultureItems.GRAFTER_PROVEN);
		models.generic2d(CoreItems.REFRACTORY_WAX);
		models.generic2d(ApicultureItems.ROYAL_JELLY);
		models.generic2d(ApicultureItems.SCOOP);
		models.generic2d(CoreItems.SOLDERING_IRON);
		models.generic2d(CoreItems.SPECTACLES);
		models.generic2d(CoreItems.STURDY_CASING);
		models.generic2d(ApicultureItems.FRAME_UNTREATED);

		// what kind of fruit is this?
		models.generic2d(CoreItems.FRUITS.get(ItemFruit.EnumFruit.CHERRY));
		models.generic2d(CoreItems.FRUITS.get(ItemFruit.EnumFruit.CHESTNUT));
		models.generic2d(CoreItems.FRUITS.get(ItemFruit.EnumFruit.COCONUT));
		models.generic2d(CoreItems.FRUITS.get(ItemFruit.EnumFruit.DATES));
		models.generic2d(CoreItems.FRUITS.get(ItemFruit.EnumFruit.FEIJOA));
		models.generic2d(CoreItems.FRUITS.get(ItemFruit.EnumFruit.LEMON));
		models.generic2d(CoreItems.FRUITS.get(ItemFruit.EnumFruit.OLIVE));
		models.generic2d(CoreItems.FRUITS.get(ItemFruit.EnumFruit.ORANGE));
		models.generic2d(CoreItems.FRUITS.get(ItemFruit.EnumFruit.PAPAYA));
		models.generic2d(CoreItems.FRUITS.get(ItemFruit.EnumFruit.PEAR));
		models.generic2d(CoreItems.FRUITS.get(ItemFruit.EnumFruit.PLUM));
		models.generic2d(CoreItems.FRUITS.get(ItemFruit.EnumFruit.WALNUT));

		models.generic2d(CoreItems.CRAFTING_MATERIALS.get(EnumCraftingMaterial.PULSATING_DUST));
		models.generic2d(CoreItems.CRAFTING_MATERIALS.get(EnumCraftingMaterial.PULSATING_MESH));
		models.generic2d(CoreItems.CRAFTING_MATERIALS.get(EnumCraftingMaterial.WOOD_PULP));
		models.generic2d(CoreItems.CRAFTING_MATERIALS.get(EnumCraftingMaterial.SILK_WISP));
		models.generic2d(CoreItems.CRAFTING_MATERIALS.get(EnumCraftingMaterial.WOVEN_SILK));
		models.generic2d(CoreItems.CRAFTING_MATERIALS.get(EnumCraftingMaterial.ICE_SHARD));
		models.generic2d(CoreItems.CRAFTING_MATERIALS.get(EnumCraftingMaterial.PHOSPHOR));
		models.generic2d(CoreItems.CRAFTING_MATERIALS.get(EnumCraftingMaterial.IMPREGNATED_STICK));
		models.generic2d(CoreItems.CRAFTING_MATERIALS.get(EnumCraftingMaterial.SCENTED_PANELING));

		models.generic2d(MailItems.LETTERS.get(LetterItem.Size.BIG, LetterItem.State.EMPTIED));
		models.generic2d(MailItems.LETTERS.get(LetterItem.Size.BIG, LetterItem.State.FRESH));
		models.generic2d(MailItems.LETTERS.get(LetterItem.Size.BIG, LetterItem.State.OPENED));
		models.generic2d(MailItems.LETTERS.get(LetterItem.Size.BIG, LetterItem.State.STAMPED));
		models.generic2d(MailItems.LETTERS.get(LetterItem.Size.EMPTY, LetterItem.State.EMPTIED));
		models.generic2d(MailItems.LETTERS.get(LetterItem.Size.EMPTY, LetterItem.State.FRESH));
		models.generic2d(MailItems.LETTERS.get(LetterItem.Size.EMPTY, LetterItem.State.OPENED));
		models.generic2d(MailItems.LETTERS.get(LetterItem.Size.EMPTY, LetterItem.State.STAMPED));
		models.generic2d(MailItems.LETTERS.get(LetterItem.Size.SMALL, LetterItem.State.EMPTIED));
		models.generic2d(MailItems.LETTERS.get(LetterItem.Size.SMALL, LetterItem.State.FRESH));
		models.generic2d(MailItems.LETTERS.get(LetterItem.Size.SMALL, LetterItem.State.OPENED));
		models.generic2d(MailItems.LETTERS.get(LetterItem.Size.SMALL, LetterItem.State.STAMPED));

		// Legacy models that still ship but have no registered item in this port (lang/JEI entries exist, the item
		// does not) - referenced by id since there is no symbol to point at.
		models.generic2d(models.modLoc("exp_drop"));
		models.generic2d(models.modLoc("filled_crate"));

		models.handheld(models.modLoc("wrench"));

		// Tier 2: bee comb items all inherit the shared `item/bee_combs` template model.
		for (String id : BEE_COMB_ITEMS) {
			models.withExistingParent(id, models.modLoc("item/bee_combs"));
		}

		// Tier 3: layered electron tube / circuit board / pollen / stamp models (each family = one texture pair).
		for (String id : ELECTRON_TUBES) layered(models, id, "item/thermionic_tubes.0", "item/thermionic_tubes.1");
		for (String id : CIRCUIT_BOARDS) layered(models, id, "item/chipsets.1", "item/chipsets.0");
		for (String id : POLLEN_ITEMS) layered(models, id, "item/pollen.0", "item/pollen.1");
		for (String id : STAMPS) layered(models, id, "item/stamps.0", "item/stamps.1");

		models.generic2d(ApicultureItems.HONEY_DROP);
		models.generic2d(ApicultureItems.HONEYDEW);
		models.generic2d(ApicultureItems.EXPERIENCE_DROP);
		models.generic2d(ApicultureItems.HONEY_POT);
		models.generic2d(ApicultureItems.HONEYED_SLICE);

		for (ForestryWoodType type : ForestryWoodType.VALUES) {
			models.generic2d(ArboricultureItems.BOAT.get(type));
			models.generic2d(ArboricultureItems.CHEST_BOAT.get(type));
		}

		models.generic2d(CoreItems.CARTON.get());
		models.generic2d(CoreItems.BROKEN_BRONZE_PICKAXE.get());
		models.generic2d(CoreItems.BROKEN_BRONZE_SHOVEL.get());
		models.generic2d(CoreItems.BROKEN_BRONZE_AXE.get());
		models.generic2d(CoreItems.BROKEN_BRONZE_SWORD.get());
		models.generic2d(CoreItems.BROKEN_BRONZE_HOE.get());
		models.handheld(CoreItems.BRONZE_PICKAXE.id());
		models.handheld(CoreItems.BRONZE_SHOVEL.id());
		models.handheld(CoreItems.BRONZE_AXE.id());
		models.handheld(CoreItems.BRONZE_SWORD.id());
		models.handheld(CoreItems.BRONZE_HOE.id());
		models.generic2d(CoreItems.KIT_SHOVEL.get());
		models.generic2d(CoreItems.KIT_PICKAXE.get());
		models.generic2d(CoreItems.KIT_AXE.get());
		models.generic2d(CoreItems.KIT_SWORD.get());
		models.generic2d(CoreItems.KIT_HOE.get());

		Set<ResourceLocation> vanillaIds = Set.of(
			ForestryTreeSpecies.OAK,
			ForestryTreeSpecies.DARK_OAK,
			ForestryTreeSpecies.BIRCH,
			ForestryTreeSpecies.ACACIA_VANILLA,
			ForestryTreeSpecies.SPRUCE,
			ForestryTreeSpecies.JUNGLE,
			ForestryTreeSpecies.CHERRY_VANILLA
		);
		// Saplings
		for (ITreeSpecies species : SpeciesUtil.getAllTreeSpecies()) {
			if (vanillaIds.contains(species.id())) continue;

			// Tree species ids are bare (e.g. "oak", "ipe", "hill_cherry") - they carry no "tree_" prefix
			// (see ForestryTreeSpecies), and sapling assets are named "<id>_sapling" (e.g. ipe_sapling).
			String name = species.id().getPath() + "_sapling";
			models.cross("block/" + name, models.modLoc("item/" + name));
			models.generic2d(models.modLoc(name));
		}
	}
}
