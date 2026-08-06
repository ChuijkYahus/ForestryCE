# Per-jar generated resource directories Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the ownership machinery with one generated resource root per jar, so datagen output lands in the jar that owns it instead of being reassembled by inference in `build.gradle`.

**Architecture:** Each jar gets its own `PackOutput` rooted at its own directory under `src/generated`. Datagen moves out of `src/datagen` into the production source set that owns it, so the compile classpath enforces the reference closure. Content jars attach to core's single `GatherDataEvent` through a `ServiceLoader` SPI, the same mechanism `IForestryPlugin` already uses, so there is one `DataGenerator`, one `HashCache` and one `ExistingFileHelper`.

**Tech Stack:** Gradle 8 (Groovy DSL), NeoForge 21.1.230, Minecraft 1.21.1, Java 21, ModKit (JitPack, pinned by commit sha).

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-05-per-jar-generated-resources-design.md`. Read it before Task 1.
- Branch: `1.21.1-restructure`. Do not create a new branch in ForestryCE.
- ModKit work happens in `/home/thedarkcolour/IdeaProjects/ModKit` on branch `1.21-neoforge`. Do not touch any other ModKit branch.
- Every `gradlew` invocation runs from `/home/thedarkcolour/IdeaProjects/ForestryCE`.
- All source and resource moves use `git mv`, never `cp` + `rm`.
- Java packages never change. `forestry.agriculture`, `forestry.lepidopterology` and `forestry.mail` keep their names.
- Comment style is binding: see `CLAUDE.md`. ASCII only. No em-dash, no en-dash, no curly quotes. No terminal period on a single-sentence inline comment.
- Scratchpad directory, used for the baseline and the check script:
  `/tmp/claude-1000/-home-thedarkcolour-IdeaProjects-ForestryCE/1cbf0a04-f2c7-46b2-beaf-738e3a10dbf4/scratchpad`
  Referred to below as `$SCRATCH`. Export it at the start of every task:
  ```bash
  export SCRATCH=/tmp/claude-1000/-home-thedarkcolour-IdeaProjects-ForestryCE/1cbf0a04-f2c7-46b2-beaf-738e3a10dbf4/scratchpad
  ```
- **The relocation gate** is the primary correctness check for every task from Task 4 on. Run `bash $SCRATCH/check_relocation.sh` after every `runData`. It must print `OK`. A file is keyed by its path relative to its own root, so relocating a file between roots is invisible to it; what it catches is a file that vanished, appeared, changed bytes, or landed in two roots at once.
- Never run `./gradlew clean` between tasks unless a step says to. It deletes `build/resources/*`, which the boot configurations read.
- Do not delete anything from the ownership machinery before Task 9. Tasks 3 through 8 rely on it continuing to govern whatever has not moved yet. The two mechanisms are disjoint by construction: `generateResourceOwners` walks only `src/generated/resources` and `src/main/resources`, and the new roots sit outside both.

## File Structure

| file | responsibility | tasks |
| --- | --- | --- |
| `../ModKit/src/main/java/thedarkcolour/modkit/data/DataHelper.java` | builder, overridable event-derived values | 1 |
| `../ModKit/src/main/java/thedarkcolour/modkit/data/ProviderRegistrar.java` | new functional interface for `addProvider` | 1 |
| `../ModKit/src/main/java/thedarkcolour/modkit/data/MKTagsProvider.java` | read helper fields instead of `helper.event` | 1 |
| `../ModKit/src/main/java/thedarkcolour/modkit/data/MKEnglishProvider.java` | entry filter on the autogen loop | 1 |
| `../ModKit/src/main/java/thedarkcolour/modkit/data/MKItemModelProvider.java` | entry filter on the autogen loop | 1 |
| `build.gradle` | roots, source sets, jar excludes, deletion of the machinery | 2, 4, 8, 9 |
| `src/datagen/java/forestry/core/data/DataRoots.java` | derives a per-jar `PackOutput` from the run's output folder | 4 |
| `src/datagen/java/forestry/core/data/IForestryDataProvider.java` | SPI content jars attach through | 4 |
| `src/datagen/java/forestry/core/data/JarModules.java` | module id set to owned registry id set | 4, 9 |
| `src/datagen/java/forestry/core/data/Data.java` | core's providers, SPI dispatch | 4, 5, 6, 7, 9 |
| `src/datagen/java/forestry/lepidopterology/data/**` | butterflies datagen, later moved to `src/butterflies/java` | 5, 8 |
| `src/datagen/java/forestry/mail/data/**` | mail datagen, later moved to `src/mail/java` | 6, 8 |
| `src/datagen/java/forestry/agriculture/data/**` | farms datagen, later moved to `src/farms/java` | 7, 8 |
| `src/{butterflies,farms,mail}/resources/META-INF/services/forestry.core.data.IForestryDataProvider` | SPI registration | 5, 6, 7 |

---

### Task 1: ModKit gains DataHelper.Builder

**Files:**
- Create: `/home/thedarkcolour/IdeaProjects/ModKit/src/main/java/thedarkcolour/modkit/data/ProviderRegistrar.java`
- Modify: `/home/thedarkcolour/IdeaProjects/ModKit/src/main/java/thedarkcolour/modkit/data/DataHelper.java`
- Modify: `/home/thedarkcolour/IdeaProjects/ModKit/src/main/java/thedarkcolour/modkit/data/MKTagsProvider.java`
- Modify: `/home/thedarkcolour/IdeaProjects/ModKit/src/main/java/thedarkcolour/modkit/data/MKEnglishProvider.java`
- Modify: `/home/thedarkcolour/IdeaProjects/ModKit/src/main/java/thedarkcolour/modkit/data/MKItemModelProvider.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `new DataHelper.Builder(String modid, GatherDataEvent event)` with chainable
  `packOutput(PackOutput)`, `existingFileHelper(ExistingFileHelper)`,
  `lookupProvider(CompletableFuture<HolderLookup.Provider>)`, `includeServer(boolean)`,
  `includeClient(boolean)`, `addProvider(ProviderRegistrar)`, `entryFilter(Predicate<ResourceLocation>)`,
  `logger(Logger)` and `build()` returning `DataHelper`. `DataHelper(String, GatherDataEvent)` is retained.

- [ ] **Step 1: Switch to the target branch and confirm it is clean**

```bash
cd /home/thedarkcolour/IdeaProjects/ModKit
git status --short
git checkout 1.21-neoforge
git log --oneline -1
```

Expected: a clean tree and `51b8760 don't require JetBrains JDK`. If the tree is dirty, stop and report.

- [ ] **Step 2: Add the ProviderRegistrar functional interface**

Create `src/main/java/thedarkcolour/modkit/data/ProviderRegistrar.java`:

```java
package thedarkcolour.modkit.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;

/**
 * How a {@link DataHelper} attaches a provider to the generator. The default is
 * {@link DataGenerator#addProvider}; override it through {@link DataHelper.Builder#addProvider} when a
 * mod needs to wrap, collect or redirect the providers a helper creates.
 */
@FunctionalInterface
public interface ProviderRegistrar {
	void addProvider(DataGenerator generator, boolean run, DataProvider provider);
}
```

- [ ] **Step 3: Give DataHelper the builder and the fields**

In `DataHelper.java`, replace the field block and constructor. Keep every existing field; add six.

```java
	protected final String modid;
	protected final GatherDataEvent event;
	protected final Logger logger;
	protected final Map<ResourceKey<?>, MKTagsProvider<?>> tags;
	protected final PackOutput packOutput;
	protected final ExistingFileHelper existingFileHelper;
	protected final CompletableFuture<HolderLookup.Provider> lookupProvider;
	protected final boolean includeServer;
	protected final boolean includeClient;
	protected final ProviderRegistrar providerRegistrar;
	protected final Predicate<ResourceLocation> entryFilter;

	public DataHelper(String modid, GatherDataEvent event) {
		this(new Builder(modid, event));
	}

	protected DataHelper(Builder builder) {
		this.modid = builder.modid;
		this.event = builder.event;
		this.logger = builder.logger != null ? builder.logger : LoggerFactory.getLogger(ModKit.ID + "/" + builder.modid);
		this.tags = new HashMap<>();
		this.packOutput = builder.packOutput != null ? builder.packOutput : builder.event.getGenerator().getPackOutput();
		this.existingFileHelper = builder.existingFileHelper != null ? builder.existingFileHelper : builder.event.getExistingFileHelper();
		this.lookupProvider = builder.lookupProvider != null ? builder.lookupProvider : builder.event.getLookupProvider();
		this.includeServer = builder.includeServer != null ? builder.includeServer : builder.event.includeServer();
		this.includeClient = builder.includeClient != null ? builder.includeClient : builder.event.includeClient();
		this.providerRegistrar = builder.providerRegistrar != null ? builder.providerRegistrar : DataGenerator::addProvider;
		this.entryFilter = builder.entryFilter != null ? builder.entryFilter : id -> true;
	}
```

Add the nested builder at the bottom of the class, before the closing brace:

```java
	/**
	 * Overrides for the values a DataHelper otherwise reads off the GatherDataEvent. Every field left unset
	 * keeps the event's answer, so a builder with no overrides is the plain constructor.
	 *
	 * <p>Ex. one helper per output root:
	 * {@code new DataHelper.Builder(modid, event).packOutput(new PackOutput(root)).build()}
	 */
	public static class Builder {
		private final String modid;
		private final GatherDataEvent event;
		@Nullable private PackOutput packOutput;
		@Nullable private ExistingFileHelper existingFileHelper;
		@Nullable private CompletableFuture<HolderLookup.Provider> lookupProvider;
		@Nullable private Boolean includeServer;
		@Nullable private Boolean includeClient;
		@Nullable private ProviderRegistrar providerRegistrar;
		@Nullable private Predicate<ResourceLocation> entryFilter;
		@Nullable private Logger logger;

		public Builder(String modid, GatherDataEvent event) {
			this.modid = modid;
			this.event = event;
		}

		/**
		 * @param packOutput The root every provider this helper creates writes to
		 */
		public Builder packOutput(PackOutput packOutput) {
			this.packOutput = packOutput;
			return this;
		}

		public Builder existingFileHelper(ExistingFileHelper existingFileHelper) {
			this.existingFileHelper = existingFileHelper;
			return this;
		}

		public Builder lookupProvider(CompletableFuture<HolderLookup.Provider> lookupProvider) {
			this.lookupProvider = lookupProvider;
			return this;
		}

		public Builder includeServer(boolean includeServer) {
			this.includeServer = includeServer;
			return this;
		}

		public Builder includeClient(boolean includeClient) {
			this.includeClient = includeClient;
			return this;
		}

		public Builder addProvider(ProviderRegistrar providerRegistrar) {
			this.providerRegistrar = providerRegistrar;
			return this;
		}

		/**
		 * @param entryFilter The registry ids this helper's automatic generation covers. Ex. one mod split
		 *                    across several jars gives each jar the ids that jar registers
		 */
		public Builder entryFilter(Predicate<ResourceLocation> entryFilter) {
			this.entryFilter = entryFilter;
			return this;
		}

		public Builder logger(Logger logger) {
			this.logger = logger;
			return this;
		}

		public DataHelper build() {
			return new DataHelper(this);
		}
	}
```

Add the imports `net.minecraft.data.DataGenerator`, `net.minecraft.resources.ResourceLocation`, `net.neoforged.neoforge.common.data.ExistingFileHelper`, `java.util.concurrent.CompletableFuture` and `java.util.function.Predicate`.

- [ ] **Step 4: Route every create* method through the fields**

In the same file, replace each `this.event.getGenerator().getPackOutput()` with `this.packOutput`, each `this.event.getExistingFileHelper()` with `this.existingFileHelper`, each `this.event.getLookupProvider()` with `this.lookupProvider`, and each `this.event.getGenerator().addProvider(X, Y)` with `this.providerRegistrar.addProvider(this.event.getGenerator(), X, Y)` where `X` becomes `this.includeServer` or `this.includeClient` as it was before (the two `addProvider(true, ...)` calls in `createEnglish` for Modonomicon books keep `true`).

Pass the filter into the two providers that autogenerate:

```java
		this.english = new MKEnglishProvider(this.packOutput, this.modid, this.logger, generateNames, this.entryFilter, addTranslations);
```

```java
		this.itemModels = new MKItemModelProvider(this.packOutput, this.existingFileHelper, this.modid, this.logger, generate3dBlockItems, generate2dItems, generateSpawnEggs, this.entryFilter, addItemModels);
```

- [ ] **Step 5: MKTagsProvider reads the helper instead of the event**

In `MKTagsProvider.java`, change the `super(...)` call:

```java
		super(helper.packOutput, registry, helper.lookupProvider, helper.modid, helper.existingFileHelper);
```

- [ ] **Step 6: MKEnglishProvider takes the filter**

Add the parameter to the constructor and store it:

```java
	private final Predicate<ResourceLocation> entryFilter;

	public MKEnglishProvider(PackOutput output, String modid, Logger logger, boolean generateNames, Predicate<ResourceLocation> entryFilter, @Nullable Consumer<MKEnglishProvider> addNames) {
```

In `addTranslations()`, gate the loop body:

```java
						MKUtils.forModRegistry(optional.get(), this.modid, (id, obj) -> {
							if (!this.entryFilter.test(id)) {
								return;
							}
							String name = WordUtils.capitalize(id.getPath().replace('_', ' '));
```

- [ ] **Step 7: MKItemModelProvider takes the filter**

Add the parameter before `addItemModels`, store it as `entryFilter`, and gate the autogen loop:

```java
			MKUtils.forModRegistry(Registries.ITEM, modid, (id, item) -> {
				if (!this.entryFilter.test(id)) {
					return;
				}
				if (generate3dBlockItems && item instanceof BlockItem) {
```

- [ ] **Step 8: Compile and publish to the local maven repository**

```bash
cd /home/thedarkcolour/IdeaProjects/ModKit
./gradlew build publishToMavenLocal
ls ~/.m2/repository/thedarkcolour/modkit/modkit/1.0/
```

Expected: `BUILD SUCCESSFUL` and a `modkit-1.0.jar` in the listing.

- [ ] **Step 9: Commit**

```bash
cd /home/thedarkcolour/IdeaProjects/ModKit
git add -A
git commit -m "$(cat <<'EOF'
data: DataHelper.Builder for the values it reads off the event

A mod split across several jars needs one helper per output root, and the
autogen loops need to know which registry entries belong to the jar they are
generating for. Every value DataHelper took from the GatherDataEvent becomes an
overridable field, defaulting to what the event supplies, so existing callers
are unaffected.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: ForestryCE consumes the local ModKit and records the baseline

**Files:**
- Modify: `build.gradle` (repositories, `modkit` dependency)
- Create: `$SCRATCH/generated-baseline.sha256`
- Create: `$SCRATCH/relocation-allowlist.txt`
- Create: `$SCRATCH/check_relocation.sh`

**Interfaces:**
- Consumes: `thedarkcolour.modkit:modkit:1.0` from `mavenLocal()`, published by Task 1.
- Produces: `bash $SCRATCH/check_relocation.sh` printing `OK`, used by every later task.

- [ ] **Step 1: Record the pre-change baseline**

Run this before touching anything. It is the reference every later task is checked against.

```bash
export SCRATCH=/tmp/claude-1000/-home-thedarkcolour-IdeaProjects-ForestryCE/1cbf0a04-f2c7-46b2-beaf-738e3a10dbf4/scratchpad
mkdir -p "$SCRATCH"
cd /home/thedarkcolour/IdeaProjects/ForestryCE/src/generated/resources
find . -type f -not -path './.cache/*' -print0 | sort -z | xargs -0 sha256sum > "$SCRATCH/generated-baseline.sha256"
wc -l < "$SCRATCH/generated-baseline.sha256"
```

Expected: `10207`. That is every file in the root: the 9,633 core owns, the 571 the content jars
own, and the 3 the game merges across packs.

- [ ] **Step 2: Create the empty allowlist**

```bash
printf '# Files permitted to differ from the baseline. One path per line, relative to a root.\n' > "$SCRATCH/relocation-allowlist.txt"
```

- [ ] **Step 3: Write the relocation check script**

Create `$SCRATCH/check_relocation.sh`:

```bash
#!/usr/bin/env bash
# Compares the generated roots against the pre-change baseline. A file is keyed by its path
# relative to its own root, so moving a file between roots is invisible here on purpose
set -uo pipefail
SCRATCH=/tmp/claude-1000/-home-thedarkcolour-IdeaProjects-ForestryCE/1cbf0a04-f2c7-46b2-beaf-738e3a10dbf4/scratchpad
cd /home/thedarkcolour/IdeaProjects/ForestryCE

: > "$SCRATCH/current.sha256"
for root in src/generated/resources src/generated/resources_farms src/generated/resources_mail src/generated/resources_butterflies; do
	[ -d "$root" ] || continue
	# -r so a root that exists but holds no files yet contributes nothing. Without it xargs still
	# runs sha256sum once with no argument, it reads stdin, and the run gains a phantom entry named -
	if ! (cd "$root" && find . -type f -not -path './.cache/*' -print0 | sort -z | xargs -0 -r sha256sum) >> "$SCRATCH/current.sha256"; then
		echo "failed to hash ${root}"
		exit 2
	fi
done

python3 - "$SCRATCH/generated-baseline.sha256" "$SCRATCH/current.sha256" "$SCRATCH/relocation-allowlist.txt" <<'PY'
import sys, collections

# find prints ./a/b, so every key carries a leading ./ that an allowlist entry copied out of a
# report will not. Both sides are stripped rather than one
def norm(name):
	return name[2:] if name.startswith('./') else name

def load(path):
	table = collections.defaultdict(list)
	for line in open(path):
		digest, name = line.rstrip('\n').split('  ', 1)
		table[norm(name)].append(digest)
	return table

base, cur = load(sys.argv[1]), load(sys.argv[2])
allow = {norm(l.strip()) for l in open(sys.argv[3]) if l.strip() and not l.startswith('#')}

missing = [f for f in base if f not in cur and f not in allow]
added = [f for f in cur if f not in base and f not in allow]
changed = [f for f in base if f in cur and sorted(base[f]) != sorted(cur[f]) and f not in allow]
# Allowlisted like the three above, and for the one reason a path belongs in two roots: the five
# files the game merges across packs. Each jar ships its own entries and the game reassembles them.
# Any other path in two roots is a jar shipping another jar's file
duplicated = [f for f, digests in cur.items() if len(digests) > 1 and f not in allow]

bad = False
for label, items in (('MISSING', missing), ('ADDED', added), ('CHANGED', changed), ('DUPLICATED ACROSS ROOTS', duplicated)):
	if items:
		bad = True
		print(f'{label}: {len(items)}')
		for item in sorted(items)[:40]:
			print('   ', item)
print('FAIL' if bad else 'OK')
sys.exit(1 if bad else 0)
PY
```

```bash
chmod +x "$SCRATCH/check_relocation.sh"
bash "$SCRATCH/check_relocation.sh"
```

Expected: `OK`.

- [ ] **Step 4: Point the modkit configuration at mavenLocal**

In `build.gradle`, add `mavenLocal()` as the first entry of the `repositories { }` block, and change the dependency:

```groovy
	// ModKit DEV ONLY - datagen and nothing else. Keeping it off main's classpath is the point of
	// the datagen source set; see the sourceSets block above
	// todo pin the JitPack sha again once the DataHelper.Builder commit is pushed
	modkit 'thedarkcolour.modkit:modkit:1.0'
```

- [ ] **Step 5: Regenerate and verify nothing moved**

```bash
cd /home/thedarkcolour/IdeaProjects/ForestryCE
./gradlew runData
bash "$SCRATCH/check_relocation.sh"
```

Expected: `BUILD SUCCESSFUL`, then `OK`. If the ModKit refactor changed behaviour, this is where it shows.

- [ ] **Step 6: Commit**

```bash
git add build.gradle
git commit -m "$(cat <<'EOF'
build: consume the local ModKit while the builder change is unpublished

Temporary. The JitPack sha goes back in the last task of the per-jar generated
resources work, once the ModKit commit is pushed.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: Move the hand-written non-core resources into their jars

**Files:**
- Move: 33 files from `src/main/resources` to `src/butterflies/resources`
- Move: 30 files from `src/main/resources` to `src/farms/resources`
- Move: 34 files from `src/main/resources` to `src/mail/resources`
- Modify: `src/generated/resource-owners.json` (regenerated)

**Interfaces:**
- Consumes: nothing.
- Produces: `src/mail/resources/` now exists. The three content resource roots hold every hand-written file their jar owns.

- [ ] **Step 1: Move the files**

The set is exactly the non-core entries of `src/generated/resource-owners.json` that exist under `src/main/resources`. Derive it rather than typing it:

```bash
cd /home/thedarkcolour/IdeaProjects/ForestryCE
python3 - <<'PY' > /tmp/handwritten-moves.txt
import json, os
owners = json.load(open('src/generated/resource-owners.json'))
for rel, jar in sorted(owners.items()):
	if jar in ('core', 'split'):
		continue
	if os.path.exists(os.path.join('src/main/resources', rel)):
		print(jar, rel)
PY
wc -l < /tmp/handwritten-moves.txt
```

Expected: `97`.

```bash
while read -r jar rel; do
	dest="src/${jar}/resources/${rel}"
	mkdir -p "$(dirname "$dest")"
	git mv "src/main/resources/${rel}" "$dest"
done < /tmp/handwritten-moves.txt
git status --short | wc -l
```

Expected: `194` (97 deletions plus 97 additions, or fewer lines if git detects renames; either is fine).

- [ ] **Step 2: Regenerate the ownership map**

The moved files leave `resourceRoots`, so they leave the map.

```bash
./gradlew generateResourceOwners
python3 -c "
import json, collections
print(collections.Counter(json.load(open('src/generated/resource-owners.json')).values()))"
```

Expected: `Counter({'core': 11497, 'farms': 435, 'butterflies': 74, 'mail': 62, 'split': 3})`. The three content counts have dropped by exactly the number of files moved into each.

- [ ] **Step 3: Verify the jars still carry the moved files**

```bash
./gradlew jar butterfliesJar farmsJar mailJar
unzip -l build/libs/forestrymail-1.21.1-*.jar | grep -c 'textures/item/stamps'
unzip -l build/libs/forestry-1.21.1-*.jar | grep -c 'textures/item/stamps' || true
```

Expected: `2` from the mail jar, `0` from core.

- [ ] **Step 4: Boot check**

```bash
rm -rf run/boot-*
./gradlew runBootAll 2>&1 | tail -40
```

If no `runBootAll` task exists, run each boot configuration listed under `runs { }` in `build.gradle` in turn. Every one must log `Done`. Read the `Done` line; the absence of errors proves nothing.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "$(cat <<'EOF'
build: hand-written content resources move into their own jars

97 textures, Patchouli entries and hand-authored models leave
src/main/resources for src/{butterflies,farms,mail}/resources. A git mv places
them exactly, so they stop needing the ownership map to place them.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: Four output roots and the SPI

**Files:**
- Create: `src/datagen/java/forestry/core/data/DataRoots.java`
- Create: `src/datagen/java/forestry/core/data/IForestryDataProvider.java`
- Create: `src/datagen/java/forestry/core/data/JarModules.java`
- Modify: `src/datagen/java/forestry/core/data/Data.java`
- Modify: `build.gradle` (the `data` run block, the three new `srcDir`s)

**Interfaces:**
- Consumes: `DataHelper.Builder` from Task 1.
- Produces:
  - `DataRoots.of(GatherDataEvent event, String directory)` returning `PackOutput`.
  - `DataRoots.CORE`, `DataRoots.FARMS`, `DataRoots.MAIL`, `DataRoots.BUTTERFLIES` as `String` directory names.
  - `interface IForestryDataProvider { void gather(GatherDataEvent event); }`
  - `JarModules.ownedIds(Set<ResourceLocation> moduleIds)` returning `Set<ResourceLocation>`.

- [ ] **Step 1: Add DataRoots**

Create `src/datagen/java/forestry/core/data/DataRoots.java`:

```java
package forestry.core.data;

import java.nio.file.Path;

import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * The generated resource root of each jar. Core's root is the data run's output folder and the other
 * three are its siblings, so every root is derived from the run rather than assumed relative to a
 * working directory.
 *
 * <p>Core's root stays the output folder because {@code HashCache} deletes every file under that
 * folder which no provider wrote. The ownership manifests sit beside it and would not survive a run
 * from the parent. They are deleted in the last task of this work, and the output folder moves up to
 * the parent then.
 */
public final class DataRoots {
	public static final String CORE = "resources";
	public static final String FARMS = "resources_farms";
	public static final String MAIL = "resources_mail";
	public static final String BUTTERFLIES = "resources_butterflies";

	private DataRoots() {
	}

	/**
	 * @param event     The gather event the run's output folder is read from
	 * @param directory The root directory name, one of the constants above
	 * @return The pack output every provider belonging to that jar writes to
	 */
	public static PackOutput of(GatherDataEvent event, String directory) {
		Path root = event.getGenerator().getPackOutput().getOutputFolder().getParent();
		return new PackOutput(root.resolve(directory));
	}
}
```

- [ ] **Step 2: Add the SPI**

Create `src/datagen/java/forestry/core/data/IForestryDataProvider.java`:

```java
package forestry.core.data;

import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * How a content jar attaches its own data providers to core's gather event. Core loads these through
 * {@link java.util.ServiceLoader}, the same way {@code IForestryPlugin} is loaded, so core never names a
 * content jar's types and the compile classpath keeps enforcing what each jar can see.
 *
 * <p>Implementations are listed in
 * {@code META-INF/services/forestry.core.data.IForestryDataProvider} in their own jar.
 */
public interface IForestryDataProvider {
	/**
	 * Called once during core's gather event, after core has registered its own providers.
	 *
	 * @param event The gather event to register providers with
	 */
	void gather(GatherDataEvent event);
}
```

- [ ] **Step 3: Add JarModules**

Create `src/datagen/java/forestry/core/data/JarModules.java`:

```java
package forestry.core.data;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import forestry.core.platform.registration.FeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

/**
 * Which registry ids a set of modules registered. Read out of the live registries, so it cannot drift
 * from what the code registers. Used to scope automatic name generation to one jar.
 */
public final class JarModules {
	private JarModules() {
	}

	/**
	 * @param moduleIds The modules a jar owns. Ex. {@code Set.of(ForestryModuleIds.MAIL)}
	 * @return Every id those modules registered, in any registry
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static Set<ResourceLocation> ownedIds(Set<ResourceLocation> moduleIds) {
		Set<ResourceLocation> ids = new HashSet<>();
		for (ModFeatureRegistry modRegistry : ModFeatureRegistry.getRegistries().values()) {
			for (Map.Entry<ResourceLocation, FeatureRegistry> module : modRegistry.getModules().entrySet()) {
				if (!moduleIds.contains(module.getKey())) {
					continue;
				}
				for (Map.Entry<ResourceKey, DeferredRegister> registry : module.getValue().getRegistries().entrySet()) {
					for (DeferredHolder<?, ?> holder : (Collection<DeferredHolder<?, ?>>) registry.getValue().getEntries()) {
						ids.add(holder.getId());
					}
				}
			}
		}
		// A module named here that registered nothing is a typo or a rename, and it would silently scope
		// a jar's generation to less than it owns
		if (ids.isEmpty()) {
			throw new IllegalStateException("No registered ids for modules: " + moduleIds);
		}
		return ids;
	}
}
```

- [ ] **Step 4: Core builds its PackOutput explicitly and dispatches the SPI**

In `Data.java`, replace `PackOutput output = generator.getPackOutput();` with:

```java
		PackOutput output = DataRoots.of(event, DataRoots.CORE);
```

and pass `output` where `dataHelper` is constructed, replacing the `DataHelper` line:

```java
		DataHelper dataHelper = new DataHelper.Builder(ForestryConstants.MOD_ID, event).packOutput(output).build();
```

At the very end of `gatherData`, after the last `generator.addProvider(...)` line, add:

```java
		// Content jars attach here. Loaded rather than named, so core's compile classpath still cannot
		// see a content jar's types. Sorted so the run is deterministic
		ServiceLoader.load(IForestryDataProvider.class).stream()
				.map(ServiceLoader.Provider::get)
				.sorted(Comparator.comparing(provider -> provider.getClass().getName()))
				.forEachOrdered(provider -> provider.gather(event));
```

Add the imports `java.util.Comparator` and `java.util.ServiceLoader`.

- [ ] **Step 5: Teach the run about the content resource roots**

In `build.gradle`, in the `data { }` run block, replace the `programArguments.addAll(...)` line with:

```groovy
			programArguments.addAll('--mod', 'forestry', '--all',
					'--output', file('src/generated/resources/').absolutePath,
					'--existing', file('src/main/resources/').absolutePath,
					'--existing', file('src/farms/resources/').absolutePath,
					'--existing', file('src/mail/resources/').absolutePath,
					'--existing', file('src/butterflies/resources/').absolutePath)
```

`--output` does not move. `HashCache.purgeStaleAndWrite` walks the whole output folder and deletes every file no provider wrote this run (`HashCache.java:121-135`), so pointing it at `src/generated` would delete `ownership.json` and `resource-owners.json` on every run. Task 9 deletes both files and moves `--output` up to `src/generated` there, once nothing but the four roots is left under it.

The cost until then is that stale purging covers core's root only. A file that stops being generated into a content root lingers rather than being removed. `check_relocation.sh` reports exactly that as an `ADDED` file, so it is covered for the length of this work.

The three added `--existing` roots are where the hand-written models moved in Task 3.

- [ ] **Step 6: Add the three new source directories**

In `build.gradle`, in the `contentModules.each { m -> ... }` block that already calls `sourceSets[m].resources.srcDir contentModMetadata[m]`, add above it:

```groovy
	sourceSets[m].resources.srcDir "src/generated/resources_${m}"
```

- [ ] **Step 7: Regenerate and verify**

`.cache` stays at `src/generated/resources/.cache`, which `.gitignore` already covers. Nothing to change here.

```bash
cd /home/thedarkcolour/IdeaProjects/ForestryCE
./gradlew runData
ls src/generated/
bash "$SCRATCH/check_relocation.sh"
```

Expected: `src/generated/` lists `ownership.json`, `resource-owners.json` and `resources`, and the check prints `OK`. The three new roots do not exist yet because nothing writes to them.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "$(cat <<'EOF'
data: per-jar output roots and the provider SPI

Core's generated root is still the data run's output folder and the other three
are its siblings, because HashCache deletes every file under the output folder
that no provider wrote, and the two ownership manifests sit beside it. The
output folder moves up once they are gone.

Content jars attach through a ServiceLoader SPI rather than being named by core.
Nothing writes to the new roots yet.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: Butterflies datagen

**Files:**
- Create: `src/datagen/java/forestry/lepidopterology/data/LepidopterologyData.java`
- Create: `src/datagen/java/forestry/lepidopterology/data/ButterflyTaxonProvider.java`
- Create: `src/butterflies/resources/META-INF/services/forestry.core.data.IForestryDataProvider`
- Move: `src/datagen/java/forestry/core/data/ButterflySpeciesProvider.java` -> `src/datagen/java/forestry/lepidopterology/data/`
- Move: `src/datagen/java/forestry/core/data/taxonomy/ButterflyTaxonomy.java` -> `src/datagen/java/forestry/lepidopterology/data/`
- Modify: `src/datagen/java/forestry/core/data/Data.java`, `TaxonProvider.java`, `MutationProvider.java`, `ForestryBlockLootTables.java`, `ForestryChestLootTables.java`, `ForestryItemTagsProvider.java`, `models/ForestryBlockStateProvider.java`, `models/ForestryItemModelProvider.java`, `recipe/ForestryRecipeProvider.java`, `taxonomy/ForestryTaxonomy.java`

**Interfaces:**
- Consumes: `DataRoots`, `IForestryDataProvider` from Task 4.
- Produces: `src/generated/resources_butterflies` holding 74 files. `ForestryTaxonomy.defineSpine(GeneticRegistration)` becomes `public static`.

- [ ] **Step 1: Record which files must end up in the butterflies root**

```bash
cd /home/thedarkcolour/IdeaProjects/ForestryCE
python3 -c "
import json
o = json.load(open('src/generated/resource-owners.json'))
print('\n'.join(sorted(k for k, v in o.items() if v == 'butterflies')))" > "$SCRATCH/expected-butterflies.txt"
wc -l < "$SCRATCH/expected-butterflies.txt"
```

Expected: `74`.

- [ ] **Step 2: Open the spine for reuse**

In `taxonomy/ForestryTaxonomy.java`, change `private static void defineSpine` to `public static void defineSpine`, and remove `ButterflyTaxonomy.defineTaxa(genetics);` from `buildDefaultTaxa()` along with its import. The class javadoc names `{@link ButterflyTaxonomy}` in its list of per-kingdom definitions; drop that link rather than repointing it, since the class is no longer visible from core. Give `defineSpine` a javadoc line saying the butterflies jar builds it too, to hang the lepidoptera order off, and subtracts it again.

- [ ] **Step 3: Move the butterfly-only classes**

```bash
mkdir -p src/datagen/java/forestry/lepidopterology/data
git mv src/datagen/java/forestry/core/data/ButterflySpeciesProvider.java src/datagen/java/forestry/lepidopterology/data/ButterflySpeciesProvider.java
git mv src/datagen/java/forestry/core/data/taxonomy/ButterflyTaxonomy.java src/datagen/java/forestry/lepidopterology/data/ButterflyTaxonomy.java
```

Change the `package` line of each to `forestry.lepidopterology.data` and fix imports in every file that referenced them.

- [ ] **Step 4: Add the butterfly taxon provider**

Create `src/datagen/java/forestry/lepidopterology/data/ButterflyTaxonProvider.java`:

```java
package forestry.lepidopterology.data;

import java.util.Set;

import net.minecraft.data.PackOutput;

import forestry.apiimpl.plugin.GeneticRegistration;
import forestry.core.data.TaxonProvider;
import forestry.core.data.taxonomy.ForestryTaxonomy;

/**
 * Generates the lepidoptera subtree of {@code data/forestry/taxon}. Everything above the order - the
 * domains, kingdoms and the arthropod to insect spine - is shared with bees and ships in core, so the
 * spine is built here only to hang the order off and is then subtracted rather than written again.
 */
public class ButterflyTaxonProvider extends TaxonProvider {
	public ButterflyTaxonProvider(PackOutput output) {
		super(output);
	}

	@Override
	protected void addTaxa() {
		GeneticRegistration spineOnly = new GeneticRegistration();
		ForestryTaxonomy.defineSpine(spineOnly);
		Set<String> shared = spineOnly.buildTaxa().keySet();

		GeneticRegistration genetics = new GeneticRegistration();
		ForestryTaxonomy.defineSpine(genetics);
		ButterflyTaxonomy.defineTaxa(genetics);
		genetics.buildTaxa().forEach((name, taxon) -> {
			if (!shared.contains(name)) {
				add(toDefinition(taxon));
			}
		});
	}
}
```

`TaxonProvider.addTaxa`, `TaxonProvider.add` and `TaxonProvider.toDefinition` must be `protected` and non-final for this to compile; widen them if they are not. Add a matching `seedLiveTaxaForDatagen` path: whatever static seeding `TaxonProvider.seedLiveTaxaForDatagen()` does with `ForestryTaxonomy.buildDefaultTaxa()`, expose it as a `protected static void seedLiveTaxa(Map<String, ITaxon> taxa)` in `TaxonProvider` and call it from a `ButterflyTaxonProvider.seedLiveTaxaForDatagen()` with the subtracted map.

- [ ] **Step 5: Add the entry point**

Create `src/datagen/java/forestry/lepidopterology/data/LepidopterologyData.java`:

```java
package forestry.lepidopterology.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.data.event.GatherDataEvent;

import forestry.api.ForestryConstants;
import forestry.core.data.DataRoots;
import forestry.core.data.IForestryDataProvider;

/**
 * Registers every provider that writes into the butterflies jar. Loaded by core through
 * {@code META-INF/services/forestry.core.data.IForestryDataProvider}.
 */
public class LepidopterologyData implements IForestryDataProvider {
	@Override
	public void gather(GatherDataEvent event) {
		// Butterfly taxa must be live before any butterfly species is built, because a species resolves
		// its genus through the taxonomy
		ButterflyTaxonProvider.seedLiveTaxaForDatagen();
		ButterflySpeciesProvider.seedLiveSpeciesForDatagen();

		DataGenerator generator = event.getGenerator();
		PackOutput output = DataRoots.of(event, DataRoots.BUTTERFLIES);

		generator.addProvider(event.includeServer(), new ButterflyTaxonProvider(output));
		generator.addProvider(event.includeServer(), new ButterflySpeciesProvider(output, event.getLookupProvider()));
	}
}
```

The remaining providers are added in the steps below as their content is moved across.

- [ ] **Step 6: Register the service**

```bash
mkdir -p src/butterflies/resources/META-INF/services
printf 'forestry.lepidopterology.data.LepidopterologyData\n' > src/butterflies/resources/META-INF/services/forestry.core.data.IForestryDataProvider
```

- [ ] **Step 7: Remove the butterfly seeding and providers from core**

In `Data.java`, delete the `ButterflySpeciesProvider.seedLiveSpeciesForDatagen();` call and its comment from `preDataGen()`, and delete the `new ButterflySpeciesProvider(...)` provider line. Delete any now-unused imports.

- [ ] **Step 8: Move the remaining butterfly content across**

Each of these is a verbatim move of existing code from a core provider into a new provider under `forestry.lepidopterology.data`, registered in `LepidopterologyData.gather` against the butterflies `PackOutput`:

| from | move | to |
| --- | --- | --- |
| `models/ForestryBlockStateProvider` | the cocoon blockstates and their block models | `LepidopterologyBlockStateProvider` |
| `models/ForestryItemModelProvider` | the `LepidopterologyItems` entries | `LepidopterologyItemModelProvider` |
| `ForestryItemModels` | any `LepidopterologyItems` entries | the butterflies `DataHelper.createItemModels` consumer |
| `ForestryBlockLootTables` | the `LepidopterologyBlocks` entries | `LepidopterologyBlockLootTables` |
| `ForestryChestLootTables` | the `lepidopterology` sub-table | `LepidopterologyChestLootTables` |
| `MutationProvider` | the butterfly mutation section | `ButterflyMutationProvider` |
| `recipe/ForestryRecipeProvider` | `registerLepidopterologyRecipes`, plus `foresters_manual_butterfly` and `lepidopterists_chest` wherever they are declared | the butterflies `DataHelper.createRecipes` consumer |
| `ForestryItemTagsProvider` | the caterpillar entry of `forestry:genetic_samples` | the butterflies `DataHelper.createTags(Registries.ITEM, ...)` consumer |

The butterflies helper is built the same way core's is:

```java
		DataHelper dataHelper = new DataHelper.Builder(ForestryConstants.MOD_ID, event).packOutput(output).build();
```

Do not call `createEnglish` here; lang stays whole in core until Task 9.

- [ ] **Step 9: Allow the tag file to change**

Moving the caterpillar out of core's item tags changes `genetic_samples` in core and creates a butterflies copy. Add both facts to the allowlist:

```bash
printf 'data/forestry/tags/item/genetic_samples.json\n' >> "$SCRATCH/relocation-allowlist.txt"
```

- [ ] **Step 10: Regenerate and verify**

```bash
./gradlew runData
bash "$SCRATCH/check_relocation.sh"
find src/generated/resources_butterflies -type f | sed 's|^src/generated/resources_butterflies/||' | sort > "$SCRATCH/actual-butterflies.txt"
diff <(sort "$SCRATCH/expected-butterflies.txt") "$SCRATCH/actual-butterflies.txt"
```

Expected: `OK` from the check, and `diff` reporting **no missing lines** (nothing the map assigns to this jar may be absent from its root).

Extra lines are possible and are not automatically defects. The map is the thing being deleted, and in places it is wrong: it places a file by inference, and the code places it by knowing. Justify every extra line individually in your report, or move the file back. Task 5 found six, all legitimate: three childless butterfly taxa the map's upward-propagation left in core because no species claims them, two recipe advancements that a `RecipeOutput` writes alongside the recipe they unlock, and one entry-keyed tag that now ships from both roots by design.

- [ ] **Step 11: Boot check and commit**

```bash
rm -rf run/boot-*
./gradlew check
git add -A
git commit -m "$(cat <<'EOF'
data: butterflies datagen writes to its own root

Species, taxa, mutations, models, loot and recipes for the butterflies jar move
into forestry.lepidopterology.data and write to src/generated/resources_butterflies.
The lepidoptera taxa are the spine build subtracted from the spine-plus-order
build, so the shared ancestors stay in core rather than being emitted twice.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 6: Mail datagen

**Files:**
- Create: `src/datagen/java/forestry/mail/data/MailData.java` and its providers
- Create: `src/mail/resources/META-INF/services/forestry.core.data.IForestryDataProvider`
- Modify: the same core providers as Task 5, for the mail entries

**Interfaces:**
- Consumes: `DataRoots`, `IForestryDataProvider`, and the pattern established by `LepidopterologyData`.
- Produces: `src/generated/resources_mail` holding 62 files.

- [ ] **Step 1: Record the expected file set**

```bash
cd /home/thedarkcolour/IdeaProjects/ForestryCE
python3 -c "
import json
o = json.load(open('src/generated/resource-owners.json'))
print('\n'.join(sorted(k for k, v in o.items() if v == 'mail')))" > "$SCRATCH/expected-mail.txt"
wc -l < "$SCRATCH/expected-mail.txt"
```

Expected: `62`.

- [ ] **Step 2: Add the entry point**

Create `src/datagen/java/forestry/mail/data/MailData.java`, following `LepidopterologyData` exactly, using `DataRoots.MAIL`. It seeds nothing; mail has no species or taxa.

```java
package forestry.mail.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.data.event.GatherDataEvent;

import forestry.api.ForestryConstants;
import forestry.core.data.DataRoots;
import forestry.core.data.IForestryDataProvider;

import thedarkcolour.modkit.data.DataHelper;

/**
 * Registers every provider that writes into the mail jar. Loaded by core through
 * {@code META-INF/services/forestry.core.data.IForestryDataProvider}.
 */
public class MailData implements IForestryDataProvider {
	@Override
	public void gather(GatherDataEvent event) {
		DataGenerator generator = event.getGenerator();
		PackOutput output = DataRoots.of(event, DataRoots.MAIL);
		DataHelper dataHelper = new DataHelper.Builder(ForestryConstants.MOD_ID, event).packOutput(output).build();

		dataHelper.createRecipes(MailRecipeProvider::addRecipes);
		dataHelper.createTags(net.minecraft.core.registries.Registries.ITEM, (tags, lookup) -> MailItemTagsProvider.addTags(tags));
		dataHelper.createItemModels(false, false, false, MailItemModels::addModels);

		generator.addProvider(event.includeClient(), new MailBlockStateProvider(output, event.getExistingFileHelper()));
		generator.addProvider(event.includeClient(), new MailItemModelProvider(output, event.getExistingFileHelper()));
		generator.addProvider(event.includeServer(), new MailLootTableProvider(output, event.getLookupProvider()));
	}
}
```

Create only the providers this jar actually needs; drop any line above whose content turns out to be empty.

- [ ] **Step 3: Register the service**

```bash
mkdir -p src/mail/resources/META-INF/services
printf 'forestry.mail.data.MailData\n' > src/mail/resources/META-INF/services/forestry.core.data.IForestryDataProvider
```

- [ ] **Step 4: Move the mail content across**

| from | move | to |
| --- | --- | --- |
| `models/ForestryBlockStateProvider` | the three `MailBlocks` machine blockstates and their block models | `MailBlockStateProvider` |
| `ForestryItemModels` | every `MailItems` entry, including the `LETTERS` and `STAMPS` groups | `MailItemModels` |
| `models/ForestryItemModelProvider` | any `MailItems` entries | `MailItemModelProvider` |
| `ForestryBlockLootTables` | the `MailBlocks` entries | `MailBlockLootTables`, wrapped in a `MailLootTableProvider` |
| `ForestryChestLootTables` | the `mail` sub-table | `MailChestLootTables` |
| `recipe/ForestryRecipeProvider` | `registerMailRecipes` plus the 8 mail carpenter recipes inside `registerCarpenter` | `MailRecipeProvider.addRecipes` |
| `ForestryItemTagsProvider` | the one mail item tag | `MailItemTagsProvider` |

- [ ] **Step 5: Regenerate and verify**

```bash
./gradlew runData
bash "$SCRATCH/check_relocation.sh"
find src/generated/resources_mail -type f | sed 's|^src/generated/resources_mail/||' | sort > "$SCRATCH/actual-mail.txt"
diff <(sort "$SCRATCH/expected-mail.txt") "$SCRATCH/actual-mail.txt"
```

Expected: `OK`, and no diff output.

- [ ] **Step 6: Boot check and commit**

```bash
rm -rf run/boot-*
./gradlew check
git add -A
git commit -m "$(cat <<'EOF'
data: mail datagen writes to its own root

Blockstates, models, loot, recipes and the one item tag for the mail jar move
into forestry.mail.data and write to src/generated/resources_mail. The eight
mail carpenter recipes leave registerCarpenter, which was the only place a
machine method held another jar's recipes.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 7: Farms datagen

**Files:**
- Create: `src/datagen/java/forestry/agriculture/data/AgricultureData.java` and its providers
- Create: `src/farms/resources/META-INF/services/forestry.core.data.IForestryDataProvider`
- Modify: the same core providers, for the farms entries

**Interfaces:**
- Consumes: `DataRoots`, `IForestryDataProvider`, and the pattern established by the previous two tasks.
- Produces: `src/generated/resources_farms` holding 435 files.

- [ ] **Step 1: Record the expected file set**

```bash
cd /home/thedarkcolour/IdeaProjects/ForestryCE
python3 -c "
import json
o = json.load(open('src/generated/resource-owners.json'))
print('\n'.join(sorted(k for k, v in o.items() if v == 'farms')))" > "$SCRATCH/expected-farms.txt"
wc -l < "$SCRATCH/expected-farms.txt"
```

Expected: `435`.

- [ ] **Step 2: Add the entry point and register the service**

Create `src/datagen/java/forestry/agriculture/data/AgricultureData.java` following `MailData` exactly, using `DataRoots.FARMS`.

```bash
mkdir -p src/farms/resources/META-INF/services
printf 'forestry.agriculture.data.AgricultureData\n' > src/farms/resources/META-INF/services/forestry.core.data.IForestryDataProvider
```

- [ ] **Step 3: Move the farms content across**

| from | move | to |
| --- | --- | --- |
| `models/ForestryBlockStateProvider` | the `FarmingBlocks.FARM` loop, `plainFarm`, `singleFarm`, the `BlockTypePlanter` loop over `CultivationBlocks`, and the arboretum entries | `AgricultureBlockStateProvider` |
| `ForestryItemModels` | every `FarmingBlocks` and `CultivationBlocks` item entry | `AgricultureItemModels` |
| `ForestryBlockLootTables` | the farms block entries | `AgricultureBlockLootTables` |
| `ForestryChestLootTables` | the `farming` and `cultivation` sub-tables | `AgricultureChestLootTables` |
| `recipe/ForestryRecipeProvider` | `registerFarmingRecipes`, `registerCultivationRecipes`, the 3 farms carpenter recipes, the 1 farms fabricator recipe (`electron_tubes/ender`) and the 3 farms fermenter recipes | `AgricultureRecipeProvider.addRecipes` |
| `ForestryBlockTagsProvider` | the 3 farms block tags | `AgricultureBlockTagsProvider` |
| `ForestryBlockTagsProvider` | the farms entries of `minecraft:mineable/pickaxe` | `AgricultureBlockTagsProvider` |
| `ForestryDataMapProvider` | the farms entries of `neoforge:compostables` | `AgricultureDataMapProvider` |

`electron_tubes/ender.json` is the one recipe whose result is a core item and whose ingredient is a farms item. It belongs to farms because farms is the only jar that can see both.

- [ ] **Step 4: Allow the two remaining entry-keyed files to change**

```bash
printf 'data/minecraft/tags/block/mineable/pickaxe.json\ndata/neoforge/data_maps/item/compostables.json\n' >> "$SCRATCH/relocation-allowlist.txt"
```

- [ ] **Step 5: Regenerate and verify**

```bash
./gradlew runData
bash "$SCRATCH/check_relocation.sh"
find src/generated/resources_farms -type f | sed 's|^src/generated/resources_farms/||' | sort > "$SCRATCH/actual-farms.txt"
diff <(sort "$SCRATCH/expected-farms.txt") "$SCRATCH/actual-farms.txt"
```

Expected: `OK`, and no diff output.

- [ ] **Step 6: Confirm core no longer generates content files**

```bash
python3 -c "
import json
o = json.load(open('src/generated/resource-owners.json'))
import collections
print(collections.Counter(o.values()))"
```

Expected: `Counter({'core': 11497})`, with no `farms`, `mail`, `butterflies` or `split` keys left. The map now describes core alone.

- [ ] **Step 7: Boot check and commit**

```bash
rm -rf run/boot-*
./gradlew check
git add -A
git commit -m "$(cat <<'EOF'
data: farms datagen writes to its own root

The largest of the three: 435 files of blockstates, models, loot tables,
recipes, their generated advancements and three block tags move into
forestry.agriculture.data. The ownership map now describes core alone.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 8: Datagen moves into the source sets that own it

**Files:**
- Move: `src/datagen/java/forestry/core/data/**` -> `src/main/java/forestry/core/data/**`
- Move: `src/datagen/java/forestry/agriculture/data/**` -> `src/farms/java/forestry/agriculture/data/**`
- Move: `src/datagen/java/forestry/mail/data/**` -> `src/mail/java/forestry/mail/data/**`
- Move: `src/datagen/java/forestry/lepidopterology/data/**` -> `src/butterflies/java/forestry/lepidopterology/data/**`
- Delete: `src/datagen/`
- Modify: `build.gradle`

**Interfaces:**
- Consumes: the four entry points from Tasks 4 through 7.
- Produces: no `datagen` source set. The compile classpath now enforces the reference closure.

- [ ] **Step 1: Move the sources**

```bash
cd /home/thedarkcolour/IdeaProjects/ForestryCE
mkdir -p src/farms/java/forestry/agriculture src/mail/java/forestry/mail src/butterflies/java/forestry/lepidopterology
git mv src/datagen/java/forestry/agriculture/data src/farms/java/forestry/agriculture/data
git mv src/datagen/java/forestry/mail/data src/mail/java/forestry/mail/data
git mv src/datagen/java/forestry/lepidopterology/data src/butterflies/java/forestry/lepidopterology/data
git mv src/datagen/java/forestry/core/data src/main/java/forestry/core/data
find src/datagen -type f | head
```

Expected: no output from `find`. If files remain, move them to `src/main/java` under their existing package and report what they were.

```bash
git rm -r --cached src/datagen 2>/dev/null || true
rmdir -p src/datagen/java/forestry 2>/dev/null || true
```

- [ ] **Step 2: Delete the datagen source set**

In `build.gradle`:

- Remove `datagen` from the `sourceSets { }` block.
- Remove the four lines wiring `sourceSets.datagen.compileClasspath` and `sourceSets.datagen.runtimeClasspath`.
- Remove `sourceSet sourceSets.datagen` from the `forestry` entry of `neoForge.mods`.
- Remove `addModdingDependenciesTo sourceSets.datagen`.
- Remove the `datagenImplementation.extendsFrom` and `datagenCompileOnly.extendsFrom` lines from `configurations { }`.
- Replace `testImplementation sourceSets.datagen.output` with nothing; the test source set already gets `sourceSets.main.output` and every content source set.
- In `checkResourceReferences`, remove `project.file('src/datagen/java')` from `javaDirs`.

- [ ] **Step 3: Put ModKit on the four compile classpaths**

In `configurations { }`, replace the `datagen*` lines:

```groovy
	// ModKit, dev only. It reaches the four production compile classpaths because the data providers
	// now live beside the code they generate for, and every jar task excludes those packages. What
	// keeps it out of a shipped jar is checkJarPartition, which reads the built artifact
	modkit
	runtimeOnly.extendsFrom modkit
	compileOnly.extendsFrom modkit
```

The existing `contentModules.each { configurations["${m}CompileOnly"].extendsFrom configurations.compileOnly }` line then carries it to the three content source sets.

- [ ] **Step 4: Exclude the datagen packages from every jar**

In the `jar { }` block add:

```groovy
	// Datagen providers reference ModKit, which is not shipped. FML's annotation scanner resolves
	// method signatures at boot via getDeclaredMethods0, and a lambda method reference in an
	// @EventBusSubscriber class carries ModKit types in its synthetic descriptor. See c540144fc
	exclude 'forestry/**/data/**'
	exclude 'META-INF/services/forestry.core.data.IForestryDataProvider'
```

Add the same two lines to each task in `contentJars`.

- [ ] **Step 5: Assert no shipped class references ModKit**

In `checkJarPartition`'s `doLast`, after `jarFiles` is populated, add:

```groovy
		// A source set boundary used to keep ModKit off the production compile classpath. The datagen
		// providers live in the production source sets now, so the artifact is what has to prove it
		jarFiles.each { jarName, file ->
			new java.util.zip.ZipFile(file).withCloseable { zip ->
				zip.entries().each { entry ->
					if (entry.name.endsWith('.class')) {
						var text = new String(zip.getInputStream(entry).bytes, 'ISO-8859-1')
						if (text.contains('thedarkcolour/modkit')) {
							problems.add("${jarName} jar: ${entry.name} references ModKit")
						}
					}
				}
			}
		}
```

- [ ] **Step 6: Compile, and expect the closure to be enforced**

```bash
./gradlew compileJava compileFarmsJava compileMailJava compileButterfliesJava
```

Expected: `BUILD SUCCESSFUL`. If `src/main/java/forestry/core/data` fails to compile because it names a content type, that is a file Tasks 5 through 7 missed. Move it to the owning jar's datagen package; do not add the content source set to main's classpath.

- [ ] **Step 7: Full verification**

```bash
./gradlew runData
bash "$SCRATCH/check_relocation.sh"
rm -rf run/boot-*
./gradlew clean check
./gradlew runGameTestServer
```

Expected: `OK` from the check, `BUILD SUCCESSFUL` from both gradle runs. `clean` is called here on purpose: Task 4 of `jar-split-reference-closure` records that deleting a source set does not delete its build output, and `build/classes/java/datagen` would otherwise outlive `src/datagen`.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "$(cat <<'EOF'
build: datagen moves into the source set that owns it

src/datagen is deleted. Each jar's providers live beside the content they
generate for, so the reference closure stops being a rule checked over
generated JSON and becomes a property of the compile classpath: a farms
provider cannot name a butterflies block.

ModKit returns to the production compile classpaths, which c540144fc's exclude
and a new bytecode assertion in checkJarPartition keep out of every artifact.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 9: Per-jar lang, and the machinery comes out

**Files:**
- Modify: `src/main/java/forestry/core/data/Data.java`
- Modify: `src/{farms,mail,butterflies}/java/**/data/*Data.java`
- Modify: `build.gradle`
- Delete: `src/generated/ownership.json`, `src/generated/resource-owners.json`, `src/main/java/forestry/core/data/OwnershipManifest.java`

**Interfaces:**
- Consumes: `JarModules.ownedIds` from Task 4.
- Produces: no ownership machinery. Four `srcDir`s carry the whole partition.

- [ ] **Step 1: Give each jar its lang filter**

In each content entry point, build the helper with a filter and create English:

```java
		Set<ResourceLocation> owned = JarModules.ownedIds(Set.of(ForestryModuleIds.MAIL));
		DataHelper dataHelper = new DataHelper.Builder(ForestryConstants.MOD_ID, event)
				.packOutput(output)
				.entryFilter(owned::contains)
				.build();

		dataHelper.createEnglish(true, null);
```

Farms declares `Set.of(ForestryModuleIds.FARMING, ForestryModuleIds.CULTIVATION)`; butterflies declares `Set.of(ForestryModuleIds.LEPIDOPTEROLOGY)`.

In core's `Data`, filter by the negation, so anything registered outside a content module keeps its name in core:

```java
		// Core takes everything the content jars do not, which is the safe direction: an id registered
		// outside a feature module still gets its name, and it gets it in the jar that is always installed
		Set<ResourceLocation> contentOwned = JarModules.ownedIds(Set.of(
				ForestryModuleIds.FARMING,
				ForestryModuleIds.CULTIVATION,
				ForestryModuleIds.MAIL,
				ForestryModuleIds.LEPIDOPTEROLOGY));
		DataHelper dataHelper = new DataHelper.Builder(ForestryConstants.MOD_ID, event)
				.packOutput(output)
				.entryFilter(id -> !contentOwned.contains(id))
				.build();
```

- [ ] **Step 2: Allow en_us to change**

```bash
printf 'assets/forestry/lang/en_us.json\n' >> "$SCRATCH/relocation-allowlist.txt"
```

- [ ] **Step 3: Delete the machinery from build.gradle**

Remove, in this order:

- the `ownershipFile`, `resourceOwnersFile`, `resourceRoots`, `entryKeyed`, `lootModuleJars`, `worldgenOwners` and `folderOwners` definitions
- the whole `tasks.register('generateResourceOwners')` block
- `resourceOwnerOf`, `idOwnerOf`, `qualifiedIdOwnerOf`
- `partitionDir` and the whole `partitionSharedResources` task
- in the `contentModules.each` block, the `from(root) { include { ... } }` loop over the two resource roots, the `dependsOn(partitionSharedResources)` line and the `from(partitionDir.map { it.dir(m) })` line
- in `processResources`, the `dependsOn(partitionSharedResources)` line, the `from(partitionDir.map { it.dir('core') })` line, the `partitionRoot` variable and the whole `exclude { details -> ... }` closure. Keep `exclude 'assets/forestry/lang/check_translations.py'`
- in `checkJarPartition`, `dependsOn(partitionSharedResources)` and any use of the two json files
- in the `data` run block, the `systemProperty 'forestry.ownershipManifest'` line

`generateEnUsLang` stays exactly as it is: it merges hand-written core `en_us` with generated core `en_us`, and core's `sourceSets.main.resources` still picks up `build/generated/sources/mergedAssets`.

- [ ] **Step 4: Delete the manifest and its writer**

```bash
cd /home/thedarkcolour/IdeaProjects/ForestryCE
git rm src/generated/ownership.json src/generated/resource-owners.json
git rm src/main/java/forestry/core/data/OwnershipManifest.java
```

Remove the `OwnershipManifest.write();` call and its comment from `Data.gatherData`, and the import.

- [ ] **Step 4b: Move the output folder up, now that nothing else lives under it**

Task 4 left `--output` at `src/generated/resources` because `HashCache.purgeStaleAndWrite` deletes
every file under the output folder that no provider wrote, and the two manifests sat beside it. They
are gone as of the previous step, so `src/generated` now holds nothing but the four roots and the
output folder can move up. That is what makes stale purging span all four: a file that stops being
generated into a content root is deleted rather than left behind.

In `build.gradle`, in the `data { }` run block:

```groovy
					'--output', file('src/generated/').absolutePath,
```

`DataRoots.of` resolves against `getOutputFolder().getParent()`, so it must lose the `.getParent()`
call in the same commit:

```java
	public static PackOutput of(GatherDataEvent event, String directory) {
		Path root = event.getGenerator().getPackOutput().getOutputFolder();
		return new PackOutput(root.resolve(directory));
	}
```

Update its class javadoc: the output folder is the parent of all four roots again, and the paragraph
explaining why core's root was the output folder no longer applies. Delete that paragraph.

`.cache` moves from `src/generated/resources/.cache` to `src/generated/.cache`, which the existing
ignore pattern `**/src/generated/**/.cache/` still matches. Confirm with
`git check-ignore -v src/generated/.cache/`. `.gitignore` needs no change.

Remove the old cache so the first run does not read a cache rooted at the wrong folder:

```bash
rm -rf src/generated/resources/.cache
```

Confirm after the `runData` in Step 6 that `git status --short` shows no untracked `.cache` path and
that `ls src/generated/` lists exactly `.cache`, `resources`, `resources_butterflies`,
`resources_farms` and `resources_mail`.

- [ ] **Step 5: Restore the JitPack dependency**

Push the ModKit commit first:

```bash
cd /home/thedarkcolour/IdeaProjects/ModKit
git push origin 1.21-neoforge
git rev-parse --short=7 HEAD
```

Then in ForestryCE's `build.gradle`, remove `mavenLocal()` from the repositories block and pin the sha printed above:

```groovy
	modkit 'com.github.thedarkcolour:ModKit:<sha>'
```

Delete the `todo` comment added in Task 2.

```bash
cd /home/thedarkcolour/IdeaProjects/ForestryCE
./gradlew --refresh-dependencies compileJava
```

Expected: `BUILD SUCCESSFUL`. JitPack builds on first request, so this can take several minutes.

- [ ] **Step 6: Full verification**

```bash
./gradlew runData
bash "$SCRATCH/check_relocation.sh"
rm -rf run/boot-*
./gradlew clean check
./gradlew runGameTestServer
```

Expected: `OK`, then `BUILD SUCCESSFUL` twice.

- [ ] **Step 7: Confirm the lang split is complete and correct**

```bash
python3 - <<'PY'
import json, itertools
roots = {
	'core': 'build/resources/main/assets/forestry/lang/en_us.json',
	'farms': 'src/generated/resources_farms/assets/forestry/lang/en_us.json',
	'mail': 'src/generated/resources_mail/assets/forestry/lang/en_us.json',
	'butterflies': 'src/generated/resources_butterflies/assets/forestry/lang/en_us.json',
}
loaded = {}
for jar, path in roots.items():
	try:
		loaded[jar] = json.load(open(path))
	except FileNotFoundError:
		print('missing', jar, path)
for a, b in itertools.combinations(loaded, 2):
	shared = set(loaded[a]) & set(loaded[b])
	if shared:
		print('overlap', a, b, sorted(shared)[:10])
print({jar: len(keys) for jar, keys in loaded.items()})
PY
```

Expected: no `missing` and no `overlap` lines. A shared bare id registered in two registries owned by different jars is the one acceptable overlap; if any appears, confirm the two values are identical and note it in the commit message.

- [ ] **Step 7b: Confirm the ten mis-shipped core files now ship in core**

The map's longest-prefix name walk assigns ten core files to farms, because the cultivation module registers block entity types named `forestry:ender`, `forestry:gourd`, `forestry:mushroom` and `forestry:nether`. None of the ten names a farms-owned id. Until the map is deleted they ship in `forestryfarms` and nowhere else, so a core-only install has no fermenter mushroom recipes and no `flowers/gourd` or `flowers/nether` bee flower tags. Deleting the map is what fixes it, and this is the check that proves it did.

```bash
for f in \
  data/forestry/recipe/fermenter/mushroom.json \
  data/forestry/recipe/fermenter/mushroom_honey.json \
  data/forestry/recipe/fermenter/mushroom_juice.json \
  data/forestry/recipe/carpenter/crates/unpack/minecraft/nether_bricks.json \
  data/forestry/recipe/carpenter/crates/unpack/minecraft/nether_wart.json \
  data/forestry/recipe/carpenter/ender_pearl.json \
  data/forestry/recipe/fabricator/electron_tubes/ender.json \
  data/forestry/tags/block/flowers/gourd.json \
  data/forestry/tags/block/flowers/nether.json \
  data/forestry/tags/block/hive_grounds/nether_extra_replaceable.json ; do
  c=$(unzip -l build/libs/forestry-1.21.1-*.jar | grep -c "$f")
  fm=$(unzip -l build/libs/forestryfarms-1.21.1-*.jar | grep -c "$f")
  echo "$f core=$c farms=$fm"
done
```

Expected: `core=1 farms=0` on every line. Before this task every line reads `core=0 farms=1`.

- [ ] **Step 8: Confirm the jars are still correctly partitioned**

```bash
unzip -l build/libs/forestry-1.21.1-*.jar | grep -c 'forestry/agriculture/data' || true
unzip -l build/libs/forestryfarms-1.21.1-*.jar | grep -c 'blockstates/farm_crops_manual.json'
unzip -l build/libs/forestry-1.21.1-*.jar | grep -c 'blockstates/farm_crops_manual.json' || true
```

Expected: `0`, `1`, `0`.

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "$(cat <<'EOF'
build: delete the ownership machinery

Roughly 600 lines of Groovy that inferred which jar owns which resource, from
file names and from regex over generated JSON, come out. Four srcDir lines
replace them. en_us is the last file to split, by registry id at generation
time rather than by key prefix afterwards.

Two defects the previous spec left open close by construction: the loot
modifier list is now emitted per jar rather than resolved through folderOwners,
and nothing derives ownership from file contents at all.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Self-Review

**Spec coverage.** Section 1 of the spec is Task 8. Section 2 is Task 8's step 6. Section 3 is Task 8's steps 4 and 5. Section 4 is Task 1. Section 5 is Tasks 4 and 9. Section 6 is Tasks 5, 6 and 7. Section 7 is Tasks 5, 6, 7 and 9. Section 8 is Task 4. Section 9 is Task 9. The 97 hand-written files are Task 3. The relocation gate is Task 2.

**One deviation from the spec as first written, worth stating.** Section 1 described four `@EventBusSubscriber` entry points; it now describes what this plan built, which is one subscriber in core plus a `ServiceLoader` SPI. The reason is that a second `@EventBusSubscriber(modid = ...)` in a content source set would either be rejected by `AutomaticEventSubscriber` (which requires the annotation's `modid` to equal the mod being injected) or force `--mod` to name all four, giving four `DataGenerator`s sharing one `.cache` directory and one output folder. One generator, one `HashCache` and one `ExistingFileHelper` is both safer and closer to the existing `IForestryPlugin` pattern. Core still never names a content type, so section 2's guarantee is unaffected.
