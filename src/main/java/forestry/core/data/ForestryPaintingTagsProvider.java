package forestry.core.data;

import forestry.core.features.CorePaintings;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.world.entity.decoration.PaintingVariant;
import thedarkcolour.modkit.data.MKTagsProvider;

public class ForestryPaintingTagsProvider {
	public static void addTags(MKTagsProvider<PaintingVariant> tags, HolderLookup.Provider lookup) {
		tags.tag(PaintingVariantTags.PLACEABLE)
			.add(CorePaintings.MOUSETREE)
			.add(CorePaintings.WASPHOL)
			.add(CorePaintings.CAGE)
			.add(CorePaintings.LEWIS)
			.add(CorePaintings.SITEBEE)
			.add(CorePaintings.ALEXBLOOME)
			.add(CorePaintings.DEKU)
			.add(CorePaintings.SUSPICIOUS_LOOKING_TREE)
			.add(CorePaintings.WISDOM)
			.add(CorePaintings.JAZZ)
			.add(CorePaintings.TOOLS)
			.add(CorePaintings.THERE_IS_NOT_A_MAN_HERE)
			.add(CorePaintings.THE_HUNNY_TREE);
	}
}
