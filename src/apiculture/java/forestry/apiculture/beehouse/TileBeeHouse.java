package forestry.apiculture.beehouse;

import forestry.api.apiculture.IBeeHousingInventory;
import forestry.api.apiculture.IBeeListener;
import forestry.api.apiculture.IBeeModifier;
import forestry.apiculture.beehouse.BeehouseBeeModifier;
import forestry.apiculture.bees.InventoryBeeHousing;
import forestry.apiculture.features.ApicultureTiles;
import forestry.apiculture.bees.ContainerBeeHousing;
import forestry.apiculture.bees.GuiBeeHousing;
import forestry.core.platform.util.NetworkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import forestry.apiculture.bees.TileBeeHousingBase;

public class TileBeeHouse extends TileBeeHousingBase {
	private static final IBeeModifier beeModifier = new BeehouseBeeModifier();

	private final InventoryBeeHousing beeInventory;

	public TileBeeHouse(BlockPos pos, BlockState state) {
		super(ApicultureTiles.BEE_HOUSE.tileType(), pos, state, "bee.house");

        this.beeInventory = new InventoryBeeHousing(12);
        this.beeInventory.disableAutomation();
		setInternalInventory(this.beeInventory);
	}

	@Override
	public IBeeHousingInventory getBeeInventory() {
		return this.beeInventory;
	}

	@Override
	public Iterable<IBeeModifier> getBeeModifiers() {
		return List.of(beeModifier);
	}

	@Override
	public Iterable<IBeeListener> getBeeListeners() {
		return List.of();
	}

	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
		return new ContainerBeeHousing(windowId, player.getInventory(), this, false, GuiBeeHousing.Icon.BEE_HOUSE);
	}

	@Override
	public void openGui(ServerPlayer player, InteractionHand hand, BlockPos pos) {
		player.openMenu(this, buffer -> {
			buffer.writeBlockPos(pos);
			buffer.writeBoolean(false);
			NetworkUtil.writeEnum(buffer, GuiBeeHousing.Icon.BEE_HOUSE);
		});
	}
}
