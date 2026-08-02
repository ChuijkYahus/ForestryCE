package forestry.apiimpl.client.fake;

import java.util.Collection;
import java.util.List;

import com.mojang.datafixers.util.Pair;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.client.arboriculture.ILeafSprite;
import forestry.api.client.arboriculture.ILeafTint;
import forestry.api.client.arboriculture.ITreeClientManager;

/**
 * The tree client manager used when the arboriculture module is absent. Leaves fall back to the
 * missing sprite and the vanilla foliage color, and no sapling model is registered.
 */
public enum FakeTreeClientManager implements ITreeClientManager {
	INSTANCE;

	private static final ResourceLocation MISSING = ResourceLocation.withDefaultNamespace("missingno");
	private static final Pair<ResourceLocation, ResourceLocation> MISSING_MODELS = Pair.of(MISSING, MISSING);

	@Override
	public boolean isLoaded() {
		return false;
	}

	@Override
	public ILeafSprite getLeafSprite(@Nullable ITreeSpecies species) {
		return ILeafSprite.MISSING;
	}

	@Override
	public Collection<ILeafSprite> getAllLeafSprites() {
		return List.of();
	}

	@Override
	public ILeafTint getTint(@Nullable ITreeSpecies species) {
		return ILeafTint.DEFAULT;
	}

	@Override
	public Pair<ResourceLocation, ResourceLocation> getSaplingModels(ITreeSpecies species) {
		return MISSING_MODELS;
	}

	@Override
	public Pair<ResourceLocation, ResourceLocation> getDefaultSaplingModels() {
		return MISSING_MODELS;
	}

	@Override
	public Collection<Pair<ResourceLocation, ResourceLocation>> getAllSaplingModels() {
		return List.of();
	}
}
