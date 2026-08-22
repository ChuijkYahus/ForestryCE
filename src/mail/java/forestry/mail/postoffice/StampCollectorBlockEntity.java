package forestry.mail.postoffice;

import forestry.api.mail.IStamps;
import forestry.api.core.IInventoryAdapter;
import forestry.core.platform.tile.TileBase;
import forestry.core.platform.util.InventoryUtil;
import forestry.mail.features.MailBlockEntities;
import forestry.mail.gui.StampCollectorMenu;
import forestry.mail.inventory.StampCollectorInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class StampCollectorBlockEntity extends TileBase implements Container {
	public StampCollectorBlockEntity(BlockPos pos, BlockState state) {
		super(MailBlockEntities.STAMP_COLLECTOR.tileType(), pos, state);
		setInternalInventory(new StampCollectorInventory(this));
	}

	@Override
	public void serverTick(Level level, BlockPos pos, BlockState state) {
		if (!updateOnInterval(20)) {
			return;
		}

		ItemStack stamp = null;

		IInventoryAdapter inventory = getInternalInventory();
		if (inventory.getItem(StampCollectorInventory.SLOT_FILTER).isEmpty()) {
			stamp = PostOffice.getOrCreate((ServerLevel) level).getAnyStamp(1);
		} else {
			ItemStack filter = inventory.getItem(StampCollectorInventory.SLOT_FILTER);
			if (filter.getItem() instanceof IStamps) {
				stamp = PostOffice.getOrCreate((ServerLevel) level).getAnyStamp(((IStamps) filter.getItem()).getPostage(filter), 1);
			}
		}

		if (stamp == null) {
			return;
		}

		// Store it.
		InventoryUtil.stowInInventory(stamp, inventory, true, StampCollectorInventory.SLOT_BUFFER_1, StampCollectorInventory.SLOT_BUFFER_COUNT);
	}

	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
		return new StampCollectorMenu(windowId, inv, this);
	}
}
