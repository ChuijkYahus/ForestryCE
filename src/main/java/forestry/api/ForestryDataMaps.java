package forestry.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

import static forestry.api.ForestryConstants.forestry;

/**
 * All data maps added by base Forestry.
 */
public class ForestryDataMaps {
	/**
	 * The postage an item is worth when it is attached to a letter. An item with no entry is not a
	 * stamp. Mods add their own stamps with a data map file and no dependency on Forestry.
	 *
	 * <p>Registered by the mail jar, so the data map is absent when that jar is not installed. Nothing
	 * in base Forestry reads it.
	 *
	 * <p>A file goes under the namespace of the data map rather than the namespace of the mod adding
	 * to it, and every mod's file at that path is merged.
	 * <p>
	 * Ex. {@code data/forestry/data_maps/item/postage.json}
	 */
	public static final DataMapType<Item, Integer> POSTAGE = DataMapType
		.builder(forestry("postage"), Registries.ITEM, ExtraCodecs.POSITIVE_INT)
		.synced(ExtraCodecs.POSITIVE_INT, false)
		.build();
}
