package forestry.core.data;

import java.util.Set;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.data.event.GatherDataEvent;

import thedarkcolour.modkit.data.DataHelper;
import thedarkcolour.modkit.data.MKEnglishProvider;
import thedarkcolour.modkit.data.ProviderRegistrar;

import forestry.api.ForestryConstants;

/**
 * The assembly every content jar's entry point shares. A subclass names its jar, the modules it
 * ships and its providers; the root, the registrar and the helper are the same three lines in each
 * of them.
 */
public abstract class ContentJarData implements IForestryDataProvider {
	private final String jar;
	private final String root;
	private final Set<ResourceLocation> moduleIds;

	/**
	 * @param jar       The jar name the generator keys this jar's providers by. Ex. {@code "mail"}
	 * @param root      The generated resource root, one of the {@link DataRoots} constants
	 * @param moduleIds The modules this jar ships, which decide the ids it names
	 */
	protected ContentJarData(String jar, String root, Set<ResourceLocation> moduleIds) {
		this.jar = jar;
		this.root = root;
		this.moduleIds = moduleIds;
	}

	@Override
	public final void gather(GatherDataEvent event) {
		PackOutput output = DataRoots.of(event, this.root);

		// The generator keys a provider by its name and rejects a duplicate, and core has already
		// registered a provider of most of these kinds under the name the kind gives itself. Every
		// provider here goes in under the jar's name instead
		ProviderRegistrar registrar = RenamedProvider.under(this.jar);

		// A name is generated for an id this jar's modules registered and for nothing else, so the
		// lang file lands in the jar shipping the thing it names
		Set<ResourceLocation> owned = JarModules.ownedIds(this.moduleIds);
		DataHelper helper = new DataHelper.Builder(ForestryConstants.MOD_ID, event)
			.packOutput(output)
			.addProvider(registrar)
			.entryFilter(owned::contains)
			.build();

		addProviders(new JarScope(event, output, helper, registrar));

		// Last, so a provider a subclass adds can still seed whatever the names are read off
		helper.createEnglish(true, this::addTranslations);
	}

	/**
	 * Called once during core's gather event, with everything already scoped to this jar.
	 *
	 * @param jar The pieces this jar's providers are registered from
	 */
	protected abstract void addProviders(JarScope jar);

	/**
	 * Called when this jar's English is written, before any name is generated. A name added here wins,
	 * since generation fills in only the keys that are still empty.
	 *
	 * @param english The provider this jar's {@code en_us} is written by
	 */
	protected void addTranslations(MKEnglishProvider english) {
	}
}
