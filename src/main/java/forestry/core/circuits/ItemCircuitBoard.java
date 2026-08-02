package forestry.core.circuits;

import forestry.api.IForestryApi;
import forestry.api.core.circuits.ICircuit;
import forestry.api.core.circuits.ICircuitBoard;
import forestry.api.core.circuits.ICircuitLayout;
import forestry.core.features.CoreItems;
import forestry.core.items.ItemForestry;
import forestry.core.items.definitions.IColoredItem;
import forestry.core.utils.NBTUtilForestry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class ItemCircuitBoard extends ItemForestry implements IColoredItem {
	private final EnumCircuitBoardType type;

	public ItemCircuitBoard(EnumCircuitBoardType type) {
		this.type = type;
	}

	public EnumCircuitBoardType getType() {
		return this.type;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public int getColorFromItemStack(ItemStack itemstack, int tintIndex) {
		if (tintIndex == 0) {
			return this.type.getPrimaryColor();
		} else {
			return this.type.getSecondaryColor();
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		ICircuitBoard circuitboard = IForestryApi.INSTANCE.getCircuitManager().getCircuitBoard(stack);
		if (circuitboard != null) {
			circuitboard.addTooltip(list);
		}
	}

	public static ItemStack createCircuitboard(EnumCircuitBoardType type, @Nullable ICircuitLayout layout, ICircuit[] circuits) {
		CompoundTag compoundNBT = new CompoundTag();
		new CircuitBoard(type, layout, circuits).write(compoundNBT, RegistryAccess.EMPTY);
		ItemStack stack = CoreItems.CIRCUITBOARDS.stack(type, 1);
		NBTUtilForestry.setItemStackTag(stack, compoundNBT);
		return stack;
	}

	public ItemStack get(EnumCircuitBoardType type) {
		return CoreItems.CIRCUITBOARDS.stack(type, 1);
	}
}
