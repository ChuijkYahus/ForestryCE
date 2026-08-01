# Phase 1b: sever api to impl, the api-additions half

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Drive `checkApiBoundary` from 9 files / 16 imports to green, clearing the last
api-to-impl references.

**Architecture:** Three fix shapes. Five types turn out to reference only api or Minecraft
already, so they are straight relocations into `api` rather than new interfaces invented to
wrap them. Two api types call impl statics and need the call inverted behind an api interface
the impl implements. The three chromosome holders need their construction machinery, which
only they use, moved into api alongside them.

**Tech Stack:** Java 21, NeoForge 21.1.230, Minecraft 1.21.1, ModDevGradle 2.0.x. GameTests
only, no JUnit.

## Global Constraints

- Comment and Javadoc style is binding; see `CLAUDE.md`. ASCII only. `@return` and `@param`
  are noun-phrase fragments with no terminal period. Lowercase `todo`.
- Every task ends with `./gradlew checkApiBoundary` showing a strictly lower count, plus
  `./gradlew runData` producing no diff in `src/generated/resources`.
- Do not reformat or reorder imports beyond what a task requires. The `runData` diff is the
  oracle and unrelated edits pollute it.
- Every type moved into `api` becomes published surface in `apiJar` and javadoc. That is
  intended - this whole restructure is one breaking wave and no release is cut ahead of it.
- The JetBrains MCP server exposes `rename_refactoring` only, no move. These moves are small
  (2 to 26 referencing files each), so change the `package` line and fix imports directly, or
  drive IntelliJ's Move by hand.

## Starting state

`checkApiBoundary` reports 9 files / 16 imports:

```
apiculture/genetics/IBeeEffect.java              -> core.render.ParticleRender
apiculture/hives/IHiveManager.java               -> apiculture.VillageHive
genetics/alleles/BeeChromosomes.java             -> Forestry, ChromosomeFactory, SpeciesUtil
genetics/alleles/ButterflyChromosomes.java       -> Forestry, ChromosomeFactory, SpeciesUtil
genetics/alleles/TreeChromosomes.java            -> Forestry, ChromosomeFactory, SpeciesUtil
genetics/alyzer/IAlleleDisplayHelper.java        -> apiculture.genetics.IGeneticTooltipProvider
genetics/capability/IIndividualHandlerItem.java  -> core.genetics.ItemGE
mail/ITradeStation.java                          -> core.inventory.IInventoryAdapter,
                                                    mail.IWatchable
multiblock/IMultiblockComponent.java             -> core.inventory.IInventoryAdapter
```

## File Structure

| Action | File | Note |
| --- | --- | --- |
| Move | `forestry/mail/IWatchable.java` -> `forestry/api/mail/IWatchable.java` | no dependencies at all |
| Move | `forestry/core/tiles/IFilterSlotDelegate.java` -> `forestry/api/core/IFilterSlotDelegate.java` | Minecraft types only |
| Move | `forestry/core/inventory/IInventoryAdapter.java` -> `forestry/api/core/IInventoryAdapter.java` | api-only once the above moves |
| Move | `forestry/apiculture/VillageHive.java` -> `forestry/api/apiculture/hives/VillageHive.java` | record, api types only |
| Move | `forestry/apiculture/genetics/IGeneticTooltipProvider.java` -> `forestry/api/genetics/alyzer/IGeneticTooltipProvider.java` | api types only |
| Move | `forestry/core/genetics/alleles/Chromosome.java` -> `forestry/api/genetics/alleles/Chromosome.java` | 74 lines, imports api only |
| Move | `forestry/core/genetics/alleles/ChromosomeFactory.java` -> `forestry/api/genetics/alleles/ChromosomeFactory.java` | one core dependency, handled in the same task |
| Create | `forestry/api/genetics/IIndividualItem.java` | api-side accessor `ItemGE` implements |
| Create | `forestry/api/genetics/GeneticTranslationKeys.java` | home for `createTranslationKey` |
| Modify | `forestry/api/apiculture/genetics/IBeeEffect.java` | invert the particle call |
| Modify | the three `*Chromosomes` holders | api lookups instead of `SpeciesUtil`, api logger instead of `Forestry.LOGGER` |

---

### Task 1: Promote IWatchable

`IWatchable` has no imports whatsoever - a two-method interface plus a nested `Watcher`. It is
referenced by 5 files.

**Files:**
- Move: `src/main/java/forestry/mail/IWatchable.java` -> `src/main/java/forestry/api/mail/IWatchable.java`
- Modify: the 4 other referencing files

**Interfaces:**
- Consumes: nothing.
- Produces: `forestry.api.mail.IWatchable`, unchanged in shape.

- [ ] **Step 1: Find every reference**

```bash
grep -rlw IWatchable src/main/java src/test/java
```

Expected: 5 files, one of which is `src/main/java/forestry/api/mail/ITradeStation.java`.

- [ ] **Step 2: Move the file and change its package**

```bash
git mv src/main/java/forestry/mail/IWatchable.java src/main/java/forestry/api/mail/IWatchable.java
```

Change line 1 from `package forestry.mail;` to `package forestry.api.mail;`.

- [ ] **Step 3: Fix imports at every call site**

In each file from Step 1 other than the moved file itself, replace
`import forestry.mail.IWatchable;` with `import forestry.api.mail.IWatchable;`. In
`ITradeStation.java` the import is deleted outright rather than rewritten, because it is now in
the same package.

- [ ] **Step 4: Compile**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Run the gate**

```bash
./gradlew checkApiBoundary 2>&1 | grep -E "file\(s\), .* import\(s\)"
```

Expected: `9 file(s), 15 import(s):`. The file count does not drop yet - `ITradeStation` still
imports `IInventoryAdapter`, which Task 2 handles.

- [ ] **Step 6: Commit**

```bash
git add -A src/main/java/forestry
git commit -m "api: promote IWatchable

A two-method interface with no dependencies, referenced by ITradeStation in
the api. Moves to forestry.api.mail unchanged.

checkApiBoundary: 16 -> 15 imports."
```

---

### Task 2: Promote IFilterSlotDelegate and IInventoryAdapter

These move together: `IInventoryAdapter extends WorldlyContainer, IFilterSlotDelegate,
INbtWritable, INbtReadable`. Two of those are already api; `IFilterSlotDelegate` is core and
must lead. `IFilterSlotDelegate` itself imports only `ItemStack`.

`IInventoryAdapter` is referenced by 26 files and `IFilterSlotDelegate` by 11 - the largest
move in this plan. It clears both `IMultiblockComponent` and `ITradeStation`.

**Files:**
- Move: `src/main/java/forestry/core/tiles/IFilterSlotDelegate.java` -> `src/main/java/forestry/api/core/IFilterSlotDelegate.java`
- Move: `src/main/java/forestry/core/inventory/IInventoryAdapter.java` -> `src/main/java/forestry/api/core/IInventoryAdapter.java`
- Modify: the referencing files found in Step 1

**Interfaces:**
- Consumes: nothing.
- Produces: `forestry.api.core.IFilterSlotDelegate` and `forestry.api.core.IInventoryAdapter`,
  unchanged in shape.

- [ ] **Step 1: Find every reference**

```bash
grep -rlw IFilterSlotDelegate src/main/java src/test/java
grep -rlw IInventoryAdapter src/main/java src/test/java
```

Expected: 11 and 26 files respectively.

- [ ] **Step 2: Move IFilterSlotDelegate**

```bash
git mv src/main/java/forestry/core/tiles/IFilterSlotDelegate.java src/main/java/forestry/api/core/IFilterSlotDelegate.java
```

Change line 1 to `package forestry.api.core;`.

- [ ] **Step 3: Move IInventoryAdapter**

```bash
git mv src/main/java/forestry/core/inventory/IInventoryAdapter.java src/main/java/forestry/api/core/IInventoryAdapter.java
```

Change line 1 to `package forestry.api.core;` and delete `import forestry.core.tiles.IFilterSlotDelegate;`
(same package now). The remaining `forestry.api.core.INbtReadable` and `INbtWritable` imports
also become same-package and should be deleted.

The result:

```java
package forestry.api.core;

import net.minecraft.world.WorldlyContainer;

public interface IInventoryAdapter extends WorldlyContainer, IFilterSlotDelegate, INbtWritable, INbtReadable {

}
```

- [ ] **Step 4: Fix imports across all referencing files**

Replace `import forestry.core.tiles.IFilterSlotDelegate;` with
`import forestry.api.core.IFilterSlotDelegate;` and `import forestry.core.inventory.IInventoryAdapter;`
with `import forestry.api.core.IInventoryAdapter;` throughout. In `ITradeStation.java` and
`IMultiblockComponent.java` delete the `IInventoryAdapter` import outright if those files are
in `forestry.api.core`; they are not, so rewrite rather than delete.

- [ ] **Step 5: Compile**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`. A `cannot find symbol` here means a call site was missed; the
compiler names it.

- [ ] **Step 6: Run the gate**

```bash
./gradlew checkApiBoundary 2>&1 | grep -E "file\(s\), .* import\(s\)"
```

Expected: `7 file(s), 13 import(s):`. Both `ITradeStation.java` and
`IMultiblockComponent.java` are gone from the list.

- [ ] **Step 7: Verify datagen and tests**

```bash
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: no output from the diff, and `All 98 required tests passed`.

- [ ] **Step 8: Commit**

```bash
git add -A src/main/java/forestry
git commit -m "api: promote IInventoryAdapter and IFilterSlotDelegate

IMultiblockComponent.getInternalInventory() and ITradeStation's extends
clause both named IInventoryAdapter from core. It extends IFilterSlotDelegate,
so that leads; both reference only Minecraft and api types.

checkApiBoundary: 9 -> 7 files, 15 -> 13 imports."
```

---

### Task 3: Promote VillageHive

`IHiveManager` returns `ImmutableList<VillageHive>`. `VillageHive` is a record over
`ResourceLocation`, `IChromosome` and `Allele` - all api. Referenced by 5 files.

This is the leak a constant-pool scan cannot see, because the generic erases. It is only
visible to the import-based gate.

**Files:**
- Move: `src/main/java/forestry/apiculture/VillageHive.java` -> `src/main/java/forestry/api/apiculture/hives/VillageHive.java`
- Modify: the 4 other referencing files

**Interfaces:**
- Consumes: nothing.
- Produces: `forestry.api.apiculture.hives.VillageHive`, unchanged in shape.

- [ ] **Step 1: Find references**

```bash
grep -rlw VillageHive src/main/java src/test/java
```

Expected: 5 files.

- [ ] **Step 2: Move it**

```bash
git mv src/main/java/forestry/apiculture/VillageHive.java src/main/java/forestry/api/apiculture/hives/VillageHive.java
```

Change line 1 to `package forestry.api.apiculture.hives;`.

- [ ] **Step 3: Fix imports**

Replace `import forestry.apiculture.VillageHive;` with
`import forestry.api.apiculture.hives.VillageHive;` at each call site. Delete it outright in
`IHiveManager.java`, which is now in the same package.

- [ ] **Step 4: Compile and gate**

```bash
./gradlew compileJava
./gradlew checkApiBoundary 2>&1 | grep -E "file\(s\), .* import\(s\)"
```

Expected: `BUILD SUCCESSFUL`, then `6 file(s), 12 import(s):`.

- [ ] **Step 5: Commit**

```bash
git add -A src/main/java/forestry
git commit -m "api: promote VillageHive

IHiveManager returns ImmutableList<VillageHive>, and the record holds only
api types. Note this leak is invisible to a constant-pool scan because the
generic erases - the import gate is what caught it.

checkApiBoundary: 7 -> 6 files, 13 -> 12 imports."
```

---

### Task 4: Promote IGeneticTooltipProvider

`IAlleleDisplayHelper` takes `IGeneticTooltipProvider` as a parameter in three methods. The
interface has one method over `ToolTip`, `IGenome` and `IIndividual` - all api. Referenced by
only 2 files.

Like `VillageHive`, this appears only in method descriptors and is invisible to a
constant-pool scan.

**Files:**
- Move: `src/main/java/forestry/apiculture/genetics/IGeneticTooltipProvider.java` -> `src/main/java/forestry/api/genetics/alyzer/IGeneticTooltipProvider.java`
- Modify: `src/main/java/forestry/api/genetics/alyzer/IAlleleDisplayHelper.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `forestry.api.genetics.alyzer.IGeneticTooltipProvider`, unchanged in shape.

- [ ] **Step 1: Move it**

```bash
git mv src/main/java/forestry/apiculture/genetics/IGeneticTooltipProvider.java src/main/java/forestry/api/genetics/alyzer/IGeneticTooltipProvider.java
```

Change line 1 to `package forestry.api.genetics.alyzer;`.

- [ ] **Step 2: Fix the one call site**

In `IAlleleDisplayHelper.java`, delete
`import forestry.apiculture.genetics.IGeneticTooltipProvider;` - same package now.

- [ ] **Step 3: Find any other reference**

```bash
grep -rlw IGeneticTooltipProvider src/main/java src/test/java
```

Expected: 2 files, both already handled. If more appear, fix their imports to
`forestry.api.genetics.alyzer.IGeneticTooltipProvider`.

- [ ] **Step 4: Compile and gate**

```bash
./gradlew compileJava
./gradlew checkApiBoundary 2>&1 | grep -E "file\(s\), .* import\(s\)"
```

Expected: `BUILD SUCCESSFUL`, then `5 file(s), 11 import(s):`.

- [ ] **Step 5: Commit**

```bash
git add -A src/main/java/forestry
git commit -m "api: promote IGeneticTooltipProvider

IAlleleDisplayHelper takes it in three method signatures and it holds only
api types. Parameter-only, so a constant-pool scan misses it too.

checkApiBoundary: 6 -> 5 files, 12 -> 11 imports."
```

---

### Task 5: Invert IIndividualHandlerItem's calls into an api interface

`IIndividualHandlerItem` is a pure facade - every static forwards to `ItemGE`. It cannot simply
absorb the bodies, because they test `stack.getItem() instanceof ItemGE` and read
`CoreDataComponents.GENOME`, both impl.

The inversion is an api interface the item class implements, so api asks the item rather than
naming its class.

**Files:**
- Create: `src/main/java/forestry/api/genetics/IIndividualItem.java`
- Modify: `src/main/java/forestry/api/genetics/capability/IIndividualHandlerItem.java`
- Modify: `src/main/java/forestry/core/genetics/ItemGE.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `forestry.api.genetics.IIndividualItem` with
  `IIndividual getIndividual(ItemStack)`, `IGenome getGenome(ItemStack)`,
  `ILifeStage getLifeStage()`, `ISpeciesType<?, ?> getSpeciesType()`. `ItemGE` implements it.

- [ ] **Step 1: Write the api interface**

Create `src/main/java/forestry/api/genetics/IIndividualItem.java`:

```java
package forestry.api.genetics;

import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Implemented by items that carry a genetic individual. Lets the api read an individual out of a
 * stack without naming the item implementation.
 *
 * Ex. the bee, tree, and butterfly items
 */
public interface IIndividualItem {
	/**
	 * @return The individual stored in the stack, or null if it has none
	 */
	@Nullable
	IIndividual getIndividual(ItemStack stack);

	/**
	 * @return The genome stored in the stack, or null if it has none
	 */
	@Nullable
	IGenome getGenome(ItemStack stack);

	/**
	 * @return The life stage this item represents
	 */
	ILifeStage getLifeStage();

	/**
	 * @return The species type this item belongs to
	 */
	ISpeciesType<?, ?> getSpeciesType();
}
```

- [ ] **Step 2: Make ItemGE implement it**

In `src/main/java/forestry/core/genetics/ItemGE.java`, add `forestry.api.genetics.IIndividualItem`
to the class's `implements` clause and add the four methods, delegating to what is already
there:

```java
	@Override
	@Nullable
	public IIndividual getIndividual(ItemStack stack) {
		return getIndividualFromComponent(stack);
	}

	@Override
	@Nullable
	public IGenome getGenome(ItemStack stack) {
		return stack.get(CoreDataComponents.GENOME);
	}

	@Override
	public ILifeStage getLifeStage() {
		return this.stage;
	}
```

`getSpeciesType()` is a rename of the existing `getType()`; keep `getType()` and add
`getSpeciesType()` returning `getType()` so no existing call site changes.

- [ ] **Step 3: Rewrite the api facade**

Replace the body of `IIndividualHandlerItem.java`'s statics so none of them name `ItemGE`.
Delete `import forestry.core.genetics.ItemGE;` and add
`import forestry.api.genetics.IIndividualItem;`. Each static becomes:

```java
	@Nullable
	static IIndividual getIndividual(ItemStack stack) {
		return stack.getItem() instanceof IIndividualItem item ? item.getIndividual(stack) : null;
	}

	@Nullable
	static ILifeStage getLifeStage(ItemStack stack) {
		return stack.getItem() instanceof IIndividualItem item ? item.getLifeStage() : null;
	}

	static boolean hasIndividual(ItemStack stack) {
		return getIndividual(stack) != null;
	}

	static boolean isIndividual(ItemStack stack) {
		return getIndividual(stack) != null;
	}

	static void ifPresent(ItemStack stack, Consumer<IIndividual> action) {
		IIndividual individual = getIndividual(stack);
		if (individual != null) {
			action.accept(individual);
		}
	}

	static void ifPresent(ItemStack stack, BiConsumer<IIndividual, ILifeStage> action) {
		IIndividual individual = getIndividual(stack);
		ILifeStage lifeStage = getLifeStage(stack);
		if (individual != null && lifeStage != null) {
			action.accept(individual, lifeStage);
		}
	}

	static boolean filter(ItemStack stack, Predicate<IIndividual> predicate) {
		IIndividual individual = getIndividual(stack);
		return individual != null && predicate.test(individual);
	}

	static boolean filter(ItemStack stack, BiPredicate<IIndividual, ILifeStage> predicate) {
		IIndividual individual = getIndividual(stack);
		ILifeStage lifeStage = getLifeStage(stack);
		return individual != null && lifeStage != null && predicate.test(individual, lifeStage);
	}
```

Note the behavior change in `hasIndividual`: `ItemGE.hasIndividual` tested
`stack.has(CoreDataComponents.GENOME)` while `isIndividual` tested `getIndividual(stack) != null`.
Both now test the latter. Verify that is acceptable in Step 5 - if the distinction matters, add
`boolean hasGenome(ItemStack)` to `IIndividualItem` and keep them separate.

- [ ] **Step 4: Compile**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Check the hasIndividual/isIndividual distinction**

```bash
grep -rn "hasIndividual\|isIndividual" src/main/java src/test/java | grep -v "IIndividualHandlerItem.java\|ItemGE.java"
```

Read each call site. If any relies on `hasIndividual` being true for a stack that has a GENOME
component but is not an `IIndividualItem`, restore the distinction as described in Step 3.

- [ ] **Step 6: Gate, tests, datagen**

```bash
./gradlew checkApiBoundary 2>&1 | grep -E "file\(s\), .* import\(s\)"
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
./gradlew runData && git status --porcelain src/generated/resources
```

Expected: `4 file(s), 10 import(s):`, `All 98 required tests passed`, no datagen diff.

- [ ] **Step 7: Commit**

```bash
git add -A src/main/java/forestry
git commit -m "api: read individuals through IIndividualItem instead of ItemGE

IIndividualHandlerItem was a pure facade over ItemGE statics, which test
'instanceof ItemGE' and read CoreDataComponents.GENOME. The api now asks the
item through a new IIndividualItem interface that ItemGE implements.

checkApiBoundary: 5 -> 4 files, 11 -> 10 imports."
```

---

### Task 6: Invert IBeeEffect's particle call

`IBeeEffect.doFX` has a default body calling `ParticleRender.addBeeHiveFX(housing, genome,
flowerPositions)`. That is client-only rendering reached from an api default method.

`IForestryClientApi` already exists as the client-side entry point and already exposes
`getBeeManager()` returning `IBeeClientManager`.

**This home is the least-bad option, not an obviously right one.** `IBeeClientManager` is
documented as "Tracks client-only data for bee species" and its three existing methods all
return model locations. A particle-spawning method is behavior, not data. The alternatives are
worse: a new single-method service adds a `ServiceLoader` entry for one call, and making
`doFX`'s default body empty would silently drop particles for every addon effect relying on the
default. Take `IBeeClientManager` and widen its class javadoc from "data" to "client-side
concerns", or push back with a better home before starting.

**Files:**
- Modify: `src/main/java/forestry/api/client/apiculture/IBeeClientManager.java`
- Modify: `src/main/java/forestry/api/apiculture/genetics/IBeeEffect.java`
- Modify: the `IBeeClientManager` implementation under `src/main/java/forestry/apiimpl/client/`

**Interfaces:**
- Consumes: nothing.
- Produces: `IBeeClientManager.addBeeHiveParticles(IBeeHousing, IGenome, List<BlockPos>)`.

- [ ] **Step 1: Find the implementation**

```bash
grep -rl "implements IBeeClientManager" src/main/java
```

Note the file; Step 3 edits it.

- [ ] **Step 2: Add the hook to the client api**

In `src/main/java/forestry/api/client/apiculture/IBeeClientManager.java`, add:

```java
	/**
	 * Spawns the ambient particles for a bee working in a hive or apiary.
	 *
	 * @param housing         The hive or apiary the bee resides in
	 * @param genome          The genome of the working bee
	 * @param flowerPositions The flower positions the bee is servicing
	 */
	void addBeeHiveParticles(IBeeHousing housing, IGenome genome, List<BlockPos> flowerPositions);
```

Add imports for `forestry.api.apiculture.IBeeHousing`, `forestry.api.genetics.IGenome`,
`net.minecraft.core.BlockPos` and `java.util.List` as needed.

- [ ] **Step 3: Implement it**

In the class found in Step 1, add:

```java
	@Override
	public void addBeeHiveParticles(IBeeHousing housing, IGenome genome, List<BlockPos> flowerPositions) {
		ParticleRender.addBeeHiveFX(housing, genome, flowerPositions);
	}
```

with `import forestry.core.render.ParticleRender;`. The impl may reference core freely.

- [ ] **Step 4: Rewrite IBeeEffect.doFX**

Delete `import forestry.core.render.ParticleRender;`, add
`import forestry.api.client.IForestryClientApi;`, and change the body:

```java
	default IEffectData doFX(IGenome genome, IEffectData storedData, IBeeHousing housing) {
		IBeekeepingLogic beekeepingLogic = housing.getBeekeepingLogic();
		List<BlockPos> flowerPositions = beekeepingLogic.getFlowerPositions();

		IForestryClientApi.INSTANCE.getBeeManager().addBeeHiveParticles(housing, genome, flowerPositions);
		return storedData;
	}
```

- [ ] **Step 5: Confirm doFX is only reached client-side**

```bash
grep -rn "doFX" src/main/java | grep -v "IBeeEffect.java"
```

`IForestryClientApi.INSTANCE` is loaded via `ServiceLoader` and its implementation is
client-only, so a server-side call would throw. Read each caller and confirm every one is
guarded by a client-side check. If any is not, report it rather than proceeding - the previous
code had the same exposure through `ParticleRender`, but this task must not widen it.

- [ ] **Step 6: Compile, gate, tests**

```bash
./gradlew compileJava
./gradlew checkApiBoundary 2>&1 | grep -E "file\(s\), .* import\(s\)"
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `BUILD SUCCESSFUL`, `3 file(s), 9 import(s):`, `All 98 required tests passed`.

- [ ] **Step 7: Commit**

```bash
git add -A src/main/java/forestry
git commit -m "api: route bee hive particles through IBeeClientManager

IBeeEffect.doFX called ParticleRender directly from an api default method.
The call now goes through IForestryClientApi, which already exists as the
client-side entry point.

checkApiBoundary: 4 -> 3 files, 10 -> 9 imports."
```

---

### Task 7: Move the chromosome construction machinery into api

The three `*Chromosomes` holders are the ONLY users of `ChromosomeFactory`, and
`ChromosomeFactory` is the only user of `Chromosome`. Since the holders are api, their factory
belongs there too.

`Chromosome.java` is 74 lines and imports only api. `ChromosomeFactory` has exactly one core
dependency: `GeneticsUtil.createTranslationKey`, a pure string builder with one other caller
(`core/genetics/Species.java:52`).

**Files:**
- Create: `src/main/java/forestry/api/genetics/GeneticTranslationKeys.java`
- Move: `src/main/java/forestry/core/genetics/alleles/Chromosome.java` -> `src/main/java/forestry/api/genetics/alleles/Chromosome.java`
- Move: `src/main/java/forestry/core/genetics/alleles/ChromosomeFactory.java` -> `src/main/java/forestry/api/genetics/alleles/ChromosomeFactory.java`
- Modify: `src/main/java/forestry/core/utils/GeneticsUtil.java`
- Modify: `src/main/java/forestry/core/genetics/Species.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `forestry.api.genetics.alleles.ChromosomeFactory` with its five existing static
  methods unchanged, and `forestry.api.genetics.GeneticTranslationKeys.createTranslationKey(String, ResourceLocation, ResourceLocation)`.

- [ ] **Step 1: Extract createTranslationKey into api**

Create `src/main/java/forestry/api/genetics/GeneticTranslationKeys.java` holding the body
currently at `src/main/java/forestry/core/utils/GeneticsUtil.java:155`, verbatim, as
`public static String createTranslationKey(String type, ResourceLocation typeId, ResourceLocation objectId)`
in a final class with a private constructor. Copy the existing javadoc with it.

- [ ] **Step 2: Point the old callers at it**

Delete the method from `GeneticsUtil` and update its two callers,
`core/genetics/alleles/ChromosomeFactory.java:65` and `core/genetics/Species.java:52`, to call
`GeneticTranslationKeys.createTranslationKey(...)`.

- [ ] **Step 3: Compile**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`. This step alone changes no behavior.

- [ ] **Step 4: Move Chromosome**

```bash
git mv src/main/java/forestry/core/genetics/alleles/Chromosome.java src/main/java/forestry/api/genetics/alleles/Chromosome.java
```

Change line 1 to `package forestry.api.genetics.alleles;` and delete any now-same-package
imports of `forestry.api.genetics.alleles.*`.

- [ ] **Step 5: Move ChromosomeFactory**

```bash
git mv src/main/java/forestry/core/genetics/alleles/ChromosomeFactory.java src/main/java/forestry/api/genetics/alleles/ChromosomeFactory.java
```

Change line 1 to `package forestry.api.genetics.alleles;`, delete
`import forestry.core.utils.GeneticsUtil;` and the now-same-package
`import forestry.api.genetics.alleles.IChromosome;`, and add
`import forestry.api.genetics.GeneticTranslationKeys;`.

- [ ] **Step 6: Fix references**

```bash
grep -rlw ChromosomeFactory src/main/java src/test/java
grep -rlw Chromosome src/main/java src/test/java
```

Expected: 5 and 4 files. Replace `import forestry.core.genetics.alleles.ChromosomeFactory;`
with `import forestry.api.genetics.alleles.ChromosomeFactory;` at each; in the three
`*Chromosomes` holders the import is deleted outright, being same-package now.

- [ ] **Step 7: Compile and gate**

```bash
./gradlew compileJava
./gradlew checkApiBoundary 2>&1 | grep -E "file\(s\), .* import\(s\)"
```

Expected: `BUILD SUCCESSFUL`, then `3 file(s), 6 import(s):` - the three holders remain, each
still importing `Forestry` and `SpeciesUtil`.

- [ ] **Step 8: Verify datagen and tests**

```bash
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: no diff, all tests pass. `AlleleTranslationKeyTest` is the specific guard here - it
walks every chromosome's `translationKey`, so a mistake in the `createTranslationKey` move
fails it.

- [ ] **Step 9: Commit**

```bash
git add -A src/main/java/forestry
git commit -m "api: move chromosome construction into api

The three *Chromosomes holders are api and are the only users of
ChromosomeFactory, which is the only user of Chromosome. Both reference api
types exclusively once GeneticsUtil.createTranslationKey - a pure string
builder - moves alongside as GeneticTranslationKeys.

checkApiBoundary: 3 files, 9 -> 6 imports."
```

---

### Task 8: Clear the last three holders and turn the gate green

Each of `BeeChromosomes`, `TreeChromosomes` and `ButterflyChromosomes` retains two imports:
`forestry.Forestry` (for `Forestry.LOGGER`) and `forestry.core.utils.SpeciesUtil` (for
`SpeciesUtil.BEE_TYPE.get()` and siblings).

`SpeciesUtil` uses appear only inside lambdas, evaluated after registries populate, so routing
them through `IForestryApi` introduces no class-init ordering risk. The logger has no such
constraint either.

**Files:**
- Modify: `src/main/java/forestry/api/genetics/alleles/BeeChromosomes.java`
- Modify: `src/main/java/forestry/api/genetics/alleles/TreeChromosomes.java`
- Modify: `src/main/java/forestry/api/genetics/alleles/ButterflyChromosomes.java`

**Interfaces:**
- Consumes: `ChromosomeFactory` in api, from Task 7.
- Produces: no public signature changes. The chromosome constants keep their names, types and
  ids.

- [ ] **Step 1: Inventory the exact uses**

```bash
grep -n "Forestry\.\|SpeciesUtil\." src/main/java/forestry/api/genetics/alleles/BeeChromosomes.java \
                                     src/main/java/forestry/api/genetics/alleles/TreeChromosomes.java \
                                     src/main/java/forestry/api/genetics/alleles/ButterflyChromosomes.java
```

In `BeeChromosomes` this is `SpeciesUtil.BEE_TYPE.get()` at lines 39, 71, 83 and 87, and
`Forestry.LOGGER.warn` at line 44. The other two files follow the same shape with their own
species type.

- [ ] **Step 2: Replace the species type lookups, keeping the memoization**

`SpeciesUtil.BEE_TYPE` is already nothing but a memoized wrapper around the api call
(`core/utils/SpeciesUtil.java:34`):

```java
public static final Lazy<IBeeSpeciesType> BEE_TYPE = Lazy.of(() -> IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(ForestrySpeciesTypes.BEE, IBeeSpeciesType.class));
```

`getSpeciesType(ResourceLocation, Class<T>)` is a default method on `IGeneticManager` and is
already api, so no cast, no widening and no new accessor is needed.

**Move the `Lazy`, not just the call.** `BeeChromosomes.resolveSpeciesOrDefault` carries a
comment stating it "backs every SPECIES chromosome read, so it is exercised whenever a bee
item/individual's genome is decoded (tooltips, the analyzer, breeding, etc.)". Inlining the
manager lookup at each of the four call sites would turn a cached reference into a registry
lookup plus a cast on a hot path. Give each holder its own private field instead:

```java
	private static final Lazy<IBeeSpeciesType> BEE_TYPE = Lazy.of(() -> IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(ForestrySpeciesTypes.BEE, IBeeSpeciesType.class));
```

with `import forestry.api.IForestryApi;` and `import net.neoforged.neoforge.common.util.Lazy;`.
Then `SpeciesUtil.BEE_TYPE.get()` becomes `BEE_TYPE.get()` at all four sites, an identical
expression with identical caching.

`TreeChromosomes` and `ButterflyChromosomes` take the same treatment with
`SpeciesUtil.java:35` and `:36` as their models - `ForestrySpeciesTypes.TREE` with
`ITreeSpeciesType`, and `ForestrySpeciesTypes.BUTTERFLY` with `IButterflySpeciesType`.

Leave `SpeciesUtil` itself in place; it has many other callers in core and is bucket C's
problem, not this task's.

- [ ] **Step 3: Replace the logger**

Delete `import forestry.Forestry;`. Add a package-private logger to each file:

```java
	private static final Logger LOGGER = LogUtils.getLogger();
```

with `import com.mojang.logging.LogUtils;` and `import org.slf4j.Logger;`, then change
`Forestry.LOGGER.warn(...)` to `LOGGER.warn(...)`. Keep the message text byte-identical - the
fallback path it reports is exercised by the species-fallback GameTests.

- [ ] **Step 4: Compile**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Run the gate - this is the one that turns green**

```bash
./gradlew checkApiBoundary
```

Expected: `BUILD SUCCESSFUL` and the line `checkApiBoundary: forestry.api is clean`.

- [ ] **Step 6: Full verification**

```bash
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
./gradlew build
```

Expected: no datagen diff, `All 98 required tests passed`, and `BUILD SUCCESSFUL` for the full
build - note `build` now includes `checkApiBoundary` via `check`, so no `-x` is needed any
more.

`ButterflySpeciesFallbackTest` and `BeeSpeciesDefinitionTest` specifically exercise the
resolver lambdas this task rewrote.

- [ ] **Step 7: Commit**

```bash
git add -A src/main/java/forestry
git commit -m "api: clear the last impl references from the chromosome holders

Species type lookups go through IForestryApi's genetic manager rather than
core's SpeciesUtil, and each holder carries its own slf4j logger rather than
Forestry.LOGGER. Both uses sit inside lambdas evaluated after registries
populate, so there is no class-init ordering change.

checkApiBoundary: green. forestry.api imports nothing outside forestry.api."
```

---

### Task 9: Record phase 1 completion

**Files:**
- Modify: `docs/superpowers/specs/2026-07-30-feature-package-reorg-design.md`

- [ ] **Step 1: Confirm the gate is green from a clean build**

```bash
./gradlew clean build
```

Expected: `BUILD SUCCESSFUL`, including `checkApiBoundary`.

- [ ] **Step 2: Update the spec**

Replace the phase 1a progress paragraph with:

```markdown
Phase 1 landed 2026-07-31. `checkApiBoundary` is green: `forestry.api` imports nothing outside
`forestry.api`. Bucket F is closed. Nine types moved into api (`IWatchable`,
`IFilterSlotDelegate`, `IInventoryAdapter`, `VillageHive`, `IGeneticTooltipProvider`,
`Chromosome`, `ChromosomeFactory`, plus the new `IIndividualItem` and `GeneticTranslationKeys`),
and two api default methods were inverted behind api interfaces.
```

Also update the sequencing block's `1b` line to `DONE 2026-07-31`.

- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/specs/2026-07-30-feature-package-reorg-design.md
git commit -m "docs: record phase 1 completion, checkApiBoundary green"
```

---

## Notes for phase 2

- The gate now proves only that `api` is clean. Phases 2 through 6 need the same treatment for
  the rest of the base artifact - `core` (49 files), `apiimpl` (12), `plugin` (7). Extending
  `checkApiBoundary` to cover those is the natural first step of phase 2, scoped so it fails on
  base-artifact packages importing the five split modules rather than on api alone.
- `IInventoryAdapter` moving to api touches 26 files, the widest blast radius in phase 1. If
  phase 2 needs a similar promotion, expect the same shape: find the leaf dependency first
  (`IFilterSlotDelegate` here), move it, then the type that needs it.
- Task 5's `IIndividualItem` is the pattern for bucket B's extension points. `ForestryCreativeTabs`
  and the `PacketId*` enums want the same inversion: an api-side interface that content jars
  implement, rather than base naming them.
