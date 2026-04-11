package forestry.factory.inventory;


import forestry.Forestry;
import forestry.core.inventory.InventoryAdapterTile;
import forestry.core.inventory.wrappers.InventoryMapper;
import forestry.core.recipes.IngredientStack;
import forestry.core.utils.InventoryUtil;
import forestry.core.utils.RecipeUtils;
import forestry.core.utils.SlotUtil;
import forestry.factory.tiles.TileCarpenter;
import forestry.factory.tiles.TileSmelter;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidUtil;

import java.util.HashMap;
import java.util.List;

public class InventorySmelter extends InventoryAdapterTile<TileSmelter> {
	public final static int SLOT_INPUT_1 = 0;
	public final static int SLOT_INPUT_COUNT = 9;
	public final static int SLOT_OUTPUT = 10;

	public InventorySmelter(TileSmelter smelter) {
		super(smelter, 11, "Items");
	}

	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		return SlotUtil.isSlotInRange(slotIndex, SLOT_INPUT_1, SLOT_INPUT_COUNT);
	}

	@Override
	public boolean canTakeItemThroughFace(int slotIndex, ItemStack itemstack, Direction side) {
		return slotIndex == SLOT_OUTPUT;
	}

	public boolean hasResources() {
		return !InventoryUtil.isEmpty(this, SLOT_INPUT_1, SLOT_INPUT_COUNT);
	}
	public List<ItemStack> getResources() {
		return InventoryUtil.getStacks(this, SLOT_INPUT_1, SLOT_INPUT_COUNT);
	}


	public boolean addResult(ItemStack product, boolean doAdd) {
		return InventoryUtil.tryAddStack(this, product, SLOT_OUTPUT, 1, true, doAdd);
	}

	public boolean removeResources(List<IngredientStack> stacks) {
		Container inventory = new InventoryMapper(this, SLOT_INPUT_1, SLOT_INPUT_COUNT);

		HashMap<Integer, Integer> removeAmounts =  new HashMap<>();

		//Check we can actually remove all resources.
		for (IngredientStack ingredientStack: stacks){
			int amountToRemove = ingredientStack.getCount();

			for(int s = SLOT_INPUT_1; s < SLOT_INPUT_1 + SLOT_INPUT_COUNT-1; s++){
				//Found a valid stack.
				if (ingredientStack.getIngredient().test(inventory.getItem(s))){

					if (inventory.getItem(s).getCount() >= amountToRemove){
						//There are more than the amount required.
						removeAmounts.put(s, amountToRemove);
						amountToRemove = 0;
						break;
					}
					else {
						//Remove all that we can from this slot, and carry on.
						int delta = amountToRemove-inventory.getItem(s).getCount();

						removeAmounts.put(s, delta);
						amountToRemove -= delta;
					}
				}
			}

			//If we loop through all slots and we still haven't got enough resources, exit.
			if (amountToRemove > 0) return false;
		}

		//We have enough resources, now let's actually remove them
		for(Integer slot: removeAmounts.keySet()){
			ItemStack oldStack = inventory.getItem(slot);
			ItemStack newStack = oldStack.copyWithCount(oldStack.getCount()-removeAmounts.get(slot));
			inventory.setItem(slot, newStack);
		}
		return true;
	}
}
