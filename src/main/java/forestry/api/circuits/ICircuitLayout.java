package forestry.api.circuits;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public interface ICircuitLayout {
	/**
	 * unique ID for this circuit layout
	 */
	String getId();

	/**
	 * localized name for this circuit layout
	 */
	Component getName();

	/**
	 * localized string for how this circuit layout is used
	 */
	MutableComponent getUsage();

	/**
	 * Specifies where a circuit layout is used.
	 */
	ResourceLocation getSocketType();
}
