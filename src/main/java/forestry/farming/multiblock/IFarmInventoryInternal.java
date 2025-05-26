package forestry.farming.multiblock;

import forestry.api.farming.IFarmInventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;

public interface IFarmInventoryInternal extends IFarmInventory {
	int getFertilizerValue();

	boolean useFertilizer();

	void stowProducts(Iterable<ItemStack> harvested, ArrayDeque<ItemStack> pendingProduce);

	boolean tryAddPendingProduce(ArrayDeque<ItemStack> pendingProduce);
}
