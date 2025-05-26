package forestry.core.genetics.root;

import com.mojang.authlib.GameProfile;
import forestry.api.genetics.IBreedingTracker;
import forestry.api.genetics.ISpeciesType;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClientBreedingHandler extends ServerBreedingHandler {
	private final Map<ISpeciesType<?, ?>, IBreedingTracker> trackerByUID = new LinkedHashMap<>();

	@Override
	@SuppressWarnings("unchecked")
	public <T extends IBreedingTracker> T getTracker(ISpeciesType<?, ?> type, LevelAccessor level, @Nullable GameProfile profile) {
		if (level instanceof ServerLevel) {
			return super.getTracker(type, level, profile);
		}
		T tracker = (T) this.trackerByUID.computeIfAbsent(type, (key) -> type.createBreedingTracker());
		type.initializeBreedingTracker(tracker, Minecraft.getInstance().level, profile);
		return tracker;
	}
}
