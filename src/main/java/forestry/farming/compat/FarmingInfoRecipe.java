package forestry.farming.compat;

import forestry.api.circuits.ICircuit;
import forestry.api.farming.IFarmType;
import net.minecraft.world.item.ItemStack;

public record FarmingInfoRecipe(ItemStack tube, IFarmType properties, ICircuit circuit) {
}
