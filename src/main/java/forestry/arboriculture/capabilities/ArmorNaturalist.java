package forestry.arboriculture.capabilities;

import forestry.api.core.IArmorNaturalist;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public enum ArmorNaturalist implements IArmorNaturalist {
	INSTANCE;

	@Override
	public boolean canSeePollination(Player player, ItemStack armor, boolean doSee) {
		return true;
	}
}
