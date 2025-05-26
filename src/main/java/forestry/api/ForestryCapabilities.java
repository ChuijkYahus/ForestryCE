package forestry.api;

import forestry.api.apiculture.IArmorApiarist;
import forestry.api.core.IArmorNaturalist;
import forestry.api.genetics.capability.IIndividualHandlerItem;
import forestry.api.genetics.filter.IFilterLogic;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * All capabilities added by base Forestry.
 * <p>
 * If your mod does not require a dependency on Forestry, it is recommended to use your own CapabilityTokens instead
 * of the ones below, as recommended by {@link net.minecraftforge.common.capabilities.ForgeCapabilities}.
 */
public class ForestryCapabilities {
	// Apiculture
	public static Capability<IArmorApiarist> ARMOR_APIARIST = CapabilityManager.get(new CapabilityToken<>() {
	});

	// Arboriculture
	public static Capability<IArmorNaturalist> ARMOR_NATURALIST = CapabilityManager.get(new CapabilityToken<>() {
	});

	// Genetics
	public static Capability<IIndividualHandlerItem> INDIVIDUAL_HANDLER_ITEM = CapabilityManager.get(new CapabilityToken<>() {
	});

	// Genetic Filter
	public static Capability<IFilterLogic> FILTER_LOGIC = CapabilityManager.get(new CapabilityToken<>() {
	});
}
