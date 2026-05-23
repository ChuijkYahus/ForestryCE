package forestry.core.items;

import forestry.core.features.CoreDataComponents;
import forestry.core.gui.ContainerAlyzer;
import forestry.core.inventory.ItemInventoryAlyzer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ItemAlyzer extends ItemWithGui {
	public ItemAlyzer(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag advanced) {
		super.appendHoverText(stack, context, tooltip, advanced);
		int charges = stack.getOrDefault(CoreDataComponents.ALYZER_CHARGES, 0);
		tooltip.add(Component.translatable(stack.getDescriptionId() + ".charges", charges).withStyle(ChatFormatting.GOLD));
	}

	@Override
	public AbstractContainerMenu getContainer(int containerId, Player player, ItemStack heldItem) {
		return new ContainerAlyzer(containerId, new ItemInventoryAlyzer(player, heldItem), player);
	}
}
