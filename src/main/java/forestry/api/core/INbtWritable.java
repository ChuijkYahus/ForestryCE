package forestry.api.core;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public interface INbtWritable {
	default CompoundTag write(CompoundTag nbt, HolderLookup.Provider registries) {
		return write(nbt);
	}

	@Deprecated(forRemoval = true)
	default CompoundTag write(CompoundTag nbt) {
		return nbt;
	}
}
