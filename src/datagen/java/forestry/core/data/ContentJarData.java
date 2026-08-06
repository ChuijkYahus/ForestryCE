package forestry.core.data;

import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.data.event.GatherDataEvent;

import thedarkcolour.modkit.data.DataHelper;
import thedarkcolour.modkit.data.ProviderRegistrar;

import forestry.api.ForestryConstants;

/**
 * The assembly every content jar's entry point shares. A subclass names its jar and lists its
 * providers; the root, the registrar and the helper are the same three lines in each of them.
 */
public abstract class ContentJarData implements IForestryDataProvider {
	private final String jar;
	private final String root;

	/**
	 * @param jar  The jar name the generator keys this jar's providers by. Ex. {@code "mail"}
	 * @param root The generated resource root, one of the {@link DataRoots} constants
	 */
	protected ContentJarData(String jar, String root) {
		this.jar = jar;
		this.root = root;
	}

	@Override
	public final void gather(GatherDataEvent event) {
		PackOutput output = DataRoots.of(event, this.root);

		// The generator keys a provider by its name and rejects a duplicate, and core has already
		// registered a provider of most of these kinds under the name the kind gives itself. Every
		// provider here goes in under the jar's name instead
		ProviderRegistrar registrar = RenamedProvider.under(this.jar);

		DataHelper helper = new DataHelper.Builder(ForestryConstants.MOD_ID, event)
			.packOutput(output)
			.addProvider(registrar)
			.build();

		addProviders(new JarScope(event, output, helper, registrar));
	}

	/**
	 * Called once during core's gather event, with everything already scoped to this jar.
	 *
	 * @param jar The pieces this jar's providers are registered from
	 */
	protected abstract void addProviders(JarScope jar);
}
