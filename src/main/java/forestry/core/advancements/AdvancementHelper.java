package forestry.core.advancements;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class AdvancementHelper {

	public static void tryUnlock(Player player, ResourceLocation id){
		if (player instanceof ServerPlayer serverPlayer){
			Advancement adv = serverPlayer.getServer().getAdvancements().getAdvancement(id);
			if (adv != null) {
				AdvancementProgress progress = serverPlayer.getAdvancements().getOrStartProgress(adv);
				for (String criterion: progress.getRemainingCriteria()){
					serverPlayer.getAdvancements().award(adv, criterion);
				}
			}
		}
	}

}
