package forestry.core.inventory;

import forestry.Forestry;
import forestry.api.ForestryTags;
import forestry.api.genetics.capability.IIndividualHandlerItem;
import forestry.core.blocks.BlockBurnBarrel;
import forestry.core.features.CoreItems;
import forestry.core.tiles.TileBurnBarrel;
import forestry.core.utils.SlotUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

import java.util.Optional;

public class InventoryBurnBarrel extends InventoryAdapterTile<TileBurnBarrel> {

	public static int SLOT_INPUT_1 = 0;
	public static int SLOT_INPUT_COUNT = 4;

	public static int SLOT_OUTPUT_1 = SLOT_INPUT_1 + SLOT_INPUT_COUNT;

	public static int SLOT_OUTPUT_COUNT = 3;

	public InventoryBurnBarrel(TileBurnBarrel tile) {
		super(tile, SLOT_INPUT_COUNT + SLOT_OUTPUT_COUNT, "Items");
	}

	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		if (SlotUtil.isSlotInRange(slotIndex, SLOT_INPUT_1, SLOT_INPUT_COUNT)) {
			return (!stack.is(ForestryTags.Items.BURN_BARREL_BLACKLIST)) && (ForgeHooks.getBurnTime(stack, null) > 0);
		}
		return false;
	}

	//Thought about restricting to only the bottom face, but like, go for gold I guess.
	@Override
	public boolean canTakeItemThroughFace(int slotIndex, ItemStack stack, Direction side) {
		return SlotUtil.isSlotInRange(slotIndex, SLOT_OUTPUT_1, SLOT_OUTPUT_COUNT);
	}

	@Override
	public void setItem(int slotId, ItemStack itemstack) {
		if (SlotUtil.isSlotInRange(slotId,
			InventoryBurnBarrel.SLOT_OUTPUT_1,
			InventoryBurnBarrel.SLOT_OUTPUT_COUNT)){
			if (countItem(CoreItems.ASH.item()) <= 0){
				//Forestry.LOGGER.info("Updating state to no ash");
				this.tile.getLevel().setBlock(this.tile.getBlockPos(),
					this.tile.getLevel().getBlockState(this.tile.getBlockPos())
						.setValue(BlockBurnBarrel.HAS_ASH, false), 3);
			}
		}
		super.setItem(slotId, itemstack);
	}
}

