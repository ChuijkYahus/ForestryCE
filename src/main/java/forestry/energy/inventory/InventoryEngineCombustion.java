package forestry.energy.inventory;

import forestry.core.inventory.InventoryAdapterTile;
import forestry.energy.tiles.CombustionEngineBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

import java.util.Optional;

public class InventoryEngineCombustion extends InventoryAdapterTile<CombustionEngineBlockEntity> {
	public static final short SLOT_CAN = 0;

	public InventoryEngineCombustion(CombustionEngineBlockEntity engineIron) {
		super(engineIron, 1, "Items");
	}

	@Override
	public boolean canTakeItemThroughFace(int slotIndex, ItemStack stack, Direction side) {
		return true;
	}

	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		if (slotIndex == SLOT_CAN) {
			Optional<FluidStack> fluid = FluidUtil.getFluidContained(stack);
			return fluid.map(f -> this.tile.getTankManager().canFillFluidType(f)).orElse(false);
		}

		return false;
	}
}
