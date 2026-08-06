package forestry.mail.data;

import net.minecraft.core.registries.Registries;

import forestry.core.data.ContentJarData;
import forestry.core.data.DataRoots;
import forestry.core.data.JarData;

/**
 * Registers every provider that writes into the mail jar. Loaded by core through
 * {@code META-INF/services/forestry.core.data.IForestryDataProvider}.
 */
public class MailData extends ContentJarData {
	public MailData() {
		super("mail", DataRoots.MAIL);
	}

	@Override
	protected void addProviders(JarData jar) {
		jar.helper().createTags(Registries.BLOCK, MailBlockTagsProvider::addTags);
		jar.helper().createTags(Registries.ITEM, MailItemTagsProvider::addTags);
		jar.helper().createRecipes(MailRecipeProvider::addRecipes);
		jar.helper().createItemModels(false, false, false, MailItemModels::addModels);

		jar.addServer(new MailLootTableProvider(jar.output(), jar.lookup()));
		jar.addClient(new MailBlockStateProvider(jar.output(), jar.existingFileHelper()));
	}
}
