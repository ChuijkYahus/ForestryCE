package forestry.api.core;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public interface INbtReadable {
	default void read(CompoundTag nbt, HolderLookup.Provider registries) {
		read(nbt);
	}

	@Deprecated(forRemoval = true)
	default void read(CompoundTag nbt) {
	}
}
