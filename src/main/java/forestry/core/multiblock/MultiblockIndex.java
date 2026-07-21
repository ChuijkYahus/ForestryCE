package forestry.core.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A per-{@link Level} map of active multiblock machines, keyed by the holder (anchor) {@link BlockPos}
 * (plan Task 2.1, spec 6.1 and 7.1). This is the lightweight replacement for the deleted
 * {@code MultiblockRegistry}, {@code MultiblockWorldRegistry} and {@code MultiblockServerTickHandler}
 * stack. It does <em>no</em> ticking, no flood-fill, no merge or split, and no chunk bookkeeping. It is
 * purely a lookup so a member BE can resolve its controller from its stored {@code anchorPos} (Task 2.4),
 * and the event-driven triggers (Task 2.5) can register and deregister a controller as it (de)assembles.
 *
 * <p><b>Threading.</b> Chunk load and unload and the BE lifecycle run on the main server thread in 1.20.1
 * (spec constraint), so this uses plain {@link HashMap}s with no synchronization. All access is on the
 * main thread. There is one inner map per {@code Level}. Server and client levels are distinct keys, so
 * the client's own validation and ticking is naturally separated (spec 9).
 *
 * <p>This class is intentionally inert in Task 2.1. Nothing populates or consults it yet. It is wired in
 * by the controller and BE rework (Tasks 2.4 and 2.5).
 */
public final class MultiblockIndex {
	// Level -> (holderPos -> controller), keyed by Level identity since server and client levels differ
	private static final Map<LevelAccessor, Map<BlockPos, MultiblockController>> LEVELS = new HashMap<>();

	private MultiblockIndex() {
	}

	private static Map<BlockPos, MultiblockController> mapFor(LevelAccessor level) {
		return LEVELS.computeIfAbsent(level, l -> new HashMap<>());
	}

	/**
	 * Registers, or replaces, the controller whose holder is at the given anchor position. Called on
	 * (re)assembly and on a 6.4 re-anchor hand-off, with the new holder position.
	 */
	public static void register(Level level, BlockPos holderPos, MultiblockController controller) {
		mapFor(level).put(holderPos.immutable(), controller);
	}

	/**
	 * Removes the entry at the given holder position, on deactivation or full dismantle. A no-op if absent.
	 * Returns the removed controller, or {@code null}.
	 */
	@Nullable
	public static MultiblockController deregister(Level level, BlockPos holderPos) {
		Map<BlockPos, MultiblockController> map = LEVELS.get(level);
		if (map == null) {
			return null;
		}
		MultiblockController removed = map.remove(holderPos.immutable());
		if (map.isEmpty()) {
			LEVELS.remove(level);
		}
		return removed;
	}

	/**
	 * Resolves the active controller hosted at the given holder position, or {@code null} if none. A member
	 * BE passes its stored {@code anchorPos} here to find its controller (Task 2.4).
	 */
	@Nullable
	public static MultiblockController get(Level level, BlockPos holderPos) {
		Map<BlockPos, MultiblockController> map = LEVELS.get(level);
		return map == null ? null : map.get(holderPos.immutable());
	}

	/** All active controllers in the given level, as an unmodifiable snapshot view. */
	public static Collection<MultiblockController> getControllers(Level level) {
		Map<BlockPos, MultiblockController> map = LEVELS.get(level);
		return map == null ? Collections.emptyList() : Collections.unmodifiableCollection(map.values());
	}

	/** Drops all tracking for a level. Called when the level is unloaded. */
	public static void clear(LevelAccessor level) {
		LEVELS.remove(level);
	}
}
