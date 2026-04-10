package forestry.compat.curios;

import forestry.core.utils.GeneticsUtil;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public class CuriosCompat {
	public static final boolean IS_LOADED = ModList.get().isLoaded("curios");

	public static boolean hasNaturalistEye(Player player) {
		return CuriosApi.getCuriosInventory(player).map(inventory -> inventory.getStacksHandler("head").map(handler -> {
			IDynamicStackHandler stacks = handler.getStacks();

			for (int i = 0; i < stacks.getSlots(); i++) {
				if (GeneticsUtil.hasNaturalistEye(player, stacks.getStackInSlot(i))) {
					return true;
				}
			}

			return false;
		}).orElse(false)).orElse(false);
	}
}
