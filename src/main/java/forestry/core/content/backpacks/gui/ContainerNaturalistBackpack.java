package forestry.core.content.backpacks.gui;

import forestry.api.IForestryApi;
import forestry.api.core.genetics.ISpeciesType;
import forestry.core.platform.gui.ContainerItemInventory;
import forestry.core.platform.gui.ContainerNaturalistInventory;
import forestry.core.platform.gui.IGuiSelectable;
import forestry.core.platform.gui.INaturalistMenu;
import forestry.core.content.backpacks.features.BackpackMenuTypes;
import forestry.core.content.backpacks.inventory.PagedBackpackInventory;
import forestry.core.content.backpacks.items.BackpackItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ContainerNaturalistBackpack extends ContainerItemInventory<PagedBackpackInventory> implements IGuiSelectable, INaturalistMenu {
	private final int currentPage;
	private final ISpeciesType<?, ?> speciesRoot;

	public ContainerNaturalistBackpack(int windowId, Inventory inv, PagedBackpackInventory inventory, int selectedPage, ResourceLocation rootUid) {
		super(windowId, inventory, inv, 18, 120, BackpackMenuTypes.NATURALIST_BACKPACK.menuType());

		ContainerNaturalistInventory.addInventory(this, inventory, selectedPage);

		this.currentPage = selectedPage;
		this.speciesRoot = IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(rootUid);
	}

	public static ContainerNaturalistBackpack makeContainer(int windowId, Player player, ItemStack heldItem, int page, ResourceLocation typeId) {
		PagedBackpackInventory inventory = new PagedBackpackInventory(player, BackpackItem.SLOTS_BACKPACK_APIARIST, heldItem, typeId);
		return new ContainerNaturalistBackpack(windowId, player.getInventory(), inventory, page, typeId);
	}

	@Override
	public void handleSelectionRequest(ServerPlayer player, int primary, int secondary) {
        this.inventory.flipPage(player, (short) primary);
	}

	@Override
	public ISpeciesType<?, ?> getSpeciesType() {
		return this.speciesRoot;
	}

	@Override
	public int getCurrentPage() {
		return this.currentPage;
	}

	public static ContainerNaturalistBackpack fromNetwork(int windowId, Inventory playerInventory, FriendlyByteBuf buffer) {
		int page = buffer.readByte();
		ResourceLocation typeId = buffer.readResourceLocation();
		ItemStack parent = playerInventory.getSelected();

		return makeContainer(windowId, playerInventory.player, parent, page, typeId);
	}
}
