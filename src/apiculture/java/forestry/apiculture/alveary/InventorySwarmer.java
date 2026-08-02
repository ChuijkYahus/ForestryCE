package forestry.apiculture.alveary;

import forestry.api.IForestryApi;
import forestry.apiculture.alveary.multiblock.TileAlvearySwarmer;
import forestry.core.platform.inventory.InventoryAdapterTile;
import net.minecraft.world.item.ItemStack;

public class InventorySwarmer extends InventoryAdapterTile<TileAlvearySwarmer> {
	public InventorySwarmer(TileAlvearySwarmer alvearySwarmer) {
		super(alvearySwarmer, 4, "SwarmInv");
	}

	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		return IForestryApi.INSTANCE.getHiveManager().getSwarmingMaterialChance(stack.getItem()) != 0f;
	}
}
