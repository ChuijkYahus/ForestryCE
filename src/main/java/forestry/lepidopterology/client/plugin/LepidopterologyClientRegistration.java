package forestry.lepidopterology.client.plugin;

import java.util.function.Consumer;

import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.lepidopterology.ForestryButterflySpecies;
import forestry.plugin.client.ButterflyAnalyzerPlugin;
import net.minecraft.resources.ResourceLocation;
import forestry.api.client.plugin.IClientRegistration;

/**
 * Base Forestry's lepidopterology client registrations. Split out of
 * base Forestry's default plugin so the base artifact does not name
 * lepidopterology client types.
 */
public class LepidopterologyClientRegistration implements Consumer<IClientRegistration> {
	@Override
	public void accept(IClientRegistration client) {

		client.setAnalyzerPlugin(ForestrySpeciesTypes.BUTTERFLY, new ButterflyAnalyzerPlugin());

		// Register the default item/entity texture naming convention for every built-in butterfly species, so
		// ButterflyItemModel can bake a per-species model instead of falling back to the default (cabbage white)
		// for all of them. Sourced from the static compile-time id list, not the live/reloadable species map.
		for (ResourceLocation speciesId : ForestryButterflySpecies.ALL) {
			registerDefaultButterflyTextures(client, speciesId);
		}
	}

	private static void registerDefaultButterflyTextures(IClientRegistration client, ResourceLocation speciesId) {
		// Mirrors ButterflyClientManager#defaultTexturesFor's render-time fallback naming convention.
		String path = speciesId.getPath().replace("butterfly_", "");
		ResourceLocation itemTexture = ResourceLocation.fromNamespaceAndPath(speciesId.getNamespace(), "item/butterfly/" + path);
		ResourceLocation entityTexture = ResourceLocation.fromNamespaceAndPath(speciesId.getNamespace(), "textures/entity/butterfly/" + path + ".png");
		client.setButterflySprites(speciesId, itemTexture, entityTexture);
	}
}
