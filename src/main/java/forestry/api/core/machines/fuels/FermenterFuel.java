package forestry.api.core.machines.fuels;

import net.minecraft.world.item.ItemStack;

/**
 * todo data driven
 *
 * @param item            Item that is a valid fuel for the fermenter (i.e. fertilizer).
 * @param fermentPerCycle How much is fermented per work cycle, i.e. how much biomass is produced per cycle.
 * @param burnDuration    Amount of work cycles a single item of this fuel lasts before expiring.
 */
public record FermenterFuel(ItemStack item, int fermentPerCycle, int burnDuration) {
}
