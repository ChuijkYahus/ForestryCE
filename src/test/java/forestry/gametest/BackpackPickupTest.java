package forestry.gametest;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.storage.PickupHandlerStorage;

/**
 * Regression suite for {@link PickupHandlerStorage#onItemPickup} respecting vanilla's pickup rules.
 * <p>
 * The handler runs from {@code ModuleStorage.onItemPickup}, a listener on Forge's {@code EntityItemPickupEvent}. In
 * 1.20.1 {@code ItemEntity.playerTouch} fires that event BEFORE it consults {@code ItemEntity.target}:
 * <pre>
 *     if (this.pickupDelay &gt; 0) return;                                  // pickupDelay guarded here
 *     int hook = ForgeEventFactory.onItemPickup(this, entity);           // Forestry runs here
 *     if (hook &lt; 0) return;
 *     if (this.pickupDelay == 0 &amp;&amp; (this.target == null || this.target.equals(entity.getUUID())) &amp;&amp; ...)
 * </pre>
 * so an item reserved for another player is still handed to Forestry, which tops it off into this player's inventory
 * and stows the rest in their backpacks. Vanilla then declines to complete the pickup — but the items are already gone.
 * <p>
 * The tests drive {@code onItemPickup} directly rather than simulating a collision, because that is the method the
 * guard lives in and it keeps the assertions off entity-movement timing.
 * <p>
 * No backpack is involved: {@code topOffPlayerInventory} runs before any backpack filter is consulted and is enough on
 * its own to consume the stack, so a single partial stack in the player's inventory reproduces the theft. That also
 * keeps these tests independent of which items each backpack definition's tag filter happens to accept.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class BackpackPickupTest {
	private static final BlockPos ITEM_POS = new BlockPos(8, 2, 8);

	// Cobblestone stacks to 64, so 32 held + 16 dropped merges without overflow.
	private static final int HELD_COUNT = 32;
	private static final int DROPPED_COUNT = 16;

	@GameTest(template = "empty")
	public static void pickupIsRefusedForAnItemTargetedAtAnotherPlayer(GameTestHelper helper) {
		Player player = playerHoldingPartialStack(helper);
		ItemEntity item = droppedStack(helper);
		// Reserved for somebody else. Vanilla would refuse to give it to this player.
		item.setTarget(UUID.randomUUID());

		boolean handled = PickupHandlerStorage.onItemPickup(player, item);

		helper.assertTrue(!handled, "onItemPickup claimed an item targeted at another player");
		assertNothingWasTaken(helper, player, item);
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void pickupIsRefusedWhileTheItemStillHasPickupDelay(GameTestHelper helper) {
		Player player = playerHoldingPartialStack(helper);
		ItemEntity item = droppedStack(helper);
		// Freshly tossed items are untouchable for two seconds.
		item.setPickUpDelay(40);

		boolean handled = PickupHandlerStorage.onItemPickup(player, item);

		helper.assertTrue(!handled, "onItemPickup claimed an item that still had pickup delay");
		assertNothingWasTaken(helper, player, item);
		helper.succeed();
	}

	/**
	 * Guards the fix against over-blocking: an ordinary unreserved, delay-free item must still be picked up.
	 */
	@GameTest(template = "empty")
	public static void ordinaryItemsAreStillPickedUp(GameTestHelper helper) {
		Player player = playerHoldingPartialStack(helper);
		ItemEntity item = droppedStack(helper);

		boolean handled = PickupHandlerStorage.onItemPickup(player, item);

		helper.assertTrue(handled, "onItemPickup refused an ordinary item with no target and no pickup delay");
		helper.assertTrue(item.getItem().isEmpty(),
			"expected the dropped stack to be fully consumed, " + item.getItem().getCount() + " left");
		int held = player.getInventory().getItem(0).getCount();
		helper.assertTrue(held == HELD_COUNT + DROPPED_COUNT,
			"expected the held stack to be topped off to " + (HELD_COUNT + DROPPED_COUNT) + ", found " + held);
		helper.succeed();
	}

	/**
	 * The 1.20.1 counterpart of the 1.21.1 "pickup animation and sound lost for topped-off stacks" fix, asserting the
	 * bug is <em>absent</em> here.
	 * <p>
	 * On NeoForge the handler answers {@code ItemEntityPickupEvent.Pre} with {@code setCanPickup(FALSE)}, which skips
	 * vanilla's completion entirely — so nothing ever called {@code take()} and the client got no packet. Forge 1.20.1
	 * instead reports {@code Result.ALLOW}, and {@code ItemEntity.playerTouch} still runs {@code entity.take(this, i)}
	 * and {@code discard()} afterwards. {@code take()} is what broadcasts {@code ClientboundTakeItemEntityPacket}, and
	 * that packet is the sole source of both the fly-to-player animation and {@code SoundEvents.ITEM_PICKUP}, so
	 * exercising the real event path and observing take+discard is equivalent to observing the effects on a client.
	 */
	@GameTest(template = "empty")
	public static void toppedOffStackIsTakenAndDiscardedThroughTheVanillaPath(GameTestHelper helper) {
		Player player = playerHoldingPartialStack(helper);
		ItemEntity item = droppedStack(helper);

		// The real path: fires EntityItemPickupEvent, which ModuleStorage listens to.
		item.playerTouch(player);

		int held = player.getInventory().getItem(0).getCount();
		helper.assertTrue(held == HELD_COUNT + DROPPED_COUNT,
			"expected the held stack to be topped off to " + (HELD_COUNT + DROPPED_COUNT) + ", found " + held);
		// discard() only runs inside the same branch as take(), so a removed entity proves the packet was broadcast.
		helper.assertTrue(item.isRemoved(),
			"the item entity survived a completed pickup: vanilla's take()/discard() branch was skipped, so no "
				+ "ClientboundTakeItemEntityPacket was sent and the client saw neither animation nor sound");
		helper.succeed();
	}

	private static Player playerHoldingPartialStack(GameTestHelper helper) {
		Player player = helper.makeMockSurvivalPlayer();
		// A partial stack is what topOffPlayerInventory merges into; an empty inventory would leave it a no-op.
		player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, HELD_COUNT));
		return player;
	}

	private static ItemEntity droppedStack(GameTestHelper helper) {
		ItemEntity item = helper.spawnItem(Items.COBBLESTONE, ITEM_POS);
		item.setItem(new ItemStack(Items.COBBLESTONE, DROPPED_COUNT));
		return item;
	}

	private static void assertNothingWasTaken(GameTestHelper helper, Player player, ItemEntity item) {
		int left = item.getItem().getCount();
		helper.assertTrue(left == DROPPED_COUNT,
			"expected the dropped stack to be untouched at " + DROPPED_COUNT + ", found " + left);

		int held = player.getInventory().getItem(0).getCount();
		helper.assertTrue(held == HELD_COUNT,
			"expected the held stack to be untouched at " + HELD_COUNT + ", found " + held
				+ ": topOffPlayerInventory ran on an item the player may not pick up");
	}
}
