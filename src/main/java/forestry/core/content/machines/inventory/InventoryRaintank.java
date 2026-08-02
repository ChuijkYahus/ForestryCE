package forestry.core.content.machines.inventory;

import forestry.core.platform.inventory.InventoryAdapterTile;
import forestry.core.content.machines.tiles.TileRaintank;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.Optional;

public class InventoryRaintank extends InventoryAdapterTile<TileRaintank> {
	public static final short SLOT_RESOURCE = 0;
	public static final short SLOT_PRODUCT = 1;

	public InventoryRaintank(TileRaintank raintank) {
		super(raintank, 3, "Items");
	}

	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		if (slotIndex == SLOT_RESOURCE) {
			Optional<IFluidHandlerItem> fluidHandler = FluidUtil.getFluidHandler(stack);
			return fluidHandler.map(handler -> handler.fill(new FluidStack(Fluids.WATER, Integer.MAX_VALUE), IFluidHandler.FluidAction.SIMULATE) > 0).orElse(false);
		}
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slotIndex, ItemStack itemstack, Direction side) {
		return slotIndex == SLOT_PRODUCT;
	}
}
