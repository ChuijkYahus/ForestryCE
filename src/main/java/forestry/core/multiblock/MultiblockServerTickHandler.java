package forestry.core.multiblock;

import forestry.api.ForestryConstants;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * This is a generic multiblock tick handler. If you are using this code on your own,
 * you will need to register this with the Forge TickRegistry on both the
 * client AND server sides.
 * Note that different types of ticks run on different parts of the system.
 * CLIENT ticks only run on the client, at the start/end of each game loop.
 * SERVER and WORLD ticks only run on the server.
 * WORLDLOAD ticks run only on the server, and only when worlds are loaded.
 */
@EventBusSubscriber(modid = ForestryConstants.MOD_ID)
public class MultiblockServerTickHandler {

	@SubscribeEvent
	public static void onWorldTick(LevelTickEvent.Pre event) {
		MultiblockRegistry.tickStart(event.getLevel());
	}
}
