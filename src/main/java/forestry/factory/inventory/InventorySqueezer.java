package forestry.factory.inventory;

import forestry.api.core.ForestryError;
import forestry.core.fluids.FluidHelper;
import forestry.core.fluids.ForestryFluids;
import forestry.core.fluids.TankManager;
import forestry.core.inventory.InventoryAdapterTile;
import forestry.core.inventory.wrappers.InventoryMapper;
import forestry.core.utils.InventoryUtil;
import forestry.core.utils.RecipeUtils;
import forestry.factory.tiles.TileSqueezer;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;

public class InventorySqueezer extends InventoryAdapterTile<TileSqueezer> {
	public static final short SLOT_RESOURCE_1 = 0;
	public static final short SLOTS_RESOURCE_COUNT = 9;
	public static final short SLOT_REMNANT = 9;
	public static final short SLOT_REMNANT_COUNT = 1;
	public static final short SLOT_CAN_INPUT = 10;
	public static final short SLOT_CAN_OUTPUT = 11;

	public InventorySqueezer(TileSqueezer squeezer) {
		super(squeezer, 12, "Items");
	}

	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		if (slotIndex == SLOT_CAN_INPUT) {
			return FluidHelper.isFillableEmptyContainer(stack) || stack.is(Items.GLASS_BOTTLE);
		}

		if (slotIndex >= SLOT_RESOURCE_1 && slotIndex < SLOT_RESOURCE_1 + SLOTS_RESOURCE_COUNT) {
			if (FluidHelper.isFillableEmptyContainer(stack)) {
				return false;
			}

			RecipeManager recipeManager = this.tile.getLevel().getRecipeManager();
			return RecipeUtils.isSqueezerIngredient(recipeManager, stack) || RecipeUtils.getSqueezerContainerRecipe(recipeManager, stack) != null;
		}

		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slotIndex, ItemStack itemstack, Direction side) {
		return slotIndex == SLOT_REMNANT || slotIndex == SLOT_CAN_OUTPUT;
	}

	public boolean hasResources() {
		return !InventoryUtil.isEmpty(this, SLOT_RESOURCE_1, SLOTS_RESOURCE_COUNT);
	}

	public List<ItemStack> getResources() {
		return InventoryUtil.getStacks(this, SLOT_RESOURCE_1, SLOTS_RESOURCE_COUNT);
	}

	public boolean removeResources(List<Ingredient> stacks) {
		Container inventory = new InventoryMapper(this, SLOT_RESOURCE_1, SLOTS_RESOURCE_COUNT);
		return InventoryUtil.consumeIngredients(inventory, stacks, null, false, false, true);
	}

	public boolean addRemnant(ItemStack remnant, boolean doAdd) {
		return InventoryUtil.tryAddStack(this, remnant, SLOT_REMNANT, SLOT_REMNANT_COUNT, true, doAdd);
	}

	public void fillContainers(FluidStack fluidStack, TankManager tankManager) {
		if (getItem(SLOT_CAN_INPUT).isEmpty()) {
			return;
		}
		//dirty dirty dirty!
		if (FluidHelper.isFillableEmptyContainer(getItem(SLOT_CAN_INPUT)))
			FluidHelper.fillContainers(tankManager, this, SLOT_CAN_INPUT, SLOT_CAN_OUTPUT, fluidStack.getFluid(), true);
		else {
			//I'm so sorry I literally don't know any way to make this better.
			//fillContainers(fluidHandler, inv, inputSlot, outputSlot, fluidToFill, getEmptyContainer(inv.getItem(inputSlot)), doFill);
			ItemStack input = this.getItem(SLOT_CAN_INPUT);
			if (input.isEmpty() || (!input.is(Items.GLASS_BOTTLE))) return;

			//First, check what the fluid is to determine the resultant item
			ItemStack result = null;
			int fluidAmount = 0;

			//GROSS DIRTY HARD CODED BLEHHHHH
			if (fluidStack.getFluid().isSame(ForestryFluids.EXPERIENCE.getFluid())) {
				result = new ItemStack(Items.EXPERIENCE_BOTTLE);
				fluidAmount = 250;
			}
			else if (fluidStack.getFluid().isSame(ForestryFluids.HONEY.getFluid())) {
				result = new ItemStack(Items.HONEY_BOTTLE);
				fluidAmount = 200; //This is because of the recipe for Bottled Honey taking 2 honeydrops which equate to 100mb each. It's... odd.
			}
			else if (fluidStack.getFluid().isSame(Fluids.WATER)) {
				result = new ItemStack(Items.HONEY_BOTTLE);
				fluidAmount = 250;
			}

			//Apparently if you put an invalid container in, it doesn't give an error
			//Tested by putting a wax capsule in with lava
			if (result == null) return;

			//Then, check if there is actually enough fluid to make this item.
			if (fluidStack.getAmount() < fluidAmount) return;

			//Then, check if the resultant item can be put into the output
			ItemStack out = this.getItem(SLOT_CAN_OUTPUT);
			if ((out.is(result.getItem()) && out.getCount() < result.getMaxStackSize()) || out.isEmpty()){
				//If it can, move it there
				this.setItem(SLOT_CAN_INPUT, input.copyWithCount(input.getCount()-1));
				tankManager.drain(new FluidStack(fluidStack.getFluid(), fluidAmount), IFluidHandler.FluidAction.EXECUTE);

				if (out.isEmpty()) {
					this.setItem(SLOT_CAN_OUTPUT, result);
				}
				else {
					int amount = this.getItem(SLOT_CAN_OUTPUT).getCount();
					this.setItem(SLOT_CAN_OUTPUT, result.copyWithCount(amount + 1));
				}
			}
		}
	}
}
