package forestry.api.mail;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public interface ILetterHandler {
	IPostalState handleLetter(ServerLevel world, IMailAddress recipient, ItemStack letterStack, boolean doLodge);
}
