package forestry.core.engine.genetics.root;

import com.mojang.authlib.GameProfile;
import forestry.api.core.genetics.IBreedingTracker;
import forestry.api.core.genetics.ISpeciesType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;

public class ServerBreedingHandler implements BreedingTrackerManager.SidedHandler {
	@Override
	@SuppressWarnings("unchecked")
	public <T extends IBreedingTracker> T getTracker(ISpeciesType<?, ?> type, LevelAccessor level, @Nullable GameProfile profile) {
		String filename = type.getBreedingTrackerFile(profile);
		ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
		SavedData.Factory<SavedData> factory = new SavedData.Factory<>(
			() -> (SavedData) type.createBreedingTracker(),
			(tag, registries) -> (SavedData) type.createBreedingTracker(tag)
		);
		T tracker = (T) overworld.getDataStorage().computeIfAbsent(factory, filename);
		type.initializeBreedingTracker(tracker, overworld, profile);
		return tracker;
	}
}
