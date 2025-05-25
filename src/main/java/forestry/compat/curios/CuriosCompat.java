package forestry.compat.curios;

import net.minecraft.world.entity.player.Player;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

import net.minecraftforge.fml.ModList;

import forestry.core.utils.GeneticsUtil;

import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public class CuriosCompat {
	public static final boolean IS_LOADED = ModList.get().isLoaded("curios");

	public static final Capability<ICuriosItemHandler> CURIOS_INVENTORY = CapabilityManager.get(new CapabilityToken<>() {
	});

	public static boolean hasNaturalistEye(Player player) {
		return player.getCapability(CURIOS_INVENTORY).map(inventory -> inventory.getStacksHandler("head").map(handler -> {
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
