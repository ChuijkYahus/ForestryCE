package forestry.core.multiblock;

import forestry.api.ForestryConstants;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

/**
 * In your mod, subscribe this on both the client and server sides side to handle chunk
 * load events for your multiblock machines.
 * Chunks can load asynchronously in environments like MCPC+, so we cannot
 * process any blocks that are in chunks which are still loading.
 */
@Mod.EventBusSubscriber(modid = ForestryConstants.MOD_ID)
public class MultiblockEventHandler {
	@SubscribeEvent
	public static void onChunkLoad(ChunkEvent.Load loadEvent) {
		ChunkAccess chunk = loadEvent.getChunk();
		LevelAccessor world = loadEvent.getLevel();
		MultiblockRegistry.onChunkLoaded(world, chunk.getPos().x, chunk.getPos().z);
	}

	// Cleanup, for nice memory usageness
	@SubscribeEvent
	public static void onWorldUnload(LevelEvent.Unload unloadEvent) {
		MultiblockRegistry.onWorldUnloaded(unloadEvent.getLevel());
	}
}
