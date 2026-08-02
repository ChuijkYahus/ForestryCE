package forestry.api;

import forestry.api.apiculture.IBeeProtection;
import forestry.api.core.ISpectacleVision;
import forestry.api.core.genetics.filter.IFilterLogic;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;

import static forestry.api.ForestryConstants.forestry;

/**
 * All capabilities added by base Forestry.
 */
public class ForestryCapabilities {
	/**
	 * Items with this capability can protect the wearer from harmful bee effects.
	 */
	public static final ItemCapability<IBeeProtection, Void> BEE_PROTECTION = ItemCapability.createVoid(forestry("bee_protection"), IBeeProtection.class);

	/**
	 * Grants the wearer the ability to see wild bee hives and pollinated leaves more easily.
	 */
	public static final ItemCapability<ISpectacleVision, Void> SPECTACLE_VISION = ItemCapability.createVoid(forestry("spectacle_vision"), ISpectacleVision.class);

	/**
	 * Genetic filters expose their configurable sorting logic through this block capability.
	 */
	public static final BlockCapability<IFilterLogic, Void> FILTER_LOGIC = BlockCapability.createVoid(forestry("filter_logic"), IFilterLogic.class);
}
