package forestry.core.gui.slots;

import forestry.api.client.ForestrySprites;
import forestry.core.tiles.IFilterSlotDelegate;
import net.minecraft.world.Container;

public class SlotLiquidIn extends SlotFiltered {
	public <T extends Container & IFilterSlotDelegate> SlotLiquidIn(T inventory, int slotIndex, int xPos, int yPos) {
		super(inventory, slotIndex, xPos, yPos);
		setBackgroundSprite(ForestrySprites.SLOT_LIQUID);
	}
}
