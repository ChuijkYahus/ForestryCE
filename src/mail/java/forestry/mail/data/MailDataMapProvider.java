package forestry.mail.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.common.data.DataMapProvider;

import forestry.api.ForestryDataMaps;
import forestry.mail.features.MailItems;
import forestry.mail.letters.EnumStampDefinition;

/**
 * Generates the postage every Forestry stamp is worth. Other mods add their own stamps by shipping a
 * file of this shape, which is the whole point of the data map.
 */
public class MailDataMapProvider extends DataMapProvider {
	public MailDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
		super(output, lookup);
	}

	@Override
	protected void gather(HolderLookup.Provider provider) {
		var postage = builder(ForestryDataMaps.POSTAGE);

		for (EnumStampDefinition stamp : EnumStampDefinition.VALUES) {
			postage.add(MailItems.STAMPS.item(stamp).builtInRegistryHolder(), stamp.getPostage().getValue(), false);
		}
	}
}
