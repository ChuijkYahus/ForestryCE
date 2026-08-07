# Phase 2: turn core's central indexes into extension points

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Clear bucket B - the seven base-artifact files that index every content module - and put a
ratcheting gate around the whole base artifact so no new leak can be added.

**Architecture:** Phase 1's gate proved `forestry.api` clean. This phase widens it to the rest of
the base artifact (`core`, `apiimpl`, `plugin`, `modules`, `compat`), which currently has 68 leaking
files. Because that cannot go green until phase 6, the gate carries a checked-in baseline: it fails
on any leak not in the baseline, and equally on a baseline entry that no longer leaks. That makes it
a ratchet rather than a wall. Then the seven bucket-B files are fixed, four of them by relocating a
single misfiled type.

**Tech Stack:** Java 21, NeoForge 21.1.230, Minecraft 1.21.1, ModDevGradle 2.0.x, Gradle Groovy DSL.
GameTests only, no JUnit.

## Global Constraints

- Comment and Javadoc style is binding; see `CLAUDE.md`. ASCII only. `@return` and `@param` are
  noun-phrase fragments with no terminal period. Lowercase `todo`.
- Every task ends with `./gradlew runData` producing no diff in `src/generated/resources`, and
  `./gradlew runGameTestServer` reporting all tests passed.
- `checkApiBoundary` must stay green throughout. It is wired into `check` and phase 1 left it clean.
- All source files are LF. `.gitattributes` enforces it as of 2026-07-31. Do not use `$`-anchored
  `sed` patterns on the assumption a file is CRLF; that bug cost real time in phase 1b.
- Types moved into `core.platform.*` are not yet in api and remain unpublished. Only `forestry/api/**`
  ships in `apiJar`.

## Starting state

Base artifact, measured 2026-07-31 after phase 1:

| package | leaking files | leaking imports |
| --- | --- | --- |
| `core` | 49 | 214 |
| `apiimpl` | 12 | 22 |
| `plugin` | 7 | 42 |
| `modules`, `compat`, `Forestry.java` | 0 | 0 |
| **total** | **68** | **278** |

Bucket B, this phase's target:

| File | Leaks | Nature |
| --- | --- | --- |
| `core/tab/ForestryCreativeTabs.java` | 19 | five per-module tabs plus 7 content entries in the base tab |
| `core/network/PacketIdClient.java` | 8 | packet id constants for other modules |
| `core/network/PacketIdServer.java` | 3 | same |
| `core/features/CoreItems.java` | 2 | registers two item classes that live in apiculture |
| `core/features/CoreBlocks.java` | 1 | `NaturalistChestBlockType` |
| `core/features/CoreTiles.java` | 1 | `NaturalistChestBlockType` |
| `core/features/CoreDataComponents.java` | 1 | a data component over `forestry.mail.Letter` |

## File Structure

| Action | File | Responsibility |
| --- | --- | --- |
| Modify | `build.gradle` | add `checkBaseBoundary` and wire into `check` |
| Create | `gradle/base-boundary-baseline.txt` | the ratchet's checked-in baseline |
| Move | `forestry/apiculture/blocks/NaturalistChestBlockType.java` -> `forestry/core/blocks/NaturalistChestBlockType.java` | naturalist chest variants, a base concept |
| Move | `forestry/apiculture/items/ItemBeesWax.java` -> `forestry/core/items/ItemBeesWax.java` | already registered by `CoreItems` |
| Move | `forestry/apiculture/items/ItemRefractoryWax.java` -> `forestry/core/items/ItemRefractoryWax.java` | same |
| Move | `LETTER_DATA` out of `CoreDataComponents` into a new `forestry/mail/features/MailDataComponents.java` | mail's own component registry |
| Create | `forestry/apiculture/network/ApiculturePacketIds.java` and siblings | per-module packet id holders |
| Create | `forestry/apiculture/tab/ApicultureCreativeTab.java` and siblings | per-module tab definitions |
| Modify | `forestry/core/tab/ForestryCreativeTabs.java` | keeps only FORESTRY and STORAGE |

---

### Task 1: A ratcheting boundary gate for the base artifact

`checkApiBoundary` covers `forestry/api` only. The base artifact is bigger, and the phase-6 gate is
stated in the spec as "the base artifact references no split-jar types". This task builds the
instrument that measures it.

It cannot fail the build outright - 68 files leak today and will until phase 6. Instead it compares
against a checked-in baseline and fails in two directions: a leak not in the baseline is a new
regression, and a baseline entry that no longer leaks is a stale baseline. The second half is what
forces the count down instead of letting it sit.

**Files:**
- Modify: `build.gradle` (after the `checkApiBoundary` block)
- Create: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: nothing.
- Produces: Gradle task `checkBaseBoundary`, wired as a dependency of `check`. Later tasks run
  `./gradlew checkBaseBoundary` and trim the baseline.

- [ ] **Step 1: Write the gate**

Add to `build.gradle` immediately after the `tasks.named('check') { dependsOn checkApiBoundary }`
block:

```groovy
// Phase 2 of the feature reorg: the base artifact (everything that ships in the forestry jar and
// is not a content module) must stop referencing the five split modules. 68 files still do, so
// this gate ratchets against a checked-in baseline rather than failing outright: a leak that is
// not in the baseline is a new regression, and a baseline entry that no longer leaks is a stale
// baseline that must be trimmed. Both directions fail, which is what drives the count down.
var checkBaseBoundary = tasks.register('checkBaseBoundary') {
	group = 'verification'
	description = 'Fails if a base-artifact package gains a new reference to a split content module'

	var javaDir = project.file('src/main/java/forestry')
	var baselineFile = project.file('gradle/base-boundary-baseline.txt')
	var basePackages = ['core', 'apiimpl', 'plugin', 'modules', 'compat']
	var splitModules = ['apiculture', 'arboriculture', 'lepidopterology', 'farming', 'cultivation', 'mail']

	inputs.dir(javaDir)
	inputs.file(baselineFile)
	outputs.upToDateWhen { false }

	doLast {
		var pattern = ~/^import (static )?forestry\.(${splitModules.join('|')})\..*/
		var actual = new TreeSet<String>()

		basePackages.each { pkg ->
			var dir = new File(javaDir, pkg)
			if (!dir.exists()) {
				return
			}
			dir.eachFileRecurse(groovy.io.FileType.FILES) { file ->
				if (!file.name.endsWith('.java')) {
					return
				}
				if (file.readLines('UTF-8').any { it ==~ pattern }) {
					actual.add(javaDir.toPath().relativize(file.toPath()).toString())
				}
			}
		}

		var baseline = new TreeSet<String>(
				baselineFile.exists()
						? baselineFile.readLines('UTF-8').findAll { !it.trim().isEmpty() && !it.startsWith('#') }
						: [])

		var added = new TreeSet<String>(actual); added.removeAll(baseline)
		var fixed = new TreeSet<String>(baseline); fixed.removeAll(actual)

		if (!added.isEmpty()) {
			throw new GradleException(
					"New base-artifact reference(s) to a split content module:\n  "
					+ added.join('\n  ')
					+ "\nFix them, or if intentional add them to gradle/base-boundary-baseline.txt.")
		}
		if (!fixed.isEmpty()) {
			throw new GradleException(
					"gradle/base-boundary-baseline.txt is stale. These no longer leak and must be removed from it:\n  "
					+ fixed.join('\n  '))
		}

		logger.lifecycle("checkBaseBoundary: ${actual.size()} known leaking file(s) remaining")
	}
}

tasks.named('check') {
	dependsOn checkBaseBoundary
}
```

- [ ] **Step 2: Generate the baseline**

```bash
cd src/main/java/forestry && \
grep -rlE '^import (static )?forestry\.(apiculture|arboriculture|lepidopterology|farming|cultivation|mail)\.' \
  core apiimpl plugin modules compat 2>/dev/null | sort > /tmp/baseline.txt && \
cd - && mkdir -p gradle && \
{ echo "# Base-artifact files that still reference a split content module."; \
  echo "# Generated 2026-07-31 at the start of phase 2. Shrinks each phase; never grows."; \
  cat /tmp/baseline.txt; } > gradle/base-boundary-baseline.txt
wc -l gradle/base-boundary-baseline.txt
```

Expected: 70 lines - 68 file paths plus the 2 comment lines.

- [ ] **Step 3: Run the gate green**

```bash
./gradlew checkBaseBoundary
```

Expected: `BUILD SUCCESSFUL` and `checkBaseBoundary: 68 known leaking file(s) remaining`.

- [ ] **Step 4: Verify it catches a new leak**

Do not skip this. A gate that has never failed is not known to work.

```bash
printf '\nimport forestry.mail.Letter;\n' >> src/main/java/forestry/core/utils/StringUtil.java
./gradlew checkBaseBoundary
```

Expected: `BUILD FAILED` naming `core/utils/StringUtil.java` under "New base-artifact reference(s)".

- [ ] **Step 5: Verify it catches a stale baseline**

```bash
git checkout src/main/java/forestry/core/utils/StringUtil.java
sed -i '$ a core/utils/StringUtil.java' gradle/base-boundary-baseline.txt
./gradlew checkBaseBoundary
```

Expected: `BUILD FAILED` naming `core/utils/StringUtil.java` under "stale".

```bash
sed -i '/^core\/utils\/StringUtil.java$/d' gradle/base-boundary-baseline.txt
./gradlew checkBaseBoundary
```

Expected: `BUILD SUCCESSFUL`, back to 68.

- [ ] **Step 6: Commit**

```bash
git add build.gradle gradle/base-boundary-baseline.txt
git commit -m "build: add a ratcheting checkBaseBoundary gate

Widens phase 1's boundary check from forestry.api to the whole base
artifact. 68 files still leak and will until phase 6, so this ratchets
against a checked-in baseline: a leak not in the baseline fails as a
regression, and a baseline entry that no longer leaks fails as stale.
The second direction is what drives the count down.

Both failure modes verified by injection."
```

---

### Task 2: Relocate NaturalistChestBlockType

The enum of naturalist chest variants (apiarist, arborist, lepidopterist) sits in
`forestry.apiculture.blocks`, but it is a base concept: `CoreBlocks` and `CoreTiles` register the
chests from it, `ForestryBewlr` renders them, `ForestryCreativeTabs` lists them and
`ForestryRecipeProvider` generates their recipes. One arboriculture file uses it too, which is the
`arboriculture -> apiculture` edge recorded in the spec's Graph decisions.

It is the sole leak in three of those files, so this one move clears them outright.

**Files:**
- Move: `src/main/java/forestry/apiculture/blocks/NaturalistChestBlockType.java` -> `src/main/java/forestry/core/blocks/NaturalistChestBlockType.java`
- Modify: every referencing file
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: `checkBaseBoundary` from Task 1.
- Produces: `forestry.core.blocks.NaturalistChestBlockType`, unchanged in shape.

- [ ] **Step 1: Confirm it references nothing apiculture-specific**

```bash
grep -n "^import" src/main/java/forestry/apiculture/blocks/NaturalistChestBlockType.java
```

Expected: no `forestry.apiculture.*` imports. If any appear, stop and report - the enum is not the
base concept this task assumes, and its destination needs rethinking.

- [ ] **Step 2: Move it**

```bash
git mv src/main/java/forestry/apiculture/blocks/NaturalistChestBlockType.java \
       src/main/java/forestry/core/blocks/NaturalistChestBlockType.java
sed -i '1s|^package forestry\.apiculture\.blocks;|package forestry.core.blocks;|' \
       src/main/java/forestry/core/blocks/NaturalistChestBlockType.java
```

- [ ] **Step 3: Rewrite every import**

```bash
grep -rl "import forestry.apiculture.blocks.NaturalistChestBlockType;" src/main/java src/test/java \
  | xargs -r sed -i 's|import forestry\.apiculture\.blocks\.NaturalistChestBlockType;|import forestry.core.blocks.NaturalistChestBlockType;|'
```

- [ ] **Step 4: Add imports where it was same-package, and remove where it now is**

`forestry.apiculture.blocks` classes used it without an import and now need one; anything in
`forestry.core.blocks` needs its new import deleted.

```bash
grep -rlw NaturalistChestBlockType src/main/java/forestry/apiculture/blocks/ 2>/dev/null
grep -rlw NaturalistChestBlockType src/main/java/forestry/core/blocks/ 2>/dev/null
```

For each file in the first list, add `import forestry.core.blocks.NaturalistChestBlockType;`. For
each in the second other than the enum itself, delete that import. This exact class of mistake cost
a debugging cycle in phase 1b - three files used the moved type without an import.

- [ ] **Step 5: Compile**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Trim the baseline**

Three files no longer leak. Remove exactly these lines from `gradle/base-boundary-baseline.txt`:

```
core/features/CoreBlocks.java
core/features/CoreTiles.java
core/render/ForestryBewlr.java
```

- [ ] **Step 7: Run both gates**

```bash
./gradlew checkBaseBoundary checkApiBoundary
```

Expected: `BUILD SUCCESSFUL` and `checkBaseBoundary: 65 known leaking file(s) remaining`.

- [ ] **Step 8: Verify datagen and tests**

```bash
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: no diff, all tests passed. `ForestryRecipeProvider` generates the chest recipes, so a
mistake here shows as a datagen diff.

- [ ] **Step 9: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "core: relocate NaturalistChestBlockType out of apiculture

The enum of naturalist chest variants is a base concept - CoreBlocks and
CoreTiles register from it, ForestryBewlr renders them, ForestryCreativeTabs
lists them, ForestryRecipeProvider generates their recipes - but it lived in
forestry.apiculture.blocks. That is also the arboriculture -> apiculture edge
the spec records.

Sole leak in three of those files, so this clears them outright.

checkBaseBoundary: 68 -> 65 files."
```

---

### Task 3: Move the wax items to core

`CoreItems` registers `BEESWAX` and `REFRACTORY_WAX` but their item classes live in
`forestry.apiculture.items`. The registration is in base, so under D2 the classes belong in base too.
Both classes reference only core and api types, so this is a relocation and not a redesign.

**Files:**
- Move: `src/main/java/forestry/apiculture/items/ItemBeesWax.java` -> `src/main/java/forestry/core/items/ItemBeesWax.java`
- Move: `src/main/java/forestry/apiculture/items/ItemRefractoryWax.java` -> `src/main/java/forestry/core/items/ItemRefractoryWax.java`
- Modify: `src/main/java/forestry/core/features/CoreItems.java`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: `checkBaseBoundary` from Task 1.
- Produces: `forestry.core.items.ItemBeesWax` and `forestry.core.items.ItemRefractoryWax`.

- [ ] **Step 1: Confirm neither class depends on apiculture**

```bash
grep -n "^import forestry" src/main/java/forestry/apiculture/items/ItemBeesWax.java \
                          src/main/java/forestry/apiculture/items/ItemRefractoryWax.java
```

Expected: only `forestry.api.*` and `forestry.core.*` imports. `ItemRefractoryWax` uses
`forestry.core.network.packets.PacketRefractoryWax`, `forestry.core.items.ItemForestry`,
`forestry.core.utils.NetworkUtil` and `forestry.api.IForestryApi`. If an apiculture import appears,
stop and report.

- [ ] **Step 2: Move both**

```bash
git mv src/main/java/forestry/apiculture/items/ItemBeesWax.java src/main/java/forestry/core/items/ItemBeesWax.java
git mv src/main/java/forestry/apiculture/items/ItemRefractoryWax.java src/main/java/forestry/core/items/ItemRefractoryWax.java
sed -i '1s|^package forestry\.apiculture\.items;|package forestry.core.items;|' \
  src/main/java/forestry/core/items/ItemBeesWax.java src/main/java/forestry/core/items/ItemRefractoryWax.java
```

- [ ] **Step 3: Fix imports**

```bash
grep -rl "import forestry.apiculture.items.ItemBeesWax;\|import forestry.apiculture.items.ItemRefractoryWax;" src/main/java src/test/java \
  | xargs -r sed -i -e 's|import forestry\.apiculture\.items\.ItemBeesWax;|import forestry.core.items.ItemBeesWax;|' \
                    -e 's|import forestry\.apiculture\.items\.ItemRefractoryWax;|import forestry.core.items.ItemRefractoryWax;|'
```

`ItemRefractoryWax` is now in `forestry.core.items` alongside `ItemForestry`, so delete its
`import forestry.core.items.ItemForestry;` line. Check for other newly-same-package imports the same
way.

- [ ] **Step 4: Compile and check the gates**

```bash
./gradlew compileJava
./gradlew checkBaseBoundary checkApiBoundary
```

Expected: `BUILD SUCCESSFUL`, then a stale-baseline failure naming `core/features/CoreItems.java`.
That failure is correct - it is the ratchet telling you to trim.

- [ ] **Step 5: Trim the baseline**

Remove this line from `gradle/base-boundary-baseline.txt`:

```
core/features/CoreItems.java
```

Re-run `./gradlew checkBaseBoundary`. Expected: `checkBaseBoundary: 64 known leaking file(s) remaining`.

- [ ] **Step 6: Verify datagen and tests**

```bash
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: no diff, all tests passed. The items keep their registry names, so datagen must not move.

- [ ] **Step 7: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "core: move the wax item classes alongside their registration

CoreItems registers BEESWAX and REFRACTORY_WAX but both classes lived in
forestry.apiculture.items. Neither references apiculture - they use
ItemForestry, PacketRefractoryWax, NetworkUtil and IForestryApi - so the
classes move to where they are registered.

checkBaseBoundary: 65 -> 64 files."
```

---

### Task 4: Give mail its own data components

`CoreDataComponents` registers `LETTER_DATA`, a `DataComponentType<Letter>` over
`forestry.mail.Letter`. Mail owns the type, so mail should own the component.

**Files:**
- Create: `src/main/java/forestry/mail/features/MailDataComponents.java`
- Modify: `src/main/java/forestry/core/features/CoreDataComponents.java`
- Modify: every referencing file
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: `checkBaseBoundary` from Task 1.
- Produces: `forestry.mail.features.MailDataComponents.LETTER_DATA`, same
  `DeferredHolder<DataComponentType<?>, DataComponentType<Letter>>` type as before.

- [ ] **Step 1: Read the existing registration and its registry**

```bash
sed -n '1,30p' src/main/java/forestry/core/features/CoreDataComponents.java
grep -n "LETTER_DATA" -A 10 src/main/java/forestry/core/features/CoreDataComponents.java
grep -rn "LETTER_DATA" src/main/java --include='*.java' | grep -v CoreDataComponents
```

Note the `DeferredRegister` the class uses and how it is bound to the mod event bus - the new class
must use the same mechanism, and mail's registry must actually be registered. Check how another
per-module feature holder does it, ex. `src/main/java/forestry/mail/features/MailItems.java`.

- [ ] **Step 2: Create the mail holder**

Create `src/main/java/forestry/mail/features/MailDataComponents.java` mirroring
`CoreDataComponents`' structure exactly - same `DeferredRegister` pattern, same builder chain - but
holding only `LETTER_DATA`, with the body copied verbatim from `CoreDataComponents`:

```java
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Letter>> LETTER_DATA =
		REGISTRY.register("letter_data",
			() -> DataComponentType.<Letter>builder()
				.persistent(Letter.CODEC)
				.networkSynchronized(ByteBufCodecs.fromCodec(Letter.CODEC))
				.build());
```

Use the same registry name string, `letter_data`, that `CoreDataComponents` used. Changing it would
orphan the component on existing letters in saved worlds.

- [ ] **Step 3: Delete it from CoreDataComponents**

Remove the `LETTER_DATA` field and `import forestry.mail.Letter;` from
`src/main/java/forestry/core/features/CoreDataComponents.java`.

- [ ] **Step 4: Repoint every reference**

```bash
grep -rln "CoreDataComponents.LETTER_DATA" src/main/java src/test/java \
  | xargs -r sed -i 's|CoreDataComponents\.LETTER_DATA|MailDataComponents.LETTER_DATA|'
```

Then fix the imports in those files: replace
`import forestry.core.features.CoreDataComponents;` with
`import forestry.mail.features.MailDataComponents;` where `CoreDataComponents` is no longer used, or
add the new import alongside where it still is.

- [ ] **Step 5: Confirm the registry is actually registered**

```bash
grep -rn "MailDataComponents" src/main/java/forestry/mail/ModuleMail.java src/main/java/forestry/mail/features/
```

A `DeferredRegister` that is never bound to the event bus registers nothing and the component
silently fails to exist. Follow whatever `ModuleMail` already does for `MailItems` and `MailBlocks`.

- [ ] **Step 6: Compile and trim**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`. Then remove this line from `gradle/base-boundary-baseline.txt`:

```
core/features/CoreDataComponents.java
```

```bash
./gradlew checkBaseBoundary checkApiBoundary
```

Expected: `checkBaseBoundary: 63 known leaking file(s) remaining`.

- [ ] **Step 7: Verify the component still works end to end**

```bash
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: no diff, all tests passed. If a test involving letters fails, the `DeferredRegister` is
not bound - revisit Step 5.

- [ ] **Step 8: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "mail: own the letter data component

CoreDataComponents registered a DataComponentType<Letter> over
forestry.mail.Letter. Mail owns the type, so mail owns the component. The
registry name letter_data is unchanged so existing letters keep their data.

checkBaseBoundary: 64 -> 63 files."
```

---

### Task 5: Split the packet id holders per module

`PacketIdClient` and `PacketIdServer` are constant holders of `CustomPacketPayload.Type<P>` values,
created by a `type(String)` helper. They are a naming index, not a registration mechanism -
registration already goes through `IPacketRegistry` in each module's `registerPackets`. So this is
not an extension point at all; each module simply declares its own constants.

**Files:**
- Create: `src/main/java/forestry/apiculture/network/ApiculturePacketIds.java`
- Create: `src/main/java/forestry/arboriculture/network/ArboriculturePacketIds.java`
- Create: `src/main/java/forestry/mail/network/MailPacketIds.java`
- Modify: `src/main/java/forestry/core/network/PacketIdClient.java`
- Modify: `src/main/java/forestry/core/network/PacketIdServer.java`
- Modify: each module's packet registration and every reference to the moved constants
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: `checkBaseBoundary` from Task 1.
- Produces, all `CustomPacketPayload.Type<P>` with the same generic parameter and the same string
  path as today:

| New constant | Path string |
| --- | --- |
| `ApiculturePacketIds.BEE_LOGIC_ACTIVE` | `bee_logic_active` |
| `ApiculturePacketIds.HABITAT_BIOME_POINTER` | `habitat_biome_pointer` |
| `ApiculturePacketIds.ALVEARY_CONTROLLER_CHANGE` | `alveary_controller_change` |
| `ArboriculturePacketIds.RIPENING_UPDATE` | `ripening_update` |
| `MailPacketIds.TRADING_ADDRESS_RESPONSE` | `trading_address_response` |
| `MailPacketIds.LETTER_INFO_RESPONSE_PLAYER` | `letter_info_response_player` |
| `MailPacketIds.LETTER_INFO_RESPONSE_TRADER` | `letter_info_response_trader` |
| `MailPacketIds.POBOX_INFO_RESPONSE` | `pobox_info_response` |
| `MailPacketIds.LETTER_INFO_REQUEST` | `letter_info_request` |
| `MailPacketIds.TRADING_ADDRESS_REQUEST` | `trading_address_request` |
| `MailPacketIds.LETTER_TEXT_SET` | `letter_text_set` |

- [ ] **Step 1: Make the `type` helper reachable**

`PacketIdServer.type(String)` is package-private to `forestry.core.network`:

```java
	static <P extends CustomPacketPayload> CustomPacketPayload.Type<P> type(String path) {
		return new CustomPacketPayload.Type<>(ForestryConstants.forestry(path));
	}
```

The new holders live in other packages and need it. Make it `public` and leave it where it is; the
constants it builds all use the `forestry` namespace regardless of which module declares them.

- [ ] **Step 2: Record every path string before moving anything**

```bash
grep -nE 'type\("' src/main/java/forestry/core/network/PacketIdClient.java src/main/java/forestry/core/network/PacketIdServer.java
```

Copy the exact `type("...")` string for each constant being moved. These strings are the wire
identity of the packet. Changing one breaks client/server compatibility silently - the packet is
simply never delivered.

- [ ] **Step 3: Create the apiculture holder**

Create `src/main/java/forestry/apiculture/network/ApiculturePacketIds.java`:

```java
package forestry.apiculture.network;

import forestry.apiculture.network.packets.PacketAlvearyChange;
import forestry.apiculture.network.packets.PacketBeeLogicActive;
import forestry.apiculture.network.packets.PacketHabitatBiomePointer;
import forestry.core.network.PacketIdServer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Packet ids owned by the apiculture module.
 */
public class ApiculturePacketIds {
	public static final CustomPacketPayload.Type<PacketBeeLogicActive> BEE_LOGIC_ACTIVE = PacketIdServer.type("bee_logic_active");
	public static final CustomPacketPayload.Type<PacketHabitatBiomePointer> HABITAT_BIOME_POINTER = PacketIdServer.type("habitat_biome_pointer");
	public static final CustomPacketPayload.Type<PacketAlvearyChange> ALVEARY_CONTROLLER_CHANGE = PacketIdServer.type("alveary_controller_change");
}
```

Note the constant is `ALVEARY_CONTROLLER_CHANGE` over path `alveary_controller_change`, not the
shorter name the class `PacketAlvearyChange` suggests. Cross-check every constant against the table
in the Interfaces block above; the path strings there were read from the source.

- [ ] **Step 4: Create the arboriculture and mail holders**

Same shape. `ArboriculturePacketIds` holds the single `PacketRipeningUpdate` constant.
`MailPacketIds` holds all seven mail constants - the four clientbound ones from `PacketIdClient` and
the three serverbound ones from `PacketIdServer` - in one class, since a module's packets are one
concern regardless of direction.

- [ ] **Step 5: Delete the moved constants from the core holders**

Remove those constants and their now-unused imports from `PacketIdClient.java` and
`PacketIdServer.java`. Keep every core, factory, energy, sorting and worktable constant where it is -
those modules all land in the base jar under D2.

- [ ] **Step 6: Repoint every reference**

```bash
grep -rn "PacketIdClient\.\|PacketIdServer\." src/main/java --include='*.java' \
  | grep -E "ALVEARY|BEE_LOGIC|HABITAT|RIPENING|LETTER|POBOX|TRADER"
```

Update each to the new holder and fix imports.

- [ ] **Step 7: Compile and trim**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`. Then remove these lines from `gradle/base-boundary-baseline.txt`:

```
core/network/PacketIdClient.java
core/network/PacketIdServer.java
```

```bash
./gradlew checkBaseBoundary checkApiBoundary
```

Expected: `checkBaseBoundary: 61 known leaking file(s) remaining`.

- [ ] **Step 8: Verify the wire identity did not change**

```bash
git diff HEAD~1 --unified=0 | grep -E '^\+.*type\("' | grep -oE 'type\("[^"]+"\)' | sort > /tmp/after.txt
git show HEAD:src/main/java/forestry/core/network/PacketIdClient.java src/main/java/forestry/core/network/PacketIdServer.java 2>/dev/null | grep -oE 'type\("[^"]+"\)' | sort > /tmp/before.txt
diff /tmp/before.txt /tmp/after.txt
```

Every path string that existed before must still exist somewhere. GameTests do not exercise
client/server packet round trips, so this textual check is the only guard.

- [ ] **Step 9: Verify datagen and tests**

```bash
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: no diff, all tests passed.

- [ ] **Step 10: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "network: give each module its own packet id holder

PacketIdClient and PacketIdServer are constant holders, not a registration
mechanism - registration already goes through IPacketRegistry per module. So
each module declares its own CustomPacketPayload.Type constants and the core
holders keep only core, factory, energy, sorting and worktable, all of which
land in the base jar.

Every type(\"...\") path string is unchanged; those strings are the packet's
wire identity.

checkBaseBoundary: 63 -> 61 files."
```

---

### Task 6: Split the creative tabs

`ForestryCreativeTabs` defines seven tabs. Five of them - `APICULTURE`, `ARBORICULTURE`,
`LEPIDOPTEROLOGY`, `AGRICULTURE`, `MAIL` - belong to content jars and already have their own
`addXItems` method, so they move wholesale. `FORESTRY` and `STORAGE` stay in base; `STORAGE`
references no content at all.

The complication is that the base `FORESTRY` tab itself lists 7 content entries - 3 from
`ApicultureItems`, 3 from `ArboricultureItems`, 1 from `CharcoalBlocks`. Those cannot move with a
tab because the tab is staying. They go through NeoForge's `BuildCreativeModeTabContentsEvent`,
which lets any mod add entries to any tab, so the owning jar contributes them from its own side.

**Files:**
- Create: `src/main/java/forestry/apiculture/tab/ApicultureCreativeTab.java`
- Create: `src/main/java/forestry/arboriculture/tab/ArboricultureCreativeTab.java`
- Create: `src/main/java/forestry/lepidopterology/tab/LepidopterologyCreativeTab.java`
- Create: `src/main/java/forestry/farming/tab/AgricultureCreativeTab.java`
- Create: `src/main/java/forestry/mail/tab/MailCreativeTab.java`
- Modify: `src/main/java/forestry/core/tab/ForestryCreativeTabs.java`
- Modify: `gradle/base-boundary-baseline.txt`

**Interfaces:**
- Consumes: `checkBaseBoundary` from Task 1.
- Produces: `ForestryCreativeTabs.FORESTRY` and `.STORAGE` remain where they are and keep their
  names. The five moved tabs keep their registry paths - `apiculture`, `arboriculture`,
  `lepidopterology`, `agriculture`, `mail` - so no tab id changes.

- [ ] **Step 1: Read the whole file before splitting it**

```bash
sed -n '60,115p' src/main/java/forestry/core/tab/ForestryCreativeTabs.java
```

Note the `REGISTRY.creativeTab(path, consumer)` shape, the `withTabsBefore` chain, and that each tab
is a `FeatureCreativeTab`. Each module's registry is `ModFeatureRegistry.get(ForestryModuleIds.X)`;
see `ApicultureItems` line 15 for the pattern.

- [ ] **Step 2: Move the apiculture tab**

Create `src/main/java/forestry/apiculture/tab/ApicultureCreativeTab.java` holding the `APICULTURE`
tab definition and the `addApicultureItems` method, both copied verbatim from
`ForestryCreativeTabs` (lines 75-79 and 201-253), with the registry switched to apiculture's:

```java
	public static final FeatureCreativeTab APICULTURE = REGISTRY.creativeTab("apiculture", tab -> {
		// body copied from ForestryCreativeTabs
		tab.displayItems(ApicultureCreativeTab::addApicultureItems);
		tab.withTabsBefore(ForestryCreativeTabs.FORESTRY.getKey());
	});
```

Point `withTabsBefore` at `FORESTRY` rather than at the previous tab in the old chain. `FORESTRY` is
in base and therefore always present, whereas the old chain
(`APICULTURE` <- `ARBORICULTURE` <- `LEPIDOPTEROLOGY` <- `AGRICULTURE` <- `STORAGE` <- `MAIL`) would
reference tabs that may be absent. NeoForge's `CreativeModeTabRegistry` null-checks both endpoints of
a sort edge so a dangling reference degrades rather than crashes, but pointing at a guaranteed tab is
correct rather than merely survivable. The visible cost is that tab order among the five becomes
unspecified.

- [ ] **Step 3: Move the other four tabs**

Same treatment for `ARBORICULTURE` (lines 81-86 and 254-297), `LEPIDOPTEROLOGY` (87-91 and 298-310),
`AGRICULTURE` (93-97 and 311-360) and `MAIL` (105-109 and 434-465). Each goes to its own module,
each points `withTabsBefore` at `ForestryCreativeTabs.FORESTRY.getKey()`.

`AGRICULTURE` draws on both `farming` and `cultivation`, which merge into the agriculture jar under
D1, so it goes in `forestry.farming.tab` for now and moves with the rest in phase 7.

- [ ] **Step 4: Move the base tab's content entries to event subscribers**

The 7 remaining entries in `addForestryItems` cannot move with a tab. In each owning module, add a
subscriber:

```java
	@SubscribeEvent
	public static void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == ForestryCreativeTabs.FORESTRY.getKey()) {
			event.accept(ApicultureItems.HONEY_DROP);
			// ... the other entries this module contributed
		}
	}
```

The 7 entries, with their line numbers inside `addForestryItems` and the module that takes each:

| Line | Entry | Goes to |
| --- | --- | --- |
| 115 | `ApicultureItems.SCOOP` | apiculture |
| 116 | `ApicultureItems.SMOKER` | apiculture |
| 117 | `ArboricultureItems.GRAFTER` | arboriculture |
| 118 | `ArboricultureItems.PROVEN_GRAFTER` | arboriculture |
| 151 | `ApicultureItems.AMBER_DRONE` | apiculture |
| 152 | `ArboricultureItems.AMBER_SAPLING_FOSSIL` | arboriculture |
| 160 | `CharcoalBlocks.CHARCOAL` | arboriculture |

So apiculture contributes 3 and arboriculture contributes 4, not the 3/3/1 split the raw import
counts suggest - `CharcoalBlocks` is arboriculture's.

Note lines 115-118 sit adjacent to core tool entries and 151-152 adjacent to core fossil entries.
`BuildCreativeModeTabContentsEvent` appends rather than splicing, so those groupings will not survive
exactly. Accept the reordering; it is cosmetic and the alternative is keeping base aware of content.

Register each subscriber the same way the module registers its other event handlers.

- [ ] **Step 5: Compile and trim**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`. Then remove this line from `gradle/base-boundary-baseline.txt`:

```
core/tab/ForestryCreativeTabs.java
```

```bash
./gradlew checkBaseBoundary checkApiBoundary
```

Expected: `checkBaseBoundary: 60 known leaking file(s) remaining`.

- [ ] **Step 6: Verify the tabs in game**

Creative tab contents are not covered by GameTests or datagen, so this needs a visual check.

```bash
./gradlew runClient
```

Open the creative menu and confirm: seven Forestry tabs are present; each holds what it held before;
the main Forestry tab still shows the honey drop, the wood entries and the charcoal entry contributed
by the event subscribers.

- [ ] **Step 7: Verify datagen and tests**

```bash
./gradlew runData && git status --porcelain src/generated/resources
./gradlew runGameTestServer 2>&1 | grep -E "required tests"
```

Expected: no diff, all tests passed.

- [ ] **Step 8: Commit**

```bash
git add -A src/main/java/forestry gradle/base-boundary-baseline.txt
git commit -m "tabs: each module owns its creative tab

Five of the seven tabs already had their own addXItems method, so they move
to their modules wholesale. FORESTRY and STORAGE stay in base; STORAGE
references no content.

The 7 content entries inside the base FORESTRY tab move to
BuildCreativeModeTabContentsEvent subscribers in the owning modules, which is
how a jar contributes to a tab it does not own.

withTabsBefore now points at FORESTRY rather than chaining tab to tab, so no
tab references one that may be absent. Order among the five is unspecified as
a result.

checkBaseBoundary: 61 -> 60 files."
```

---

### Task 7: Record phase 2 completion

**Files:**
- Modify: `docs/superpowers/specs/2026-07-30-feature-package-reorg-design.md`

- [ ] **Step 1: Confirm the state from a clean build**

```bash
./gradlew clean build
```

Expected: `BUILD SUCCESSFUL`, with both `checkApiBoundary` and `checkBaseBoundary` running as part of
`check`, and `checkBaseBoundary: 60 known leaking file(s) remaining`.

- [ ] **Step 2: Update the spec**

In the sequencing block, change the `2` line to `DONE 2026-07-31` and add after the block:

```markdown
Phase 2 landed 2026-07-31. Bucket B is closed and `checkBaseBoundary` now ratchets the whole base
artifact against a checked-in baseline: 68 files at the start of the phase, 60 remaining. Four of the
seven bucket-B files were cleared by relocating a single misfiled type each rather than by building
an extension point; only the creative tabs needed one.
```

- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/specs/2026-07-30-feature-package-reorg-design.md
git commit -m "docs: record phase 2 completion, base boundary at 60 of 68"
```

---

## Notes for phase 3

- `checkBaseBoundary`'s baseline is the phase tracker from here on. Phase 3 (bucket C, the
  species-type-aware engine) should trim 11 more entries, phase 4 (buckets D, E, H) another 11.
- Bucket B turned out to contain only one real extension point. The other six files were misfiled
  types and per-module constant holders. Expect the same when sizing later buckets - check whether
  the leak is a design problem or just a file in the wrong package before planning a mechanism.
- `ForestryRecipeProvider` still leaks 26 imports and is the single worst file in the base artifact.
  It belongs to bucket A and dissolves in phase 8 when datagen splits per jar, so do not attack it
  earlier.
