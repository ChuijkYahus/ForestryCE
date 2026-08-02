package forestry.api.client.arboriculture;

import com.mojang.datafixers.util.Pair;
import forestry.api.arboriculture.ITreeSpecies;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Tracks client-only data for tree species.
 */
public interface ITreeClientManager {
	/**
	 * Used to check whether the module that supplies this manager is installed. Base ships a no-op
	 * implementation of every manager whose module can be absent, so this returns {@code false}
	 * rather than the getter returning null or throwing.
	 *
	 * @return Whether a real implementation is installed
	 * @since 2.10.0
	 */
	default boolean isLoaded() {
		return true;
	}

	/**
	 * @return The leaf sprite for the given species.
	 */
	ILeafSprite getLeafSprite(@Nullable ITreeSpecies species);

	/**
	 * @return A collection of every leaf sprite that was registered.
	 */
	Collection<ILeafSprite> getAllLeafSprites();

	/**
	 * @return The leaf tint for the given species, or {@link ILeafTint#DEFAULT} if none was found.
	 */
	ILeafTint getTint(@Nullable ITreeSpecies species);

	/**
	 * @return A pair containing locations of the block and item models for this species, in that order.
	 */
	Pair<ResourceLocation, ResourceLocation> getSaplingModels(ITreeSpecies species);

	/**
	 * @return The block+item sapling model pair used when a species has no registered pair (defaults to oak's). Never
	 * requires the species list, so it is safe before/after a datapack species reload.
	 */
	Pair<ResourceLocation, ResourceLocation> getDefaultSaplingModels();

	Collection<Pair<ResourceLocation, ResourceLocation>> getAllSaplingModels();
}
