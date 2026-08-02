package forestry.api.agriculture;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

public interface IFarmingManager {
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

	default List<IFarmable> getFarmables(ResourceLocation farmTypeId) {
		IFarmType farmType = getFarmType(farmTypeId);
		return farmType == null ? List.of() : farmType.getFarmables();
	}

	/**
	 * @return The value of the fertilizer when used in a farm.
	 */
	int getFertilizeValue(ItemStack stack);

	@Nullable
	IFarmType getFarmType(ResourceLocation id);
}
