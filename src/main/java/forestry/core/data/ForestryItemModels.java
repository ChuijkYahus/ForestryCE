package forestry.core.data;

import forestry.api.arboriculture.ForestryTreeSpecies;
import forestry.api.arboriculture.ITreeSpecies;
import forestry.apiculture.features.ApicultureItems;
import forestry.arboriculture.ForestryWoodType;
import forestry.arboriculture.features.ArboricultureItems;
import forestry.core.features.CoreItems;
import forestry.core.utils.SpeciesUtil;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import thedarkcolour.modkit.data.MKItemModelProvider;

import java.util.List;
import java.util.Set;

public class ForestryItemModels {
	// Simple 2D items whose model is `item/generated` with a single `layer0` texture named
	// `forestry:item/<id>`. Previously hand-authored under assets/.../models/item; now generated.
	// (Model-datagen migration.) Add new plain items here instead of authoring a JSON.
	private static final List<String> SIMPLE_2D_ITEMS = List.of(
		"amber", "amber_drone_fossil", "amber_sapling_fossil", "ambrosia", "apatite",
		"apiarists_hat", "apiarists_pants", "apiarists_shirt", "apiarists_shoes", "ash",
		"bee_smoker", "beeswax", "bituminous_peat", "catalog", "cherry",
		"chestnut", "coconut", "compost", "crate", "date",
		"decaying_wheat", "dissipation_charge", "exp_drop", "feijoa", "fertilizer",
		"filled_crate", "flexible_casing", "foresters_manual", "grafter", "habitat_locator",
		"hardened_casing", "ice_shard", "impregnated_casing", "impregnated_frame", "impregnated_stick",
		"imprinter", "infuser", "iodine_capsule", "lemon", "letter_big_emptied",
		"letter_big_fresh", "letter_big_opened", "letter_big_stamped", "letter_empty_emptied", "letter_empty_fresh",
		"letter_empty_opened", "letter_empty_stamped", "letter_small_emptied", "letter_small_fresh", "letter_small_opened",
		"letter_small_stamped", "mouldy_wheat", "mulch", "olive", "orange",
		"papaya", "peat", "pear", "phosphor", "plum",
		"portable_analyzer", "proven_frame", "proven_grafter", "pulsating_dust", "pulsating_mesh",
		"refractory_wax", "royal_jelly", "scented_paneling", "scoop", "silk_wisp",
		"soldering_iron", "spectacles", "sturdy_casing", "untreated_frame", "walnut",
		"wax_cast", "wood_pulp", "woven_silk"
	);

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
		for (String id : SIMPLE_2D_ITEMS) {
			models.generic2d(models.modLoc(id));
		}
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

			String name = species.id().getPath().substring("tree_".length()) + "_sapling";
			models.cross("block/" + name, models.modLoc("item/" + name));
			models.generic2d(models.modLoc(name));
		}
	}
}
