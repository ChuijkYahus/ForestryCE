package forestry.lepidopterology.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import thedarkcolour.modkit.data.DataHelper;
import thedarkcolour.modkit.data.ProviderRegistrar;

import forestry.api.ForestryConstants;
import forestry.core.data.DataRoots;
import forestry.core.data.IForestryDataProvider;
import forestry.core.data.RenamedProvider;

/**
 * Registers every provider that writes into the butterflies jar. Loaded by core through
 * {@code META-INF/services/forestry.core.data.IForestryDataProvider}.
 */
public class LepidopterologyData implements IForestryDataProvider {
	// The jar these providers belong to. Only ever seen in the names the generator keys them by
	private static final String JAR = "butterflies";

	@Override
	public void gather(GatherDataEvent event) {
		// Butterfly taxa must be live before any butterfly species is built, because a species resolves
		// its genus through the taxonomy
		ButterflyTaxonProvider.seedLiveTaxaForDatagen();
		ButterflySpeciesProvider.seedLiveSpeciesForDatagen();

		DataGenerator generator = event.getGenerator();
		PackOutput output = DataRoots.of(event, DataRoots.BUTTERFLIES);
		ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
		CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

		// The generator keys a provider by its name and rejects a duplicate, and core has already
		// registered a provider of most of these kinds under the name the kind gives itself. Every
		// provider here goes in under the jar's name instead
		ProviderRegistrar registrar = RenamedProvider.under(JAR);

		DataHelper dataHelper = new DataHelper.Builder(ForestryConstants.MOD_ID, event)
			.packOutput(output)
			.addProvider(registrar)
			.build();

		dataHelper.createTags(Registries.ITEM, LepidopterologyItemTagsProvider::addTags);
		dataHelper.createRecipes(LepidopterologyRecipeProvider::addRecipes);

		registrar.addProvider(generator, event.includeServer(), new ButterflyTaxonProvider(output));
		registrar.addProvider(generator, event.includeServer(), new ButterflySpeciesProvider(output, lookup));
		registrar.addProvider(generator, event.includeServer(), new ButterflyMutationProvider(output, lookup));
		registrar.addProvider(generator, event.includeServer(), new LepidopterologyLootTableProvider(output, lookup));
		registrar.addProvider(generator, event.includeServer(), new LepidopterologyDataMapProvider(output, lookup));
		registrar.addProvider(generator, event.includeClient(), new LepidopterologyItemModelProvider(output, existingFileHelper));
	}
}
