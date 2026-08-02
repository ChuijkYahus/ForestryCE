package forestry.core.gui;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface IContainerLiquidTanks extends IContainerTank {
	@OnlyIn(Dist.CLIENT)
	void handlePipetteClickClient(int slot, Player player);

	void handlePipetteClick(int slot, ServerPlayer player);
}
