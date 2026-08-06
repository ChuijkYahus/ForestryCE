package forestry.core.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import thedarkcolour.modkit.data.DataHelper;
import thedarkcolour.modkit.data.ProviderRegistrar;

/**
 * The pieces a content jar registers its providers from, assembled by {@link ContentJarData}. Every
 * one of them is already scoped to that jar, so a provider added here writes into the jar's own root
 * and is keyed by a name no other jar uses.
 *
 * @param event     The gather event this run was started from
 * @param output    The root every provider of this jar writes to
 * @param helper    The helper the ModKit-backed providers are created through
 * @param registrar The registrar every provider of this jar is attached with
 */
public record JarData(GatherDataEvent event, PackOutput output, DataHelper helper, ProviderRegistrar registrar) {
	/**
	 * Adds a provider that runs when the data run includes server data.
	 *
	 * @param provider The provider to attach
	 */
	public void addServer(DataProvider provider) {
		this.registrar.addProvider(this.event.getGenerator(), this.event.includeServer(), provider);
	}

	/**
	 * Adds a provider that runs when the data run includes client data.
	 *
	 * @param provider The provider to attach
	 */
	public void addClient(DataProvider provider) {
		this.registrar.addProvider(this.event.getGenerator(), this.event.includeClient(), provider);
	}

	/**
	 * @return The lookup a provider resolves registry entries through
	 */
	public CompletableFuture<HolderLookup.Provider> lookup() {
		return this.event.getLookupProvider();
	}

	/**
	 * @return The helper a model provider checks referenced files against
	 */
	public ExistingFileHelper existingFileHelper() {
		return this.event.getExistingFileHelper();
	}
}
