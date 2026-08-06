package forestry.mail.data;

import java.util.Set;

import net.minecraft.core.registries.Registries;

import thedarkcolour.modkit.data.MKEnglishProvider;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.data.ContentJarData;
import forestry.core.data.DataRoots;
import forestry.core.data.JarLootTableProvider;
import forestry.core.data.JarScope;

/**
 * Registers every provider that writes into the mail jar. Loaded by core through
 * {@code META-INF/services/forestry.core.data.IForestryDataProvider}.
 */
public class MailData extends ContentJarData {
	public MailData() {
		super("mail", DataRoots.MAIL, Set.of(ForestryModuleIds.MAIL));
	}

	@Override
	protected void addProviders(JarScope jar) {
		jar.helper().createTags(Registries.BLOCK, MailBlockTagsProvider::addTags);
		jar.helper().createTags(Registries.ITEM, MailItemTagsProvider::addTags);
		jar.helper().createRecipes(MailRecipeProvider::addRecipes);
		jar.helper().createItemModels(false, false, false, MailItemModels::addModels);

		jar.addServer(new JarLootTableProvider(jar.output(), jar.lookup(), MailBlockLootTables::new));
		jar.addClient(new MailBlockStateProvider(jar.output(), jar.existingFileHelper()));
	}

	@Override
	protected void addTranslations(MKEnglishProvider english) {
		// Every letter item shares one description id, so a generated name would be whichever letter
		// the registry walk reached first
		english.add("item.forestry.letter", "Letter");
		// The denomination is part of the id, so a generated name reads "Stamp 1n"
		english.add("item.forestry.stamp_1n", "Stamp (1n)");
		english.add("item.forestry.stamp_2n", "Stamp (2n)");
		english.add("item.forestry.stamp_5n", "Stamp (5n)");
		english.add("item.forestry.stamp_10n", "Stamp (10n)");
		english.add("item.forestry.stamp_20n", "Stamp (20n)");
		english.add("item.forestry.stamp_50n", "Stamp (50n)");
		english.add("item.forestry.stamp_100n", "Stamp (100n)");
	}
}
