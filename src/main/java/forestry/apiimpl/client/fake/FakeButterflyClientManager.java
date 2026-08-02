package forestry.apiimpl.client.fake;

import java.util.Collection;
import java.util.List;

import com.mojang.datafixers.util.Pair;

import net.minecraft.resources.ResourceLocation;

import forestry.api.client.lepidopterology.IButterflyClientManager;
import forestry.api.lepidopterology.genetics.IButterflySpecies;

/**
 * The butterfly client manager used when the lepidopterology module is absent. Every texture
 * resolves to the missing one, and nothing is registered to iterate.
 */
public enum FakeButterflyClientManager implements IButterflyClientManager {
	INSTANCE;

	private static final ResourceLocation MISSING = ResourceLocation.withDefaultNamespace("missingno");
	private static final Pair<ResourceLocation, ResourceLocation> MISSING_TEXTURES = Pair.of(MISSING, MISSING);

	@Override
	public boolean isLoaded() {
		return false;
	}

	@Override
	public Pair<ResourceLocation, ResourceLocation> getTextures(IButterflySpecies species) {
		return MISSING_TEXTURES;
	}

	@Override
	public Pair<ResourceLocation, ResourceLocation> getDefaultTextures() {
		return MISSING_TEXTURES;
	}

	@Override
	public Collection<Pair<ResourceLocation, ResourceLocation>> getAllTextures() {
		return List.of();
	}
}
