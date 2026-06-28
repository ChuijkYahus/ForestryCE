package forestry.core.data;

import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.neoforged.neoforge.common.Tags;

import forestry.api.ForestryConstants;
import forestry.api.ForestryTags;
import forestry.api.core.HumidityType;
import forestry.api.core.TemperatureType;
import forestry.api.genetics.ForestrySpeciesTypes;
import forestry.core.data.builder.MutationRecipeBuilder;

import static forestry.api.apiculture.ForestryBeeSpecies.*;
import static forestry.api.arboriculture.ForestryTreeSpecies.*;
import static forestry.api.lepidopterology.ForestryButterflySpecies.*;

/**
 * Generates the built-in mutations as {@code forestry:{bee,tree,butterfly}_mutation} recipe JSON. These faithfully
 * reproduce the mutations that used to be code-registered via {@code .addMutations(...)} on the species builders.
 */
public class MutationProvider {
	private final RecipeOutput output;
	private final List<MutationRecipeBuilder> pending = new ArrayList<>();

	private MutationProvider(RecipeOutput output) {
		this.output = output;
	}

	public static void buildRecipes(RecipeOutput output) {
		MutationProvider provider = new MutationProvider(output);
		provider.bees();
		provider.trees();
		provider.butterflies();
		provider.flush();
	}

	// chance percent (legacy int overload): chance = percent / 100
	private MutationRecipeBuilder add(ResourceLocation speciesTypeId, ResourceLocation first, ResourceLocation second, ResourceLocation result, int chancePercent) {
		return add(speciesTypeId, first, second, result, chancePercent / 100.0f);
	}

	// chance in [0, 1]
	private MutationRecipeBuilder add(ResourceLocation speciesTypeId, ResourceLocation first, ResourceLocation second, ResourceLocation result, float chance) {
		MutationRecipeBuilder builder = new MutationRecipeBuilder(speciesTypeId, first, second, result, chance);
		this.pending.add(builder);
		return builder;
	}

	private MutationRecipeBuilder bee(ResourceLocation first, ResourceLocation second, ResourceLocation result, int chancePercent) {
		return add(ForestrySpeciesTypes.BEE, first, second, result, chancePercent);
	}

	private MutationRecipeBuilder tree(ResourceLocation first, ResourceLocation second, ResourceLocation result, float chance) {
		return add(ForestrySpeciesTypes.TREE, first, second, result, chance);
	}

	private MutationRecipeBuilder butterfly(ResourceLocation first, ResourceLocation second, ResourceLocation result, float chance) {
		return add(ForestrySpeciesTypes.BUTTERFLY, first, second, result, chance);
	}

	private void flush() {
		Map<String, Integer> counters = new HashMap<>();
		for (MutationRecipeBuilder builder : this.pending) {
			String base = folder(builder.getSpeciesTypeId()) + "/" + builder.getResultId().getPath();
			int n = counters.merge(base, 1, Integer::sum);
			builder.build(this.output, ForestryConstants.forestry(base + "_" + n));
		}
	}

	private static String folder(ResourceLocation speciesTypeId) {
		if (speciesTypeId.equals(ForestrySpeciesTypes.BEE)) {
			return "bee_mutation";
		} else if (speciesTypeId.equals(ForestrySpeciesTypes.TREE)) {
			return "tree_mutation";
		} else if (speciesTypeId.equals(ForestrySpeciesTypes.BUTTERFLY)) {
			return "butterfly_mutation";
		}
		throw new IllegalArgumentException("Unknown species type for mutation: " + speciesTypeId);
	}

	private void bees() {
		ResourceLocation[] overworldHiveBees = {FOREST, MARSHY, MEADOWS, MODEST, SAVANNA, TROPICAL, VALIANT, WINTRY, LUSH, AQUATIC};

		// Common
		for (int i = 0; i < overworldHiveBees.length; i++) {
			ResourceLocation firstParent = overworldHiveBees[i];
			for (int j = i + 1; j < overworldHiveBees.length; j++) {
				bee(firstParent, overworldHiveBees[j], COMMON, 15);
			}
		}

		// Cultivated
		for (ResourceLocation secondParent : overworldHiveBees) {
			bee(COMMON, secondParent, CULTIVATED, 12);
		}

		bee(COMMON, CULTIVATED, NOBLE, 10);
		bee(NOBLE, CULTIVATED, MAJESTIC, 8);
		bee(NOBLE, MAJESTIC, IMPERIAL, 8);
		bee(COMMON, CULTIVATED, DILIGENT, 10);
		bee(DILIGENT, CULTIVATED, UNWEARY, 8);
		bee(DILIGENT, UNWEARY, INDUSTRIOUS, 8);

		// Sinister
		for (ResourceLocation parent : new ResourceLocation[]{MODEST, TROPICAL}) {
			bee(CULTIVATED, parent, SINISTER, 60).biome(BiomeTags.IS_NETHER);
		}

		// Fiendish
		for (ResourceLocation parent : new ResourceLocation[]{CULTIVATED, MODEST, TROPICAL}) {
			bee(SINISTER, parent, FIENDISH, 40).biome(BiomeTags.IS_NETHER);
		}

		bee(SINISTER, FIENDISH, DEMONIC, 25).biome(BiomeTags.IS_NETHER);

		// Frugal
		bee(MODEST, SINISTER, FRUGAL, 16).temperature(TemperatureType.HOT, TemperatureType.HELLISH).humidity(HumidityType.ARID);
		bee(MODEST, FIENDISH, FRUGAL, 10).temperature(TemperatureType.HOT, TemperatureType.HELLISH).humidity(HumidityType.ARID);

		// Austere
		bee(MODEST, FRUGAL, AUSTERE, 8).temperature(TemperatureType.HOT, TemperatureType.HELLISH).humidity(HumidityType.ARID);

		bee(AUSTERE, TROPICAL, EXOTIC, 12);
		bee(EXOTIC, TROPICAL, EDENIC, 8);
		bee(MONASTIC, AUSTERE, SECLUDED, 12);
		bee(MONASTIC, SECLUDED, HERMITIC, 8);
		bee(HERMITIC, ENDED, SPECTRAL, 4);
		bee(SPECTRAL, ENDED, PHANTASMAL, 2);

		bee(INDUSTRIOUS, WINTRY, ICY, 12).temperature(TemperatureType.ICY, TemperatureType.COLD);
		bee(ICY, WINTRY, GLACIAL, 8).temperature(TemperatureType.ICY, TemperatureType.COLD);

		bee(MARSHY, NOBLE, MIRY, 15).temperature(TemperatureType.WARM).humidity(HumidityType.DAMP);
		bee(MARSHY, MIRY, BOGGY, 9).temperature(TemperatureType.WARM).humidity(HumidityType.DAMP);

		bee(SAVANNA, DILIGENT, ARGIL, 15).temperature(TemperatureType.WARM, TemperatureType.HOT).humidity(HumidityType.ARID);
		bee(SAVANNA, ARGIL, PRIDE, 9).biome(ForestryTags.Biomes.SHATTERED_SAVANNA);

		bee(SAVANNA, COMMON, VINDICTIVE, 12);
		bee(VINDICTIVE, CULTIVATED, VENGEFUL, 8);
		bee(VINDICTIVE, VENGEFUL, AVENGING, 4);

		bee(STEADFAST, VALIANT, HEROIC, 6).biome(BiomeTags.IS_FOREST);

		bee(LUSH, VALIANT, VERDANT, 10).cave();
		bee(LUSH, VERDANT, LUXURIANT, 8).cave();
		bee(LUXURIANT, MONASTIC, KLEPTOPLASTIC, 12);
		bee(KLEPTOPLASTIC, LUXURIANT, PHOTOSYNTHETIC, 8);
		bee(KLEPTOPLASTIC, MONASTIC, PHOTOSYNTHETIC, 8);
		bee(KLEPTOPLASTIC, PHOTOSYNTHETIC, AUTOTROPHIC, 4);

		bee(AQUATIC, PIRATE, PRISMATIC, 8);
		bee(PIRATE, ENDED, ABYSSAL, 40).cave();
		bee(AQUATIC, ENDED, ABYSSAL, 40).cave();
		bee(PIRATE, SHULKING, ABYSSAL, 60).cave();
		bee(AQUATIC, SHULKING, ABYSSAL, 60).cave();

		bee(EMBITTERED, FIENDISH, SPITEFUL, 12);
		bee(SPITEFUL, EMBITTERED, SEETHING, 8);
		bee(EMBITTERED, ENDED, WARPED, 40).biome(ForestryTags.Biomes.WARPED_FOREST);
		bee(SPITEFUL, ENDED, WARPED, 40).biome(ForestryTags.Biomes.WARPED_FOREST);
		bee(EMBITTERED, SHULKING, WARPED, 40).biome(ForestryTags.Biomes.WARPED_FOREST);
		bee(SPITEFUL, SHULKING, WARPED, 40).biome(ForestryTags.Biomes.WARPED_FOREST);

		bee(ABYSSAL, HERMITIC, SCULK, 6).biome(ForestryTags.Biomes.DEEP_DARK);

		bee(MEADOWS, DILIGENT, RURAL, 12).biome(Tags.Biomes.IS_PLAINS);
		bee(RURAL, UNWEARY, FARMERLY, 10).biome(Tags.Biomes.IS_PLAINS);
		bee(FARMERLY, INDUSTRIOUS, AGRARIAN, 6).biome(Tags.Biomes.IS_PLAINS);

		bee(ANACHRONE, STEADFAST, PRIMEVAL, 15);
		bee(RELIC, STEADFAST, ANACHRONE, 10);

		// Festive (secret, date-restricted)
		bee(MEADOWS, FOREST, LEPORINE, 10).dateRange(Month.MARCH.getValue(), 29, Month.APRIL.getValue(), 15);
		bee(WINTRY, FOREST, MERRY, 10).dateRange(Month.DECEMBER.getValue(), 21, Month.DECEMBER.getValue(), 27);
		bee(WINTRY, MEADOWS, TIPSY, 10).dateRange(Month.DECEMBER.getValue(), 27, Month.JANUARY.getValue(), 2);
		bee(SINISTER, COMMON, TRICKY, 10).dateRange(Month.OCTOBER.getValue(), 15, Month.NOVEMBER.getValue(), 3);
		bee(RURAL, NOBLE, PATRIOTIC, 15).dateRange(Month.JULY.getValue(), 1, Month.JULY.getValue(), 17);
	}

	private void trees() {
		tree(OAK, BIRCH, LIME, 0.15f);
		tree(LIME, OAK, SOUR_CHERRY, 0.10f);
		tree(SOUR_CHERRY, DARK_OAK, WALNUT, 0.10f);
		tree(WALNUT, LIME, CHESTNUT, 0.05f).temperature(TemperatureType.NORMAL, TemperatureType.NORMAL).humidity(HumidityType.NORMAL);
		tree(SOUR_CHERRY, OAK, PEAR, 0.10f);
		tree(PEAR, SOUR_CHERRY, PLUM, 0.05f).temperature(TemperatureType.NORMAL, TemperatureType.NORMAL).humidity(HumidityType.NORMAL);
		tree(PEAR, LIME, FEIJOA, 0.05f).temperature(TemperatureType.NORMAL, TemperatureType.WARM).humidity(HumidityType.NORMAL, HumidityType.DAMP);
		tree(LIME, BIRCH, ELM, 0.10f);
		tree(ELM, OAK, MAPLE, 0.05f);
		tree(ELM, LIME, BEECH, 0.05f);
		tree(ELM, BIRCH, POPLAR, 0.05f);
		tree(POPLAR, DARK_OAK, WILLOW, 0.10f).temperature(TemperatureType.NORMAL).humidity(HumidityType.DAMP);
		tree(LIME, CHERRY_VANILLA, DOGWOOD, 0.10f);
		tree(DOGWOOD, CHERRY_VANILLA, JACARANDA, 0.05f).temperature(TemperatureType.NORMAL, TemperatureType.WARM).humidity(HumidityType.NORMAL, HumidityType.DAMP);
		tree(DOGWOOD, TEAK, IPE, 0.05f).temperature(TemperatureType.WARM).humidity(HumidityType.DAMP);
		tree(SPRUCE, OAK, LARCH, 0.15f);
		tree(LARCH, SPRUCE, PINE, 0.10f);
		tree(LARCH, OAK, FIR, 0.10f);
		tree(PINE, FIR, MACROCARPA, 0.10f);
		tree(PINE, LARCH, SEQUOIA, 0.10f);
		tree(SEQUOIA, GINKGO, GIANT_SEQUOIA, 0.05f).temperature(TemperatureType.ICY, TemperatureType.COLD).humidity(HumidityType.NORMAL);
		tree(MACROCARPA, FIR, PEWEN, 0.05f).temperature(TemperatureType.ICY, TemperatureType.COLD).humidity(HumidityType.NORMAL);
		tree(MACROCARPA, PINE, KAURI, 0.05f).temperature(TemperatureType.ICY, TemperatureType.COLD).humidity(HumidityType.NORMAL);
		tree(JUNGLE, DARK_OAK, TEAK, 0.15f);
		tree(TEAK, JUNGLE, KAPOK, 0.10f);
		tree(TEAK, BIRCH, BALSA, 0.10f);
		tree(LIME, JUNGLE, ORANGE, 0.10f);
		tree(BALSA, TEAK, EBONY, 0.10f);
		tree(KAPOK, TEAK, GREENHEART, 0.05f);
		tree(ORANGE, LIME, LEMON, 0.10f);
		tree(EBONY, BALSA, ZEBRANO, 0.05f).temperature(TemperatureType.WARM, TemperatureType.HOT).humidity(HumidityType.DAMP);
		tree(EBONY, KAPOK, MAHOGANY, 0.05f).temperature(TemperatureType.WARM, TemperatureType.HOT).humidity(HumidityType.DAMP);
		tree(WALNUT, KAPOK, COCONUT, 0.05f).temperature(TemperatureType.WARM, TemperatureType.HOT).humidity(HumidityType.DAMP);
		tree(LEMON, KAPOK, PAPAYA, 0.05f).temperature(TemperatureType.WARM, TemperatureType.HOT).humidity(HumidityType.DAMP);
		tree(ACACIA_VANILLA, JUNGLE, CAMELTHORN, 0.15f);
		tree(CAMELTHORN, JUNGLE, PADAUK, 0.10f);
		tree(CAMELTHORN, DARK_OAK, COCOBOLO, 0.10f);
		tree(CAMELTHORN, ACACIA_VANILLA, WENGE, 0.10f);
		tree(COCOBOLO, CAMELTHORN, MAHOE, 0.05f).temperature(TemperatureType.WARM, TemperatureType.HOT).humidity(HumidityType.ARID);
		tree(PADAUK, WENGE, BAOBAB, 0.05f).temperature(TemperatureType.WARM, TemperatureType.HOT).humidity(HumidityType.ARID);
		tree(COCOBOLO, SOUR_CHERRY, DATE, 0.05f).temperature(TemperatureType.WARM, TemperatureType.HOT).humidity(HumidityType.ARID);
		tree(WENGE, SOUR_CHERRY, OLIVE, 0.05f).temperature(TemperatureType.WARM, TemperatureType.HOT).humidity(HumidityType.ARID);
	}

	private void butterflies() {
		butterfly(LATTICED_HEATH, BRIMSTONE, BOMBYX_MORI, 0.07f);
	}
}
