package forestry.apiimpl.client.fake;

import java.util.Collection;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import forestry.api.apiculture.IBeeHousing;
import forestry.api.client.apiculture.IBeeClientManager;
import forestry.api.genetics.IGenome;
import forestry.api.genetics.ILifeStage;

/**
 * The bee client manager used when the apiculture module is absent. No model is registered, so
 * CoreClientHandler adds no extra baked models and bee effects draw no particles.
 */
public enum FakeBeeClientManager implements IBeeClientManager {
	INSTANCE;

	private static final ResourceLocation MISSING = ResourceLocation.withDefaultNamespace("missingno");

	@Override
	public boolean isLoaded() {
		return false;
	}

	@Override
	public ResourceLocation getModelLocation(ILifeStage stage, ResourceLocation speciesId) {
		return MISSING;
	}

	@Override
	public ResourceLocation getDefaultModelLocation(ILifeStage stage) {
		return MISSING;
	}

	@Override
	public Collection<ResourceLocation> getAllModelLocations(ILifeStage stage) {
		return List.of();
	}

	@Override
	public void addBeeHiveParticles(IBeeHousing housing, IGenome genome, List<BlockPos> flowerPositions) {
	}
}
