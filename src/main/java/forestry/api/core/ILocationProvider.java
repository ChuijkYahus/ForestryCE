package forestry.api.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Interface for things, that have a location.
 * Must not be named "getWorld" and "getPos" to avoid
 * SpecialSource issue https://github.com/md-5/SpecialSource/issues/12
 * TODO rename to getBlockPos, getLevel in 1.21
 */
public interface ILocationProvider {
	BlockPos getCoordinates();

	@Nullable
	Level getWorldObj();
}
