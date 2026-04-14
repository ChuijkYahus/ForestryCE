package forestry.core.inventory;

import com.google.common.base.Preconditions;
import forestry.core.tiles.IFilterSlotDelegate;
import forestry.core.utils.InventoryUtil;
import forestry.core.utils.NBTUtilForestry;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import javax.annotation.Nullable;
import java.util.Random;

public abstract class ItemInventory implements Container, IFilterSlotDelegate {
	private static final String KEY_SLOTS = "Slots";
	private static final String KEY_UID = "UID";
	private static final Random rand = new Random();

	private final IItemHandler itemHandler = new InvWrapper(this);

	protected final Player player;
	private ItemStack parent;    //TODO not final any more. Is this a problem
	private final NonNullList<ItemStack> inventoryStacks;

	public ItemInventory(Player player, int size, ItemStack parent) {
		Preconditions.checkArgument(!parent.isEmpty(), "Parent cannot be empty.");

		this.player = player;
		this.parent = parent;
		this.inventoryStacks = NonNullList.withSize(size, ItemStack.EMPTY);

		CompoundTag nbt = NBTUtilForestry.getItemStackTag(parent);
		if (nbt == null) {
			nbt = new CompoundTag();
		}
		setUID(nbt); // Set a uid to identify the itemStack on SMP
		NBTUtilForestry.setItemStackTag(parent, nbt);

		CompoundTag nbtSlots = nbt.getCompound(KEY_SLOTS);
		for (int i = 0; i < this.inventoryStacks.size(); i++) {
			String slotKey = getSlotNBTKey(i);
			if (nbtSlots.contains(slotKey)) {
				CompoundTag itemNbt = nbtSlots.getCompound(slotKey);
				ItemStack itemStack = InventoryUtil.deserializeItemStack(itemNbt);
				this.inventoryStacks.set(i, itemStack);
			} else {
				this.inventoryStacks.set(i, ItemStack.EMPTY);
			}
		}
	}

	public static int getOccupiedSlotCount(ItemStack itemStack) {
		CompoundTag nbt = NBTUtilForestry.getItemStackTag(itemStack);
		if (nbt == null) {
			return 0;
		}

		CompoundTag slotNbt = nbt.getCompound(KEY_SLOTS);
		return slotNbt.size();
	}

	private void setUID(CompoundTag nbt) {
		if (!nbt.contains(KEY_UID)) {
			nbt.putInt(KEY_UID, rand.nextInt());
		}
	}

	public boolean isParentItemInventory(ItemStack itemStack) {
		ItemStack parent = getParent();
		return isSameItemInventory(parent, itemStack);
	}

	protected ItemStack getParent() {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack held = this.player.getItemInHand(hand);
			if (isSameItemInventory(held, this.parent)) {
				return held;
			}
		}
		return this.parent;
	}

	protected void setParent(ItemStack parent) {
		this.parent = parent;
	}

	@Nullable
	protected InteractionHand getHand() {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack held = this.player.getItemInHand(hand);
			if (isSameItemInventory(held, this.parent)) {
				return hand;
			}
		}
		return null;
	}

	private static boolean isSameItemInventory(ItemStack base, ItemStack comparison) {
		if (base.isEmpty() || comparison.isEmpty()) {
			return false;
		}

		if (base.getItem() != comparison.getItem()) {
			return false;
		}

		CompoundTag baseTagCompound = NBTUtilForestry.getItemStackTag(base);
		CompoundTag comparisonTagCompound = NBTUtilForestry.getItemStackTag(comparison);
		if (baseTagCompound == null || comparisonTagCompound == null) {
			return false;
		}

		if (!baseTagCompound.contains(KEY_UID) || !comparisonTagCompound.contains(KEY_UID)) {
			return false;
		}

		int baseUID = baseTagCompound.getInt(KEY_UID);
		int comparisonUID = comparisonTagCompound.getInt(KEY_UID);
		return baseUID == comparisonUID;
	}

	private void writeToParentNBT() {
		ItemStack parent = getParent();

		CompoundTag nbt = NBTUtilForestry.getItemStackTag(parent);
		if (nbt == null) {
			nbt = new CompoundTag();
		}

		CompoundTag slotsNbt = new CompoundTag();
		for (int i = 0; i < getContainerSize(); i++) {
			ItemStack itemStack = getItem(i);
			if (!itemStack.isEmpty()) {
				String slotKey = getSlotNBTKey(i);
				CompoundTag itemNbt = InventoryUtil.serializeItemStack(itemStack);
				slotsNbt.put(slotKey, itemNbt);
			}
		}

		nbt.put(KEY_SLOTS, slotsNbt);
		onWriteNBT(nbt);
		NBTUtilForestry.setItemStackTag(parent, nbt);
	}

	private static String getSlotNBTKey(int i) {
		return Integer.toString(i, Character.MAX_RADIX);
	}

	protected void onWriteNBT(CompoundTag nbt) {
	}

	public void onSlotClick(int slotIndex, Player player) {
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack itemstack : this.inventoryStacks) {
			if (!itemstack.isEmpty()) {
				return false;
			}
		}

		return true;
	}


	@Override
	public ItemStack removeItem(int index, int count) {
		ItemStack itemstack = ContainerHelper.removeItem(this.inventoryStacks, index, count);

		if (!itemstack.isEmpty()) {
			this.setChanged();
		}

		return itemstack;
	}

	@Override
	public void setItem(int index, ItemStack itemstack) {
		this.inventoryStacks.set(index, itemstack);

		ItemStack parent = getParent();

		CompoundTag nbt = NBTUtilForestry.getItemStackTag(parent);
		if (nbt == null) {
			nbt = new CompoundTag();
		}

		CompoundTag slotNbt;
		if (!nbt.contains(KEY_SLOTS)) {
			slotNbt = new CompoundTag();
			nbt.put(KEY_SLOTS, slotNbt);
		} else {
			slotNbt = nbt.getCompound(KEY_SLOTS);
		}

		String slotKey = getSlotNBTKey(index);

		if (itemstack.isEmpty()) {
			slotNbt.remove(slotKey);
		} else {
			slotNbt.put(slotKey, InventoryUtil.serializeItemStack(itemstack));
		}
		NBTUtilForestry.setItemStackTag(parent, nbt);
	}

	@Override
	public ItemStack getItem(int i) {
		return this.inventoryStacks.get(i);
	}

	@Override
	public int getContainerSize() {
		return this.inventoryStacks.size();
	}

	@Override
	public int getMaxStackSize() {
		return 64;
	}

	@Override
	public final void setChanged() {
		writeToParentNBT();
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public boolean canPlaceItem(int slotIndex, ItemStack itemStack) {
		return canSlotAccept(slotIndex, itemStack);
	}

	@Override
	public void startOpen(Player player) {
	}

	@Override
	public void stopOpen(Player player) {
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack toReturn = getItem(slot);

		if (!toReturn.isEmpty()) {
			setItem(slot, ItemStack.EMPTY);
		}

		return toReturn;
	}

	/* IFilterSlotDelegate */
	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		return true;
	}

	@Override
	public boolean isLocked(int slotIndex) {
		return false;
	}

	/* Fields */

	public IItemHandler getItemHandler() {
		return this.itemHandler;
	}

	@Override
	public void clearContent() {
		this.inventoryStacks.clear();
	}
}
