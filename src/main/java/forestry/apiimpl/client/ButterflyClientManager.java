package forestry.apiimpl.client;

import com.mojang.datafixers.util.Pair;
import forestry.api.client.lepidopterology.IButterflyClientManager;
import forestry.api.lepidopterology.genetics.IButterflySpecies;
import net.minecraft.resources.ResourceLocation;

import java.util.IdentityHashMap;

public class ButterflyClientManager implements IButterflyClientManager {
	private final IdentityHashMap<IButterflySpecies, Pair<ResourceLocation, ResourceLocation>> textures;

	public ButterflyClientManager(IdentityHashMap<IButterflySpecies, Pair<ResourceLocation, ResourceLocation>> textures) {
		this.textures = textures;
	}

	@Override
	public Pair<ResourceLocation, ResourceLocation> getTextures(IButterflySpecies species) {
		return this.textures.get(species);
	}
}
