# Postage Data Map Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `IStamps` marker interface and `EnumPostage` with a synced NeoForge item data map, and rework `PostOffice`, `StampCollectorBlockEntity` and `TradeStation` to track stamp items rather than enum ordinals.

**Architecture:** `forestry.api.ForestryDataMaps.POSTAGE` is a `DataMapType<Item, Integer>` declared in the core jar and registered by the mail jar, so a third party declares stamp values with one JSON file and no code dependency. `PostageUtil` is the single reader. `PostageSelector` extracts the change-making solver out of `TradeStation` into a pure, directly testable unit. `PostOffice` stores `Object2IntOpenHashMap<Item>`.

**Tech Stack:** Minecraft 1.21.1, NeoForge 21.1.230, ModKit datagen, NeoForge GameTest (`./gradlew runGameTestServer`).

Design spec: `docs/superpowers/specs/2026-08-22-postage-data-map-design.md`

## Global Constraints

- Comment and Javadoc style is binding: see `CLAUDE.md`. **ASCII only** in comments and Javadoc, no em-dash, no curly quotes. Inline comments are terse fragments with no terminal period. Reuse the identical opening verb across parallel members.
- Tabs for indentation. Match the surrounding file's import style (`forestry.mail.*` files use unsorted single imports; `src/test` and newer core files group `net.minecraft` / `net.neoforged` / `forestry` with blank lines between).
- The mail source set compiles against `sourceSets.main.output` only. Core must never reference anything in `src/mail`.
- Data map ID is `forestry:postage`, in the `forestry` namespace, even though the mail jar registers it.
- The generated file lands under `src/generated/resources_mail/`, never `src/generated/resources/`.
- Never delete a type in the same task that still has callers. Every task must leave the tree compiling.
- Fast inner loop: `./gradlew compileJava compileMailJava compileTestJava`. Full test gate: `./gradlew runGameTestServer`.
- `GameTestHelper` has **no** `assertValueEqual`. The only assertions available are `assertTrue`, `assertFalse`, the block/entity ones, and `helper.fail(String)`. Every test below declares its own `assertEquals` helper for that reason. Do not reach for a JUnit assertion; `src/test` is a GameTest source set, not a JUnit one.
- Write `Comparator` chains as an explicit two-argument lambda. `Comparator.comparingInt(X::y).thenComparing(...)` does not infer its type parameter from the assignment target, and `thenComparing` with an implicit lambda is ambiguous between its `Function` and `Comparator` overloads.

---

## File Structure

**Created:**

| Path | Responsibility |
| --- | --- |
| `src/main/java/forestry/api/ForestryDataMaps.java` | The `POSTAGE` data map constant. Core jar, sibling of `ForestryCapabilities`. |
| `src/mail/java/forestry/mail/letters/PostageUtil.java` | The only reader of `POSTAGE`. `getPostage` / `isStamp` / `sumPostage`. |
| `src/mail/java/forestry/mail/letters/PostageSelector.java` | The change-making solver and the denomination snapshot, pure and testable. |
| `src/mail/java/forestry/mail/data/MailDataMapProvider.java` | Writes `postage.json` for Forestry's own stamps. |
| `src/test/java/forestry/gametest/PostageDataMapTest.java` | Data map registration, values, and `PostageUtil` behavior. |
| `src/test/java/forestry/gametest/PostOfficeStampVaultTest.java` | Legacy migration, save round-trip, the `setDirty` regression pin, drain order. |
| `src/test/java/forestry/gametest/PostageSelectorTest.java` | The three solver passes and the virtual-station denomination source. |

**Modified:**

| Path | Change |
| --- | --- |
| `src/mail/java/forestry/mail/ModuleMail.java` | Adds the `RegisterDataMapTypesEvent` listener. |
| `src/mail/java/forestry/mail/data/MailData.java` | Registers `MailDataMapProvider`. |
| `src/mail/java/forestry/mail/letters/Letter.java` | `isPostPaid` uses `PostageUtil.sumPostage`. |
| `src/mail/java/forestry/mail/tradestation/TradeStationBlockEntity.java` | `hasPostageMin` uses `PostageUtil.sumPostage`. |
| `src/mail/java/forestry/mail/inventory/TradeStationInventory.java` | Stamp slot predicate uses `PostageUtil.isStamp`. |
| `src/mail/java/forestry/mail/inventory/StampCollectorInventory.java` | Slot predicate uses `PostageUtil.isStamp`. |
| `src/main/java/forestry/api/mail/IPostOffice.java` | The two `EnumPostage` overloads become one `Item` overload. |
| `src/mail/java/forestry/mail/postoffice/PostOffice.java` | Item-keyed vault, migration, `setDirty` fix. |
| `src/mail/java/forestry/mail/postoffice/StampCollectorBlockEntity.java` | Exact-item filter. |
| `src/mail/java/forestry/mail/carriers/trading/TradeStation.java` | Delegates to `PostageSelector`, `removeStamps` takes stacks. |
| `src/mail/java/forestry/mail/letters/EnumStampDefinition.java` | `postage` becomes `int`, reverse lookup deleted. |
| `src/mail/java/forestry/mail/letters/ItemStamp.java` | Drops `implements IStamps`. |

**Deleted:** `src/main/java/forestry/api/mail/IStamps.java`, `src/main/java/forestry/api/mail/EnumPostage.java`

---

### Task 1: The postage data map

**Files:**
- Create: `src/main/java/forestry/api/ForestryDataMaps.java`
- Create: `src/mail/java/forestry/mail/data/MailDataMapProvider.java`
- Modify: `src/mail/java/forestry/mail/ModuleMail.java`
- Modify: `src/mail/java/forestry/mail/data/MailData.java`
- Test: `src/test/java/forestry/gametest/PostageDataMapTest.java`

**Interfaces:**
- Consumes: `ForestryConstants.forestry(String)`, `EnumStampDefinition.VALUES`, `EnumStampDefinition.getPostage()` (still returns `EnumPostage` in this task), `MailItems.STAMPS.item(EnumStampDefinition)`, `JarScope.addServer(DataProvider)`, `JarScope.output()`, `JarScope.lookup()`.
- Produces: `forestry.api.ForestryDataMaps.POSTAGE` of type `DataMapType<Item, Integer>`.

This task is purely additive. Nothing is deleted and `IStamps` still works.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/forestry/gametest/PostageDataMapTest.java`:

```java
package forestry.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.ForestryDataMaps;
import forestry.mail.features.MailItems;
import forestry.mail.letters.EnumStampDefinition;

/**
 * Guard for the postage data map. The data map is what lets another mod declare a stamp with one JSON
 * file and no dependency on the mail jar, so a data map that fails to register, or a generated file
 * that fails to load, silently turns every Forestry stamp into a worthless item.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class PostageDataMapTest {
	@GameTest(template = "empty")
	public static void everyForestryStampCarriesItsPostage(GameTestHelper helper) {
		List<String> broken = new ArrayList<>();

		for (EnumStampDefinition stamp : EnumStampDefinition.VALUES) {
			Item item = MailItems.STAMPS.item(stamp);
			Integer postage = item.builtInRegistryHolder().getData(ForestryDataMaps.POSTAGE);
			int expected = stamp.getPostage().getValue();

			if (postage == null) {
				broken.add(stamp.getSerializedName() + " has no postage entry at all");
			} else if (postage != expected) {
				broken.add(stamp.getSerializedName() + " is worth " + postage + " instead of " + expected);
			}
		}

		if (!broken.isEmpty()) {
			helper.fail(broken.size() + " stamp(s) did not resolve their postage:\n  " + String.join("\n  ", broken));
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void nonStampsCarryNoPostage(GameTestHelper helper) {
		helper.assertTrue(Items.PAPER.builtInRegistryHolder().getData(ForestryDataMaps.POSTAGE) == null,
			"Paper resolved a postage value, so the data map is matching items it should not");
		helper.succeed();
	}
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew compileTestJava`
Expected: FAIL, `cannot find symbol: class ForestryDataMaps`.

- [ ] **Step 3: Create the data map constant**

Create `src/main/java/forestry/api/ForestryDataMaps.java`:

```java
package forestry.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

import static forestry.api.ForestryConstants.forestry;

/**
 * All data maps added by base Forestry.
 */
public class ForestryDataMaps {
	/**
	 * The postage an item is worth when it is attached to a letter. An item with no entry is not a
	 * stamp. Mods add their own stamps with a data map file and no dependency on Forestry.
	 *
	 * <p>Registered by the mail jar, so the data map is absent when that jar is not installed. Nothing
	 * in base Forestry reads it.
	 *
	 * <p>A file goes under the namespace of the data map rather than the namespace of the mod adding
	 * to it, and every mod's file at that path is merged.
	 *
	 * Ex. {@code data/forestry/data_maps/item/postage.json}
	 */
	public static final DataMapType<Item, Integer> POSTAGE = DataMapType
		.builder(forestry("postage"), Registries.ITEM, ExtraCodecs.POSITIVE_INT)
		.synced(ExtraCodecs.POSITIVE_INT, false)
		.build();
}
```

`DataMapType.builder` takes the JSON codec as its third argument, and its type parameters are registry-first, so `build()` returns `DataMapType<Item, Integer>`. `synced` is required rather than optional: the slot predicates in Task 2 run on the client. It is non-mandatory so a vanilla client can still connect.

- [ ] **Step 4: Register the data map from the mail jar**

In `src/mail/java/forestry/mail/ModuleMail.java`, add these imports:

```java
import forestry.api.ForestryDataMaps;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
```

Add the listener next to the existing capability listener:

```java
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(ModuleMail::registerCapabilities);
		modBus.addListener(ModuleMail::registerDataMaps);
		NeoForge.EVENT_BUS.addListener(ModuleMail::handlePlayerLoggedIn);
	}

	private static void registerDataMaps(RegisterDataMapTypesEvent event) {
		event.register(ForestryDataMaps.POSTAGE);
	}
```

- [ ] **Step 5: Add the data map provider**

Create `src/mail/java/forestry/mail/data/MailDataMapProvider.java`:

```java
package forestry.mail.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.common.data.DataMapProvider;

import forestry.api.ForestryDataMaps;
import forestry.mail.features.MailItems;
import forestry.mail.letters.EnumStampDefinition;

/**
 * Generates the postage every Forestry stamp is worth. Other mods add their own stamps by shipping a
 * file of this shape, which is the whole point of the data map.
 */
public class MailDataMapProvider extends DataMapProvider {
	public MailDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
		super(output, lookup);
	}

	@Override
	protected void gather(HolderLookup.Provider provider) {
		var postage = builder(ForestryDataMaps.POSTAGE);

		for (EnumStampDefinition stamp : EnumStampDefinition.VALUES) {
			postage.add(MailItems.STAMPS.item(stamp).builtInRegistryHolder(), stamp.getPostage().getValue(), false);
		}
	}
}
```

In `src/mail/java/forestry/mail/data/MailData.java`, add the provider inside `addProviders`, after the existing `jar.addServer` line:

```java
		jar.addServer(new MailDataMapProvider(jar.output(), jar.lookup()));
```

`jar.addServer` routes through `RenamedProvider.under("mail")`, so the provider is keyed as `mail/Data Maps` and cannot collide with another jar's.

- [ ] **Step 6: Generate the data map file**

Run: `./gradlew runData`
Expected: BUILD SUCCESSFUL, and `src/generated/resources_mail/data/forestry/data_maps/item/postage.json` now exists.

Verify its contents:

Run: `cat src/generated/resources_mail/data/forestry/data_maps/item/postage.json`
Expected: a `values` object with seven entries, `forestry:stamp_1n` through `forestry:stamp_100n`, worth 1, 2, 5, 10, 20, 50 and 100.

If the file landed under `src/generated/resources/` instead, the provider was attached to the wrong `PackOutput`. Check that it was constructed with `jar.output()`.

- [ ] **Step 7: Run the test to verify it passes**

Run: `./gradlew runGameTestServer`
Expected: PASS. `PostageDataMapTest.everyForestryStampCarriesItsPostage` and `nonStampsCarryNoPostage` both succeed, and the rest of the suite stays green.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/forestry/api/ForestryDataMaps.java \
        src/mail/java/forestry/mail/data/MailDataMapProvider.java \
        src/mail/java/forestry/mail/data/MailData.java \
        src/mail/java/forestry/mail/ModuleMail.java \
        src/generated/resources_mail/data/forestry/data_maps \
        src/test/java/forestry/gametest/PostageDataMapTest.java
git commit -m "Add the forestry:postage data map"
```

---

### Task 2: PostageUtil and the read-only call sites

**Files:**
- Create: `src/mail/java/forestry/mail/letters/PostageUtil.java`
- Modify: `src/mail/java/forestry/mail/letters/Letter.java:140-151`
- Modify: `src/mail/java/forestry/mail/tradestation/TradeStationBlockEntity.java:216-234`
- Modify: `src/mail/java/forestry/mail/carriers/trading/TradeStation.java:383-401`
- Modify: `src/mail/java/forestry/mail/inventory/TradeStationInventory.java:63-66`
- Modify: `src/mail/java/forestry/mail/inventory/StampCollectorInventory.java:16-18`
- Test: `src/test/java/forestry/gametest/PostageDataMapTest.java`

**Interfaces:**
- Consumes: `ForestryDataMaps.POSTAGE` from Task 1.
- Produces:
  - `PostageUtil.getPostage(Item item)` returns `int`, 0 when the item has no entry
  - `PostageUtil.getPostage(ItemStack stack)` returns `int`, 0 for an empty stack
  - `PostageUtil.isStamp(ItemStack stack)` returns `boolean`
  - `PostageUtil.sumPostage(Iterable<ItemStack> stacks)` returns `int`, postage times count summed

`IStamps` still exists after this task and `ItemStamp` still implements it. Only the readers move.

- [ ] **Step 1: Write the failing test**

Append these two methods to `src/test/java/forestry/gametest/PostageDataMapTest.java`, and add the imports `net.minecraft.world.item.ItemStack`, `java.util.List` (already present) and `forestry.mail.letters.PostageUtil`:

```java
	@GameTest(template = "empty")
	public static void postageUtilReadsTheDataMap(GameTestHelper helper) {
		ItemStack tenner = MailItems.STAMPS.stack(EnumStampDefinition.P_10, 3);

		assertEquals(helper, PostageUtil.getPostage(tenner), 10, "postage of a 10n stamp");
		helper.assertTrue(PostageUtil.isStamp(tenner), "A 10n stamp did not read as a stamp");
		assertEquals(helper, PostageUtil.getPostage(new ItemStack(Items.PAPER)), 0, "postage of paper");
		helper.assertFalse(PostageUtil.isStamp(new ItemStack(Items.PAPER)), "Paper read as a stamp");
		assertEquals(helper, PostageUtil.getPostage(ItemStack.EMPTY), 0, "postage of an empty stack");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void sumPostageMultipliesByCount(GameTestHelper helper) {
		List<ItemStack> stamps = List.of(
			MailItems.STAMPS.stack(EnumStampDefinition.P_10, 3),
			MailItems.STAMPS.stack(EnumStampDefinition.P_1, 4),
			new ItemStack(Items.PAPER, 64),
			ItemStack.EMPTY);

		// 10*3 + 1*4, and neither the paper nor the empty stack contributes
		assertEquals(helper, PostageUtil.sumPostage(stamps), 34, "summed postage");
		helper.succeed();
	}

	private static void assertEquals(GameTestHelper helper, int actual, int expected, String what) {
		helper.assertTrue(actual == expected, what + " was " + actual + " instead of " + expected);
	}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew compileTestJava`
Expected: FAIL, `package forestry.mail.letters.PostageUtil does not exist` / `cannot find symbol: class PostageUtil`.

- [ ] **Step 3: Create PostageUtil**

Create `src/mail/java/forestry/mail/letters/PostageUtil.java`:

```java
package forestry.mail.letters;

import forestry.api.ForestryDataMaps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The only reader of the postage data map. Every place that used to test for {@code IStamps} and call
 * {@code getPostage} goes through here instead.
 */
public class PostageUtil {
	private PostageUtil() {
	}

	/**
	 * @param item The item to read the postage of
	 * @return The postage the item is worth, or zero when it is not a stamp
	 */
	public static int getPostage(Item item) {
		Integer postage = item.builtInRegistryHolder().getData(ForestryDataMaps.POSTAGE);
		return postage == null ? 0 : postage;
	}

	/**
	 * @param stack The stack to read the postage of
	 * @return The postage one item of the stack is worth, or zero when it is not a stamp
	 */
	public static int getPostage(ItemStack stack) {
		return stack.isEmpty() ? 0 : getPostage(stack.getItem());
	}

	/**
	 * @param stack The stack to test
	 * @return Whether the stack is worth postage
	 */
	public static boolean isStamp(ItemStack stack) {
		return getPostage(stack) > 0;
	}

	/**
	 * @param stacks The stacks to add up
	 * @return The total postage of every stack, counting stack size
	 */
	public static int sumPostage(Iterable<ItemStack> stacks) {
		int posted = 0;

		for (ItemStack stack : stacks) {
			posted += getPostage(stack) * stack.getCount();
		}

		return posted;
	}
}
```

- [ ] **Step 4: Move Letter.isPostPaid onto it**

In `src/mail/java/forestry/mail/letters/Letter.java`, replace the whole `isPostPaid` body:

```java
	@Override
	public boolean isPostPaid() {
		return PostageUtil.sumPostage(getPostage()) >= requiredPostage();
	}
```

Delete the now-unused `import forestry.api.mail.IStamps;` from that file. `PostageUtil` is in the same package, so it needs no import.

- [ ] **Step 5: Move TradeStationBlockEntity.hasPostageMin onto it**

In `src/mail/java/forestry/mail/tradestation/TradeStationBlockEntity.java`, replace the whole `hasPostageMin` body:

```java
	public boolean hasPostageMin(int postage) {
		return PostageUtil.sumPostage(InventoryUtil.getStacks(getInternalInventory(), TradeStation.SLOT_STAMPS_1, TradeStation.SLOT_STAMPS_COUNT)) >= postage;
	}
```

Add `import forestry.core.platform.util.InventoryUtil;` and `import forestry.mail.letters.PostageUtil;` if not already present, and delete `import forestry.api.mail.IStamps;`. If the `net.minecraft.world.Container` import becomes unused, delete it too.

- [ ] **Step 6: Move TradeStation.canPayPostage onto it**

In `src/mail/java/forestry/mail/carriers/trading/TradeStation.java`, replace the whole `canPayPostage` body:

```java
	private boolean canPayPostage(int postage) {
		return PostageUtil.sumPostage(InventoryUtil.getStacks(this.inventory, SLOT_STAMPS_1, SLOT_STAMPS_COUNT)) >= postage;
	}
```

Add `import forestry.mail.letters.PostageUtil;`. Leave the other `IStamps` uses in this file alone: Task 4 removes them.

- [ ] **Step 7: Move both slot predicates onto it**

In `src/mail/java/forestry/mail/inventory/TradeStationInventory.java`, in the stamp slot branch of `canSlotAccept`:

```java
		} else if (SlotUtil.isSlotInRange(slotIndex, TradeStation.SLOT_STAMPS_1, TradeStation.SLOT_STAMPS_COUNT)) {
			return PostageUtil.isStamp(stack);
		}
```

Add `import forestry.mail.letters.PostageUtil;` and delete `import forestry.api.mail.IStamps;`. If the local `Item item` in that branch is now unused, delete it; leave the `Items.PAPER` branch's local alone.

In `src/mail/java/forestry/mail/inventory/StampCollectorInventory.java`:

```java
	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		return PostageUtil.isStamp(stack);
	}
```

Add `import forestry.mail.letters.PostageUtil;` and delete `import forestry.api.mail.IStamps;`.

- [ ] **Step 8: Run the tests to verify they pass**

Run: `./gradlew compileJava compileMailJava compileTestJava`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew runGameTestServer`
Expected: PASS. All four `PostageDataMapTest` methods succeed, and the rest of the suite stays green.

- [ ] **Step 9: Commit**

```bash
git add src/mail/java/forestry/mail/letters/PostageUtil.java \
        src/mail/java/forestry/mail/letters/Letter.java \
        src/mail/java/forestry/mail/tradestation/TradeStationBlockEntity.java \
        src/mail/java/forestry/mail/carriers/trading/TradeStation.java \
        src/mail/java/forestry/mail/inventory/TradeStationInventory.java \
        src/mail/java/forestry/mail/inventory/StampCollectorInventory.java \
        src/test/java/forestry/gametest/PostageDataMapTest.java
git commit -m "Read postage through the data map instead of IStamps"
```

---

### Task 3: The post office holds stamp items

**Files:**
- Modify: `src/main/java/forestry/api/mail/IPostOffice.java`
- Modify: `src/mail/java/forestry/mail/postoffice/PostOffice.java`
- Modify: `src/mail/java/forestry/mail/postoffice/StampCollectorBlockEntity.java`
- Test: `src/test/java/forestry/gametest/PostOfficeStampVaultTest.java`

**Interfaces:**
- Consumes: `PostageUtil.getPostage(Item)`, `PostageUtil.isStamp(ItemStack)` from Task 2. `EnumStampDefinition.VALUES` and `MailItems.STAMPS.item(EnumStampDefinition)`.
- Produces:
  - `IPostOffice.getAnyStamp(int max)` returns `ItemStack`, cheapest denomination first
  - `IPostOffice.getAnyStamp(Item stamp, int max)` returns `ItemStack`, that exact item only
  - `PostOffice(CompoundTag tag)` reads both the new `collected` compound and the legacy `CPS<n>` keys
  - `PostOffice.save(CompoundTag, HolderLookup.Provider)` writes the `collected` compound

After this task nothing outside `EnumStampDefinition` references `EnumPostage` except `TradeStation`, which Task 4 handles.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/forestry/gametest/PostOfficeStampVaultTest.java`:

```java
package forestry.gametest;

import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.mail.features.MailItems;
import forestry.mail.letters.EnumStampDefinition;
import forestry.mail.postoffice.PostOffice;

/**
 * Conservation and persistence oracle for the post office stamp vault.
 *
 * <p>{@link PostOffice#getAnyStamp} used to mutate the vault without calling {@code setDirty}, and the
 * stamp collector is the one caller that never marks the data dirty afterwards. Withdrawing a stamp
 * and then reloading left it both in the vault and in the collector, which is a duplication path
 * rather than a cosmetic bug. {@link #withdrawingStampsMarksTheVaultDirty} is the pin for it.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class PostOfficeStampVaultTest {
	@GameTest(template = "empty")
	public static void withdrawingStampsMarksTheVaultDirty(GameTestHelper helper) {
		PostOffice office = new PostOffice();
		office.collectPostage(stamps(MailItems.STAMPS.stack(EnumStampDefinition.P_10, 4)));
		office.setDirty(false);

		ItemStack withdrawn = office.getAnyStamp(1);

		assertEquals(helper, withdrawn.getCount(), 1, "withdrawn stamp count");
		helper.assertTrue(office.isDirty(), "getAnyStamp changed the vault without marking it dirty, which loses the withdrawal on reload");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void collectedStampsSurviveASaveRoundTrip(GameTestHelper helper) {
		PostOffice office = new PostOffice();
		office.collectPostage(stamps(
			MailItems.STAMPS.stack(EnumStampDefinition.P_10, 4),
			MailItems.STAMPS.stack(EnumStampDefinition.P_1, 7)));

		PostOffice reloaded = new PostOffice(office.save(new CompoundTag(), helper.getLevel().registryAccess()));

		assertEquals(helper, reloaded.getAnyStamp(MailItems.STAMPS.item(EnumStampDefinition.P_10), 64).getCount(), 4, "reloaded 10n count");
		assertEquals(helper, reloaded.getAnyStamp(MailItems.STAMPS.item(EnumStampDefinition.P_1), 64).getCount(), 7, "reloaded 1n count");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void legacyOrdinalKeysMigrate(GameTestHelper helper) {
		// CPS<n> was indexed by EnumPostage.ordinal(), so CPS1 was P_1 and CPS7 was P_100. CPS0 was
		// never incremented and CPS8 (P_200) never had a stamp item
		CompoundTag legacy = new CompoundTag();
		legacy.putInt("CPS0", 0);
		legacy.putInt("CPS1", 5);
		legacy.putInt("CPS4", 2);
		legacy.putInt("CPS7", 1);
		legacy.putInt("CPS8", 9);

		PostOffice office = new PostOffice(legacy);

		assertEquals(helper, office.getAnyStamp(MailItems.STAMPS.item(EnumStampDefinition.P_1), 64).getCount(), 5, "migrated 1n count");
		assertEquals(helper, office.getAnyStamp(MailItems.STAMPS.item(EnumStampDefinition.P_10), 64).getCount(), 2, "migrated 10n count");
		assertEquals(helper, office.getAnyStamp(MailItems.STAMPS.item(EnumStampDefinition.P_100), 64).getCount(), 1, "migrated 100n count");
		helper.assertTrue(office.getAnyStamp(1).isEmpty(), "The vault held a stamp after every migrated denomination was drained");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void unfilteredWithdrawalTakesTheCheapestFirst(GameTestHelper helper) {
		PostOffice office = new PostOffice();
		office.collectPostage(stamps(
			MailItems.STAMPS.stack(EnumStampDefinition.P_100, 1),
			MailItems.STAMPS.stack(EnumStampDefinition.P_2, 1),
			MailItems.STAMPS.stack(EnumStampDefinition.P_20, 1)));

		helper.assertTrue(office.getAnyStamp(1).is(MailItems.STAMPS.item(EnumStampDefinition.P_2)), "First withdrawal was not the cheapest stamp");
		helper.assertTrue(office.getAnyStamp(1).is(MailItems.STAMPS.item(EnumStampDefinition.P_20)), "Second withdrawal was not the next cheapest stamp");
		helper.assertTrue(office.getAnyStamp(1).is(MailItems.STAMPS.item(EnumStampDefinition.P_100)), "Third withdrawal was not the dearest stamp");
		helper.assertTrue(office.getAnyStamp(1).isEmpty(), "The vault was not empty after every stamp was withdrawn");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void filteredWithdrawalMatchesTheExactItem(GameTestHelper helper) {
		PostOffice office = new PostOffice();
		office.collectPostage(stamps(MailItems.STAMPS.stack(EnumStampDefinition.P_5, 3)));

		helper.assertTrue(office.getAnyStamp(MailItems.STAMPS.item(EnumStampDefinition.P_10), 1).isEmpty(),
			"A filter the vault does not hold still yielded a stamp");
		assertEquals(helper, office.getAnyStamp(MailItems.STAMPS.item(EnumStampDefinition.P_5), 2).getCount(), 2, "filtered withdrawal count");
		helper.succeed();
	}

	private static NonNullList<ItemStack> stamps(ItemStack... stacks) {
		NonNullList<ItemStack> list = NonNullList.create();
		for (ItemStack stack : stacks) {
			list.add(stack);
		}
		return list;
	}

	private static void assertEquals(GameTestHelper helper, int actual, int expected, String what) {
		helper.assertTrue(actual == expected, what + " was " + actual + " instead of " + expected);
	}
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew compileTestJava`
Expected: FAIL. `getAnyStamp(Item, int)` does not exist, so the compiler reports `incompatible types: Item cannot be converted to EnumPostage`.

- [ ] **Step 3: Change the IPostOffice API**

Replace `src/main/java/forestry/api/mail/IPostOffice.java` with:

```java
package forestry.api.mail;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface IPostOffice {

	void collectPostage(NonNullList<ItemStack> stamps);

	IPostalState lodgeLetter(ServerLevel world, ItemStack itemstack, boolean doLodge);

	/**
	 * Used to withdraw collected stamps, taking the cheapest denomination the post office holds.
	 *
	 * @param max The most stamps to withdraw
	 * @return The withdrawn stamps, or an empty stack when the post office holds none
	 */
	ItemStack getAnyStamp(int max);

	/**
	 * Used to withdraw collected stamps of one exact item.
	 *
	 * @param stamp The stamp item to withdraw
	 * @param max   The most stamps to withdraw
	 * @return The withdrawn stamps, or an empty stack when the post office holds none of that item
	 */
	ItemStack getAnyStamp(Item stamp, int max);
}
```

- [ ] **Step 4: Rewrite the post office vault**

Replace the top of `src/mail/java/forestry/mail/postoffice/PostOffice.java` down to and including `collectPostage`, keeping `lodgeLetter` and `getOrCreate` exactly as they are:

```java
package forestry.mail.postoffice;

import forestry.Forestry;
import forestry.api.mail.*;
import forestry.mail.features.MailItems;
import forestry.mail.letters.EnumStampDefinition;
import forestry.mail.letters.EnumDeliveryState;
import forestry.mail.letters.PostageUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import forestry.mail.letters.LetterUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PostOffice extends SavedData implements IPostOffice {
	public static final String SAVE_NAME = "forestry_mail";
	private static final String KEY_COLLECTED = "collected";
	private static final String KEY_LEGACY = "CPS";

	// Cheapest first, matching the ordinal walk this replaced. The id breaks ties so two mods'
	// stamps of equal value keep a stable order
	private static final Comparator<Item> CHEAPEST_FIRST = (left, right) -> {
		int byPostage = Integer.compare(PostageUtil.getPostage(left), PostageUtil.getPostage(right));
		return byPostage != 0 ? byPostage : BuiltInRegistries.ITEM.getKey(left).compareTo(BuiltInRegistries.ITEM.getKey(right));
	};

	private final Object2IntOpenHashMap<Item> collectedStamps = new Object2IntOpenHashMap<>();

	public PostOffice() {
	}

	public PostOffice(CompoundTag tag) {
		if (tag.contains(KEY_COLLECTED, Tag.TAG_COMPOUND)) {
			readCollected(tag.getCompound(KEY_COLLECTED));
		} else {
			readLegacy(tag);
		}
	}

	private void readCollected(CompoundTag collected) {
		for (String key : collected.getAllKeys()) {
			int count = collected.getInt(key);
			if (count <= 0) {
				continue;
			}

			ResourceLocation id = ResourceLocation.tryParse(key);
			Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);

			// Nothing can be handed back for an item that no longer exists, so drop it loudly
			if (item == Items.AIR) {
				Forestry.LOGGER.warn("Post office dropped {} collected stamp(s) of unknown item {}", count, key);
				continue;
			}

			this.collectedStamps.put(item, count);
		}
	}

	// Saves from before the postage data map keyed an array by EnumPostage.ordinal(), so CPS1 through
	// CPS7 were P_1 through P_100. CPS0 was never incremented and CPS8 (P_200) never had a stamp item
	private void readLegacy(CompoundTag tag) {
		EnumStampDefinition[] byLegacyIndex = EnumStampDefinition.VALUES;

		for (int i = 1; i <= byLegacyIndex.length; i++) {
			int count = tag.getInt(KEY_LEGACY + i);
			if (count > 0) {
				this.collectedStamps.put(MailItems.STAMPS.item(byLegacyIndex[i - 1]), count);
			}
		}

		int dropped = tag.getInt(KEY_LEGACY + "0") + tag.getInt(KEY_LEGACY + (byLegacyIndex.length + 1));
		if (dropped > 0) {
			Forestry.LOGGER.warn("Post office dropped {} collected stamp(s) from a denomination that never had an item", dropped);
		}
	}

	@Override
	public CompoundTag save(CompoundTag compoundNBT, HolderLookup.Provider registries) {
		CompoundTag collected = new CompoundTag();

		for (Object2IntMap.Entry<Item> entry : this.collectedStamps.object2IntEntrySet()) {
			if (entry.getIntValue() > 0) {
				collected.putInt(BuiltInRegistries.ITEM.getKey(entry.getKey()).toString(), entry.getIntValue());
			}
		}

		compoundNBT.put(KEY_COLLECTED, collected);
		return compoundNBT;
	}

	// / STAMP MANAGMENT
	@Override
	public ItemStack getAnyStamp(int max) {
		List<Item> order = new ArrayList<>(this.collectedStamps.keySet());
		order.sort(CHEAPEST_FIRST);

		for (Item stamp : order) {
			ItemStack withdrawn = getAnyStamp(stamp, max);
			if (!withdrawn.isEmpty()) {
				return withdrawn;
			}
		}

		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack getAnyStamp(Item stamp, int max) {
		int available = this.collectedStamps.getInt(stamp);
		int collected = Math.min(max, available);

		if (collected <= 0) {
			return ItemStack.EMPTY;
		}

		if (collected == available) {
			this.collectedStamps.removeInt(stamp);
		} else {
			this.collectedStamps.put(stamp, available - collected);
		}

		// The stamp collector is the only caller, and it marks nothing dirty of its own
		setDirty();
		return new ItemStack(stamp, collected);
	}

	@Override
	public void collectPostage(NonNullList<ItemStack> stamps) {
		for (ItemStack stamp : stamps) {
			if (stamp == null || !PostageUtil.isStamp(stamp)) {
				continue;
			}

			this.collectedStamps.addTo(stamp.getItem(), stamp.getCount());
		}

		setDirty();
	}
```

`lodgeLetter` and `getOrCreate` below this are unchanged. The `collectPostage(letter.getPostage())` call inside `lodgeLetter` still compiles.

- [ ] **Step 5: Give the stamp collector an exact-item filter**

In `src/mail/java/forestry/mail/postoffice/StampCollectorBlockEntity.java`, replace the body of `serverTick` between the interval guard and the `stowInInventory` call:

```java
		IInventoryAdapter inventory = getInternalInventory();
		ItemStack filter = inventory.getItem(StampCollectorInventory.SLOT_FILTER);
		ItemStack stamp;

		if (filter.isEmpty()) {
			stamp = PostOffice.getOrCreate((ServerLevel) level).getAnyStamp(1);
		} else if (PostageUtil.isStamp(filter)) {
			// The filter names one stamp item rather than a postage value, so two stamps worth the
			// same are no longer interchangeable here
			stamp = PostOffice.getOrCreate((ServerLevel) level).getAnyStamp(filter.getItem(), 1);
		} else {
			return;
		}

		if (stamp.isEmpty()) {
			return;
		}
```

Replace `import forestry.api.mail.IStamps;` with `import forestry.mail.letters.PostageUtil;`.

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./gradlew compileJava compileMailJava compileTestJava`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew runGameTestServer`
Expected: PASS. All five `PostOfficeStampVaultTest` methods succeed, and the rest of the suite stays green.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/forestry/api/mail/IPostOffice.java \
        src/mail/java/forestry/mail/postoffice/PostOffice.java \
        src/mail/java/forestry/mail/postoffice/StampCollectorBlockEntity.java \
        src/test/java/forestry/gametest/PostOfficeStampVaultTest.java
git commit -m "Hold collected stamps as items and fix the missing setDirty"
```

---

### Task 4: Extract the change-making solver

**Files:**
- Create: `src/mail/java/forestry/mail/letters/PostageSelector.java`
- Modify: `src/mail/java/forestry/mail/carriers/trading/TradeStation.java:226-233,258,283,405-513`
- Test: `src/test/java/forestry/gametest/PostageSelectorTest.java`

**Interfaces:**
- Consumes: `PostageUtil.getPostage(Item)`, `PostageUtil.isStamp(ItemStack)`. `EnumStampDefinition.VALUES`, `MailItems.STAMPS.item(EnumStampDefinition)`.
- Produces:
  - `PostageSelector.Denomination` is a `record (Item item, int postage, int available)`
  - `PostageSelector.heldDenominations(Iterable<ItemStack> stamps)` returns `List<Denomination>`, sorted by postage ascending
  - `PostageSelector.virtualDenominations()` returns `List<Denomination>`, Forestry's own stamps at 99 each
  - `PostageSelector.select(List<Denomination> denominations, int postageRequired)` returns `List<ItemStack>`

After this task `TradeStation` no longer names `EnumPostage` or `IStamps`.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/forestry/gametest/PostageSelectorTest.java`:

```java
package forestry.gametest;

import java.util.List;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.mail.features.MailItems;
import forestry.mail.letters.EnumStampDefinition;
import forestry.mail.letters.PostageSelector;
import forestry.mail.letters.PostageUtil;

/**
 * Behavior lock for the change-making solver lifted out of {@code TradeStation}. The three passes are
 * preserved from the original, greedy and admittedly not optimal, so these assert what it does rather
 * than what an optimal solver would do.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class PostageSelectorTest {
	@GameTest(template = "empty")
	public static void heldDenominationsSortCheapestFirstAndIgnoreNonStamps(GameTestHelper helper) {
		List<PostageSelector.Denomination> held = PostageSelector.heldDenominations(List.of(
			MailItems.STAMPS.stack(EnumStampDefinition.P_20, 2),
			new ItemStack(Items.PAPER, 64),
			MailItems.STAMPS.stack(EnumStampDefinition.P_1, 3),
			MailItems.STAMPS.stack(EnumStampDefinition.P_1, 4),
			ItemStack.EMPTY));

		assertEquals(helper, held.size(), 2, "denomination count");
		assertEquals(helper, held.get(0).postage(), 1, "cheapest denomination postage");
		// Two stacks of the same stamp merge into one denomination
		assertEquals(helper, held.get(0).available(), 7, "cheapest denomination count");
		assertEquals(helper, held.get(1).postage(), 20, "dearest denomination postage");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void virtualDenominationsAreForestryStampsOnly(GameTestHelper helper) {
		List<PostageSelector.Denomination> virtual = PostageSelector.virtualDenominations();

		assertEquals(helper, virtual.size(), EnumStampDefinition.VALUES.length, "virtual denomination count");
		for (PostageSelector.Denomination denomination : virtual) {
			helper.assertTrue(isForestryStamp(denomination), "A virtual trade station offered a stamp Forestry does not ship: " + denomination.item());
			assertEquals(helper, denomination.available(), 99, "virtual supply of " + denomination.item());
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void exactChangeUsesTheLargestStampsThatFit(GameTestHelper helper) {
		// 27 from 20 + 5 + 1 + 1, taken largest first
		List<ItemStack> selected = PostageSelector.select(PostageSelector.heldDenominations(List.of(
			MailItems.STAMPS.stack(EnumStampDefinition.P_1, 9),
			MailItems.STAMPS.stack(EnumStampDefinition.P_5, 9),
			MailItems.STAMPS.stack(EnumStampDefinition.P_20, 9))), 27);

		assertEquals(helper, PostageUtil.sumPostage(selected), 27, "selected postage");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void noExactChangeOverpaysWithTheSmallestCoveringStamp(GameTestHelper helper) {
		// 3 required, only 20n stamps held, so one 20n overpays rather than failing
		List<ItemStack> selected = PostageSelector.select(PostageSelector.heldDenominations(List.of(
			MailItems.STAMPS.stack(EnumStampDefinition.P_20, 2),
			MailItems.STAMPS.stack(EnumStampDefinition.P_100, 2))), 3);

		assertEquals(helper, selected.size(), 1, "selected stack count");
		helper.assertTrue(selected.get(0).is(MailItems.STAMPS.item(EnumStampDefinition.P_20)), "Overpayment did not use the smallest covering stamp");
		assertEquals(helper, selected.get(0).getCount(), 1, "selected stamp count");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void noCoveringStampCombinesSmallerOnes(GameTestHelper helper) {
		// 7 required, only 2n stamps held, so four of them combine and overpay by one
		List<ItemStack> selected = PostageSelector.select(PostageSelector.heldDenominations(List.of(
			MailItems.STAMPS.stack(EnumStampDefinition.P_2, 9))), 7);

		helper.assertTrue(PostageUtil.sumPostage(selected) >= 7, "Combined stamps did not cover the required postage");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void anEmptyStationSelectsNothing(GameTestHelper helper) {
		helper.assertTrue(PostageSelector.select(PostageSelector.heldDenominations(List.of()), 5).isEmpty(),
			"A station holding no stamps still selected postage");
		helper.succeed();
	}

	private static boolean isForestryStamp(PostageSelector.Denomination denomination) {
		for (EnumStampDefinition stamp : EnumStampDefinition.VALUES) {
			if (MailItems.STAMPS.item(stamp) == denomination.item()) {
				return true;
			}
		}
		return false;
	}

	private static void assertEquals(GameTestHelper helper, int actual, int expected, String what) {
		helper.assertTrue(actual == expected, what + " was " + actual + " instead of " + expected);
	}
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew compileTestJava`
Expected: FAIL, `cannot find symbol: class PostageSelector`.

- [ ] **Step 3: Create PostageSelector**

Create `src/mail/java/forestry/mail/letters/PostageSelector.java`:

```java
package forestry.mail.letters;

import forestry.mail.features.MailItems;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Picks the stamps that pay for a letter. Lifted out of the trade station so the arithmetic can be
 * read and tested on its own.
 *
 * <p>The three passes are the original greedy ones. They are not optimal and are not meant to be.
 */
public class PostageSelector {
	private static final int VIRTUAL_SUPPLY = 99;

	private static final Comparator<Denomination> CHEAPEST_FIRST = (left, right) -> {
		int byPostage = Integer.compare(left.postage(), right.postage());
		return byPostage != 0 ? byPostage : BuiltInRegistries.ITEM.getKey(left.item()).compareTo(BuiltInRegistries.ITEM.getKey(right.item()));
	};

	private PostageSelector() {
	}

	/**
	 * One kind of stamp and how many of it are on hand.
	 *
	 * @param item      The stamp item
	 * @param postage   The postage one of the item is worth
	 * @param available The number on hand
	 */
	public record Denomination(Item item, int postage, int available) {
	}

	/**
	 * @param stamps The stacks in a trade station's stamp slots
	 * @return The denominations those stacks make up, cheapest first
	 */
	public static List<Denomination> heldDenominations(Iterable<ItemStack> stamps) {
		Object2IntOpenHashMap<Item> held = new Object2IntOpenHashMap<>();

		for (ItemStack stamp : stamps) {
			if (stamp != null && PostageUtil.isStamp(stamp)) {
				held.addTo(stamp.getItem(), stamp.getCount());
			}
		}

		List<Denomination> denominations = new ArrayList<>(held.size());
		for (Object2IntMap.Entry<Item> entry : held.object2IntEntrySet()) {
			denominations.add(new Denomination(entry.getKey(), PostageUtil.getPostage(entry.getKey()), entry.getIntValue()));
		}

		denominations.sort(CHEAPEST_FIRST);
		return denominations;
	}

	/**
	 * A virtual trade station conjures its stamps rather than holding them, so it may only conjure the
	 * stamps Forestry ships. Reading the data map here would let it mint another mod's stamps.
	 *
	 * @return The denominations a virtual trade station pays with, cheapest first
	 */
	public static List<Denomination> virtualDenominations() {
		List<Denomination> denominations = new ArrayList<>(EnumStampDefinition.VALUES.length);

		for (EnumStampDefinition stamp : EnumStampDefinition.VALUES) {
			Item item = MailItems.STAMPS.item(stamp);
			denominations.add(new Denomination(item, PostageUtil.getPostage(item), VIRTUAL_SUPPLY));
		}

		denominations.sort(CHEAPEST_FIRST);
		return denominations;
	}

	/**
	 * @param denominations The stamps on hand, cheapest first
	 * @param postageRequired The postage the letter needs
	 * @return The stamps to attach, which may fall short when the denominations cannot cover it
	 */
	public static List<ItemStack> select(List<Denomination> denominations, int postageRequired) {
		int[] taken = new int[denominations.size()];
		int postageRemaining = postageRequired;

		// Largest first, taking as many of each as fit
		for (int i = denominations.size() - 1; i >= 0; i--) {
			if (postageRemaining <= 0) {
				break;
			}

			Denomination denomination = denominations.get(i);
			if (denomination.postage() > postageRemaining) {
				continue;
			}

			int num = Math.min(denomination.available(), postageRemaining / denomination.postage());
			taken[i] = num;
			postageRemaining -= num * denomination.postage();
		}

		// Use a larger stamp if exact change isn't available
		if (postageRemaining > 0) {
			for (int i = 0; i < denominations.size(); i++) {
				Denomination denomination = denominations.get(i);

				if (denomination.postage() >= postageRequired && denomination.available() > 0) {
					int[] single = new int[denominations.size()];
					single[i] = 1;
					return toStacks(denominations, single);
				}
			}
		}

		// If there isn't a single larger stamp we will just combine smaller ones, starting with the
		// higher values. This is totally disregarding whether there's a better solution or not
		if (postageRemaining > 0) {
			postageRemaining = postageRequired;
			taken = new int[denominations.size()];

			for (int i = denominations.size() - 1; i >= 0; i--) {
				Denomination denomination = denominations.get(i);

				int reqNum = Math.min((int) Math.ceil((double) postageRemaining / denomination.postage()), denomination.available());
				taken[i] = reqNum;
				postageRemaining -= reqNum * denomination.postage();

				if (postageRemaining <= 0) {
					break;
				}
			}
		}

		return toStacks(denominations, taken);
	}

	private static List<ItemStack> toStacks(List<Denomination> denominations, int[] taken) {
		List<ItemStack> stacks = new ArrayList<>();

		for (int i = 0; i < taken.length; i++) {
			if (taken[i] > 0) {
				stacks.add(new ItemStack(denominations.get(i).item(), taken[i]));
			}
		}

		return stacks;
	}
}
```

- [ ] **Step 4: Point the trade station at it**

In `src/mail/java/forestry/mail/carriers/trading/TradeStation.java`:

Delete the `getPostage(int, boolean)`, `getNumStamps(EnumPostage)` and `removeStamps(int[])` methods entirely, and add in their place:

```java
	private List<PostageSelector.Denomination> denominations() {
		return isVirtual()
			? PostageSelector.virtualDenominations()
			: PostageSelector.heldDenominations(InventoryUtil.getStacks(this.inventory, SLOT_STAMPS_1, SLOT_STAMPS_COUNT));
	}

	private void removeStamps(List<ItemStack> stamps) {
		for (ItemStack stamp : stamps) {
			int remaining = stamp.getCount();

			for (int slot = SLOT_STAMPS_1; slot < SLOT_STAMPS_1 + SLOT_STAMPS_COUNT && remaining > 0; slot++) {
				ItemStack held = this.inventory.getItem(slot);

				if (held.isEmpty() || !ItemStack.isSameItem(held, stamp)) {
					continue;
				}

				remaining -= this.inventory.removeItem(slot, remaining).getCount();
			}
		}
	}
```

Replace the postage attachment block at line 226:

```java
		// Attach necessary postage
		List<ItemStack> stampsUsed = PostageSelector.select(denominations(), requiredPostage);
		for (ItemStack stamp : stampsUsed) {
			mail.addStamps(stamp);
		}
```

Replace the removal at line 258:

```java
		removeStamps(stampsUsed);
```

Replace the confirmation letter's removal at line 283:

```java
			removeStamps(List.of(MailItems.STAMPS.stack(EnumStampDefinition.P_1, 1)));
```

Add `import forestry.mail.letters.PostageSelector;`. `java.util.List` is already imported. Delete `import forestry.api.mail.*;` only if nothing else in the file uses it; it also supplies `ILetter`, `IMailAddress`, `IPostalState` and `ITradeStation`, so leave it.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew compileJava compileMailJava compileTestJava`
Expected: BUILD SUCCESSFUL. If `EnumPostage` is reported as unused in `TradeStation`, delete the import.

Run: `./gradlew runGameTestServer`
Expected: PASS. All six `PostageSelectorTest` methods succeed, and the rest of the suite stays green.

- [ ] **Step 6: Commit**

```bash
git add src/mail/java/forestry/mail/letters/PostageSelector.java \
        src/mail/java/forestry/mail/carriers/trading/TradeStation.java \
        src/test/java/forestry/gametest/PostageSelectorTest.java
git commit -m "Extract the postage solver and stop virtual stations minting foreign stamps"
```

---

### Task 5: Delete IStamps and EnumPostage

**Files:**
- Modify: `src/mail/java/forestry/mail/letters/EnumStampDefinition.java`
- Modify: `src/mail/java/forestry/mail/letters/ItemStamp.java`
- Modify: `src/mail/java/forestry/mail/data/MailDataMapProvider.java`
- Modify: `src/test/java/forestry/gametest/PostageDataMapTest.java`
- Delete: `src/main/java/forestry/api/mail/IStamps.java`
- Delete: `src/main/java/forestry/api/mail/EnumPostage.java`

**Interfaces:**
- Consumes: everything from Tasks 1 to 4.
- Produces: `EnumStampDefinition.getPostage()` returns `int` rather than `EnumPostage`.

- [ ] **Step 1: Change the test to expect the int**

In `src/test/java/forestry/gametest/PostageDataMapTest.java`, in `everyForestryStampCarriesItsPostage`, change:

```java
			int expected = stamp.getPostage().getValue();
```

to:

```java
			int expected = stamp.getPostage();
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew compileTestJava`
Expected: FAIL with `incompatible types: EnumPostage cannot be converted to int`.

- [ ] **Step 3: Make EnumStampDefinition hold an int**

In `src/mail/java/forestry/mail/letters/EnumStampDefinition.java`:

Change the seven constants to pass ints:

```java
	P_1("1n", 1, ForestryTags.Items.GEMS_APATITE, TextColor.fromRgb(0x4a8ca7), TextColor.fromRgb(0xffffff)),
	P_2("2n", 2, Items.COPPER_INGOT, TextColor.fromRgb(0xe8c814), TextColor.fromRgb(0xffffff)),
	P_5("5n", 5, ForestryTags.Items.INGOTS_TIN, TextColor.fromRgb(0x9c0707), TextColor.fromRgb(0xffffff)),
	P_10("10n", 10, Tags.Items.INGOTS_GOLD, TextColor.fromRgb(0x7bd1b8), TextColor.fromRgb(0xffffff)),
	P_20("20n", 20, Tags.Items.GEMS_DIAMOND, TextColor.fromRgb(0xff9031), TextColor.fromRgb(0xfff7dd)),
	P_50("50n", 50, Tags.Items.GEMS_EMERALD, TextColor.fromRgb(0x6431d7), TextColor.fromRgb(0xfff7dd)),
	P_100("100n", 100, Items.NETHER_STAR, TextColor.fromRgb(0xd731ba), TextColor.fromRgb(0xfff7dd)),
	;
```

Change all three constructors' second parameter from `EnumPostage postage` to `int postage`, change the field to `private final int postage;`, and change the getter:

```java
	public int getPostage() {
		return this.postage;
	}
```

Delete the `POSTAGE_MAP` field, the `static` block that fills it, the `getFromPostage` method, and the imports `forestry.api.mail.EnumPostage`, `java.util.EnumMap` and `java.util.Map`.

- [ ] **Step 4: Drop the interface from ItemStamp**

Replace `src/mail/java/forestry/mail/letters/ItemStamp.java` with:

```java
package forestry.mail.letters;

import forestry.core.platform.item.TwoTintItem;

public class ItemStamp extends TwoTintItem {
	public ItemStamp(EnumStampDefinition type) {
		super(type);
	}
}
```

The `type` field and `getPostage` override are both gone: postage comes from the data map now, and nothing read the field.

- [ ] **Step 5: Update the data map provider**

In `src/mail/java/forestry/mail/data/MailDataMapProvider.java`, change:

```java
			postage.add(MailItems.STAMPS.item(stamp).builtInRegistryHolder(), stamp.getPostage().getValue(), false);
```

to:

```java
			postage.add(MailItems.STAMPS.item(stamp).builtInRegistryHolder(), stamp.getPostage(), false);
```

- [ ] **Step 6: Delete the dead API types**

```bash
git rm src/main/java/forestry/api/mail/IStamps.java src/main/java/forestry/api/mail/EnumPostage.java
```

- [ ] **Step 7: Verify nothing still references them**

Run: `grep -rn "IStamps\|EnumPostage" src/`
Expected: no output.

- [ ] **Step 8: Run everything to verify it passes**

Run: `./gradlew compileJava compileMailJava compileTestJava`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew runData`
Expected: BUILD SUCCESSFUL, and `git status` shows no change to `postage.json`. The values are the same ints, so the generated file must be byte-identical.

Run: `./gradlew runGameTestServer`
Expected: PASS. The whole suite is green.

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL, including `checkJarPartition`.

- [ ] **Step 9: Commit**

```bash
git add -A src/main/java/forestry/api/mail src/mail/java/forestry/mail/letters/EnumStampDefinition.java \
        src/mail/java/forestry/mail/letters/ItemStamp.java \
        src/mail/java/forestry/mail/data/MailDataMapProvider.java \
        src/test/java/forestry/gametest/PostageDataMapTest.java
git commit -m "Delete IStamps and EnumPostage"
```

---

## Verification

After Task 5, the following must all hold:

1. `./gradlew checkJarPartition` succeeds.

   Not `./gradlew build`. That task fails at `checkApiBoundary` on a `BeeSpeciesProvider` violation
   in apiculture, and it fails identically at 79da0e00c, before any of this feature's commits. The
   original wording of this item was wrong: it named a gate this branch never passed.
2. `./gradlew runGameTestServer` is green.
3. `./gradlew runData` produces no diff. The postage values are the same integers reached by a
   different route, so `postage.json` must come out byte-identical.
4. `grep -rn "IStamps\|EnumPostage" src/` returns no code references. Three comments legitimately
   survive, because they document the historical `CPS<n>` save format and the interface the data map
   replaced: `PostageUtil.java`, `PostOffice.java` and `PostOfficeStampVaultTest.java`.
5. `./gradlew runCoreOnlyServer` boots. The core jar declares `POSTAGE` but never reads it, so a core-only install must be unaffected by the data map's absence.
