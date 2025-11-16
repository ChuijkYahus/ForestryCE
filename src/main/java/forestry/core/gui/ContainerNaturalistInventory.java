package forestry.core.gui;

import forestry.api.genetics.ISpeciesType;
import forestry.core.features.CoreMenuTypes;
import forestry.core.gui.slots.SlotFilteredInventory;
import forestry.core.tiles.IFilterSlotDelegate;
import forestry.core.tiles.TileNaturalistChest;
import forestry.core.tiles.TileUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerListener;

public class ContainerNaturalistInventory extends ContainerTile<TileNaturalistChest> implements IGuiSelectable, INaturalistMenu {
	public static final int MAX_PAGE = 5;
	private final int page;
	private boolean isFlipPage;

	public ContainerNaturalistInventory(int windowId, Inventory player, TileNaturalistChest tile, int page, boolean isFlipPage) {
		super(windowId, CoreMenuTypes.NATURALIST_INVENTORY.menuType(), player, tile, 18, 120);

		this.page = page;
		this.isFlipPage = isFlipPage;
		addInventory(this, tile, page);
	}

	public static <T extends Container & IFilterSlotDelegate> void addInventory(ContainerForestry container, T inventory, int selectedPage) {
		int page = Mth.clamp(selectedPage, 0, MAX_PAGE);
		for (int x = 0; x < 5; x++) {
			for (int y = 0; y < 5; y++) {
				int slot = y + page * 25 + x * 5;

				container.addSlot(new SlotFilteredInventory(inventory, slot, 100 + y * 18, 21 + x * 18));
			}
		}
	}

	public static ContainerNaturalistInventory fromNetwork(int windowId, Inventory playerInv, FriendlyByteBuf extraData) {
		TileNaturalistChest tile = TileUtil.getTile(playerInv.player.level(), extraData.readBlockPos(), TileNaturalistChest.class);
		return new ContainerNaturalistInventory(windowId, playerInv, tile, extraData.readVarInt(), extraData.readBoolean());
	}

	@Override
	public void handleSelectionRequest(ServerPlayer player, int primary, int secondary) {
        this.isFlipPage = true;
        this.tile.flipPage(player, (short) primary);
	}

	@Override
	public ISpeciesType<?, ?> getSpeciesType() {
		return this.tile.getSpeciesType();
	}

	@Override
	public int getCurrentPage() {
		return this.page;
	}

	@Override
	public void onFlipPage() {
		// stop chest from playing closing animation and sound
		this.isFlipPage = true;
	}

	@Override
	public void addSlotListener(ContainerListener listener) {
		super.addSlotListener(listener);

		// When a player opens a chest, they add a listener. The listener used to be the player itself, but now it's
		// a separate object that implements ContainerListener. Luckily, it's still declared as an anonymous class
		// inside of ServerPlayer, so we can identify it by its nest host. Hack fix for chests staying open :)
		if (listener.getClass().getNestHost() == ServerPlayer.class) {
			if (!this.isFlipPage) {
                this.tile.increaseNumPlayersUsing();
			} else {
				// set to false after flip is done
				this.isFlipPage = false;
			}
		}
	}

	@Override
	public void removed(Player player) {
		super.removed(player);

		if (!this.isFlipPage && player instanceof ServerPlayer) {
            this.tile.decreaseNumPlayersUsing();
		}
	}
}
