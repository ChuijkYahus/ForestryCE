package forestry.api.lepidopterology;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

import static forestry.api.ForestryConstants.forestry;

/**
 * All butterfly species registered by base Forestry.
 */
public class ForestryButterflySpecies {
	// Butterflies
	public static final ResourceLocation CABBAGE_WHITE = forestry("butterfly_cabbage_white");
	public static final ResourceLocation BRIMSTONE = forestry("butterfly_brimstone");
	public static final ResourceLocation AURORA = forestry("butterfly_aurora");
	public static final ResourceLocation CLOUDED_YELLOW = forestry("butterfly_clouded_yellow");
	public static final ResourceLocation PALAENO_SULPHUR = forestry("butterfly_palaeno_sulphur");
	public static final ResourceLocation RESEDA = forestry("butterfly_reseda");
	public static final ResourceLocation SPRING_AZURE = forestry("butterfly_spring_azure");
	public static final ResourceLocation GOZORA_AZURE = forestry("butterfly_gozora_azure");
	public static final ResourceLocation CITRUS_SWALLOWTAIL = forestry("butterfly_citrus_swallow");
	public static final ResourceLocation EMERALD_PEACOCK = forestry("butterfly_emerald_peacock");
	public static final ResourceLocation THOAS_SWALLOWTAIL = forestry("butterfly_thoas_swallow");
	public static final ResourceLocation SPICEBUSH_SWALLOWTAIL = forestry("butterfly_spicebush_swallow");
	public static final ResourceLocation BLACK_SWALLOWTAIL = forestry("butterfly_black_swallow");
	public static final ResourceLocation ZEBRA_SWALLOWTAIL = forestry("butterfly_zebra_swallow");
	public static final ResourceLocation GLASSWING = forestry("butterfly_glasswing");
	public static final ResourceLocation SPECKLED_WOOD = forestry("butterfly_speckled_wood");
	public static final ResourceLocation MADEIRAN_SPECKLED_WOOD = forestry("butterfly_mspeckled_wood");
	public static final ResourceLocation CANARY_SPECKLED_WOOD = forestry("butterfly_cspeckled_wood");
	public static final ResourceLocation MENELAUS_BLUE_MORPHO = forestry("butterfly_mbluemorpho");
	public static final ResourceLocation PELEIDES_BLUE_MORPHO = forestry("butterfly_pbluemorpho");
	public static final ResourceLocation RHETENOR_BLUE_MORPHO = forestry("butterfly_rbluemorpho");
	public static final ResourceLocation COMMA = forestry("butterfly_comma");
	public static final ResourceLocation BATESIA = forestry("butterfly_batesia");
	public static final ResourceLocation BLUE_WING = forestry("butterfly_blue_wing");
	public static final ResourceLocation MONARCH = forestry("butterfly_monarch");
	public static final ResourceLocation BLUE_DUKE = forestry("butterfly_blue_duke");
	public static final ResourceLocation GLASSY_TIGER = forestry("butterfly_glassy_tiger");
	public static final ResourceLocation POSTMAN = forestry("butterfly_postman");
	public static final ResourceLocation MALACHITE = forestry("butterfly_malachite");
	public static final ResourceLocation LEOPARD_LACEWING = forestry("butterfly_leopard_lacewing");
	public static final ResourceLocation DIANA_FRITILLARY = forestry("butterfly_diana_fritillary");

	// Moths
	public static final ResourceLocation BRIMSTONE_MOTH = forestry("butterfly_brimstone_moth");
	public static final ResourceLocation LATTICED_HEATH = forestry("butterfly_latticed_heath");
	public static final ResourceLocation ATLAS = forestry("butterfly_atlas");
	public static final ResourceLocation BOMBYX_MORI = forestry("butterfly_bombyx_mori");

	/**
	 * All built-in butterfly species ids, in declaration order. Compile-time constant list (not a view of the
	 * reloadable species registry), safe to iterate at client-registration time.
	 */
	public static final List<ResourceLocation> ALL = List.of(
		CABBAGE_WHITE, BRIMSTONE, AURORA, CLOUDED_YELLOW, PALAENO_SULPHUR, RESEDA, SPRING_AZURE, GOZORA_AZURE,
		CITRUS_SWALLOWTAIL, EMERALD_PEACOCK, THOAS_SWALLOWTAIL, SPICEBUSH_SWALLOWTAIL, BLACK_SWALLOWTAIL,
		ZEBRA_SWALLOWTAIL, GLASSWING, SPECKLED_WOOD, MADEIRAN_SPECKLED_WOOD, CANARY_SPECKLED_WOOD,
		MENELAUS_BLUE_MORPHO, PELEIDES_BLUE_MORPHO, RHETENOR_BLUE_MORPHO, COMMA, BATESIA, BLUE_WING, MONARCH,
		BLUE_DUKE, GLASSY_TIGER, POSTMAN, MALACHITE, LEOPARD_LACEWING, DIANA_FRITILLARY,
		BRIMSTONE_MOTH, LATTICED_HEATH, ATLAS, BOMBYX_MORI
	);
}
