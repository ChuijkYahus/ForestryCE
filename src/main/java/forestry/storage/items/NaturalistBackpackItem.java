package forestry.storage.items;

import forestry.api.core.backpacks.EnumBackpackType;
import forestry.api.core.backpacks.IBackpackDefinition;
import forestry.storage.gui.ContainerNaturalistBackpack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class NaturalistBackpackItem extends BackpackItem {
	public final ResourceLocation typeId;

	public NaturalistBackpackItem(ResourceLocation typeId, IBackpackDefinition definition, Item.Properties properties) {
		super(definition, EnumBackpackType.NATURALIST, properties);
		this.typeId = typeId;
	}

	@Override
	protected void writeContainerData(ServerPlayer player, ItemStack stack, RegistryFriendlyByteBuf buffer) {
		buffer.writeByte(0);
		buffer.writeResourceLocation(this.typeId);
	}

	@Override
	public AbstractContainerMenu getContainer(int containerId, Player player, ItemStack heldItem) {
		return ContainerNaturalistBackpack.makeContainer(containerId, player, heldItem, 0, this.typeId);
	}
}
