# Phase 6: no-op managers and the packaged-artifact gate

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement D7 - base supplies no-op implementations of the six split-jar managers, each
carrying `isLoaded()` - clear `PluginManager`'s four remaining leaks, and replace the import-based
gate's headline number with one that measures what actually ships.

**Architecture:** Three things, in that order. First the no-ops themselves, which are pure null
objects in the established `Fake*` idiom and are directly testable even though nothing reaches them
in a one-jar build. Then the wiring: `ForestryApiImpl` and `ForestryClientApiImpl` initialise their
six fields to the no-ops instead of leaving them null and throwing, and each module installs its
real manager over the top through one new `IForestryModule` hook - which is also what finally stops
`PluginManager` constructing content classes. Last, the gate: all 20 remaining baselined files are
under `forestry/core/data/**`, which `build.gradle:410` already strips from the jar, so the phase-6
condition is reachable now and the build should say so.

**Tech Stack:** Java 21, NeoForge 21.1.230, Minecraft 1.21.1, ModDevGradle 2.0.x, Gradle Groovy DSL.
GameTests only, no JUnit.

## Global Constraints

- Comment and Javadoc style is binding; see `CLAUDE.md`. ASCII only. `@return` and `@param` are
  noun-phrase fragments with no terminal period. Lowercase `todo`.
- Every task ends with `./gradlew runData` producing no diff in `src/generated/resources` and
  `./gradlew runGameTestServer` reporting all tests passed. **The test count rises during this
  phase** - it is 100 at the start, and each task below states the number to expect.
- Both gates stay honest: `checkApiBoundary` green, `checkBaseBoundary` trimmed in the same commit
  that clears a file. A stale baseline fails the build by design.
- **Run `./gradlew compileTestJava`, not just `compileJava`.**
- **No api signature changes that break existing implementors.** D7 says so explicitly: "Signatures
  stay non-null and non-throwing, so defensive addon code does not crash on a partial install." Every
  interface method added in this phase is `default`.
- **A moved class must move with everything that constructs it.** Standing rule since phase 4.
- **Repoint existing imports; do not add a second one.**
- When a class moves package, check for callers that used it **without an import** because they were
  in the same package. This has bitten in phases 1b, 2, 3, 4 and 5a - in 5a it bit in the reverse
  direction too, where the moved files had been using base classes as unimported neighbours.
- All source files are LF. Do not write `$`-anchored `sed` patterns.
- **No client class in server-reachable code.** GameTests run on a dedicated server. Referencing
  `MissingTextureAtlasSprite`, `Minecraft` or any `net.minecraft.client` type from a no-op that a
  GameTest constructs will crash with `NoClassDefFoundError`. All the no-ops in this plan return
  plain `ResourceLocation`s for that reason.

## Starting state

`checkBaseBoundary`: 21 files. One is `apiimpl/plugin/PluginManager.java`; the other 20 are the
bucket A datagen providers.

### The six managers, and where a no-op is actually reachable

`IForestryApi` exposes `getFarmingManager`, `getHiveManager`, `getTreeManager`.
`IForestryClientApi` exposes `getBeeManager`, `getTreeManager`, `getButterflyManager`. All six can be
queried with the owning jar absent.

Measured: **59 call sites, of which 8 are outside the owning jar.** Everything else is a module
calling its own manager, which cannot happen when that jar is absent. The eight that matter:

| Call site | Manager | Behavior with the no-op |
| --- | --- | --- |
| `core/worldgen/ForestryBiomeModifier.java:49` | hive | `getHives()` empty, loop body never runs, no hive feature added to the biome |
| `core/items/ItemRefractoryWax.java:23` | tree | `getRefractoryWaxed` returns null, which the call site already branches on |
| `core/client/CoreClientHandler.java:132` | bee client | `getAllModelLocations` empty, no extra models registered |
| `core/client/CoreClientHandler.java:140` | tree client | `getAllSaplingModels` empty, same |
| `api/apiculture/genetics/IBeeEffect.java:76` | bee client | `addBeeHiveParticles` does nothing |
| `api/client/arboriculture/ForestryLeafSprites.java:11` | client helper | see below |
| `core/data/models/ForestryBlockStateProvider.java:102` | tree | datagen, never packaged |
| `core/data/recipe/ForestryRecipeProvider.java:1938` | tree | datagen, never packaged |

Every one of the six runtime sites already handles the degenerate case. That is the finding this
phase rests on: **D7 needs no defensive rewriting of callers, only the null objects themselves.**

`IClientHelper` is the exception and it is a live bug, not a hypothetical. Phase 5a left
`ForestryClientApiImpl:17` resolving it with `ServiceLoader...orElseThrow()` in a **field
initialiser**, so with arboriculture absent every client start throws `NoSuchElementException` while
constructing `IForestryClientApi.INSTANCE` - not only the paths that use leaf sprites. Task 4 fixes
it.

### What has no oracle, and what does

Nothing in a one-jar build reaches a no-op, so the wiring cannot be exercised. Say that plainly.

But the no-ops themselves are ordinary objects, and their contract - non-null returns, empty
collections, `isLoaded() == false` - is exactly what a GameTest can assert by constructing them
directly. That is a real oracle for the thing most likely to be wrong (a no-op that returns null
where the interface promises non-null, and NPEs a defensive addon two releases later). Tasks 1, 3
and 4 each add one.

What stays unverifiable: that the *installed* manager is the real one rather than the no-op in a
real client, and that a genuinely absent jar degrades as the table above predicts. The second cannot
be tested until phase 9 produces separate jars. Task 1's test covers the first for all six.

## File Structure

| Action | File | Responsibility |
| --- | --- | --- |
| Modify | `api/farming/IFarmingManager.java`, `api/apiculture/hives/IHiveManager.java`, `api/arboriculture/ITreeManager.java` | `default boolean isLoaded()` |
| Modify | `api/client/apiculture/IBeeClientManager.java`, `api/client/arboriculture/ITreeClientManager.java`, `api/client/lepidopterology/IButterflyClientManager.java` | `default boolean isLoaded()` |
| Modify | `api/client/arboriculture/ILeafSprite.java` | `MISSING` constant, symmetric with the existing `ILeafTint.DEFAULT` |
| Move | `apiculture/FakeBeekeepingLogic.java` -> `apiimpl/fake/` | a null object, not content |
| Create | `apiimpl/fake/FakeFarmingManager.java`, `FakeHiveManager.java`, `FakeTreeManager.java`, `FakeCharcoalManager.java` | the three server no-ops plus the one they return |
| Create | `apiimpl/client/fake/FakeBeeClientManager.java`, `FakeTreeClientManager.java`, `FakeButterflyClientManager.java`, `FakeClientHelper.java` | the client no-ops |
| Modify | `apiimpl/ForestryApiImpl.java`, `apiimpl/client/ForestryClientApiImpl.java` | six fields default to the no-op, six getters stop throwing |
| Modify | `api/modules/IForestryModule.java` | two defaulted install hooks |
| Modify | `farming/ModuleFarming.java`, `apiculture/ModuleApiculture.java`, `arboriculture/ModuleArboriculture.java` | each installs its own manager |
| Modify | `apiimpl/plugin/PluginManager.java` | stops naming any content class |
| Modify | `build.gradle:140-198` | packaged vs datagen-only split, plus the bytecode gate |
| Create | `src/test/java/forestry/gametest/ManagerLoadedTest.java`, `NoOpManagerTest.java`, `NoOpClientManagerTest.java` | the three oracles |

---

### Task 1: Add isLoaded() to the six manager interfaces

D7: "Each manager interface gains `isLoaded()`, returning `true` from real implementations and
`false` from the no-ops, so addons can check functionality without reaching for `IModuleManager` and
a module id."

**The default must be `true`.** An addon that already implements one of these interfaces gets the
correct answer without recompiling; only this phase's no-ops override it. A default of `false` would
silently reclassify every existing third-party manager as absent.

**Files:**
- Modify: `src/main/java/forestry/api/farming/IFarmingManager.java`
- Modify: `src/main/java/forestry/api/apiculture/hives/IHiveManager.java`
- Modify: `src/main/java/forestry/api/arboriculture/ITreeManager.java`
- Modify: `src/main/java/forestry/api/client/apiculture/IBeeClientManager.java`
- Modify: `src/main/java/forestry/api/client/arboriculture/ITreeClientManager.java`
- Modify: `src/main/java/forestry/api/client/lepidopterology/IButterflyClientManager.java`
- Test: `src/test/java/forestry/gametest/ManagerLoadedTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `boolean isLoaded()` on all six, defaulted to `true`. Tasks 3 and 4 override it to
  `false`; task 5 relies on it being callable on whatever is installed.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/forestry/gametest/ManagerLoadedTest.java`:

```java
package forestry.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.IForestryApi;

/**
 * Asserts that the managers installed in a full install report themselves loaded. The no-op
 * implementations report false; see NoOpManagerTest for the other side of the contract.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class ManagerLoadedTest {
	@GameTest(template = "empty")
	public static void serverManagersAreLoaded(GameTestHelper helper) {
		if (!IForestryApi.INSTANCE.getFarmingManager().isLoaded()) {
			helper.fail("IFarmingManager reports not loaded in a full install");
			return;
		}
		if (!IForestryApi.INSTANCE.getHiveManager().isLoaded()) {
			helper.fail("IHiveManager reports not loaded in a full install");
			return;
		}
		if (!IForestryApi.INSTANCE.getTreeManager().isLoaded()) {
			helper.fail("ITreeManager reports not loaded in a full install");
			return;
		}
		helper.succeed();
	}
}
```

The three client managers are deliberately not asserted here. No GameTest runs a client, so
`IForestryClientApi.INSTANCE` is not installed on a dedicated server and touching it would fail for
a reason unrelated to what is being tested. Task 4 covers the client no-ops by constructing them
directly, which needs no client.

- [ ] **Step 2: Run it and watch it fail to compile**

```bash
./gradlew compileTestJava
```

Expected: FAIL, `cannot find symbol: method isLoaded()`.

- [ ] **Step 3: Add the method to all six interfaces**

To `IFarmingManager`, `IHiveManager` and `ITreeManager`, and to `IBeeClientManager`,
`ITreeClientManager` and `IButterflyClientManager`, add this **verbatim in each**, as the first
member of the interface. Reuse the identical wording; per `CLAUDE.md` the opening verb and phrasing
must not vary across parallel members.

```java
	/**
	 * Used to check whether the module that supplies this manager is installed. Base ships a no-op
	 * implementation of every manager whose module can be absent, so this returns {@code false}
	 * rather than the getter returning null or throwing.
	 *
	 * @return Whether a real implementation is installed
	 * @since 2.10.0
	 */
	default boolean isLoaded() {
		return true;
	}
```

- [ ] **Step 4: Verify**

```bash
./gradlew compileJava compileTestJava
./gradlew checkApiBoundary checkBaseBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `checkApiBoundary: forestry.api is clean`, `checkBaseBoundary: 21 known leaking file(s)
remaining`, no datagen diff, **all 101 required tests passed** (100 plus the one added here).

- [ ] **Step 5: Commit**

```bash
git add -A src/main/java/forestry src/test/java/forestry
git commit -m "api: add isLoaded() to the six splittable managers

D7 represents an absent module with a no-op manager rather than a throw or an
Optional. isLoaded() lets an addon tell the two apart without reaching for
IModuleManager and a module id.

The default is true so that an existing third-party implementation keeps
answering correctly without recompiling; only this phase's no-ops override it.

The client three are not asserted by the new test: no GameTest runs a client,
so IForestryClientApi is not installed on a dedicated server."
```

---

### Task 2: Move FakeBeekeepingLogic into base

`IHiveManager.createBeekeepingLogic(IBeeHousing)` returns a non-null `IBeekeepingLogic`, so task 3's
no-op needs one to hand back. `forestry/apiculture/FakeBeekeepingLogic.java` is exactly that object
and imports only `forestry.api.apiculture.IBeekeepingLogic` plus vanilla - it is a null object, not
bee content, and base cannot reach it where it currently sits.

**Files:**
- Move: `src/main/java/forestry/apiculture/FakeBeekeepingLogic.java` -> `src/main/java/forestry/apiimpl/fake/FakeBeekeepingLogic.java`
- Modify: whatever imports it (step 1 finds them)

**Interfaces:**
- Consumes: nothing.
- Produces: `forestry.apiimpl.fake.FakeBeekeepingLogic`, unchanged in shape. Task 3 returns it from
  `FakeHiveManager.createBeekeepingLogic`.

- [ ] **Step 1: Find every user, including same-package ones with no import**

```bash
grep -rn "FakeBeekeepingLogic" src/main/java src/test/java --include='*.java' \
  | grep -v "apiculture/FakeBeekeepingLogic\.java:"
```

Note which hits are inside `forestry/apiculture/` - those reference it **without an import** today
and will each need one added. This is the same-package effect that has bitten in five prior phases.

- [ ] **Step 2: Confirm it is safe to move**

```bash
grep -n "^import" src/main/java/forestry/apiculture/FakeBeekeepingLogic.java
```

Expected: `forestry.api.apiculture.IBeekeepingLogic` and `net.minecraft.*`/`java.util.*` only. **If
it imports anything else under `forestry.apiculture`, stop** - it is not a pure null object and task
3 needs a different source for its `IBeekeepingLogic`.

- [ ] **Step 3: Move it**

```bash
mkdir -p src/main/java/forestry/apiimpl/fake
git mv src/main/java/forestry/apiculture/FakeBeekeepingLogic.java src/main/java/forestry/apiimpl/fake/FakeBeekeepingLogic.java
sed -i '1s@^package forestry\.apiculture;@package forestry.apiimpl.fake;@' src/main/java/forestry/apiimpl/fake/FakeBeekeepingLogic.java
grep -rl "import forestry\.apiculture\.FakeBeekeepingLogic;" src/main/java \
  | xargs -r sed -i 's@import forestry\.apiculture\.FakeBeekeepingLogic;@import forestry.apiimpl.fake.FakeBeekeepingLogic;@'
```

Then add `import forestry.apiimpl.fake.FakeBeekeepingLogic;` to each file step 1 found inside
`forestry/apiculture/`. Content importing base is allowed and is not a boundary leak.

- [ ] **Step 4: Verify**

```bash
./gradlew compileJava compileTestJava
diff <(git show HEAD:src/main/java/forestry/apiculture/FakeBeekeepingLogic.java | grep -v "^package \|^import ") \
     <(grep -v "^package \|^import " src/main/java/forestry/apiimpl/fake/FakeBeekeepingLogic.java) \
  && echo "body identical"
./gradlew checkApiBoundary checkBaseBoundary
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `body identical`, `checkBaseBoundary: 21 known leaking file(s) remaining`, all 101 tests
passed. The baseline does not move - `FakeBeekeepingLogic` was in apiculture, which the gate does
not scan.

- [ ] **Step 5: Commit**

```bash
git add -A src/main/java/forestry
git commit -m "core: move FakeBeekeepingLogic into base

IHiveManager.createBeekeepingLogic returns non-null, so base's no-op hive
manager needs an IBeekeepingLogic to hand back. FakeBeekeepingLogic is that
object already and imports nothing but api and vanilla - it is a null object,
not bee content.

Body identical to HEAD apart from the package line."
```

---

### Task 3: The three server no-op managers

**Files:**
- Create: `src/main/java/forestry/apiimpl/fake/FakeFarmingManager.java`
- Create: `src/main/java/forestry/apiimpl/fake/FakeHiveManager.java`
- Create: `src/main/java/forestry/apiimpl/fake/FakeTreeManager.java`
- Create: `src/main/java/forestry/apiimpl/fake/FakeCharcoalManager.java`
- Test: `src/test/java/forestry/gametest/NoOpManagerTest.java`

**Interfaces:**
- Consumes: `forestry.apiimpl.fake.FakeBeekeepingLogic` from task 2.
- Produces: `FakeFarmingManager.INSTANCE`, `FakeHiveManager.INSTANCE`, `FakeTreeManager.INSTANCE`,
  `FakeCharcoalManager.INSTANCE`, all `IForestryApi`-facing types. Task 5 installs them as the
  initial value of the three fields on `ForestryApiImpl`.

All four follow the established idiom - `public enum FakeX implements IX { INSTANCE; ... }`, matching
`FakeClimateProvider`, `FakeErrorLogic`, `FakeTankManager` and the rest.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/forestry/gametest/NoOpManagerTest.java`:

```java
package forestry.gametest;

import java.util.List;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.arboriculture.WoodBlockKind;
import forestry.apiimpl.fake.FakeFarmingManager;
import forestry.apiimpl.fake.FakeHiveManager;
import forestry.apiimpl.fake.FakeTreeManager;

/**
 * The no-op managers are unreachable while every module ships in one jar, so this constructs them
 * directly. What it protects is the contract a defensive addon relies on under D7: non-null returns,
 * empty collections and isLoaded() false.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class NoOpManagerTest {
	@GameTest(template = "empty")
	public static void noOpsReportNotLoaded(GameTestHelper helper) {
		if (FakeFarmingManager.INSTANCE.isLoaded() || FakeHiveManager.INSTANCE.isLoaded() || FakeTreeManager.INSTANCE.isLoaded()) {
			helper.fail("A no-op manager reported itself loaded");
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void noOpFarmingManagerDegrades(GameTestHelper helper) {
		ResourceLocation any = ForestryConstants.forestry("wheat");

		if (FakeFarmingManager.INSTANCE.getFarmType(any) != null) {
			helper.fail("No-op farming manager returned a farm type");
			return;
		}
		if (!FakeFarmingManager.INSTANCE.getFarmables(any).isEmpty()) {
			helper.fail("No-op farming manager returned farmables");
			return;
		}
		if (FakeFarmingManager.INSTANCE.getFertilizeValue(new ItemStack(Items.BONE_MEAL)) != 0) {
			helper.fail("No-op farming manager valued a fertilizer");
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void noOpHiveManagerDegrades(GameTestHelper helper) {
		if (!FakeHiveManager.INSTANCE.getHives().isEmpty()
				|| !FakeHiveManager.INSTANCE.getCommonVillageHives().isEmpty()
				|| !FakeHiveManager.INSTANCE.getRareVillageHives().isEmpty()
				|| !FakeHiveManager.INSTANCE.getDrops(ForestryConstants.forestry("forest")).isEmpty()) {
			helper.fail("No-op hive manager returned a non-empty registry");
			return;
		}
		if (FakeHiveManager.INSTANCE.getSwarmingMaterialChance(Items.SLIME_BALL) != 0.0f) {
			helper.fail("No-op hive manager gave an item a swarming chance");
			return;
		}
		// The three create* methods promise non-null. An addon that calls them on a partial install
		// and gets null is the failure this whole phase exists to prevent.
		if (FakeHiveManager.INSTANCE.createBeekeepingLogic(null) == null
				|| FakeHiveManager.INSTANCE.createBeeHousingModifier(null) == null
				|| FakeHiveManager.INSTANCE.createBeeHousingListener(null) == null) {
			helper.fail("No-op hive manager returned null from a non-null factory method");
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void noOpTreeManagerDegrades(GameTestHelper helper) {
		if (FakeTreeManager.INSTANCE.getRefractoryWaxed(Blocks.OAK_PLANKS) != null) {
			helper.fail("No-op tree manager waxed a block");
			return;
		}
		if (!FakeTreeManager.INSTANCE.getRegisteredWoodTypes().isEmpty()) {
			helper.fail("No-op tree manager returned wood types");
			return;
		}
		if (FakeTreeManager.INSTANCE.getCharcoalManager() == null
				|| !FakeTreeManager.INSTANCE.getCharcoalManager().getWalls().isEmpty()) {
			helper.fail("No-op tree manager returned a null or non-empty charcoal manager");
			return;
		}
		// Vanilla resolves an undefined TagKey as empty rather than erroring, so the empty tag the
		// no-op hands back is safe to query and matches nothing.
		TagKey<Block> logs = FakeTreeManager.INSTANCE.getLogBlockTag(null, false);
		if (logs == null || Blocks.OAK_LOG.defaultBlockState().is(logs)) {
			helper.fail("No-op tree manager returned a null or matching log tag");
			return;
		}
		if (!FakeTreeManager.INSTANCE.getStack(null, WoodBlockKind.PLANKS, false).isEmpty()) {
			helper.fail("No-op tree manager returned a non-empty stack");
			return;
		}
		if (!FakeTreeManager.INSTANCE.getBlock(null, WoodBlockKind.PLANKS, false).isAir()) {
			helper.fail("No-op tree manager returned a non-air block state");
			return;
		}
		helper.succeed();
	}
}
```

Note the `null` arguments to `createBeekeepingLogic`, `getStack` and `getLogBlockTag`. That is
deliberate and is the point: a no-op must not dereference its arguments, because a caller on a
partial install may legitimately have nothing meaningful to pass.

- [ ] **Step 2: Run it and watch it fail to compile**

```bash
./gradlew compileTestJava
```

Expected: FAIL, `package forestry.apiimpl.fake does not exist`.

- [ ] **Step 3: Write FakeFarmingManager**

```java
package forestry.apiimpl.fake;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import forestry.api.farming.IFarmType;
import forestry.api.farming.IFarmingManager;

/**
 * The farming manager used when the agriculture module is absent. Farms find no types and no
 * fertilizer has any value, so a farm block installed by another mod idles instead of crashing.
 */
public enum FakeFarmingManager implements IFarmingManager {
	INSTANCE;

	@Override
	public boolean isLoaded() {
		return false;
	}

	@Override
	public int getFertilizeValue(ItemStack stack) {
		return 0;
	}

	@Nullable
	@Override
	public IFarmType getFarmType(ResourceLocation id) {
		return null;
	}
}
```

`getFarmables` is not overridden: its default already returns `List.of()` when `getFarmType` returns
null.

- [ ] **Step 4: Write FakeHiveManager**

```java
package forestry.apiimpl.fake;

import java.util.List;

import com.google.common.collect.ImmutableList;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.IBeeListener;
import forestry.api.apiculture.IBeeModifier;
import forestry.api.apiculture.IBeekeepingLogic;
import forestry.api.apiculture.hives.IHive;
import forestry.api.apiculture.hives.IHiveDrop;
import forestry.api.apiculture.hives.IHiveManager;
import forestry.api.apiculture.hives.VillageHive;

/**
 * The hive manager used when the apiculture module is absent. Every registry is empty, so
 * ForestryBiomeModifier adds no hive feature to any biome.
 */
public enum FakeHiveManager implements IHiveManager {
	INSTANCE;

	// Every method on both interfaces is defaulted, so an empty implementation is the null object
	private static final IBeeModifier MODIFIER = new IBeeModifier() {
	};
	private static final IBeeListener LISTENER = new IBeeListener() {
	};

	@Override
	public boolean isLoaded() {
		return false;
	}

	@Override
	public List<IHive> getHives() {
		return List.of();
	}

	@Override
	public ImmutableList<VillageHive> getCommonVillageHives() {
		return ImmutableList.of();
	}

	@Override
	public ImmutableList<VillageHive> getRareVillageHives() {
		return ImmutableList.of();
	}

	@Override
	public List<IHiveDrop> getDrops(ResourceLocation id) {
		return List.of();
	}

	@Override
	public float getSwarmingMaterialChance(Item swarmItem) {
		return 0.0f;
	}

	@Override
	public IBeekeepingLogic createBeekeepingLogic(IBeeHousing housing) {
		return FakeBeekeepingLogic.INSTANCE;
	}

	@Override
	public IBeeModifier createBeeHousingModifier(IBeeHousing housing) {
		return MODIFIER;
	}

	@Override
	public IBeeListener createBeeHousingListener(IBeeHousing housing) {
		return LISTENER;
	}
}
```

If `FakeBeekeepingLogic` is not an enum with an `INSTANCE` constant, use whatever singleton form it
has; check with `grep -n "class\|enum\|INSTANCE" src/main/java/forestry/apiimpl/fake/FakeBeekeepingLogic.java`
and adjust this one line.

- [ ] **Step 5: Write FakeCharcoalManager**

```java
package forestry.apiimpl.fake;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.world.level.block.state.BlockState;

import forestry.api.arboriculture.ICharcoalManager;
import forestry.api.arboriculture.ICharcoalPileWall;

/**
 * The charcoal manager used when the arboriculture module is absent. Returned by
 * {@link FakeTreeManager#getCharcoalManager()}, which promises non-null.
 */
@SuppressWarnings("deprecation")
public enum FakeCharcoalManager implements ICharcoalManager {
	INSTANCE;

	@Nullable
	@Override
	public ICharcoalPileWall getWall(BlockState state) {
		return null;
	}

	@Override
	public List<ICharcoalPileWall> getWalls() {
		return List.of();
	}
}
```

`ICharcoalManager` carries `@Deprecated`, hence the suppression; keep it so the build stays
warning-clean.

- [ ] **Step 6: Write FakeTreeManager**

```java
package forestry.apiimpl.fake;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import forestry.api.ForestryConstants;
import forestry.api.arboriculture.ICharcoalManager;
import forestry.api.arboriculture.ITreeManager;
import forestry.api.arboriculture.IWoodType;
import forestry.api.arboriculture.WoodBlockKind;

/**
 * The tree manager used when the arboriculture module is absent. Nothing is waxable and no wood
 * type is registered, so ItemRefractoryWax finds no target and passes the interaction through.
 */
public enum FakeTreeManager implements ITreeManager {
	INSTANCE;

	// An id no datapack defines. Vanilla resolves an undefined tag as empty rather than erroring,
	// so these are safe to query and match nothing
	private static final TagKey<Block> EMPTY_BLOCK_TAG = TagKey.create(Registries.BLOCK, ForestryConstants.forestry("empty"));
	private static final TagKey<Item> EMPTY_ITEM_TAG = TagKey.create(Registries.ITEM, ForestryConstants.forestry("empty"));

	@Override
	public boolean isLoaded() {
		return false;
	}

	@Nullable
	@Override
	public Block getRefractoryWaxed(Block block) {
		return null;
	}

	@Override
	public ICharcoalManager getCharcoalManager() {
		return FakeCharcoalManager.INSTANCE;
	}

	@Override
	public ItemStack getStack(IWoodType woodType, WoodBlockKind kind, boolean fireproof) {
		return ItemStack.EMPTY;
	}

	@Override
	public BlockState getBlock(IWoodType woodType, WoodBlockKind kind, boolean fireproof) {
		return Blocks.AIR.defaultBlockState();
	}

	@Override
	public TagKey<Block> getLogBlockTag(IWoodType kind, boolean fireproof) {
		return EMPTY_BLOCK_TAG;
	}

	@Override
	public TagKey<Item> getLogItemTag(IWoodType kind, boolean fireproof) {
		return EMPTY_ITEM_TAG;
	}

	@Override
	public List<IWoodType> getRegisteredWoodTypes() {
		return List.of();
	}

	@Override
	public void register(IWoodType woodType, WoodBlockKind woodBlockKind, boolean fireproof, BlockState blockState, Supplier<Item> itemStack) {
	}

	@Override
	public void registerLogTag(IWoodType woodType, boolean fireproof, TagKey<Block> logBlockTag, TagKey<Item> logItemTag) {
	}
}
```

`register` and `registerLogTag` deliberately swallow. Without arboriculture there is nothing to
register into, and an addon calling them on a partial install should be inert rather than crash -
that is the whole of D7.

- [ ] **Step 7: Run the test and watch it pass**

```bash
./gradlew compileJava compileTestJava
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: **all 105 required tests passed** (101 plus the four added here).

- [ ] **Step 8: Verify the gates**

```bash
./gradlew checkApiBoundary checkBaseBoundary
./gradlew runData && git status --porcelain src/generated/resources
```

Expected: api clean, `checkBaseBoundary: 21 known leaking file(s) remaining`, no datagen diff.
**If `checkBaseBoundary` rose, one of these no-ops named a content class** - most likely
`FakeBeekeepingLogic` if task 2 was skipped. Fix it rather than baselining it.

- [ ] **Step 9: Commit**

```bash
git add -A src/main/java/forestry src/test/java/forestry
git commit -m "core: no-op farming, hive and tree managers

D7's absent-module semantics. Each is a null object in the established Fake*
idiom and returns the degenerate value the interface already permits: empty
collections, null from the two @Nullable methods, ItemStack.EMPTY, air, and a
tag id no datapack defines, which vanilla resolves as empty.

Nothing reaches these while every module ships in one jar, so the new GameTests
construct them directly. They pass null arguments on purpose: a no-op must not
dereference what it is given, because a caller on a partial install may have
nothing meaningful to pass."
```

---

### Task 4: The client no-ops, and the IClientHelper crash

Three client managers plus `IClientHelper`. The helper is the urgent one: phase 5a left
`ForestryClientApiImpl:17` resolving it with `ServiceLoader.load(...).findFirst().orElseThrow()` in a
field initialiser, so without arboriculture **every** client start throws while constructing
`IForestryClientApi.INSTANCE`, not merely the paths that draw leaves. Phase 5a's own `todo` says to
replace it here.

`ILeafSprite` needs a constant to hand back, and api already has the matching one for tints
(`ILeafTint.DEFAULT`), so this adds the symmetric `ILeafSprite.MISSING`.

**Files:**
- Modify: `src/main/java/forestry/api/client/arboriculture/ILeafSprite.java`
- Create: `src/main/java/forestry/apiimpl/client/fake/FakeBeeClientManager.java`
- Create: `src/main/java/forestry/apiimpl/client/fake/FakeTreeClientManager.java`
- Create: `src/main/java/forestry/apiimpl/client/fake/FakeButterflyClientManager.java`
- Create: `src/main/java/forestry/apiimpl/client/fake/FakeClientHelper.java`
- Test: `src/test/java/forestry/gametest/NoOpClientManagerTest.java`

**Interfaces:**
- Consumes: `isLoaded()` from task 1.
- Produces: `ILeafSprite.MISSING`, `FakeBeeClientManager.INSTANCE`, `FakeTreeClientManager.INSTANCE`,
  `FakeButterflyClientManager.INSTANCE`, `FakeClientHelper.INSTANCE`. Task 5 installs all four.

**These classes must not touch `net.minecraft.client`.** They live under `apiimpl/client/` by
convention but are constructed by a GameTest on a dedicated server, so `MissingTextureAtlasSprite` is
out; they use a plain `ResourceLocation` instead.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/forestry/gametest/NoOpClientManagerTest.java`:

```java
package forestry.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.client.arboriculture.ILeafSprite;
import forestry.api.genetics.ILifeStage;
import forestry.apiimpl.client.fake.FakeBeeClientManager;
import forestry.apiimpl.client.fake.FakeButterflyClientManager;
import forestry.apiimpl.client.fake.FakeClientHelper;
import forestry.apiimpl.client.fake.FakeTreeClientManager;

/**
 * Constructs the client no-ops on a dedicated server. That is the point of the test as much as the
 * assertions are: if one of these ever reaches for a net.minecraft.client type, this fails with
 * NoClassDefFoundError rather than crashing a player's client after the jars are split.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class NoOpClientManagerTest {
	@GameTest(template = "empty")
	public static void noOpClientManagersReportNotLoaded(GameTestHelper helper) {
		if (FakeBeeClientManager.INSTANCE.isLoaded()
				|| FakeTreeClientManager.INSTANCE.isLoaded()
				|| FakeButterflyClientManager.INSTANCE.isLoaded()) {
			helper.fail("A no-op client manager reported itself loaded");
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void noOpClientManagersReturnNonNull(GameTestHelper helper) {
		ILifeStage stage = forestry.api.apiculture.genetics.BeeLifeStage.DRONE;

		if (FakeBeeClientManager.INSTANCE.getDefaultModelLocation(stage) == null
				|| FakeBeeClientManager.INSTANCE.getModelLocation(stage, ForestryConstants.forestry("forest")) == null
				|| !FakeBeeClientManager.INSTANCE.getAllModelLocations(stage).isEmpty()) {
			helper.fail("No-op bee client manager broke its contract");
			return;
		}
		if (FakeTreeClientManager.INSTANCE.getLeafSprite(null) == null
				|| FakeTreeClientManager.INSTANCE.getTint(null) == null
				|| FakeTreeClientManager.INSTANCE.getDefaultSaplingModels() == null
				|| !FakeTreeClientManager.INSTANCE.getAllLeafSprites().isEmpty()
				|| !FakeTreeClientManager.INSTANCE.getAllSaplingModels().isEmpty()) {
			helper.fail("No-op tree client manager broke its contract");
			return;
		}
		if (FakeButterflyClientManager.INSTANCE.getDefaultTextures() == null
				|| !FakeButterflyClientManager.INSTANCE.getAllTextures().isEmpty()) {
			helper.fail("No-op butterfly client manager broke its contract");
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void noOpClientHelperReturnsNonNull(GameTestHelper helper) {
		if (FakeClientHelper.INSTANCE.createNoneTint() == null
				|| FakeClientHelper.INSTANCE.createBiomeTint() == null
				|| FakeClientHelper.INSTANCE.createLeafSprite(ForestryConstants.forestry("leaves")) == null) {
			helper.fail("No-op client helper returned null");
			return;
		}
		if (ILeafSprite.MISSING.get(false, false) == null || ILeafSprite.MISSING.getParticle() == null) {
			helper.fail("ILeafSprite.MISSING returned null");
			return;
		}
		helper.succeed();
	}
}
```

`BeeLifeStage` is fully qualified inline because it is the only apiculture reference in the file and
importing it would read as though the test depends on bee content; it does not, it needs any
`ILifeStage`. If `BeeLifeStage` is not at `forestry.api.apiculture.genetics`, find it with
`grep -rn "enum BeeLifeStage" src/main/java` and correct the one reference.

- [ ] **Step 2: Run it and watch it fail to compile**

```bash
./gradlew compileTestJava
```

Expected: FAIL, `package forestry.apiimpl.client.fake does not exist` and `cannot find symbol:
variable MISSING`.

- [ ] **Step 3: Add ILeafSprite.MISSING**

In `src/main/java/forestry/api/client/arboriculture/ILeafSprite.java`, add as the first member:

```java
	/**
	 * The sprite used when no leaf sprite is registered for a species, and by the no-op tree client
	 * manager that base installs when the arboriculture module is absent.
	 *
	 * @since 2.10.0
	 */
	ILeafSprite MISSING = new ILeafSprite() {
		private static final ResourceLocation LOCATION = ResourceLocation.withDefaultNamespace("missingno");

		@Override
		public ResourceLocation get(boolean pollinated, boolean fancy) {
			return LOCATION;
		}

		@Override
		public ResourceLocation getParticle() {
			return LOCATION;
		}
	};
```

`minecraft:missingno` is the id vanilla's own missing texture resolves to, and naming it as a plain
`ResourceLocation` keeps this interface loadable on a dedicated server. Do **not** reach for
`MissingTextureAtlasSprite.getLocation()` - it is a client class, and `ILeafSprite` is api, which
both sides load.

- [ ] **Step 4: Write the three client managers**

`src/main/java/forestry/apiimpl/client/fake/FakeBeeClientManager.java`:

```java
package forestry.apiimpl.client.fake;

import java.util.Collection;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import forestry.api.apiculture.IBeeHousing;
import forestry.api.client.apiculture.IBeeClientManager;
import forestry.api.genetics.IGenome;
import forestry.api.genetics.ILifeStage;

/**
 * The bee client manager used when the apiculture module is absent. No model is registered, so
 * CoreClientHandler adds no extra baked models and bee effects draw no particles.
 */
public enum FakeBeeClientManager implements IBeeClientManager {
	INSTANCE;

	private static final ResourceLocation MISSING = ResourceLocation.withDefaultNamespace("missingno");

	@Override
	public boolean isLoaded() {
		return false;
	}

	@Override
	public ResourceLocation getModelLocation(ILifeStage stage, ResourceLocation speciesId) {
		return MISSING;
	}

	@Override
	public ResourceLocation getDefaultModelLocation(ILifeStage stage) {
		return MISSING;
	}

	@Override
	public Collection<ResourceLocation> getAllModelLocations(ILifeStage stage) {
		return List.of();
	}

	@Override
	public void addBeeHiveParticles(IBeeHousing housing, IGenome genome, List<BlockPos> flowerPositions) {
	}
}
```

`src/main/java/forestry/apiimpl/client/fake/FakeTreeClientManager.java`:

```java
package forestry.apiimpl.client.fake;

import java.util.Collection;
import java.util.List;

import com.mojang.datafixers.util.Pair;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.client.arboriculture.ILeafSprite;
import forestry.api.client.arboriculture.ILeafTint;
import forestry.api.client.arboriculture.ITreeClientManager;

/**
 * The tree client manager used when the arboriculture module is absent. Leaves fall back to the
 * missing sprite and the vanilla foliage color, and no sapling model is registered.
 */
public enum FakeTreeClientManager implements ITreeClientManager {
	INSTANCE;

	private static final ResourceLocation MISSING = ResourceLocation.withDefaultNamespace("missingno");
	private static final Pair<ResourceLocation, ResourceLocation> MISSING_MODELS = Pair.of(MISSING, MISSING);

	@Override
	public boolean isLoaded() {
		return false;
	}

	@Override
	public ILeafSprite getLeafSprite(@Nullable ITreeSpecies species) {
		return ILeafSprite.MISSING;
	}

	@Override
	public Collection<ILeafSprite> getAllLeafSprites() {
		return List.of();
	}

	@Override
	public ILeafTint getTint(@Nullable ITreeSpecies species) {
		return ILeafTint.DEFAULT;
	}

	@Override
	public Pair<ResourceLocation, ResourceLocation> getSaplingModels(ITreeSpecies species) {
		return MISSING_MODELS;
	}

	@Override
	public Pair<ResourceLocation, ResourceLocation> getDefaultSaplingModels() {
		return MISSING_MODELS;
	}

	@Override
	public Collection<Pair<ResourceLocation, ResourceLocation>> getAllSaplingModels() {
		return List.of();
	}
}
```

`src/main/java/forestry/apiimpl/client/fake/FakeButterflyClientManager.java`:

```java
package forestry.apiimpl.client.fake;

import java.util.Collection;
import java.util.List;

import com.mojang.datafixers.util.Pair;

import net.minecraft.resources.ResourceLocation;

import forestry.api.client.lepidopterology.IButterflyClientManager;
import forestry.api.lepidopterology.genetics.IButterflySpecies;

/**
 * The butterfly client manager used when the lepidopterology module is absent. Every texture
 * resolves to the missing one, and nothing is registered to iterate.
 */
public enum FakeButterflyClientManager implements IButterflyClientManager {
	INSTANCE;

	private static final ResourceLocation MISSING = ResourceLocation.withDefaultNamespace("missingno");
	private static final Pair<ResourceLocation, ResourceLocation> MISSING_TEXTURES = Pair.of(MISSING, MISSING);

	@Override
	public boolean isLoaded() {
		return false;
	}

	@Override
	public Pair<ResourceLocation, ResourceLocation> getTextures(IButterflySpecies species) {
		return MISSING_TEXTURES;
	}

	@Override
	public Pair<ResourceLocation, ResourceLocation> getDefaultTextures() {
		return MISSING_TEXTURES;
	}

	@Override
	public Collection<Pair<ResourceLocation, ResourceLocation>> getAllTextures() {
		return List.of();
	}
}
```

- [ ] **Step 5: Write FakeClientHelper**

```java
package forestry.apiimpl.client.fake;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;

import javax.annotation.Nullable;

import forestry.api.client.arboriculture.ILeafSprite;
import forestry.api.client.arboriculture.ILeafTint;
import forestry.api.client.plugin.IClientHelper;

/**
 * The client helper used when the arboriculture module is absent. Every method on IClientHelper
 * builds an arboriculture object, so base can only return the api-level equivalents.
 *
 * <p>This one is not optional the way the managers are: ForestryLeafSprites resolves the helper from
 * a static initializer, so without a fallback the ServiceLoader lookup throws while any client is
 * starting, not only on the paths that draw leaves.
 */
public enum FakeClientHelper implements IClientHelper {
	INSTANCE;

	private static final ILeafTint NONE = (level, pos) -> 0xffffff;

	@Override
	public ILeafTint createNoneTint() {
		return NONE;
	}

	@Override
	public ILeafTint createFixedTint(TextColor color) {
		int value = color.getValue();
		return (level, pos) -> value;
	}

	@Override
	public ILeafTint createBiomeTint() {
		return ILeafTint.DEFAULT;
	}

	@Override
	public ILeafTint createBiomeTint(Int2IntFunction mapper) {
		return (level, pos) -> mapper.applyAsInt(ILeafTint.DEFAULT.get(level, pos));
	}

	@Override
	public ILeafSprite createLeafSprite(ResourceLocation id) {
		return ILeafSprite.MISSING;
	}
}
```

Check the real `ClientHelper.createNoneTint()` before writing `0xffffff`:

```bash
grep -n "NONE" src/main/java/forestry/arboriculture/client/FixedLeafTint.java
```

Match whatever value `FixedLeafTint.NONE` uses so the two agree. Unused parameter warnings on
`level`/`pos` are expected in the lambdas and are fine.

- [ ] **Step 6: Run the test and watch it pass**

```bash
./gradlew compileJava compileTestJava
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: **all 108 required tests passed** (105 plus the three added here). If instead a test fails
with `NoClassDefFoundError` naming a `net.minecraft.client` type, one of the no-ops reached for a
client class - find it and replace it with a plain `ResourceLocation`.

- [ ] **Step 7: Verify the gates**

```bash
./gradlew checkApiBoundary checkBaseBoundary
./gradlew runData && git status --porcelain src/generated/resources
```

Expected: api clean, 21 files, no datagen diff.

- [ ] **Step 8: Commit**

```bash
git add -A src/main/java/forestry src/test/java/forestry
git commit -m "core: no-op client managers and client helper

The three client managers follow the server ones. ILeafSprite gains MISSING,
symmetric with the ILeafTint.DEFAULT that api already carried.

FakeClientHelper is not optional the way the managers are: phase 5a left
ForestryClientApiImpl resolving the helper with orElseThrow in a field
initialiser, so with arboriculture absent every client start would throw while
constructing IForestryClientApi.INSTANCE, not only the leaf-drawing paths.

None of these may touch net.minecraft.client - they use a plain missingno
ResourceLocation rather than MissingTextureAtlasSprite, and the new GameTests
construct them on a dedicated server so that a regression fails here rather
than in a player's client after phase 9."
```

---

### Task 5: Install the no-ops as the default

Six fields become non-null from construction; six getters stop throwing.

**Scope carefully.** `ForestryApiImpl` throws from eight getters and `ForestryClientApiImpl` from
five. Only six of those thirteen are split-module managers. The rest - `IErrorManager`,
`IFilterManager`, `IGeneticManager`, `ICircuitManager`, `IPollenManager`, `ITextureManager`,
`IGeneticClientManager` - are supplied by base itself, so a null there is a real lifecycle bug and
**must keep throwing.** Silencing them would convert an ordering error into a mystery NPE later.

**Files:**
- Modify: `src/main/java/forestry/apiimpl/ForestryApiImpl.java`
- Modify: `src/main/java/forestry/apiimpl/client/ForestryClientApiImpl.java`

**Interfaces:**
- Consumes: `FakeFarmingManager.INSTANCE`, `FakeHiveManager.INSTANCE`, `FakeTreeManager.INSTANCE`
  from task 3; `FakeBeeClientManager.INSTANCE`, `FakeTreeClientManager.INSTANCE`,
  `FakeButterflyClientManager.INSTANCE`, `FakeClientHelper.INSTANCE` from task 4.
- Produces: `getFarmingManager`, `getHiveManager`, `getTreeManager`, `getBeeManager`,
  `getTreeManager`, `getButterflyManager` never throw and never return null. The setters are
  unchanged - task 6 relies on them still overwriting.

- [ ] **Step 1: Change the three server fields**

In `ForestryApiImpl`, replace the `@Nullable` declarations for the three split managers:

```java
	private IFarmingManager farmingManager = FakeFarmingManager.INSTANCE;
	private IHiveManager hiveManager = FakeHiveManager.INSTANCE;
	private ITreeManager treeManager = FakeTreeManager.INSTANCE;
```

Leave the other five `@Nullable` fields exactly as they are.

- [ ] **Step 2: Simplify the three server getters**

```java
	@Override
	public IFarmingManager getFarmingManager() {
		return this.farmingManager;
	}

	@Override
	public IHiveManager getHiveManager() {
		return this.hiveManager;
	}

	@Override
	public ITreeManager getTreeManager() {
		return this.treeManager;
	}
```

Add `import forestry.apiimpl.fake.FakeFarmingManager;` and the two siblings. Do not touch
`getErrorManager`, `getFilterManager`, `getGeneticManager`, `getCircuitManager` or
`getPollenManager`.

- [ ] **Step 3: Do the same on the client side**

In `ForestryClientApiImpl`:

```java
	private IBeeClientManager beeManager = FakeBeeClientManager.INSTANCE;
	private ITreeClientManager treeManager = FakeTreeClientManager.INSTANCE;
	private IButterflyClientManager butterflyManager = FakeButterflyClientManager.INSTANCE;
```

with the three matching getters reduced to `return this.beeManager;` and so on, and the helper
field's `orElseThrow` replaced:

```java
	// Resolved by service rather than constructed: every IClientHelper method returns an
	// arboriculture type, and ForestryLeafSprites resolves the helper from a static initializer, too
	// early for any lifecycle hook to have installed one. Falls back to the no-op when the
	// arboriculture module is absent.
	private final IClientHelper helper = ServiceLoader.load(IClientHelper.class).findFirst().orElse(FakeClientHelper.INSTANCE);
```

Delete the `todo` phase 5a left above that field - this is the change it asked for. Leave
`getTextureManager` and `getGeneticManager` throwing.

- [ ] **Step 4: Verify the real managers still win**

This is the one thing that can silently break here: if a module's install ran before the field
initialiser, or not at all, the getters now return a no-op **instead of throwing**, and the failure
becomes invisible. `ManagerLoadedTest` from task 1 is exactly that check on the server side, so it
must still pass.

```bash
./gradlew compileJava compileTestJava
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: **all 108 required tests passed.** If `serverManagersAreLoaded` now fails, a real manager
is no longer being installed - do not adjust the test, find the install.

- [ ] **Step 5: Verify the rest**

```bash
./gradlew checkApiBoundary checkBaseBoundary
./gradlew runData && git status --porcelain src/generated/resources
```

Expected: api clean, `checkBaseBoundary: 21 known leaking file(s) remaining`, no datagen diff.

- [ ] **Step 6: Commit**

```bash
git add -A src/main/java/forestry
git commit -m "api: default the six splittable managers to their no-ops

The three server and three client getters no longer throw: their fields start
at the no-op and a module overwrites them with the real implementation.

The other seven getters keep throwing on purpose. Those managers are supplied by
base itself, so a null there is a lifecycle ordering bug and silencing it would
turn a clear error into an NPE somewhere later.

ManagerLoadedTest is what keeps this honest. Now that a missing install returns
a no-op instead of throwing, it is the only thing that would notice."
```

---

### Task 6: Stop PluginManager constructing content managers

The four leaks carried over from phase 5a:

```
26:import forestry.apiculture.client.BeeClientManager;
29:import forestry.arboriculture.client.TreeClientManager;
37:import forestry.farming.FarmingManager;
38:import forestry.farming.plugin.FarmingRegistration;
```

All four are base assembling a content module's manager. Task 5 made the inversion available: base no
longer needs to build anything, because the field already holds a working no-op. So each module
installs its own, through the same `IForestryModule` mechanism phase 4 used for
`registerReloadListeners` and `syncDatapack`.

**Files:**
- Modify: `src/main/java/forestry/api/modules/IForestryModule.java`
- Modify: `src/main/java/forestry/core/ModuleCore.java:145-150`
- Modify: `src/main/java/forestry/farming/ModuleFarming.java`
- Modify: `src/main/java/forestry/apiculture/ModuleApiculture.java`
- Modify: `src/main/java/forestry/arboriculture/ModuleArboriculture.java`
- Modify: `src/main/java/forestry/apiimpl/plugin/PluginManager.java`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: `PluginManager.getLoadedPlugins()` (added in step 2), the `ClientRegistration` getters
  `getDefaultBeeModel(ILifeStage)`, `getBeeModels()`, `getLeafSprites()`, `getTints()`,
  `getSaplingModels()`, all already public.
- Produces: two defaulted `IForestryModule` hooks -
  `default void installManagers() {}` and
  `default void installClientManagers(IClientRegistration registration) {}`.

- [ ] **Step 1: Add the two hooks**

In `src/main/java/forestry/api/modules/IForestryModule.java`, next to `registerReloadListeners`:

```java
	/**
	 * Called after item registration, in module dependency order. Used by a module to build its
	 * manager from the plugin data and install it. Base ships a no-op for every manager whose module
	 * can be absent, so a module that does not implement this leaves the no-op in place.
	 */
	default void installManagers() {
	}

	/**
	 * Called during client plugin registration, after every plugin has registered. Used by a module
	 * to build its client manager from the assembled registration and install it.
	 *
	 * @param registration The completed client registration
	 */
	default void installClientManagers(IClientRegistration registration) {
	}
```

Add `import forestry.api.client.plugin.IClientRegistration;`. `IClientRegistration` is api, so this
does not trip `checkApiBoundary`.

- [ ] **Step 2: Expose the loaded plugins**

`registerFarming` moves out of `PluginManager` wholesale, so the module needs the plugin list. In
`PluginManager`, next to the `LOADED_PLUGINS` field:

```java
	/**
	 * @return The loaded plugins, in the order their hooks are called
	 */
	public static List<IForestryPlugin> getLoadedPlugins() {
		return Collections.unmodifiableList(LOADED_PLUGINS);
	}
```

Add `java.util.Collections` if it is not already imported.

- [ ] **Step 3: Move registerFarming into ModuleFarming**

Delete `PluginManager.registerFarming()` entirely, and in `ModuleFarming` add:

```java
	@Override
	public void installManagers() {
		FarmingRegistration registration = new FarmingRegistration();

		for (IForestryPlugin plugin : PluginManager.getLoadedPlugins()) {
			try {
				plugin.registerFarming(registration);
			} catch (Throwable t) {
				throw new RuntimeException("An error was thrown by plugin " + plugin.id() + " during IForestryPlugin.registerFarming", t);
			}
		}

		// Defensive copy of fertilizers
		FarmingManager manager = new FarmingManager(new Object2IntOpenHashMap<>(registration.getFertilizers()), registration.buildFarmTypes());

		((ForestryApiImpl) IForestryApi.INSTANCE).setFarmingManager(manager);
	}
```

The body is the old one verbatim; keep the comment. `ModuleFarming` needs imports for
`forestry.api.IForestryApi`, `forestry.api.plugin.IForestryPlugin`, `forestry.apiimpl.ForestryApiImpl`,
`forestry.apiimpl.plugin.PluginManager`, `forestry.farming.plugin.FarmingRegistration` and
`it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap`. `FarmingManager` is in `forestry.farming`, the
same package as `ModuleFarming`, so it needs **no** import - check before adding one.

The cast to `ForestryApiImpl` is the idiom already used at every install site in `PluginManager`.

In `ModuleCore.postItemRegistry()`, replace the `PluginManager.registerFarming();` line with a loop
over the modules, matching the `registerReloadListeners` loop phase 4 added:

```java
	private static void postItemRegistry() {
		PluginManager.registerGenetics();

		// Modules load in dependency order (see ForestryModuleManager). A module that supplies one of
		// the api managers installs it here, over the no-op base put there at construction.
		for (IForestryModule module : IForestryApi.INSTANCE.getModuleManager().getLoadedModules()) {
			module.installManagers();
		}

		PluginManager.registerPollen();
	}
```

Keep `registerGenetics` first and `registerPollen` last - the farming install sat between them and
must stay there.

- [ ] **Step 4: Move the two client manager constructions**

In `PluginManager.registerClient()`, delete the bee block (the two `IdentityHashMap`s, the
`for (ILifeStage stage : ...)` loop and the `setBeeManager` call) and the tree block (the three
`HashMap` locals, the `models` synthesis loop and the `setTreeManager` call). Replace both with one
module pass, placed where the bee block was so the butterfly block below still runs after it:

```java
		// Each module builds its own client manager from the completed registration and installs it
		// over the no-op. See IForestryModule.installClientManagers.
		for (IForestryModule module : IForestryApi.INSTANCE.getModuleManager().getLoadedModules()) {
			module.installClientManagers(registration);
		}
```

Then in `ModuleApiculture`:

```java
	@Override
	public void installClientManagers(IClientRegistration registration) {
		ClientRegistration impl = (ClientRegistration) registration;

		// id-keyed: resolving a specific species happens at render time, so the (possibly
		// datapack-driven) species list is not needed here.
		IdentityHashMap<ILifeStage, ResourceLocation> defaultModels = new IdentityHashMap<>();
		IdentityHashMap<ILifeStage, Map<ResourceLocation, ResourceLocation>> customModels = new IdentityHashMap<>();

		for (ILifeStage stage : SpeciesUtil.BEE_TYPE.get().getLifeStages()) {
			ResourceLocation defaultModel = Objects.requireNonNull(impl.getDefaultBeeModel(stage), "IClientRegistration.setDefaultBeeModel has not been called for life stage " + stage.getSerializedName() + ", unable to resolve bee default model");
			defaultModels.put(stage, defaultModel);
			customModels.put(stage, impl.getBeeModels().getOrDefault(stage, Map.of()));
		}

		((ForestryClientApiImpl) IForestryClientApi.INSTANCE).setBeeManager(new BeeClientManager(defaultModels, customModels));
	}
```

and in `ModuleArboriculture`:

```java
	@Override
	public void installClientManagers(IClientRegistration registration) {
		ClientRegistration impl = (ClientRegistration) registration;

		// id-keyed: resolving a species happens at render time by id, so the (datapack-driven) species
		// list is not needed to build the sprite/model maps below.
		HashMap<ResourceLocation, ILeafSprite> spritesById = impl.getLeafSprites();
		HashMap<ResourceLocation, ILeafTint> tintsById = impl.getTints();
		HashMap<ResourceLocation, Pair<ResourceLocation, ResourceLocation>> modelsById = impl.getSaplingModels();

		// The escritoire-color tint fallback (for the ~40 built-in species that register no explicit
		// client tint) is applied lazily at render time in TreeClientManager#getTint from the species
		// object itself, so no species-list iteration is needed here and datapack-added species get the
		// same fallback reloadably.

		// For any species id that has a leaf sprite but no explicit sapling model, synthesize the
		// default-path pair (removing the "tree_" prefix), exactly as the old per-species loop did.
		Map<ResourceLocation, Pair<ResourceLocation, ResourceLocation>> models = new HashMap<>(modelsById);
		for (ResourceLocation id : spritesById.keySet()) {
			models.computeIfAbsent(id, sid -> {
				String path = sid.getPath().replace("tree_", "");
				return Pair.of(
					ResourceLocation.fromNamespaceAndPath(sid.getNamespace(), "block/" + path + "_sapling"),
					ResourceLocation.fromNamespaceAndPath(sid.getNamespace(), "item/" + path + "_sapling")
				);
			});
		}

		((ForestryClientApiImpl) IForestryClientApi.INSTANCE).setTreeManager(new TreeClientManager(
			new HashMap<>(spritesById), new HashMap<>(tintsById), models
		));
	}
```

Both bodies are the originals verbatim, including the comments, with `registration.` renamed to
`impl.`. Copy them out of `git show HEAD:src/main/java/forestry/apiimpl/plugin/PluginManager.java`
rather than retyping, then diff to be sure.

Casting the api interface to the base impl is the same idiom as the
`((ForestryClientApiImpl) IForestryClientApi.INSTANCE)` two lines below it. A module importing a base
class is allowed and is not a boundary leak.

**Leave the butterfly block where it is.** `ButterflyClientManager` is in `apiimpl` and does not
leak, so moving it is out of scope for this phase; phase 7 relocates it with the rest of the package.

- [ ] **Step 5: Confirm the file is clean, then trim**

```bash
./gradlew compileJava compileTestJava
grep -cE "^import (static )?forestry\.(apiculture|arboriculture|lepidopterology|farming|cultivation|mail)\." src/main/java/forestry/apiimpl/plugin/PluginManager.java
```

Expected: `0`. Also delete the `todo` block above `registerFarming` that phase 5a added - it is
resolved. Check for imports the deletions orphaned:

```bash
for c in IdentityHashMap Objects ILifeStage SpeciesUtil Pair ILeafSprite ILeafTint Object2IntOpenHashMap; do
  echo "$c: $(grep -c "\b$c\b" src/main/java/forestry/apiimpl/plugin/PluginManager.java)"
done
```

Any name whose count is `1` appears only in its own import line and must be removed. This is the
check phase 5a's plan got wrong by asserting a count from memory - run it, do not assume.

Remove `apiimpl/plugin/PluginManager.java` from `gradle/base-boundary-baseline.txt`.

- [ ] **Step 6: Verify**

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `checkBaseBoundary: 20 known leaking file(s) remaining`, api clean, no datagen diff, all
108 tests passed.

`runData` is the load-bearing check for the client half. `core/data/Data.java:76` calls
`PluginManager.registerClient()`, so datagen exercises the whole moved client path; if a module's
`installClientManagers` never ran, the sapling models and leaf sprites would be missing and the
generated block state files would change.

Then confirm the moved bodies are faithful:

```bash
git show HEAD:src/main/java/forestry/apiimpl/plugin/PluginManager.java | grep -c "computeIfAbsent"
grep -c "computeIfAbsent" src/main/java/forestry/arboriculture/ModuleArboriculture.java
```

Expected: `1` and `1`.

- [ ] **Step 7: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "plugin: let each module install its own manager

The last four base-boundary leaks, carried over from phase 5a. All four were
base assembling a content module's manager, and task 5 is what made the
inversion available: the field already holds a working no-op, so base has
nothing left to build.

Two defaulted IForestryModule hooks, matching the two phase 4 added.
registerFarming moves into ModuleFarming whole, and the bee and tree client
manager assembly into their modules verbatim. A module that implements neither
leaves the no-op in place, which is what an absent jar looks like.

The butterfly client manager stays in PluginManager: ButterflyClientManager is
in apiimpl and does not leak, so phase 7 moves it with the rest of the package.

checkBaseBoundary: 21 -> 20 files, and the 20 are exactly bucket A."
```

---

### Task 7: Make the gate measure what ships

`checkBaseBoundary` now reports 20, and will keep reporting 20 until phase 8. That reads as a stall,
and it is wrong in a way worth fixing: **all 20 are under `forestry/core/data/**`, which
`build.gradle:410` already excludes from the jar.** The phase-6 condition - "the base artifact
references no split-jar types" - is met right now, and the build should be able to say so.

The spec also leaves an open question here, from phase 5a's handoff: `checkBaseBoundary` only sees
imports, while D3's real invariant is that the packaged artifact contains **zero references** to
split-jar types anywhere, because FML's annotation scanner resolves method signatures at boot. A
class that names a split type only in a signature or a constant-pool entry is invisible to an
import-based scan. This task answers that question by adding the check, which is cheap because the
compiled classes are right there.

**Files:**
- Modify: `build.gradle:140-198`
- Modify: `gradle/base-boundary-baseline.txt` (comment header only)

**Interfaces:**
- Consumes: nothing.
- Produces: `checkBaseBoundary` reporting packaged and datagen-only counts separately, and a new
  `checkBaseBytecode` task wired into `check`.

- [ ] **Step 1: Split the report by what ships**

In the `doLast` block of `checkBaseBoundary`, after `actual` is built and the `added`/`fixed`
comparisons have run, replace the single summary line:

```groovy
		var packaged = actual.findAll { !it.startsWith('core/data/') }
		var datagenOnly = actual.size() - packaged.size()

		logger.lifecycle("checkBaseBoundary: ${packaged.size()} packaged leaking file(s), "
				+ "${datagenOnly} datagen-only (excluded from the jar by the 'forestry/core/data/**' rule)")
```

The ratchet itself does not change - both directions still fail, and the baseline still lists all 20.
Only the headline number changes, and it changes to the one that corresponds to the phase-6 gate.

Update the comment above the task to say so:

```groovy
// Phase 2 of the feature reorg: the base artifact (everything that ships in the forestry jar and
// is not a content module) must stop referencing the five split modules. 68 files still do, so
// this gate ratchets against a checked-in baseline rather than failing outright: a leak that is
// not in the baseline is a new regression, and a baseline entry that no longer leaks is a stale
// baseline that must be trimmed. Both directions fail, which is what drives the count down.
//
// Files under core/data are reported separately: the jar task strips that package, so they cannot
// reach a player. They dissolve in phase 8 when datagen becomes a per-jar source set.
```

- [ ] **Step 2: Add the bytecode gate**

Imports are not the invariant. Add after `checkBaseBoundary`:

```groovy
// checkBaseBoundary reads imports, which is not what D3 actually requires. FML's annotation scanner
// resolves method signatures at boot via getDeclaredMethods0, so a base class that names a split
// type only in a descriptor - a parameter, a return type, a lambda synthetic - hard-crashes in
// production without ever writing an import. This reads the compiled classes instead, and skips
// the datagen package because the jar task strips it.
var checkBaseBytecode = tasks.register('checkBaseBytecode') {
	group = 'verification'
	description = 'Fails if a packaged base class references a split content module in its bytecode'

	dependsOn tasks.named('classes')
	outputs.upToDateWhen { false }

	doLast {
		var classesDir = new File(project.layout.buildDirectory.get().asFile, 'classes/java/main/forestry')
		var splitModules = ['apiculture', 'arboriculture', 'lepidopterology', 'farming', 'cultivation', 'mail']
		var basePackages = ['core', 'apiimpl', 'plugin', 'modules', 'compat']
		var offenders = new TreeSet<String>()

		var scan = { File file ->
			if (!file.name.endsWith('.class')) {
				return
			}
			var rel = classesDir.toPath().relativize(file.toPath()).toString()
			if (rel.startsWith('core/data/')) {
				return
			}
			// Class names live in the constant pool as modified UTF-8. ISO-8859-1 maps every byte to
			// exactly one char, so decoding with it is lossless for a substring search and finds
			// descriptors and signatures as well as the names an import would have produced
			var text = new String(file.bytes, 'ISO-8859-1')
			for (String mod : splitModules) {
				if (text.contains("forestry/${mod}/")) {
					offenders.add(rel + " -> forestry/" + mod)
				}
			}
		}

		classesDir.listFiles({ File f -> f.isFile() } as FileFilter).each(scan)
		basePackages.each { pkg ->
			var dir = new File(classesDir, pkg)
			if (dir.exists()) {
				dir.eachFileRecurse(groovy.io.FileType.FILES, scan)
			}
		}

		if (!offenders.isEmpty()) {
			throw new GradleException(
					"Packaged base class(es) reference a split content module in bytecode:\n  "
					+ offenders.join('\n  ')
					+ "\nImports are not the invariant here - see the D3 safety condition in the reorg spec.")
		}
		logger.lifecycle("checkBaseBytecode: no packaged base class references a split module")
	}
}
```

Note the needle is `forestry/<module>/` with the trailing slash. Without it, `forestry/api` would
match nothing useful but `forestry/apiculture` would still be distinguishable from
`forestry/api/apiculture/` only by luck; with it, an api type like
`forestry/api/apiculture/IBeeHousing` cannot match, because the character after `forestry/` is `a`,
`p`, `i`, `/`. Base classes name api types constantly, so getting this wrong would drown the output.

Wire it in next to the existing gate:

```groovy
tasks.named('check') {
	dependsOn checkBaseBoundary
	dependsOn checkBaseBytecode
}
```

Match whatever form the existing `check` block uses at `build.gradle:200-202` rather than replacing
it wholesale.

- [ ] **Step 3: Run it and see what it finds**

```bash
./gradlew classes checkBaseBytecode
```

**Expect this to fail the first time, and read the output before changing anything.** Three
outcomes, each meaning something different:

1. **Clean.** The import gate was sufficient after all. Record that - it is a real finding and it
   closes the spec's open question in the cheap direction.
2. **A handful of classes.** These are genuine D3 hazards that six phases of import-driven work
   could not see. Fix them if the fix is small; if not, add a `gradle/base-bytecode-baseline.txt`
   in the same ratchet shape as the import one and hand the list to phase 9. Do not delete the task
   to make the build green.
3. **Very many classes.** The byte search is matching something structural - most likely a base
   class legitimately implementing an api interface whose *own* descriptor mentions nothing split,
   but which sits next to a synthetic. Check two or three by hand with
   `javap -p -c build/classes/java/main/forestry/<path>.class | grep forestry/<module>` before
   concluding. If the check turns out to have a high false-positive rate, say so and drop it rather
   than shipping a gate nobody can act on.

Whichever outcome, write the result into the commit message. This step exists to answer a question,
not to reach a predetermined number.

- [ ] **Step 4: Verify**

```bash
./gradlew clean build 2>&1 | grep -E "checkBaseBoundary|checkBaseBytecode|BUILD"
```

Expected: `checkBaseBoundary: 0 packaged leaking file(s), 20 datagen-only (excluded from the jar by
the 'forestry/core/data/**' rule)`, a `checkBaseBytecode` line, and `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add build.gradle gradle/base-boundary-baseline.txt
git commit -m "build: gate on what ships, not on what imports

Two changes to the same question - what does the base artifact actually
contain.

checkBaseBoundary now reports packaged and datagen-only counts separately. All
20 remaining baselined files are under core/data, which the jar task already
strips, so the phase 6 condition is met now rather than in phase 8. The ratchet
is unchanged; only the headline number is.

checkBaseBytecode is new and answers the question phase 5a left open. D3's real
invariant is that the packaged artifact contains no reference to a split type
anywhere, because FML resolves method signatures at boot - a descriptor or a
lambda synthetic crashes production without ever writing an import, which is the
failure build.gradle already documents for the datagen package. Reading the
compiled classes catches what the import scan cannot."
```

---

### Task 8: Record phase 6 completion

**Files:**
- Modify: `docs/superpowers/specs/2026-07-30-feature-package-reorg-design.md`

- [ ] **Step 1: Confirm from a clean build**

```bash
./gradlew clean build
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `BUILD SUCCESSFUL`, `checkApiBoundary: forestry.api is clean`, `checkBaseBoundary: 0
packaged leaking file(s), 20 datagen-only`, the `checkBaseBytecode` line, all 108 tests passed.

- [ ] **Step 2: Update the sequencing table and the narrative**

Mark phase 6 `DONE` in the block at `## Sequencing`, and move the gate line so it reads honestly -
the gate is met for the packaged artifact, with the datagen files outstanding until phase 8.

Then add a phase 6 paragraph after the phase 5a one, recording:

- that the six managers have **59 call sites of which only 8 are outside the owning jar**, and all
  six runtime ones already handled the degenerate case, so D7 needed no defensive rewriting of
  callers;
- that `IClientHelper` was a live crash rather than a hypothetical - phase 5a's `orElseThrow` sat in
  a field initialiser, so an absent arboriculture jar would have thrown while constructing
  `IForestryClientApi.INSTANCE` on every client start;
- that seven of the thirteen throwing getters deliberately keep throwing, because their managers come
  from base and a null there is an ordering bug rather than an absent module;
- that `PluginManager` cleared through the same `IForestryModule` hook mechanism phase 4 introduced,
  and that task 5 is what made it possible - once the field holds a working no-op, base has nothing
  left to build;
- what `checkBaseBytecode` found in task 7, stated as measured;
- and the remaining shape: 0 packaged, 20 datagen-only, all bucket A.

- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/specs/2026-07-30-feature-package-reorg-design.md
git commit -m "docs: record phase 6 completion, base boundary clear for the packaged artifact"
```

---

## Notes for phase 7

- The baseline should end this phase at **20, all under `core/data/`**, and `checkBaseBoundary`
  should be reporting `0 packaged`. Phase 7 moves packages wholesale and must not regress that; run
  `checkBaseBytecode` after each move step, not only at the end, because a package move is exactly
  the operation that can introduce a descriptor-level reference without an import.
- `ButterflyClientManager` is still in `apiimpl` and still constructed by `PluginManager`. It does not
  leak, so this phase left it alone, but phase 7's move manifest should place it in lepidopterology
  alongside the two managers that moved in phases 4 and 5a - and if it moves, its construction has to
  move with it into `ModuleLepidopterology.installClientManagers`, exactly as the bee and tree ones
  did in task 6.
- The `forestry.apiimpl.fake` and `forestry.apiimpl.client.fake` packages created here are base and
  should land wherever phase 7 puts the rest of `apiimpl`. They have no content dependencies, so they
  move freely.
- Phase 8 dissolves the last 20 by making datagen a per-jar source set. At that point the
  packaged/datagen split added in task 7 becomes unnecessary and the report can go back to one
  number - but not before, or the number lies again.
