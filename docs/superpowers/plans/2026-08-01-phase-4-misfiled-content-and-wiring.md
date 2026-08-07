# Phase 4: misfiled content, plumbing and lifecycle wiring

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Clear buckets D, E and H - the ten base-artifact files holding misfiled content, render/GUI
plumbing and central lifecycle wiring - taking `checkBaseBoundary` from 49 to 39.

**Architecture:** Buckets D and E are seven files and follow the pattern of the last three phases:
five are relocations or one-line redirections, one is a clean method split, one is a small extension
point. Bucket H is three files and is different in kind: `ModuleCore`, `EventHandlerCore` and
`CoreClientHandler` hold 23 of the phase's 45 leaks, and `ModuleCore`'s reload and datapack-sync
blocks encode a **cross-module ordering invariant** that does not survive naive relocation. Tasks 1
through 7 are ordered cheapest first; tasks 8 and 9 are bucket H and carry the phase's only real
design decision.

**Tech Stack:** Java 21, NeoForge 21.1.230, Minecraft 1.21.1, ModDevGradle 2.0.x. GameTests only, no
JUnit.

## Global Constraints

- Comment and Javadoc style is binding; see `CLAUDE.md`. ASCII only. `@return` and `@param` are
  noun-phrase fragments with no terminal period. Lowercase `todo`.
- Every task ends with `./gradlew runData` producing no diff in `src/generated/resources` and
  `./gradlew runGameTestServer` reporting all 100 tests passed.
- Both gates stay honest: `checkApiBoundary` green, `checkBaseBoundary` trimmed in the same commit
  that clears a file. A stale baseline fails the build by design - it did so once in phase 3 and
  that was the gate working.
- **Deleting a usage is not enough.** Java permits unused imports, so the import-based gate still
  sees a file as leaking until the import line goes too.
- **Run `./gradlew compileTestJava`, not just `compileJava`.** Phase 3 moved a class whose main-source
  callers all compiled while eleven test files did not. `build` covers both; `compileJava` does not.
- All source files are LF as of 2026-07-31. Do not write `$`-anchored `sed` patterns that assume
  otherwise.
- When a class moves package, check for callers that used it **without an import** because they were
  in the same package, and for imports inside the moved file that same-package placement now makes
  redundant. This has bitten in phases 1b, 2 and 3, in both directions.
- **Do not rewrite fully-qualified references with a bare class-name `sed`.** Phase 3 turned
  `forestry.core.genetics.GeneticsReloadHandler.rebuildFlowerTypes` into
  `forestry.core.genetics.ApicultureReloadHandler` - a package that does not exist. After any such
  sweep, grep for the old package paired with the new class name.

## Starting state

`checkBaseBoundary`: 49 files. Buckets D, E and H are 10 of them, holding 45 leaking imports.

| File | Bucket | Leaks | What it actually is |
| --- | --- | --- | --- |
| `core/ModuleCore.java` | H | 14 | reload, sync and packet wiring for five modules |
| `core/worldgen/ApiaristPoolElement.java` | D | 7 | apiculture's village structure piece |
| `core/worldgen/FeatureHelper.java` | D | 6 | arboriculture's tree worldgen helper |
| `core/render/ParticleRender.java` | E | 6 | two bee methods in an otherwise generic class |
| `core/client/CoreClientHandler.java` | H | 6 | four modules' block and item colours |
| `core/multiblock/LevelStructureView.java` | E | 5 | hardcoded BE-class to pattern-id dispatch |
| `core/EventHandlerCore.java` | H | 3 | apiculture villager AI, effects and jigsaw |
| `core/models/baker/ModelBakerModel.java` | E | 1 | borrows a constant from a leaf model |
| `core/items/ItemSpectacles.java` | D | 1 | a misfiled 14-line capability |
| `core/gui/widgets/TankWidget.java` | E | 1 | one `instanceof` that an interface covers |

### The dead-import sweep came back empty

Phase 3's handoff said to run the dead-import sweep across bucket D, E and H first because it is
nearly free and found one file that way. It was run over all ten files and all 45 leaking imports:
**zero dead imports**. The sweep is now settled for these buckets - do not repeat it.

```bash
python3 - <<'EOF'
import re
files = """core/worldgen/ApiaristPoolElement.java core/worldgen/FeatureHelper.java core/items/ItemSpectacles.java
core/render/ParticleRender.java core/models/baker/ModelBakerModel.java core/gui/widgets/TankWidget.java
core/multiblock/LevelStructureView.java core/ModuleCore.java core/EventHandlerCore.java
core/client/CoreClientHandler.java""".split()
split = r'(apiculture|arboriculture|lepidopterology|farming|cultivation|mail)'
for f in files:
    src = open('src/main/java/forestry/' + f).read()
    for m in re.finditer(r'^import (?:static )?forestry\.' + split + r'\.([\w.]+);', src, re.M):
        cls = m.group(2).split('.')[-1]
        if not re.search(r'\b' + re.escape(cls) + r'\b', src.replace(m.group(0), '')):
            print("DEAD", f, m.group(0))
EOF
```

### Two spec claims that are stale or wrong

- Bucket E says "`ParticleRender` matters extra because `api/apiculture/genetics/IBeeEffect` imports
  it". **It does not.** `checkApiBoundary` has been green since phase 1, and `grep -rn ParticleRender
  src/main/java/forestry/api/` returns nothing. `ParticleRender` is an ordinary bucket-E split.
- Bucket D lists `ItemSpectacles` as a decision - "to arboriculture **or** the capability moves to
  base". There is no decision left to make; see Task 1.

## File Structure

| Action | File | Responsibility |
| --- | --- | --- |
| Move | `arboriculture/capabilities/SpectacleVision.java` -> `core/capabilities/` | the spectacles capability, next to its only consumer |
| Create | `core/models/ModelTransforms.java` | the vanilla block item-transform table |
| Create | `core/gui/IContainerTank.java` | the minimal fluid-tank container view |
| Move | `core/worldgen/FeatureHelper.java` -> `arboriculture/worldgen/` | tree worldgen shapes |
| Move | `core/worldgen/ApiaristPoolElement.java`, `VillagerJigsaw.java` -> `apiculture/worldgen/` | the apiarist village house |
| Create | `apiculture/particles/BeeParticleRender.java` | the two bee-specific particle spawns |
| Modify | `core/multiblock/MultiblockTileEntityForestry.java` | gains `patternTypeId()` |
| Modify | `api/modules/IForestryModule.java` | gains two ordered lifecycle hooks |
| Modify | `core/ModuleCore.java`, `EventHandlerCore.java`, `client/CoreClientHandler.java` | keep the sequencing, shed the content |

---

### Task 1: Move SpectacleVision into core

`ItemSpectacles` is a core item (`CoreItems.SPECTACLES`) whose capability is registered by core
(`ModuleCore:142`). Its one leak is `forestry.arboriculture.capabilities.SpectacleVision`, a 14-line
enum that imports only `forestry.api.core.ISpectacleVision` and two Minecraft types - nothing from
arboriculture at all. `forestry/arboriculture/capabilities/` contains that file and a
`package-info.java` and nothing else, so the move empties the package.

The spec framed this as a choice between moving `ItemSpectacles` to arboriculture or moving the
capability to base. It is not a choice: the item, its registration and its only consumer are all core,
and the implementation has no arboriculture content. This is the same shape as
`NaturalistChestBlockType` in phase 2 and `TaxonManager` in phase 3.

**Files:**
- Move: `src/main/java/forestry/arboriculture/capabilities/SpectacleVision.java` -> `src/main/java/forestry/core/capabilities/SpectacleVision.java`
- Delete: `src/main/java/forestry/arboriculture/capabilities/package-info.java`
- Modify: `src/main/java/forestry/core/items/ItemSpectacles.java:6`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: nothing.
- Produces: `forestry.core.capabilities.SpectacleVision`, unchanged in shape. No behavior change.

- [ ] **Step 1: Confirm the package holds nothing else**

```bash
ls src/main/java/forestry/arboriculture/capabilities/
grep -rn "SpectacleVision" src/main/java --include='*.java'
```

Expected: `SpectacleVision.java` and `package-info.java` only, and exactly three non-api references -
the class itself, `ItemSpectacles:6` (import) and `ItemSpectacles:19` (use). `GeneticsUtil:89` goes
through the api interface and must not appear.

- [ ] **Step 2: Move it, taking the package-info with it**

```bash
mkdir -p src/main/java/forestry/core/capabilities
git mv src/main/java/forestry/arboriculture/capabilities/SpectacleVision.java src/main/java/forestry/core/capabilities/SpectacleVision.java
sed -i '1s@^package forestry\.arboriculture\.capabilities;@package forestry.core.capabilities;@' src/main/java/forestry/core/capabilities/SpectacleVision.java
```

Read `src/main/java/forestry/arboriculture/capabilities/package-info.java` first. If it is only a
`@ParametersAreNonnullByDefault`-style marker, copy it to the new package and `git rm` the old one:

```bash
git mv src/main/java/forestry/arboriculture/capabilities/package-info.java src/main/java/forestry/core/capabilities/package-info.java
sed -i 's@^package forestry\.arboriculture\.capabilities;@package forestry.core.capabilities;@' src/main/java/forestry/core/capabilities/package-info.java
```

Check whether `forestry/core/` already has a package-info convention - if core packages do not carry
one, delete it rather than moving it.

- [ ] **Step 3: Repoint the one import**

In `src/main/java/forestry/core/items/ItemSpectacles.java`, replace line 6:

```java
import forestry.arboriculture.capabilities.SpectacleVision;
```

with:

```java
import forestry.core.capabilities.SpectacleVision;
```

- [ ] **Step 4: Compile, trim, verify**

```bash
./gradlew compileJava compileTestJava
```

Remove `core/items/ItemSpectacles.java` from `gradle/base-boundary-baseline.txt`, then:

```bash
./gradlew checkBaseBoundary checkApiBoundary
```

Expected: `checkBaseBoundary: 48 known leaking file(s) remaining`.

- [ ] **Step 5: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "core: move the spectacles capability out of arboriculture

SpectacleVision is a 14-line enum implementing an api interface, with no
arboriculture content and one consumer: ItemSpectacles, a core item whose
capability core itself registers. The package it left holds nothing else.

The spec offered a choice between moving the item or moving the capability.
There was none to make.

checkBaseBoundary: 49 -> 48 files."
```

---

### Task 2: Give the block item-transform table a core home

`ModelBakerModel.getTransforms()` returns `ModelLeaves.TRANSFORMS`. That constant is annotated in
`ModelLeaves:37` with `// copied from "minecraft:block/block.json" model` - it is the vanilla block
transform table, not anything about leaves. Core render plumbing reaching into an arboriculture model
class for it is the whole leak.

**Files:**
- Create: `src/main/java/forestry/core/models/ModelTransforms.java`
- Modify: `src/main/java/forestry/arboriculture/models/ModelLeaves.java:38-49,137`
- Modify: `src/main/java/forestry/core/models/baker/ModelBakerModel.java:3,103`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: nothing.
- Produces: `ModelTransforms.BLOCK`, an `ItemTransforms` with the identical eight transforms
  `ModelLeaves.TRANSFORMS` holds today.

- [ ] **Step 1: Create the core holder**

Create `src/main/java/forestry/core/models/ModelTransforms.java`, moving the constant verbatim:

```java
package forestry.core.models;

import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * Shared item-transform tables for Forestry's baked models.
 */
@OnlyIn(Dist.CLIENT)
public class ModelTransforms {
	// copied from "minecraft:block/block.json" model
	public static final ItemTransforms BLOCK = new ItemTransforms(
		new ItemTransform(new Vector3f(75, 45, 0), new Vector3f(0, 2.5f / 16f, 0), new Vector3f(0.375f, 0.375f, 0.375f)),
		new ItemTransform(new Vector3f(75, 45, 0), new Vector3f(0, 2.5f / 16f, 0), new Vector3f(0.375f, 0.375f, 0.375f)),
		new ItemTransform(new Vector3f(0, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.4f, 0.4f, 0.4f)),
		new ItemTransform(new Vector3f(0, 45, 0), new Vector3f(0, 0, 0), new Vector3f(0.4f, 0.4f, 0.4f)),
		ItemTransform.NO_TRANSFORM,
		new ItemTransform(new Vector3f(30, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.625f, 0.625f, 0.625f)),
		new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 3 / 16f, 0), new Vector3f(0.25f, 0.25f, 0.25f)),
		new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), new Vector3f(0.5f, 0.5f, 0.5f))
	);

	private ModelTransforms() {}
}
```

Copy the numbers from `ModelLeaves.java:38-49` rather than from this plan, and diff the two blocks
before deleting the original. A transposed digit here is a silent render bug no test catches.

- [ ] **Step 2: Point both users at it**

In `ModelLeaves.java`: delete the `TRANSFORMS` constant, add
`import forestry.core.models.ModelTransforms;`, and change line 137 to `return ModelTransforms.BLOCK;`.
Delete any `ItemTransform`/`Vector3f` import the constant alone needed.

In `ModelBakerModel.java`: delete `import forestry.arboriculture.models.ModelLeaves;` (line 3), add
`import forestry.core.models.ModelTransforms;`, and change line 103 to `return ModelTransforms.BLOCK;`.

- [ ] **Step 3: Compile, trim, verify**

```bash
./gradlew compileJava compileTestJava
```

Remove `core/models/baker/ModelBakerModel.java` from the baseline, then:

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `47 known leaking file(s)`, no datagen diff, all 100 tests passed. Note that **no test
covers item transforms** - they are client-only render state. The diff of the constant in Step 1 is
the real oracle here.

- [ ] **Step 4: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "core: move the vanilla block transform table out of ModelLeaves

The constant is annotated 'copied from minecraft:block/block.json' - it is
the vanilla block transform table, not leaf state. Core's ModelBakerModel
reached into an arboriculture model class for it.

checkBaseBoundary: 48 -> 47 files."
```

---

### Task 3: Extract the minimal tank-container interface

`TankWidget.getTank()` checks `IContainerLiquidTanks` and then special-cases `ContainerFarm`:

```java
if (container instanceof IContainerLiquidTanks tanks) {
    return tanks.getTank(this.slot);
} else if (container instanceof ContainerFarm farm) {
    return farm.getTank(this.slot);
}
```

`ContainerFarm.getTank(int)` has the **identical signature** to `IContainerLiquidTanks.getTank(int)`.
The special case exists only because `IContainerLiquidTanks` also demands two pipette methods
`ContainerFarm` does not implement. Splitting the interface removes the branch entirely.

Do not instead make `ContainerFarm` implement the whole of `IContainerLiquidTanks`: that would give
farms pipette handling they do not have today, which is a behavior change.

**Files:**
- Create: `src/main/java/forestry/core/gui/IContainerTank.java`
- Modify: `src/main/java/forestry/core/gui/IContainerLiquidTanks.java`
- Modify: `src/main/java/forestry/farming/gui/ContainerFarm.java:16,66`
- Modify: `src/main/java/forestry/core/gui/widgets/TankWidget.java:59-68`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: nothing.
- Produces: `forestry.core.gui.IContainerTank` with the single member
  `@Nullable IFluidTank getTank(int slot)`. `IContainerLiquidTanks extends IContainerTank` and keeps
  its own two pipette methods, so its five existing implementors are untouched.

- [ ] **Step 1: Create the minimal interface**

```java
package forestry.core.gui;

import net.neoforged.neoforge.fluids.IFluidTank;

import javax.annotation.Nullable;

/**
 * A menu that exposes fluid tanks by slot index. Split out of {@link IContainerLiquidTanks} so a menu
 * can be drawn by {@code TankWidget} without also implementing pipette handling.
 */
public interface IContainerTank {
	/**
	 * @return The tank in the given slot, or null if the slot holds none
	 */
	@Nullable
	IFluidTank getTank(int slot);
}
```

- [ ] **Step 2: Narrow IContainerLiquidTanks**

In `IContainerLiquidTanks.java`, change the declaration to `extends IContainerTank` and delete its own
`getTank` declaration (now inherited). Its `@Nullable` and `IFluidTank` imports go with it if nothing
else in the file uses them.

- [ ] **Step 3: Declare ContainerFarm's existing method**

In `ContainerFarm.java`, change line 16 to:

```java
public class ContainerFarm extends ContainerSocketed<TileFarm> implements IContainerTank {
```

Add `import forestry.core.gui.IContainerTank;` and `@Override` on `getTank` at line 66. The method
body does not change.

- [ ] **Step 4: Collapse the branch**

In `TankWidget.java`, replace the body of `getTank()` with:

```java
	@Nullable
	public IFluidTank getTank() {
		AbstractContainerMenu container = this.manager.gui.getMenu();
		if (container instanceof IContainerTank tanks) {
			return tanks.getTank(this.slot);
		}
		return null;
	}
```

Delete `import forestry.farming.gui.ContainerFarm;`.

- [ ] **Step 5: Compile, trim, verify**

```bash
./gradlew compileJava compileTestJava
```

Remove `core/gui/widgets/TankWidget.java` from the baseline, then:

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `46 known leaking file(s)`, no datagen diff, all 100 tests passed. **No GameTest opens a
GUI**, so the oracle for this task is the type system: every menu that reached branch one still
implements `IContainerLiquidTanks`, and `ContainerFarm` now reaches the same branch through
`IContainerTank`. Confirm by listing implementors:

```bash
grep -rln "IContainerLiquidTanks\|IContainerTank" src/main/java --include='*.java'
```

Expected: the five pre-existing files plus `IContainerTank.java` and `ContainerFarm.java`.

- [ ] **Step 6: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "gui: extract IContainerTank so TankWidget stops naming the farm menu

ContainerFarm.getTank already had the exact signature IContainerLiquidTanks
declares; the instanceof special case existed only because ContainerFarm does
not implement the two pipette methods. Splitting those out removes the branch.

Deliberately not making ContainerFarm implement IContainerLiquidTanks whole:
that would give farms pipette handling they do not have.

checkBaseBoundary: 47 -> 46 files."
```

---

### Task 4: Move FeatureHelper to arboriculture

`FeatureHelper` is 647 lines of tree-shape worldgen (trunks, canopies, pods) in `core/worldgen`. It
has **47 users and every one of them is in `forestry.arboriculture.worldgen`**. Core does not use it
at all.

Because the destination is the same package as all 47 users, none of them needs an import rewrite -
they need their existing `import forestry.core.worldgen.FeatureHelper;` **deleted**. This is the
same-package effect that has bitten in three phases running, at its largest scale yet.

**Files:**
- Move: `src/main/java/forestry/core/worldgen/FeatureHelper.java` -> `src/main/java/forestry/arboriculture/worldgen/FeatureHelper.java`
- Modify: 47 files in `src/main/java/forestry/arboriculture/worldgen/`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: nothing.
- Produces: `forestry.arboriculture.worldgen.FeatureHelper`, unchanged in shape.

- [ ] **Step 1: Confirm no core user**

```bash
grep -rln "FeatureHelper" src/main/java src/test/java --include='*.java' | grep -v "arboriculture/worldgen/"
```

Expected: exactly `src/main/java/forestry/core/worldgen/FeatureHelper.java`, the file itself. **If any
other path appears, stop** - a core user means this is not a clean relocation and the plan needs
revisiting.

- [ ] **Step 2: Move it**

```bash
git mv src/main/java/forestry/core/worldgen/FeatureHelper.java src/main/java/forestry/arboriculture/worldgen/FeatureHelper.java
sed -i '1s@^package forestry\.core\.worldgen;@package forestry.arboriculture.worldgen;@' src/main/java/forestry/arboriculture/worldgen/FeatureHelper.java
```

- [ ] **Step 3: Delete the now-redundant imports**

```bash
grep -rl "^import forestry\.core\.worldgen\.FeatureHelper;" src/main/java \
  | xargs -r sed -i '/^import forestry\.core\.worldgen\.FeatureHelper;$/d'
```

Then handle the moved file's own imports in both directions:

```bash
grep -n "^import forestry" src/main/java/forestry/arboriculture/worldgen/FeatureHelper.java
```

Its `forestry.arboriculture.worldgen.*` imports (`ITreeBlockType`, `TreeBlockType`, `TreeBlockTypeLog`,
`TreeContour`) are now same-package and must be deleted. Anything it used from `forestry.core.worldgen`
without an import - `FeatureBase` is the likely one - now needs one added.

```bash
for c in $(ls src/main/java/forestry/core/worldgen/*.java | xargs -n1 basename | sed 's/.java//'); do
  grep -qw "$c" src/main/java/forestry/arboriculture/worldgen/FeatureHelper.java && echo "needs import: $c"
done
```

- [ ] **Step 4: Compile, trim, verify**

```bash
./gradlew compileJava compileTestJava
```

Remove `core/worldgen/FeatureHelper.java` from the baseline, then:

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `45 known leaking file(s)`, no datagen diff, all 100 tests passed.

Worldgen shape is not covered by any GameTest and not by datagen. The compiler is the only real
oracle, and it is a sufficient one here: this task moves no code, only a package line and 47 import
lines.

- [ ] **Step 5: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "arboriculture: take ownership of FeatureHelper

647 lines of tree-shape worldgen in core/worldgen with 47 users, every one of
them already in forestry.arboriculture.worldgen. Core never used it.

checkBaseBoundary: 46 -> 45 files."
```

---

### Task 5: Move the apiarist village house to apiculture

`ApiaristPoolElement` is the jigsaw pool element for the apiarist's house: it builds a `TileApiary`
with an `InventoryApiary` and fills it from `ApicultureItems`. Seven leaks, all apiculture.

**It cannot move alone.** Its two users are `apiculture/features/ApicultureFeatures.java` and
`core/worldgen/VillagerJigsaw.java`. `VillagerJigsaw` is **not** in the baseline today - it leaks
nothing, because `ApiaristPoolElement` currently sits in core. Moving `ApiaristPoolElement` by itself
would make `VillagerJigsaw` a **new** leaking file and `checkBaseBoundary` would fail with a
regression, correctly.

`VillagerJigsaw` moves with it. Every one of its five `init` calls adds an apiarist house; only
`addToJigsawPattern` is generic, and it has no other caller. Its own caller is `EventHandlerCore:84`,
which is already in the baseline and is cleaned in Task 9.

This coupling is a finding: the spec files `ApiaristPoolElement` under bucket D and `EventHandlerCore`
under bucket H as separate work items. They are one unit.

**Files:**
- Move: `src/main/java/forestry/core/worldgen/ApiaristPoolElement.java` -> `src/main/java/forestry/apiculture/worldgen/ApiaristPoolElement.java`
- Move: `src/main/java/forestry/core/worldgen/VillagerJigsaw.java` -> `src/main/java/forestry/apiculture/worldgen/VillagerJigsaw.java`
- Modify: `src/main/java/forestry/apiculture/features/ApicultureFeatures.java:6`
- Modify: `src/main/java/forestry/core/EventHandlerCore.java:10`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: nothing.
- Produces: `forestry.apiculture.worldgen.ApiaristPoolElement` and
  `forestry.apiculture.worldgen.VillagerJigsaw`, both unchanged in shape.
  `VillagerJigsaw.init(Registry<StructureTemplatePool>, Registry<StructureProcessorList>)` keeps its
  signature - Task 9 moves its call site, not its shape.

- [ ] **Step 1: Confirm the user set**

```bash
grep -rn "ApiaristPoolElement\|VillagerJigsaw" src/main/java src/test/java --include='*.java' \
  | grep -v "worldgen/ApiaristPoolElement.java:\|worldgen/VillagerJigsaw.java:"
```

Expected exactly three: `ApicultureFeatures:6`, `EventHandlerCore:10` and `EventHandlerCore:84`.

- [ ] **Step 2: Move both**

```bash
mkdir -p src/main/java/forestry/apiculture/worldgen
git mv src/main/java/forestry/core/worldgen/ApiaristPoolElement.java src/main/java/forestry/apiculture/worldgen/ApiaristPoolElement.java
git mv src/main/java/forestry/core/worldgen/VillagerJigsaw.java src/main/java/forestry/apiculture/worldgen/VillagerJigsaw.java
sed -i '1s@^package forestry\.core\.worldgen;@package forestry.apiculture.worldgen;@' \
  src/main/java/forestry/apiculture/worldgen/ApiaristPoolElement.java \
  src/main/java/forestry/apiculture/worldgen/VillagerJigsaw.java
```

`VillagerJigsaw` referenced `ApiaristPoolElement` without an import (same package before, same package
after), so that pairing needs nothing.

- [ ] **Step 3: Repoint the two importers**

```bash
grep -rl "import forestry\.core\.worldgen\.\(ApiaristPoolElement\|VillagerJigsaw\);" src/main/java \
  | xargs -r sed -i 's@import forestry\.core\.worldgen\.\(ApiaristPoolElement\|VillagerJigsaw\);@import forestry.apiculture.worldgen.\1;@'
```

`ApicultureFeatures` is in `forestry.apiculture.features`, a different package, so it keeps an import -
just a rewritten one. `EventHandlerCore` keeps one too, and stays a leaking file until Task 9.

Then check the moved files for imports the new package makes redundant:

```bash
grep -n "^import forestry" src/main/java/forestry/apiculture/worldgen/ApiaristPoolElement.java
```

`ApiaristPoolElement` imports `forestry.apiculture.InventoryBeeHousing`,
`forestry.apiculture.inventory.InventoryApiary`, `forestry.apiculture.tiles.TileApiary` and three
`forestry.apiculture.features.*` classes. All are sibling packages, not the same package, so all six
imports stay. Nothing to delete here - verify rather than assume.

- [ ] **Step 4: Compile, trim, verify**

```bash
./gradlew compileJava compileTestJava
```

Remove `core/worldgen/ApiaristPoolElement.java` from the baseline. `core/EventHandlerCore.java` stays -
it is cleaned in Task 9. `VillagerJigsaw` must **not** appear as a new entry; if `checkBaseBoundary`
reports it, the move was incomplete.

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `44 known leaking file(s)`, no datagen diff, all 100 tests passed.

The structure template ids (`village/apiarist_house_<biome>_1`) are strings built at runtime in
`addVillagerHouse` and are unaffected by a package move. No datagen output names either class.

- [ ] **Step 5: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "apiculture: take ownership of the apiarist village house

ApiaristPoolElement builds a TileApiary and stocks it from ApicultureItems.
VillagerJigsaw moves with it: all five of its init calls add an apiarist
house, and moving the pool element alone would have made VillagerJigsaw a
new leaking file rather than removing one.

The spec filed these under separate buckets and separate phases. They are
one unit.

checkBaseBoundary: 45 -> 44 files."
```

---

### Task 6: Split the bee particles out of ParticleRender

`ParticleRender` has nine methods. Seven are generic (`shouldSpawnParticle`, honey dust, explode,
ignition, smoke, potion, portal) and have callers in core, cultivation and factory. Two are bee-only:

| Method | Only caller | Leaks it carries |
| --- | --- | --- |
| `addBeeHiveFX` | `apiimpl/client/BeeClientManager` | `Bee`, `ThrottledBeeEffect`, `ApicultureParticles`, `BeeParticleData`, `BeeTargetParticleData` |
| `addEntitySnowFX` | `apiculture/genetics/effects/SnowingBeeEffect` | `ParticleSnow` |

Those two carry all six leaks. Moving them clears the file.

`addEntityHoneyDustFX` stays in core despite the name - it uses only a core `DustParticleOptions`
constant, and its callers (`AlvearyController`, `TileBeeHousingBase`) are apiculture calling into
core, which is the allowed direction.

**Files:**
- Create: `src/main/java/forestry/apiculture/particles/BeeParticleRender.java`
- Modify: `src/main/java/forestry/core/render/ParticleRender.java`
- Modify: `src/main/java/forestry/apiimpl/client/BeeClientManager.java`
- Modify: `src/main/java/forestry/apiculture/genetics/effects/SnowingBeeEffect.java`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: `ParticleRender.shouldSpawnParticle(Level)`, which is already `public static` and stays in
  core.
- Produces: `BeeParticleRender.addBeeHiveFX(IBeeHousing, IGenome, List<BlockPos>)` and
  `BeeParticleRender.addEntitySnowFX(Level, double, double, double)`, both with the signature they
  have today.

- [ ] **Step 1: Confirm the caller split**

```bash
grep -rn "ParticleRender\.\w*" src/main/java --include='*.java' | grep -v "core/render/ParticleRender.java"
```

Expected: nine call sites. Only `BeeClientManager` calls `addBeeHiveFX`; only `SnowingBeeEffect` calls
`addEntitySnowFX`. If any other file calls either, it must be repointed too.

- [ ] **Step 2: Create the apiculture holder**

Create `src/main/java/forestry/apiculture/particles/BeeParticleRender.java` with the two method bodies
moved **verbatim** from `ParticleRender:58-106` and `ParticleRender:128-135`. It needs:

```java
package forestry.apiculture.particles;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.genetics.IBeeSpecies;
import forestry.api.apiculture.hives.IHiveTile;
import forestry.api.genetics.IGenome;
import forestry.api.genetics.alleles.BeeChromosomes;
import forestry.apiculture.genetics.Bee;
import forestry.apiculture.genetics.effects.ThrottledBeeEffect;
import forestry.core.render.ParticleRender;
import forestry.core.utils.VecUtil;

/**
 * Bee-specific particle spawns. Split out of {@link ParticleRender} so the base artifact does not
 * name bee types; the generic spawns and {@link ParticleRender#shouldSpawnParticle} stay in core.
 */
@OnlyIn(Dist.CLIENT)
public class BeeParticleRender {
	// addBeeHiveFX and addEntitySnowFX moved verbatim, with the bare
	// shouldSpawnParticle calls qualified as ParticleRender.shouldSpawnParticle

	private BeeParticleRender() {}
}
```

`ApicultureParticles`, `BeeParticleData`, `BeeTargetParticleData` and `ParticleSnow` are all in
`forestry.apiculture.particles` - the new class's own package - so they need **no** imports. That is
the one edit the moved bodies need beyond qualifying `shouldSpawnParticle`.

- [ ] **Step 3: Delete both from core**

Remove `addBeeHiveFX` and `addEntitySnowFX` from `ParticleRender.java`, then delete every import they
alone needed. Verify:

```bash
grep -cE "^import forestry\.(apiculture|arboriculture|lepidopterology|farming|cultivation|mail)\." src/main/java/forestry/core/render/ParticleRender.java
```

Expected: `0`. The api imports (`IBeeHousing`, `IBeeSpecies`, `IHiveTile`, `IGenome`,
`BeeChromosomes`) also become unused and should go, but they were never leaks - api ships whole.

- [ ] **Step 4: Repoint the two callers**

In `BeeClientManager`, change `ParticleRender.addBeeHiveFX` to `BeeParticleRender.addBeeHiveFX` and fix
the import. In `SnowingBeeEffect`, change `ParticleRender.addEntitySnowFX` to
`BeeParticleRender.addEntitySnowFX` and fix the import.

`BeeClientManager` is `apiimpl/client/`, already in the baseline under bucket I - it does not become
clean here and must not be removed from the baseline.

- [ ] **Step 5: Compile, trim, verify**

```bash
./gradlew compileJava compileTestJava
```

Remove `core/render/ParticleRender.java` from the baseline, then:

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `43 known leaking file(s)`, no datagen diff, all 100 tests passed.

Particles are `@OnlyIn(Dist.CLIENT)` and **no GameTest runs on a client**, so the suite cannot see
this change at all. The compiler is the oracle: both methods moved verbatim with no call-site change
other than the receiver.

- [ ] **Step 6: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "apiculture: own the bee particle spawns

Two of ParticleRender's nine methods are bee-only and carried all six of its
leaks: addBeeHiveFX (one caller, BeeClientManager) and addEntitySnowFX (one
caller, SnowingBeeEffect). The seven generic spawns and shouldSpawnParticle
stay in core, where cultivation and factory still call them.

addEntityHoneyDustFX stays despite the name - it uses a core particle option
and only reads as apiculture.

checkBaseBoundary: 44 -> 43 files."
```

---

### Task 7: Let multiblock parts declare their own pattern id

`LevelStructureView.typeIdFor` is a hardcoded dispatch from block-entity class to pattern type id,
covering two machines:

```java
if (be instanceof IAlvearyComponent<?>) {
    return be instanceof TileAlvearyPlain ? AlvearyPattern.PLAIN : AlvearyPattern.PART;
}
if (be instanceof IFarmComponent<?>) {
    if (be instanceof TileFarmPlain) return FarmPattern.PLAIN;
    if (be instanceof TileFarmGearbox) return FarmPattern.GEARBOX;
    return FarmPattern.PART;
}
```

This is the phase's one genuine extension point, and the abstraction it needs already exists in the
same class hierarchy: `MultiblockTileEntityForestry` declares `public abstract MultiblockPattern
getPattern()`. A parallel `patternTypeId()` is the same shape.

There are exactly two direct subclasses - `TileAlveary` and `TileFarm` - so the whole hierarchy is
two roots and three overrides.

**Files:**
- Modify: `src/main/java/forestry/core/multiblock/MultiblockTileEntityForestry.java:66`
- Modify: `src/main/java/forestry/apiculture/multiblock/TileAlveary.java`, `TileAlvearyPlain.java`
- Modify: `src/main/java/forestry/farming/tiles/TileFarm.java`, `TileFarmPlain.java`, `TileFarmGearbox.java`
- Modify: `src/main/java/forestry/core/multiblock/LevelStructureView.java`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: `AlvearyPattern.PLAIN`/`.PART` and `FarmPattern.PLAIN`/`.GEARBOX`/`.PART`, which stay
  where they are.
- Produces: `MultiblockTileEntityForestry#patternTypeId()` returning `String`, non-null. Every part
  of a machine returns its role id.

- [ ] **Step 1: Confirm the hierarchy is closed**

```bash
grep -rln "extends MultiblockTileEntityForestry" src/main/java --include='*.java'
grep -n "class TileAlveary\b" src/main/java/forestry/apiculture/multiblock/TileAlveary.java
grep -n "class TileFarm\b" src/main/java/forestry/farming/tiles/TileFarm.java
```

Expected: exactly `TileAlveary.java` and `TileFarm.java`. `TileAlveary` must implement
`IAlvearyComponent` and `TileFarm` must implement `IFarmComponent` - if either does not, today's
`instanceof` guard is doing work an override would not reproduce, and the plan needs revisiting.

- [ ] **Step 2: Declare the method on the base class**

In `MultiblockTileEntityForestry.java`, next to `getPattern()`:

```java
	/**
	 * @return The pattern type id for this part's role in its machine
	 */
	public abstract String patternTypeId();
```

Making it abstract rather than defaulting to null is deliberate: the compiler then forces every new
multiblock part to state its role, which is exactly the drift `typeIdFor` was accumulating.

- [ ] **Step 3: Implement it, five times**

`TileAlveary`:

```java
	@Override
	public String patternTypeId() {
		return AlvearyPattern.PART;
	}
```

`TileAlvearyPlain` overrides with `AlvearyPattern.PLAIN`. `TileFarm` returns `FarmPattern.PART`;
`TileFarmPlain` returns `FarmPattern.PLAIN`; `TileFarmGearbox` returns `FarmPattern.GEARBOX`.

`TileAlveary` and `AlvearyPattern` are the same package, as are `TileFarm`/`TileFarmPlain`/
`TileFarmGearbox` and... check: `FarmPattern` is in `forestry.farming.multiblock` and the tiles are in
`forestry.farming.tiles`, so those three need `import forestry.farming.multiblock.FarmPattern;`.

An addon subclassing `TileAlvearyPlain` inherits `PLAIN`, matching the behavior the current javadoc
promises ("An addon that subclasses a role part inherits that role").

- [ ] **Step 4: Collapse typeIdFor**

Replace the body with:

```java
	@Nullable
	static String typeIdFor(BlockEntity be) {
		return be instanceof MultiblockTileEntityForestry<?> component ? component.patternTypeId() : null;
	}
```

Delete the five leaking imports and the two api component imports if they are now unused. Rewrite the
method's javadoc: the paragraphs explaining the `instanceof` chain no longer describe the code. Keep
the paragraph about `MultiblockTileEntityForestry` membership - it is still exactly the rule - and
replace the machine/role paragraphs with one sentence saying the part declares its own id.

- [ ] **Step 5: Compile, trim, verify**

```bash
./gradlew compileJava compileTestJava
```

Remove `core/multiblock/LevelStructureView.java` from the baseline, then:

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: `42 known leaking file(s)`, no datagen diff, all 100 tests passed.

**This is the best-covered task in the phase.** The multiblock conservation GameTests exercise alveary
and farm assembly, and pattern validation is exactly what `typeIdFor` feeds. A wrong id makes a
structure fail to form, which those tests catch directly. If any multiblock test fails here, the cause
is almost certainly a missing override, not the collapse.

- [ ] **Step 6: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "multiblock: let parts declare their own pattern type id

LevelStructureView.typeIdFor was a hardcoded block-entity-class to pattern-id
dispatch for two machines. MultiblockTileEntityForestry already declares an
abstract getPattern(); patternTypeId() is the same shape, and the hierarchy is
only two roots and three overrides.

Abstract rather than defaulted, so a new part cannot silently inherit the
wrong role - which is the drift typeIdFor was accumulating.

checkBaseBoundary: 43 -> 42 files."
```

---

### Task 8: Give modules an ordered reload and sync hook

This is bucket H's real work and the phase's only design decision. Read the whole task before
starting any of it.

`ModuleCore` holds two blocks that register five modules' data listeners:

- `registerReloadListeners` (`ModuleCore:183-231`) adds six reload listeners **in a documented order**:
  flower types -> bee effects -> taxa -> bee species -> tree species -> butterfly species -> mutations.
- `onDatapackSync` (`ModuleCore:243-259`) sends six packets **in a documented order**, flower
  types/effects/taxa before species, mutations rebuilt client-side by the species packets' handlers.

The order is not incidental. Species projection resolves each genome's `bee_effect` and each species'
genus against already-loaded data, and `rebuildMutations` indexes by species **object identity**, so
it must run after every species rebuild. Moving each block into its own module without preserving
order produces silently empty or mis-indexed genetics - a failure mode with no exception.

**The ordering falls out of the module dependency graph, but only after one fix.**
`ForestryModuleManager:82-97` already topologically sorts `loadedModules` (a `LinkedHashMap`) by
`getModuleDependencies()`. Lepidopterology declares `CORE, ARBORICULTURE`, cultivation declares
`CORE, FARMING`. But **apiculture and arboriculture declare no dependencies at all** - not even core -
so their position relative to core is discovery order, not declared order. Declaring `CORE` on both
makes the existing sort produce exactly the order the data needs, and costs two lines.

Core stays the sequencer. It owns the event listener, registers its own taxa first, iterates the
modules in load order, and adds the mutation rebuild strictly last.

**Files:**
- Modify: `src/main/java/forestry/api/modules/IForestryModule.java`
- Modify: `src/main/java/forestry/apiculture/ModuleApiculture.java`, `arboriculture/ModuleArboriculture.java`
- Modify: `src/main/java/forestry/lepidopterology/ModuleLepidopterology.java`
- Modify: `src/main/java/forestry/core/ModuleCore.java:183-259`

**Interfaces:**
- Consumes: `IForestryApi.INSTANCE.getModuleManager().getLoadedModules()`, which already exists and
  already returns load order.
- Produces: two `IForestryModule` default methods, both no-ops by default:
  - `default void registerReloadListeners(AddReloadListenerEvent event) {}`
  - `default void syncDatapack(OnDatapackSyncEvent event) {}`

- [ ] **Step 1: Read the two blocks and the sort**

```bash
sed -n '183,259p' src/main/java/forestry/core/ModuleCore.java
sed -n '75,100p' src/main/java/forestry/modules/ForestryModuleManager.java
```

**No new accessor is needed.** `IModuleManager.getLoadedModules()` is already api, already implemented
by `ForestryModuleManager:24-27` as `Collections.unmodifiableCollection(this.loadedModules.values())`,
and `loadedModules` is the `LinkedHashMap` the sort at `:82-97` fills in load order. `ModuleCore:268`
already reaches the manager the same way this task needs to:

```java
	for (IForestryModule module : IForestryApi.INSTANCE.getModuleManager().getLoadedModules()) {
```

Use `getLoadedModules()` rather than the `getModulesForMod(MOD_ID)` that line 268 happens to use:
reload listeners should include an addon's modules too.

- [ ] **Step 2: Declare the two hooks**

In `src/main/java/forestry/api/modules/IForestryModule.java`, alongside `registerPackets`:

```java
	/**
	 * Called when the server datapack reload listeners are gathered. Modules add their own data
	 * loaders here. Called in module load order, so a module's data may depend on any module it
	 * declares in {@link #getModuleDependencies}.
	 *
	 * @param event The reload listener registration event
	 */
	default void registerReloadListeners(AddReloadListenerEvent event) {
	}

	/**
	 * Called when datapack contents are synced to a player on login or reload. Modules send their own
	 * definitions here. Called in module load order, matching the order the reload listeners ran in.
	 *
	 * @param event The datapack sync event
	 */
	default void syncDatapack(OnDatapackSyncEvent event) {
	}
```

Both event types are NeoForge, not Forestry. `checkApiBoundary` fails only on
`^import (static )?forestry\.(?!api\.)`, so a `net.neoforged` import cannot trip it. Run the gate
anyway, but it is not at risk here.

- [ ] **Step 3: Make the load order explicit**

In `ModuleApiculture` and `ModuleArboriculture`, add:

```java
	@Override
	public List<ResourceLocation> getModuleDependencies() {
		return List.of(ForestryModuleIds.CORE);
	}
```

This is true (D1: everything requires core) and turns an accidental ordering into a declared one.

Print the resulting order before and after to prove it did not change what actually loads:

```bash
grep -rn "Loaded module\|loadedModules" src/main/java/forestry/modules/ForestryModuleManager.java
```

If the manager does not already log the order, add a temporary `Forestry.LOGGER.info` over
`getLoadedModules()`, run `./gradlew runGameTestServer`, capture the order, and **remove the logging
before committing**. The order must read: core, ..., apiculture, arboriculture, ..., lepidopterology,
with apiculture before lepidopterology and arboriculture before lepidopterology.

- [ ] **Step 4: Move the listeners into their modules**

`ModuleApiculture.registerReloadListeners` takes, in this order:

```java
		event.addListener(FlowerTypeManager.INSTANCE);
		event.addListener(BeeEffectManager.INSTANCE);
		event.addListener(BeeSpeciesManager.INSTANCE);
```

`ModuleArboriculture` takes `TreeSpeciesManager.INSTANCE`. `ModuleLepidopterology` takes
`ButterflySpeciesManager.INSTANCE`.

**Carry the explanatory comments with the code.** Each `event.addListener` line in `ModuleCore:191-223`
has a comment explaining why it sits where it does; those comments are the only record of the
invariant. Rewrite the ones that referred to cross-module order ("Registered before BeeSpeciesManager")
to name the guarantee they now rest on - module load order - rather than deleting them.

- [ ] **Step 5: Rewrite core's driver**

`ModuleCore.registerReloadListeners` keeps the recipe-cache listener it already has, then:

```java
		// Taxa are engine-level and every species type resolves its genus against them, so core
		// registers them before any module's species loader.
		event.addListener(TaxonManager.INSTANCE);

		// Modules load in dependency order (see ForestryModuleManager), which is also the order their
		// data depends on: core's taxa, then apiculture's flower types/effects/species, then
		// arboriculture's trees, then lepidopterology's butterflies.
		for (IForestryModule module : IForestryApi.INSTANCE.getModuleManager().getLoadedModules()) {
			module.registerReloadListeners(event);
		}

		// Strictly last: mutation recipes resolve species by id against the live maps and index the
		// results by object identity, so every species rebuild must already have run.
		RecipeManager recipeManager = event.getServerResources().getRecipeManager();
		event.addListener((prepBarrier, resourceManager, prepProfiler, reloadProfiler, backgroundExecutor, gameExecutor) -> {
			return prepBarrier.wait(Unit.INSTANCE).thenRunAsync(() -> GeneticsReloadHandler.rebuildMutations(recipeManager), gameExecutor);
		});
```

`ModuleCore` must **not** implement the new hook - it registers taxa in the driver directly and is
itself in `getLoadedModules()`, so implementing the hook as well would register them twice.

- [ ] **Step 6: Do the same for the sync**

`ModuleApiculture.syncDatapack` sends the flower type, bee effect and bee species packets in that
order; `ModuleArboriculture` sends the tree packet; `ModuleLepidopterology` the butterfly packet.
`ModuleCore.onDatapackSync` sends the taxon packet first, then iterates.

The per-player loop currently wraps all six sends. Preserve that shape: each module builds its packets
once outside the loop and sends inside `event.getRelevantPlayers().forEach(...)`, exactly as core does
today. Six separate `forEach` passes over the same player list is a behavior-preserving change -
`OnDatapackSyncEvent`'s contract is about ordering relative to tag and recipe sync, and all six sends
still happen inside the one event.

Carry `ModuleCore`'s method javadoc (`:236-242`) apart, distributing the sentences that describe each
packet to the module that now sends it, and keeping the ordering rationale in core's driver.

- [ ] **Step 7: Compile and verify**

```bash
./gradlew compileJava compileTestJava
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

`ModuleCore` will still leak - `registerPackets` and `GrafterLootModifier` are Task 9 - so do **not**
remove it from the baseline yet. Expected: still `42 known leaking file(s)`, no datagen diff, all 100
tests passed.

The reload tests are the oracle and they are good ones: `SpeciesReloadTest`, `TreeSpeciesReloadTest`,
`ButterflySpeciesReloadTest`, `ButterflySpeciesFallbackTest`, `ButterflyEntityReloadTest`,
`TreeReloadSideEffectsTest`, `MutationRecipeTest` and `BeeEffectSystemTest` all drive the reload path
this task rewires, and `GameTestServer` runs a real cold server start - the very path
`BeeSpeciesManager`'s class comment warns silently no-ops if listener registration is wrong.

Additionally confirm the live counts, since an empty map passes most assertions:

```bash
./gradlew runGameTestServer 2>&1 | grep -E "Loaded [0-9]+ (bee|tree|butterfly) species"
```

Expected: non-zero counts for all three, matching what the same grep produces on `HEAD~1`. **Capture
that baseline before starting this task**, because "0 bee species" is exactly the failure this
rewiring risks and it does not throw.

- [ ] **Step 8: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "modules: own their datapack reload listeners and sync packets

ModuleCore registered six reload listeners and sent six sync packets for five
modules, in an order the data genuinely requires: effects and taxa before
species projection, and mutations strictly last because MutationManager keys
by species object identity.

Modules now contribute through two IForestryModule hooks and core stays the
sequencer, iterating them in load order. That order is only correct because
ForestryModuleManager topologically sorts by getModuleDependencies - which
apiculture and arboriculture did not declare at all. Both now declare CORE,
turning an accidental order into a stated one.

checkBaseBoundary: unchanged at 42; ModuleCore clears in the next commit."
```

---

### Task 9: Move the remaining per-module wiring out of core

What is left in bucket H is mechanical: every module already overrides the SPI hooks these
registrations belong in.

| Leak | Where it is | Where it goes | Hook already overridden by that module? |
| --- | --- | --- | --- |
| 5 clientbound species packets | `ModuleCore.registerPackets:288` | each module's `registerPackets` | yes - apiculture, arboriculture all override it |
| `GrafterLootModifier` | `ModuleCore:156` | `ModuleArboriculture` | n/a, needs a `RegisterEvent` listener |
| 4 modules' block/item colours | `CoreClientHandler:240-310` | each module's client handler | yes - all four override `registerClientHandler` |
| `ApiaristAI`, `ApicultureVillagers` | `EventHandlerCore:53-54` | apiculture's event handler | n/a, needs a game-bus listener |
| `ApicultureEffects` | `EventHandlerCore:61-65` | apiculture's event handler | same |
| `VillagerJigsaw.init` | `EventHandlerCore:84` | apiculture's event handler | same |

`ModuleLepidopterology` does not currently override `registerPackets`; it will need to, following
`ModuleApiculture`'s existing override as the template.

**Files:**
- Modify: `src/main/java/forestry/core/ModuleCore.java`, `core/EventHandlerCore.java`, `core/client/CoreClientHandler.java`
- Modify: the five module classes and their client handlers
- Create: `src/main/java/forestry/apiculture/EventHandlerApiculture.java` - apiculture has no event
  handler class today, so one is needed; mirror `core/EventHandlerCore.java`'s shape
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: `registerPackets(IPacketRegistry)` and `registerClientHandler(Consumer<IClientModuleHandler>)`,
  both already on `IForestryModule`.
- Produces: nothing new.

- [ ] **Step 1: Find each module's existing hooks**

```bash
grep -rln "public void registerPackets" src/main/java --include='*.java'
grep -rln "public void registerClientHandler" src/main/java --include='*.java'
grep -rn "RegisterColorHandlersEvent" src/main/java --include='*.java'
```

Known from the survey that produced this plan: apiculture, arboriculture, core, factory, mail, sorting
and worktable override `registerPackets` - **lepidopterology does not**, and needs a new override.
All four colour-handler targets (apiculture, arboriculture, lepidopterology, mail) already override
`registerClientHandler`. `RegisterColorHandlersEvent` is listened to in exactly one place today,
`CoreClientHandler:240,250`.

- [ ] **Step 2: Move the packet registrations**

Move `ModuleCore:306-311` - the five `registry.clientbound(...)` lines for `FLOWER_TYPE_SYNC`,
`BEE_EFFECT_SYNC`, `BEE_SPECIES_SYNC`, `TREE_SPECIES_SYNC`, `BUTTERFLY_SPECIES_SYNC` - into the
`registerPackets` override of apiculture (three), arboriculture (one) and lepidopterology (one, new
override). `TAXON_SYNC` stays in core.

Registration order across modules does not matter here: `IPacketRegistry` keys by the payload type's
`ResourceLocation`, not by call order. That is unlike Task 8, where order was the whole problem.

- [ ] **Step 3: Move the loot modifier**

`ModuleCore.registerGlobalLootModifiers:153-157` registers `grafter_modifier` with
`GrafterLootModifier.CODEC`. Move that registration to `ModuleArboriculture`, adding a
`RegisterEvent` listener in its `registerEvents` if it has none. Keep the id `grafter_modifier`
exactly - it appears in generated loot modifier JSON and `runData` will catch a change.

- [ ] **Step 4: Move the colour handlers**

`CoreClientHandler.registerBlockColors`/`registerItemColors` register for four modules. Move each
module's lines into that module's client handler:

- apiculture: `ApicultureBlocks.BEE_COMB` (block and item), the seven `ApicultureItems` entries
- arboriculture: eight `ArboricultureBlocks` entries, two `ArboricultureItems` entries
- lepidopterology: `LepidopterologyItems.CATERPILLAR_GE`, `.SERUM_GE`
- mail: `MailItems.STAMPS`

`ClientManager.FORESTRY_BLOCK_COLOR`/`FORESTRY_ITEM_COLOR` are core and stay - modules import them.

Colour handler registration order is irrelevant; each call is keyed by block or item.

- [ ] **Step 5: Move the apiculture event wiring**

`EventHandlerCore` keeps only its non-apiculture listeners. The villager AI hookup (`:53-54`), the
Hakuna Matata effect handling (`:61-65`) and `VillagerJigsaw.init` (`:84`) move to apiculture. Register
them on `NeoForge.EVENT_BUS` from `ModuleApiculture.registerEvents`, matching how `ModuleCore` does it
at `:97-102`.

Check what event each block listens to before moving it - the method signatures in `EventHandlerCore`
name them.

- [ ] **Step 6: Compile, trim, verify**

```bash
./gradlew compileJava compileTestJava
```

Remove all three from the baseline:

```
core/client/CoreClientHandler.java
core/EventHandlerCore.java
core/ModuleCore.java
```

```bash
./gradlew checkBaseBoundary checkApiBoundary
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
./gradlew runGameTestServer 2>&1 | grep -E "Loaded [0-9]+ (bee|tree|butterfly) species"
```

Expected: `39 known leaking file(s) remaining`, no datagen diff, all 100 tests passed, species counts
unchanged from the Task 8 baseline.

Two blind spots to state rather than paper over. **Colour handlers are client-only** and no GameTest
runs a client, so a dropped `event.register` line is invisible to the suite - re-read the moved lines
against the originals and count them: 20 registrations in, 20 out. **`ApiaristAI` and the villager
profession hookup** are likewise uncovered; the only check is that the listener is registered on the
same bus for the same event.

- [ ] **Step 7: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "modules: own their packets, colours and event wiring

The rest of bucket H, and mechanical: every module already overrode the SPI
hooks these registrations belonged in. Five clientbound species packets, the
grafter loot modifier, twenty block and item colour registrations across four
modules, and apiculture's villager AI, effects and jigsaw wiring.

Unlike the reload listeners, none of this is order-sensitive: packets key by
payload id and colour handlers key by block or item.

checkBaseBoundary: 42 -> 39 files."
```

---

### Task 10: Record phase 4 completion

**Files:**
- Modify: `docs/superpowers/specs/2026-07-30-feature-package-reorg-design.md`

- [ ] **Step 1: Confirm from a clean build**

```bash
./gradlew clean build
```

Expected: `BUILD SUCCESSFUL`, `checkApiBoundary: forestry.api is clean`,
`checkBaseBoundary: 39 known leaking file(s) remaining`.

- [ ] **Step 2: Update the spec**

Mark phase 4 `DONE` in the sequencing block and add a paragraph covering: the dead-import sweep coming
back empty (unlike phase 3), the stale `IBeeEffect`/`ParticleRender` claim, the
`ApiaristPoolElement`/`VillagerJigsaw` coupling that crossed two buckets, and the
`getModuleDependencies` gap that Task 8 had to close before module load order could carry the reload
ordering invariant.

- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/specs/2026-07-30-feature-package-reorg-design.md
git commit -m "docs: record phase 4 completion, base boundary at 39 of 68"
```

---

## Notes for phase 5

- Phase 5 is bucket G, the seven `plugin/` files, and phase 5a is bucket I, the twelve `apiimpl/`
  files. Together they are 19 of the 39 remaining.
- Bucket I is the one place where "check whether it is really a misfiled type" will **not** apply.
  `PluginManager` and the eight registration builders implement api/plugin interfaces that D3 ships in
  base while constructing content classes. That is a genuine extension point on the most central class
  in the design, and Task 8 of this phase is a rehearsal for it: same shape, ordered per-module
  contribution behind an SPI core already owns.
- The other 20 are bucket A, the datagen providers, led by `ForestryRecipeProvider` at 26 imports.
  **They dissolve in phase 8** when datagen splits into per-jar source sets, not by being fixed in
  place. Expect the count to sit at 20 from the end of phase 5a until phase 8. That is the plan
  working, not a stall.
- Phase 6's gate - "the base artifact references no split-jar types" - becomes checkable the moment
  bucket A clears, and `checkBaseBoundary` reaching an empty baseline is exactly that gate.
