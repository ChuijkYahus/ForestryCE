package forestry.energy.inventory;

import forestry.api.fuels.FuelManager;
import forestry.core.inventory.InventoryAdapterTile;
import forestry.core.utils.SlotUtil;
import forestry.energy.tiles.PeatEngineBlockEntity;
import forestry.energy.tiles.SolarEngineBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/**
 * This class is actually not needed nor is it ever instantiated because the Solar Engine doesn't have any inventory slots.
 */
public class InventoryEngineSolar extends InventoryAdapterTile<SolarEngineBlockEntity> {

	public InventoryEngineSolar(SolarEngineBlockEntity engineTin) {
		super(engineTin, 0, "Items");
	}

}
