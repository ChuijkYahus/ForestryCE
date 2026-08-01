# Phase 1a: sever api to impl, mechanical half

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up the api boundary gate, then clear the 7 of 16 api-to-impl leaks that can be
fixed without adding any public API type.

**Architecture:** A Gradle `checkApiBoundary` task fails the build while any file under
`src/main/java/forestry/api` imports a `forestry` package outside `forestry.api`. It starts
red at 16 files and each task drives the number down. The three fix shapes here are: delete a
javadoc-only import, delete alias constants and redirect their one consumer, and retype three
api enums so they resolve their items from the registry instead of holding impl references.

**Tech Stack:** Java 21, NeoForge 21.1.230, Minecraft 1.21.1, ModDevGradle 2.0.x, Gradle
Groovy DSL. No JUnit and no ArchUnit in this project - `src/test` holds GameTests only and the
`test` task is configured with `failOnNoDiscoveredTests = false`.

## Global Constraints

- Comment and Javadoc style is binding; see `CLAUDE.md`. ASCII only - the comment corpus
  contains zero non-ASCII characters. No em-dash, no curly quotes. Lowercase `todo`.
- Inline comments are terse fragments with no terminal period unless they reach two full
  sentences.
- `forestry.api` is published as `apiJar` and javadoc is filtered to `forestry/api/**`. Every
  change to a public signature in this plan is a breaking change for addons and belongs to the
  single major-version wave described in the spec.
- Do not reformat, reorder imports, or touch anything outside the files each task names. A
  clean `runData` diff is this phase's primary oracle and unrelated edits pollute it.
- Registry names in this codebase do not match their constant names. `BEE_DRONE` is
  `drone_bee`. Never infer a registry name; copy it from the `REGISTRY.item(...)` call.

## Scope

This plan is phase 1a. It covers 7 of the 16 files:

| File | Fix shape |
| --- | --- |
| `api/apiculture/FlowerTypeType.java` | javadoc-only import |
| `api/mail/IPostalCarrier.java` | javadoc-only import |
| `api/arboriculture/ITreeManager.java` | javadoc-only imports (2) |
| `api/ForestryTags.java` | delete 84 alias constants |
| `api/apiculture/genetics/BeeLifeStage.java` | retype enum |
| `api/arboriculture/genetics/TreeLifeStage.java` | retype enum |
| `api/lepidopterology/genetics/ButterflyLifeStage.java` | retype enum |

The remaining 9 files all require adding a new public type to `api` or inverting a call into
an SPI, and are phase 1b:

`IHiveManager` (needs `IVillageHive`), `IAlleleDisplayHelper` (needs `IGeneticTooltipProvider`
promoted), `IMultiblockComponent` and `ITradeStation` (need an inventory interface, plus
`IWatchable`, promoted), `IIndividualHandlerItem` (`ItemGE` statics), `IBeeEffect`
(`ParticleRender` hook), and `BeeChromosomes` / `TreeChromosomes` / `ButterflyChromosomes`
(`ChromosomeFactory`, `SpeciesUtil`, `Forestry`).

Phase 1a is shippable alone: it removes public constants but adds and changes no public
signature.

## File Structure

| File | Responsibility | Change |
| --- | --- | --- |
| `build.gradle` | Build config | Add `checkApiBoundary` task, wire into `check` |
| `src/main/java/forestry/api/apiculture/FlowerTypeType.java` | Flower type registry key holder | Drop import, delink javadoc |
| `src/main/java/forestry/api/mail/IPostalCarrier.java` | Postal carrier contract | Drop import, delink javadoc |
| `src/main/java/forestry/api/arboriculture/ITreeManager.java` | Tree and wood registry access | Drop 2 imports, delink javadoc |
| `src/main/java/forestry/api/ForestryTags.java` | Tag key constants | Delete 84 wood aliases, drop import |
| `src/main/java/forestry/arboriculture/PodFruit.java` | Pod fruit definition | Redirect 3 tag references |
| `src/main/java/forestry/api/apiculture/genetics/BeeLifeStage.java` | Bee life stage enum | Hold `ResourceLocation`, resolve lazily |
| `src/main/java/forestry/api/arboriculture/genetics/TreeLifeStage.java` | Tree life stage enum | Same |
| `src/main/java/forestry/api/lepidopterology/genetics/ButterflyLifeStage.java` | Butterfly life stage enum | Same |

---

### Task 0: Establish the datagen oracle

The spec's phase 0a. `runData` output is this phase's primary correctness check, so prove it
is deterministic before trusting it. Determinism has been broken and fixed twice in this repo
already (commit `b8b4f9abc`, and the explicit sort at `TaxonProvider.java:71` working around
an `IdentityHashMap`).

**Files:**
- Modify: none. This task only establishes a baseline.

**Interfaces:**
- Consumes: nothing.
- Produces: a verified-clean `src/generated/resources` working tree, used as the comparison
  baseline by every later task.

- [ ] **Step 1: Confirm the working tree is clean**

```bash
git status --porcelain src/generated/resources
```

Expected: no output. If there is output, commit or discard it before continuing - this task
cannot establish a baseline over uncommitted generated files.

- [ ] **Step 2: Regenerate once**

```bash
./gradlew runData
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Confirm regeneration is a no-op**

```bash
git status --porcelain src/generated/resources
```

Expected: no output.

If there IS output, `runData` is not deterministic and the whole phase's oracle is invalid.
Stop and investigate before proceeding - look for `IdentityHashMap`, `HashSet` or `HashMap`
iteration feeding provider output, following the pattern already fixed at
`TaxonProvider.java:71`. Do not continue the plan until this step produces no output.

- [ ] **Step 4: Regenerate a second time and confirm again**

```bash
./gradlew runData && git status --porcelain src/generated/resources
```

Expected: `BUILD SUCCESSFUL` and no output. Two consecutive clean runs, not one, because a
single run only proves the committed output matches the current code.

- [ ] **Step 5: Record the result**

No commit. Note in the task tracker that the oracle is verified, with the date.

---

### Task 1: Add the api boundary gate

The gate is import-based, not bytecode-based, and that is a deliberate decision.

A constant-pool scan of the compiled api classes finds only 11 of the out-of-api types,
because `CONSTANT_Class` entries are emitted for `new`, `checkcast`, field and method owners,
and supertypes - but not for a type that appears only in a method descriptor or an erased
generic. It silently misses `IHiveManager`'s `ImmutableList<VillageHive>` return and
`IAlleleDisplayHelper`'s `IGeneticTooltipProvider` parameters. It would report clean while
those files still leak.

Imports are exact here: the entire repository contains exactly two inline fully-qualified
references (`apiimpl/plugin/PluginManager.java:151` and `core/ModuleCore.java:188`), both
outside `api`. The proper descriptor-aware and signature-aware gate arrives with ArchUnit in
phase 9.

A text-level scan would also be wrong: `api/core/Product.java:44`,
`api/core/IProduct.java:64`, `api/core/FluidProduct.java:26` and
`api/core/FluidProductType.java` deliberately write `{@code forestry.core.genetics.ProductTypes}`
as prose precisely to avoid an import, and `api/genetics/ISpeciesType.java:96` and
`api/genetics/alleles/IChromosome.java:41` document translation keys that contain the string
`forestry.bee` and `forestry.speed`. None of those are coupling. Matching on `^import` avoids
all six false positives.

**Files:**
- Modify: `build.gradle` (add task after the `tasks.named('test', Test)` block near line 87)

**Interfaces:**
- Consumes: nothing.
- Produces: Gradle task `checkApiBoundary`, wired as a dependency of `check`. Every later task
  in this plan uses `./gradlew checkApiBoundary` as its test command.

- [ ] **Step 1: Write the gate**

Add to `build.gradle`, immediately after the existing `tasks.named('test', Test) { ... }`
block:

```groovy
// Phase 1 of the feature reorg: nothing under forestry.api may import a forestry package
// outside forestry.api. Import-based on purpose. A constant-pool scan misses types that
// appear only in a method descriptor or an erased generic (IHiveManager's
// ImmutableList<VillageHive>), and a plain text scan trips over javadoc that names impl
// classes as {@code ...} prose. The descriptor-aware gate arrives with ArchUnit in phase 9.
var checkApiBoundary = tasks.register('checkApiBoundary') {
	group = 'verification'
	description = 'Fails if forestry.api imports any forestry package outside forestry.api'

	var apiDir = project.file('src/main/java/forestry/api')
	inputs.dir(apiDir)
	outputs.upToDateWhen { false }

	doLast {
		var offenders = new TreeMap<String, List<String>>()

		apiDir.eachFileRecurse(groovy.io.FileType.FILES) { file ->
			if (!file.name.endsWith('.java')) {
				return
			}
			var relative = apiDir.toPath().relativize(file.toPath()).toString()
			file.eachLine('UTF-8') { line, number ->
				if (line ==~ /^import (static )?forestry\.(?!api\.).*/) {
					offenders.computeIfAbsent(relative, { [] }).add("line ${number}: ${line.trim()}")
				}
			}
		}

		if (!offenders.isEmpty()) {
			var total = offenders.values().sum { it.size() }
			var report = offenders.collect { name, lines ->
				"  ${name}\n" + lines.collect { "      ${it}" }.join('\n')
			}.join('\n')
			throw new GradleException(
					"forestry.api must not import outside forestry.api.\n" +
					"${offenders.size()} file(s), ${total} import(s):\n${report}")
		}

		logger.lifecycle('checkApiBoundary: forestry.api is clean')
	}
}

tasks.named('check') {
	dependsOn checkApiBoundary
}
```

- [ ] **Step 2: Run the gate to verify it fails**

```bash
./gradlew checkApiBoundary
```

Expected: `BUILD FAILED`, with the message `16 file(s), 24 import(s):` followed by a sorted
list beginning `ForestryTags.java` and ending `multiblock/IMultiblockComponent.java`.

If the count is not exactly 16 files and 24 imports, do not proceed. Either the regex is wrong
or the tree has drifted since this plan was written; reconcile before continuing.

- [ ] **Step 3: Verify the gate has no false positives on the six known javadoc cases**

```bash
./gradlew checkApiBoundary 2>&1 | grep -cE 'ISpeciesType|IChromosome|core/Product|core/IProduct|core/FluidProduct'
```

Expected: `0`. Those files contain the strings `forestry.core`, `forestry.bee` and
`forestry.speed` in javadoc and must not be reported.

- [ ] **Step 4: Commit**

```bash
git add build.gradle
git commit -m "build: add checkApiBoundary gate for the api package

Fails while forestry.api imports any forestry package outside forestry.api.
Currently red at 16 files / 24 imports; phases 1a and 1b drive it to green.

Import-based rather than bytecode-based: a constant-pool scan misses types
that appear only in a method descriptor or an erased generic, and would
report clean while IHiveManager still returns ImmutableList<VillageHive>."
```

---

### Task 2: Remove javadoc-only imports

Three files import an impl class solely so a javadoc tag can link to it. No bytecode
dependency exists. The house convention for naming an impl class from api javadoc is already
established in `api/core/Product.java:44` and `api/core/IProduct.java:64`, which write
`{@code forestry.core.genetics.ProductTypes}` as prose. Follow it.

**Files:**
- Modify: `src/main/java/forestry/api/apiculture/FlowerTypeType.java`
- Modify: `src/main/java/forestry/api/mail/IPostalCarrier.java`
- Modify: `src/main/java/forestry/api/arboriculture/ITreeManager.java`

**Interfaces:**
- Consumes: `checkApiBoundary` from Task 1.
- Produces: nothing. No signature changes.

- [ ] **Step 1: Fix FlowerTypeType**

Delete line 4:

```java
import forestry.apiculture.genetics.FlowerTypeTypes;
```

Change line 11 from:

```java
 * datapacks and network sync. Registered in {@link FlowerTypeTypes}.
```

to:

```java
 * datapacks and network sync. Registered in {@code forestry.apiculture.genetics.FlowerTypeTypes}.
```

- [ ] **Step 2: Fix IPostalCarrier**

Delete line 4:

```java
import forestry.mail.LetterUtils;
```

Change line 39 from:

```java
	 * @param letterstack ItemStack representing the letter. See {@link LetterUtils} for helper functions to validate and extract it.
```

to:

```java
	 * @param letterstack ItemStack representing the letter. See {@code forestry.mail.LetterUtils} for helper functions to validate and extract it.
```

- [ ] **Step 3: Fix ITreeManager**

Delete lines 3 and 4:

```java
import forestry.arboriculture.ForestryWoodType;
import forestry.arboriculture.VanillaWoodType;
```

In the interface javadoc, delete these two lines:

```java
 * @see ForestryWoodType
 * @see VanillaWoodType
```

and change the opening description line from:

```java
 * Provides access to tree-related registries, and to Forestry and Vanilla wood items.
```

to:

```java
 * Provides access to tree-related registries, and to Forestry and Vanilla wood items.
 * Wood types are supplied by the arboriculture module as {@code IWoodType} implementations.
```

Leave `@see WoodBlockKind` and `@since 2.6.0` alone.

- [ ] **Step 4: Run the gate**

```bash
./gradlew checkApiBoundary
```

Expected: `BUILD FAILED` with `13 file(s), 20 import(s):`. The three files edited in this task
must no longer appear in the list.

- [ ] **Step 5: Verify it still compiles**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/forestry/api/apiculture/FlowerTypeType.java \
        src/main/java/forestry/api/mail/IPostalCarrier.java \
        src/main/java/forestry/api/arboriculture/ITreeManager.java
git commit -m "api: drop javadoc-only imports of impl classes

FlowerTypeType, IPostalCarrier and ITreeManager each imported an impl class
purely so a javadoc tag could link it. Named as {@code} prose instead,
matching the existing convention in api/core/Product and api/core/IProduct.

checkApiBoundary: 16 -> 13 files."
```

---

### Task 3: Delete the ForestryTags wood aliases

`ForestryTags.Blocks` and `ForestryTags.Items` each hold 42 constants that are pure aliases for
`ForestryWoodType.<TYPE>.blockTag` and `.itemTag`. Line 36 already carries
`// todo remove in favor of directly using ITreeManager`. They are the reason the base api
depends on arboriculture at all.

There are 43 wood types; `SOUR_CHERRY` has no alias, which is exactly the drift a
hand-maintained list produces.

The only in-repo consumer is `PodFruit.java:52-54`. `PodFruit` already lives in
`forestry.arboriculture`, so it references `ForestryWoodType` directly rather than going
through `ITreeManager` - it is inside the module that owns the enum.

**Files:**
- Modify: `src/main/java/forestry/api/ForestryTags.java`
- Modify: `src/main/java/forestry/arboriculture/PodFruit.java`

**Interfaces:**
- Consumes: `checkApiBoundary` from Task 1.
- Produces: removes 84 public constants. Addons using `ForestryTags.Blocks.<WOOD>_LOGS` must
  migrate to `IForestryApi.INSTANCE.getTreeManager().getLogBlockTag(IWoodType, boolean)`.

- [ ] **Step 1: Delete the block aliases**

In `ForestryTags.java`, in `public static class Blocks`, delete the `todo` comment and all 42
block alias lines. That is everything between:

```java
		public static final TagKey<Block> STORAGE_BLOCKS_RAW_TIN = commonTag("storage_blocks/raw_tin");
```

and:

```java
		// Categories of flowers
```

Leave one blank line separating those two. The deleted block starts with:

```java
		// todo remove in favor of directly using ITreeManager
		public static final TagKey<Block> LARCH_LOGS = ForestryWoodType.LARCH.blockTag;
```

and ends with:

```java
		public static final TagKey<Block> KAURI_LOGS = ForestryWoodType.KAURI.blockTag;
```

- [ ] **Step 2: Delete the item aliases**

In `public static class Items`, delete all 42 item alias lines - everything between:

```java
		public static final TagKey<Item> STORAGE_BLOCKS_RAW_TIN = commonTag("storage_blocks/raw_tin");
```

and:

```java
		public static final TagKey<Item> STAMPS = itemTag("stamps");
```

Leave one blank line separating those two. The deleted block starts with
`LARCH_LOGS = ForestryWoodType.LARCH.itemTag;` and ends with
`KAURI_LOGS = ForestryWoodType.KAURI.itemTag;`.

- [ ] **Step 3: Delete the import**

Delete line 3:

```java
import forestry.arboriculture.ForestryWoodType;
```

- [ ] **Step 4: Delete the stale class-init comment**

Near line 267, above the `blockTag` and `itemTag` helpers, delete this line:

```java
	// These have to be outside of Blocks and Items classes so that ForestryWoodType doesn't cause a circular dependency
```

Once the aliases are gone, `ForestryWoodType` is not referenced anywhere in the file, so the
cycle the comment documents no longer exists. Leave the two `@ApiStatus.Internal` helper
methods exactly where they are - only the comment goes.

- [ ] **Step 5: Redirect PodFruit**

In `src/main/java/forestry/arboriculture/PodFruit.java`, add to the imports, in alphabetical
position after `forestry.api.genetics.alleles.TreeChromosomes`:

```java
import forestry.arboriculture.ForestryWoodType;
```

Change `getLogTag()` from:

```java
	@Override
	public TagKey<Block> getLogTag() {
		return switch (this.type) {
			case DATES -> ForestryTags.Blocks.PALM_LOGS;
			case PAPAYA -> ForestryTags.Blocks.PAPAYA_LOGS;
			case COCONUT -> ForestryTags.Blocks.COCONUT_LOGS;
			default -> BlockTags.JUNGLE_LOGS;
		};
	}
```

to:

```java
	@Override
	public TagKey<Block> getLogTag() {
		return switch (this.type) {
			case DATES -> ForestryWoodType.PALM.blockTag;
			case PAPAYA -> ForestryWoodType.PAPAYA.blockTag;
			case COCONUT -> ForestryWoodType.COCONUT.blockTag;
			default -> BlockTags.JUNGLE_LOGS;
		};
	}
```

Note the mapping is not name-for-name: the alias `PALM_LOGS` pointed at `ForestryWoodType.PALM`,
but for example `ACACIA_DESERT_LOGS` pointed at `CAMELTHORN` and `CITRUS_LOGS` at `LEMON`. Only
the three above are used; do not generalize the pattern.

- [ ] **Step 6: Remove the now-unused ForestryTags import from PodFruit if it is unused**

```bash
grep -n "ForestryTags" src/main/java/forestry/arboriculture/PodFruit.java
```

If the only remaining hit is the import on line 3, delete that import. If there are other
uses, leave it.

- [ ] **Step 7: Compile**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`. A failure here means another consumer of the deleted aliases
exists that this plan did not find; report the file and line rather than re-adding a constant.

- [ ] **Step 8: Run the gate**

```bash
./gradlew checkApiBoundary
```

Expected: `BUILD FAILED` with `12 file(s), 19 import(s):`. `ForestryTags.java` must no longer
appear.

- [ ] **Step 9: Verify datagen is unchanged**

```bash
./gradlew runData && git status --porcelain src/generated/resources
```

Expected: `BUILD SUCCESSFUL` and no output. The aliases pointed at the same `TagKey` objects as
the enum fields, so tag output must be byte-identical. Any diff here is a real defect.

- [ ] **Step 10: Commit**

```bash
git add src/main/java/forestry/api/ForestryTags.java \
        src/main/java/forestry/arboriculture/PodFruit.java
git commit -m "api: delete the 84 wood tag aliases from ForestryTags

42 block and 42 item constants that were pure aliases for
ForestryWoodType.<TYPE>.blockTag/.itemTag, carrying a todo since they were
added. They are why the base api depended on arboriculture at all.

The list had already drifted: 43 wood types, 42 aliases, SOUR_CHERRY absent.

PodFruit was the only in-repo consumer and now uses ForestryWoodType
directly, being inside the module that owns it. Addons should use
ITreeManager.getLogBlockTag(IWoodType, boolean).

checkApiBoundary: 13 -> 12 files."
```

---

### Task 4: Retype the three life stage enums

`BeeLifeStage`, `TreeLifeStage` and `ButterflyLifeStage` are api enums whose constants hold an
`ItemLike` taken straight from the content module's registration holder.

This is also a genuine circular dependency that currently survives on class-init ordering:
`ApicultureItems.BEE_DRONE` is `REGISTRY.item(() -> new ItemBeeGE(BeeLifeStage.DRONE), "drone_bee")`,
so `ApicultureItems` needs `BeeLifeStage` while `BeeLifeStage` needs `ApicultureItems`. Storing
a `ResourceLocation` and resolving on demand breaks the cycle as well as the leak.

`BuiltInRegistries.ITEM` is a `DefaultedRegistry<Item>`, so `get(ResourceLocation)` is
annotated `@Nonnull` and returns `Items.AIR` for an unregistered id. With the owning jar absent
`getItemForm()` therefore yields AIR rather than throwing, which is the behaviour D7 specifies
for absent modules.

**The registry names do not match the constant names.** Copy them exactly from the table
below; they were read from the `REGISTRY.item(...)` calls.

| Constant | Registry name |
| --- | --- |
| `BEE_QUEEN` | `queen_bee` |
| `BEE_DRONE` | `drone_bee` |
| `BEE_PRINCESS` | `princess_bee` |
| `BEE_LARVAE` | `larvae_bee` |
| `TREE_SAPLING` | `tree_sapling` |
| `TREE_POLLEN` | `tree_pollen` |
| `BUTTERFLY_GE` | `butterfly` |
| `SERUM_GE` | `butterfly_serum` |
| `CATERPILLAR_GE` | `caterpillar` |
| `COCOON_GE` | `cocoon` |

**Files:**
- Modify: `src/main/java/forestry/api/apiculture/genetics/BeeLifeStage.java`
- Modify: `src/main/java/forestry/api/arboriculture/genetics/TreeLifeStage.java`
- Modify: `src/main/java/forestry/api/lepidopterology/genetics/ButterflyLifeStage.java`

**Interfaces:**
- Consumes: `checkApiBoundary` from Task 1.
- Produces: no public signature change. `getSerializedName()` returns the same strings and
  `getItemForm()` keeps returning `Item`. Enum constructors are private, so retyping them is
  not a breaking change.

- [ ] **Step 1: Rewrite BeeLifeStage**

Replace the whole of `src/main/java/forestry/api/apiculture/genetics/BeeLifeStage.java` with:

```java
package forestry.api.apiculture.genetics;

import forestry.api.ForestryConstants;
import forestry.api.genetics.ILifeStage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Locale;

public enum BeeLifeStage implements ILifeStage {
	DRONE(ForestryConstants.forestry("drone_bee")),
	PRINCESS(ForestryConstants.forestry("princess_bee")),
	QUEEN(ForestryConstants.forestry("queen_bee")),
	LARVAE(ForestryConstants.forestry("larvae_bee"));

	private final String name;
	// resolved on demand, not held: the item is registered by the apiculture module, which
	// also builds its items from these constants
	private final ResourceLocation itemId;

	BeeLifeStage(ResourceLocation itemId) {
		this.name = name().toLowerCase(Locale.ENGLISH);
		this.itemId = itemId;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	@Override
	public Item getItemForm() {
		return BuiltInRegistries.ITEM.get(this.itemId);
	}
}
```

- [ ] **Step 2: Verify the ids resolve**

The failure mode here is silent - a wrong id yields AIR, not an exception. Check the ids
against the registration site:

```bash
grep -n 'REGISTRY.item' src/main/java/forestry/apiculture/features/ApicultureItems.java | grep -E 'BEE_(QUEEN|DRONE|PRINCESS|LARVAE)'
```

Expected: four lines whose trailing string literals are exactly `"queen_bee"`, `"drone_bee"`,
`"princess_bee"`, `"larvae_bee"`. If any differ from what was written in Step 1, fix Step 1 to
match the source, not the other way round.

- [ ] **Step 3: Rewrite TreeLifeStage**

Note `getSerializedName()` here carries no `@Override`, unlike `BeeLifeStage`. Preserve that -
adding one is an unrelated change and pollutes the diff this phase is measured by.

```java
package forestry.api.arboriculture.genetics;

import forestry.api.ForestryConstants;
import forestry.api.genetics.ILifeStage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Locale;

public enum TreeLifeStage implements ILifeStage {
	SAPLING(ForestryConstants.forestry("tree_sapling")),
	POLLEN(ForestryConstants.forestry("tree_pollen"));

	private final String name;
	// resolved on demand, not held: the item is registered by the arboriculture module
	private final ResourceLocation itemId;

	TreeLifeStage(ResourceLocation itemId) {
		this.name = name().toLowerCase(Locale.ENGLISH);
		this.itemId = itemId;
	}

	public String getSerializedName() {
		return this.name;
	}

	@Override
	public Item getItemForm() {
		return BuiltInRegistries.ITEM.get(this.itemId);
	}
}
```

- [ ] **Step 4: Rewrite ButterflyLifeStage**

```java
package forestry.api.lepidopterology.genetics;

import forestry.api.ForestryConstants;
import forestry.api.genetics.ILifeStage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Locale;

public enum ButterflyLifeStage implements ILifeStage {
	BUTTERFLY(ForestryConstants.forestry("butterfly")),
	SERUM(ForestryConstants.forestry("butterfly_serum")),
	CATERPILLAR(ForestryConstants.forestry("caterpillar")),
	COCOON(ForestryConstants.forestry("cocoon"));

	private final String name;
	// resolved on demand, not held: the item is registered by the lepidopterology module
	private final ResourceLocation itemId;

	ButterflyLifeStage(ResourceLocation itemId) {
		this.name = name().toLowerCase(Locale.ENGLISH);
		this.itemId = itemId;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	@Override
	public Item getItemForm() {
		return BuiltInRegistries.ITEM.get(this.itemId);
	}
}
```

- [ ] **Step 5: Verify the tree and butterfly ids**

```bash
grep -n 'REGISTRY.item' src/main/java/forestry/arboriculture/features/ArboricultureItems.java | grep -E 'TREE_(SAPLING|POLLEN)'
grep -n 'REGISTRY.item' src/main/java/forestry/lepidopterology/features/LepidopterologyItems.java | grep -E '(BUTTERFLY_GE|SERUM_GE|CATERPILLAR_GE|COCOON_GE)'
```

Expected: string literals exactly `"tree_sapling"`, `"tree_pollen"`, `"butterfly"`,
`"butterfly_serum"`, `"caterpillar"`, `"cocoon"`.

- [ ] **Step 6: Compile**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Run the gate**

```bash
./gradlew checkApiBoundary
```

Expected: `BUILD FAILED` with `9 file(s), 16 import(s):`. None of the three life stage files
may appear.

- [ ] **Step 8: Run the GameTests**

This is the step that catches a wrong registry id, because AIR fails loudly here and nowhere
else.

```bash
./gradlew runGameTestServer
```

Expected: all tests pass. `GenomeBaselineTest`, `BeeSpeciesDefinitionTest`,
`ButterflySpeciesDefinitionTest` and `DefaultLeavesItemTest` all exercise life stage item
forms. A failure naming AIR or a missing item means an id in Steps 1, 3 or 4 is wrong.

- [ ] **Step 9: Verify datagen is unchanged**

```bash
./gradlew runData && git status --porcelain src/generated/resources
```

Expected: `BUILD SUCCESSFUL` and no output.

- [ ] **Step 10: Commit**

```bash
git add src/main/java/forestry/api/apiculture/genetics/BeeLifeStage.java \
        src/main/java/forestry/api/arboriculture/genetics/TreeLifeStage.java \
        src/main/java/forestry/api/lepidopterology/genetics/ButterflyLifeStage.java
git commit -m "api: resolve life stage items from the registry

The three life stage enums held ItemLike references taken from each
module's registration holder, which is also a class-init cycle:
ApicultureItems.BEE_DRONE is built from BeeLifeStage.DRONE while
BeeLifeStage.DRONE held ApicultureItems.BEE_DRONE.

They now hold a ResourceLocation and resolve through BuiltInRegistries.ITEM,
which is a DefaultedRegistry, so an absent module yields AIR rather than
throwing - the behaviour D7 specifies for absent modules.

No public signature changes; enum constructors are private.

checkApiBoundary: 12 -> 9 files."
```

---

### Task 5: Close out phase 1a

**Files:**
- Modify: `docs/superpowers/specs/2026-07-30-feature-package-reorg-design.md` (progress note only)

**Interfaces:**
- Consumes: everything above.
- Produces: a recorded gate count that phase 1b starts from.

- [ ] **Step 1: Confirm the gate reports exactly the phase 1b set**

```bash
./gradlew checkApiBoundary 2>&1 | grep -E '^\s{2}\S+\.java$'
```

Expected: exactly these nine files, and nothing else:

```
apiculture/genetics/IBeeEffect.java
apiculture/hives/IHiveManager.java
genetics/alleles/BeeChromosomes.java
genetics/alleles/ButterflyChromosomes.java
genetics/alleles/TreeChromosomes.java
genetics/alyzer/IAlleleDisplayHelper.java
genetics/capability/IIndividualHandlerItem.java
mail/ITradeStation.java
multiblock/IMultiblockComponent.java
```

- [ ] **Step 2: Confirm the full build is green apart from the gate**

```bash
./gradlew build -x checkApiBoundary
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Record progress in the spec**

In `docs/superpowers/specs/2026-07-30-feature-package-reorg-design.md`, under the sequencing
code block, add one line after the block:

```markdown
Phase 1a landed 2026-07-31: `checkApiBoundary` is in place and at 9 of the original 16 files.
The remaining nine all require a new public api type or an SPI inversion, and are phase 1b.
```

- [ ] **Step 4: Commit**

```bash
git add docs/superpowers/specs/2026-07-30-feature-package-reorg-design.md
git commit -m "docs: record phase 1a completion, gate at 9 of 16"
```

---

## Notes for phase 1b

Findings from 1a that 1b depends on:

- `IHiveManager` returns `ImmutableList<VillageHive>`. Because generics erase, this leak is
  invisible to a constant-pool scan; the import gate is what catches it. The same is true of
  `IAlleleDisplayHelper`'s `IGeneticTooltipProvider` parameters, which appear only in method
  descriptors.
- `ITradeStation` is the only file with two leaks of different kinds - `core.inventory.IInventoryAdapter`
  (core impl, which stays in base) and `mail.IWatchable` (content jar). Only the second is a
  hard blocker, but both interfaces are in its `extends` clause, so they resolve together.
- `IMultiblockComponent.getInternalInventory()` and `ITradeStation` both need `IInventoryAdapter`,
  so promote it once and fix both in one task.
- `IIndividualHandlerItem` calls `ItemGE` statics at six sites; `IBeeEffect` calls
  `ParticleRender.addBeeHiveFX` at one. The second is client-side, and `IForestryClientApi`
  already exists as the natural home for a particle hook.
- The three `*Chromosomes` classes share the identical import triple - `Forestry`,
  `ChromosomeFactory`, `SpeciesUtil` - so they are one task, not three.
