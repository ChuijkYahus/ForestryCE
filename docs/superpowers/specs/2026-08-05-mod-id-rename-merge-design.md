# Mod id rename and the apiculture/arboriculture merge into core

Date: 2026-08-05
Branch: `1.21.1-restructure`
Status: designed, not yet implemented
Supersedes the jar layout of `2026-07-30-feature-package-reorg-design.md` (six jars, underscored ids)

## Problem

Phase 9b shipped six jars carrying six mod ids: `forestry`, `forestry_apiculture`,
`forestry_arboriculture`, `forestry_lepidopterology`, `forestry_agriculture`,
`forestry_mail`, plus a dev-only `forestry_gametest`. Two things about that outcome are
wrong.

The underscore is wrong as a mod id. Nothing in NeoForge forbids it, but it reads as a
namespace separator rather than part of a name, and every mod id in the ecosystem that a
player types or a pack manifest names is a single unbroken token.

The granularity is wrong. Bees and trees are what Forestry is; core already describes
itself as "Trees, bees and more". Shipping them as jars a pack can decline made the
optional surface larger than it needed to be, and it bought nothing: no install that
wants Forestry wants it without bees or without trees. The three genuinely optional
things are butterflies, farms and mail.

## Target

| now | after |
| --- | --- |
| `forestry` (`src/main`) | `forestry` (`src/main`), now including apiculture and arboriculture |
| `forestry_apiculture` (`src/apiculture`) | folded into core |
| `forestry_arboriculture` (`src/arboriculture`) | folded into core |
| `forestry_lepidopterology` (`src/lepidopterology`) | `forestrybutterflies` (`src/butterflies`) |
| `forestry_agriculture` (`src/agriculture`) | `forestryfarms` (`src/farms`) |
| `forestry_mail` (`src/mail`) | `forestrymail` (`src/mail`) |
| `forestry_gametest` (`src/test`) | `forestrygametest` (`src/test`) |

Four published jars, down from six.

Java packages do not change. `forestry.apiculture`, `forestry.arboriculture`,
`forestry.lepidopterology` and `forestry.agriculture` keep their names. Only mod ids,
source set names and source directories move.

Nothing in-game changes. Every registered name stays under the `forestry:` namespace, as
it already did across all six jars.

### Two simplifications this forces

**The cross-content edge disappears.** Lepidopterology's only foreign dependency was
arboriculture, three `TreeUtil` imports for butterfly pollination. Arboriculture is core
now, so all four expressions of that edge are deleted: the `compileClasspath` line, the
`runtimeClasspath` line, the second `[[dependencies.forestry_lepidopterology]]` block in
its `neoforge.mods.toml`, and the `'lepidopterology': ['core', 'arboriculture',
'lepidopterology']` entry in the `generateResourceOwners` visibility map. Every surviving
jar's visibility set becomes exactly `['core', itself]`, so the promotion logic no longer
has a special case to get wrong.

**Ownership shrinks.** `ownership.json` maps 4,135 ids to `arboriculture` and 246 to
`apiculture`. Both become `core`, leaving roughly 324 ids spread across the three content
jars.

## Design

### The source set to package indirection

Renaming source directories without renaming packages breaks an assumption `build.gradle`
holds in four places: that a content module's source set name equals its Java package
name. `checkJarPartition` is where this matters most.

```groovy
var foreign = entries[m].findAll { it.endsWith('.class') && !it.startsWith("forestry/${m}/") }
```

With `m = 'butterflies'` and classes at `forestry/lepidopterology/`, that predicate matches
every class in the jar. The reciprocal core-side check, which asks whether the base jar
carries any `forestry/${it}/` tree, goes blind for the same reason: it would look for a
package that does not exist and find nothing, reporting a clean core jar no matter what
leaked into it. The first failure is loud; the second is silent, and it disables the check
that phase 9b's whole partition rests on.

The fix is one explicit map beside `contentModules`:

```groovy
var contentModules  = ['butterflies', 'farms', 'mail']
var contentPackages = [butterflies: 'lepidopterology', farms: 'agriculture', mail: 'mail']
```

Every `forestry/${m}/` becomes `forestry/${contentPackages[m]}/`.

This indirection is not new to the codebase. `OwnershipManifest.MODULE_TO_JAR` already maps
the internal module ids `farming` and `cultivation` onto the single jar `agriculture`, so a
build-side label distinct from the code's own naming is an established pattern here.

### Source moves

All moves are `git mv`, so history follows.

```
src/apiculture/java/forestry/apiculture/        -> src/main/java/forestry/apiculture/
src/arboriculture/java/forestry/arboriculture/  -> src/main/java/forestry/arboriculture/
src/apiculture/resources/data/forestry/neoforge/biome_modifier/hive.json
                                                -> src/main/resources/data/forestry/neoforge/biome_modifier/hive.json
src/arboriculture/resources/data/forestry/neoforge/biome_modifier/tree.json
                                                -> src/main/resources/data/forestry/neoforge/biome_modifier/tree.json
src/arboriculture/resources/META-INF/services/forestry.api.client.plugin.IClientHelper
                                                -> src/main/resources/META-INF/services/forestry.api.client.plugin.IClientHelper
src/lepidopterology/                            -> src/butterflies/
src/agriculture/                                -> src/farms/
```

`IClientHelper` is a plain move because main has no such service file today.

One file merges rather than moves.
`src/main/resources/META-INF/services/forestry.api.plugin.IForestryPlugin` gains
`forestry.apiculture.plugin.ApicultureForestryPlugin` and
`forestry.arboriculture.plugin.ArboricultureForestryPlugin`, joining the two entries it
already has.

`src/apiculture/templates/` and `src/arboriculture/templates/` are deleted. Their mod
metadata has no jar to describe.

### Mod metadata

Three content templates change modId, displayName and the `[[dependencies.<id>]]` table
headers, which are keyed by mod id and so move with it:

| file | modId | displayName |
| --- | --- | --- |
| `src/butterflies/templates/META-INF/neoforge.mods.toml` | `forestrybutterflies` | `Forestry: Butterflies` |
| `src/farms/templates/META-INF/neoforge.mods.toml` | `forestryfarms` | `Forestry: Farms` |
| `src/mail/templates/META-INF/neoforge.mods.toml` | `forestrymail` | `Forestry: Mail` |
| `src/test/templates/META-INF/neoforge.mods.toml` | `forestrygametest` | unchanged |

The butterflies template additionally drops its `forestry_arboriculture` dependency block.

`src/main/templates/META-INF/neoforge.mods.toml` is unchanged. Its id is already
`forestry` and its description, "Trees, bees and more", is now literally accurate.

### Ownership maps

Every ownership map is keyed by internal module id rather than by source set name, so all
four take value edits only. No keys change.

| map | location | edits |
| --- | --- | --- |
| `lootModuleJars` | `build.gradle` | `apiculture`, `arboriculture` -> `'core'`; `lepidopterology` -> `'butterflies'`; `farming`, `cultivation` -> `'farms'` |
| `worldgenOwners` | `build.gradle` | `hive`, `tree` -> `'core'` |
| `folderOwners` | `build.gradle` | bee and tree folders -> `'core'`; butterfly folders -> `'butterflies'` |
| `MODULE_TO_JAR` | `OwnershipManifest.java` | the same four value changes |

### Naming derivations

| expression | now | after |
| --- | --- | --- |
| mod id | `create("forestry_${m}")` | `create("forestry${m}")` |
| `archiveBaseName` | `forestry_${m}-${minecraftVersion}` | `forestry${m}-${minecraftVersion}` |
| `Implementation-Title` | `forestry_${m}` | `forestry${m}` |
| `Specification-Title` | `Forestry: ${m.capitalize()}` | unchanged |
| jar task description | `Assembles the forestry_${m} jar` | `Assembles the forestry${m} jar` |

`Specification-Title` needs no change: `m.capitalize()` over the new source set names
yields Butterflies, Farms and Mail.

The `checkJarPartition` completion message hardcodes `"6 jars"`. It becomes
`${jarFiles.size()}`, so the next layout change cannot leave it lying.

### Boot configurations

The existing content boots encode relationships that no longer exist. `apicultureServer`
is "bees without trees" and `lepidopterologyNoBeesServer` is "butterflies without bees";
both describe splits that are now interior to core and cannot be observed from outside it.

Since every surviving jar depends only on core, the coverage that means anything is one
boot per jar plus both endpoints.

| configuration | port | game directory | loads |
| --- | --- | --- | --- |
| `coreOnlyServer` | 25566 | `run/boot-core` | `forestry` |
| `butterfliesServer` | 25567 | `run/boot-butterflies` | `forestry`, `forestrybutterflies` |
| `farmsServer` | 25568 | `run/boot-farms` | `forestry`, `forestryfarms` |
| `mailServer` | 25569 | `run/boot-mail` | `forestry`, `forestrymail` |
| `allJarsServer` | 25570 | `run/boot-all` | all four |

Five boots where there were four. Farms and mail get their first dedicated boot. Neither
has ever been booted alone, and `folderOwners` names no data folder for either, so they
are the two most likely to hold an ownership bug that no run has yet been in a position to
surface.

The distinct-port rule from phase 9b is retained and extended: a surviving forked JVM can
only ever block a rerun of its own configuration, never the next one in sequence.

### Regeneration order

`ownership.json` is written by `runData` from the live feature registry.
`resource-owners.json` is derived from `ownership.json` by `generateResourceOwners`. The
order is therefore fixed:

1. Edit `MODULE_TO_JAR`.
2. `./gradlew runData`, which rewrites `src/generated/ownership.json`.
3. `./gradlew generateResourceOwners`, which rewrites `src/generated/resource-owners.json`.
4. Commit both.

Running step 3 before step 2 builds the resource map from stale ownership. That does not
fail at generation time. It fails later as a mis-partitioned jar, which is expensive to
trace back to its cause.

## Out of scope

**Publishing.** The `publishing`, `curseforge` and `modrinth` blocks ship only `jar`, the
core artifact. The content jars have never been published under any id. Renaming does not
change that, and wiring up multi-artifact publishing is separate work.

**Java package renames.** `forestry.lepidopterology` and `forestry.agriculture` keep their
names. Renaming them would rewrite imports across every source set, the datagen providers
and the GameTest suite, for consistency alone.

**Migration for existing installs.** The six-jar split has never been released. No world
or pack manifest names `forestry_apiculture` or any of its siblings, so no compatibility
shim is needed.

**Historical spec documents.** `2026-07-30-feature-package-reorg-design.md` and
`2026-08-02-phase-9b-six-jars.md` describe the six-jar state as built. They are records of
what was done, not descriptions of the current target, and are left as written. This
document's header names what it supersedes.

## Verification

In order. Each step's failure mode is distinct, and running them out of order hides the
cheap failures behind the expensive ones.

1. `./gradlew compileJava compileButterfliesJava compileFarmsJava compileMailJava` - the
   compiler is what enforces the dependency graph since phase 9a.
2. `./gradlew runData`, then diff `src/generated/ownership.json`. Roughly 4,381 ids move
   to `core` and nothing else changes owner. An id landing anywhere unexpected means a
   `MODULE_TO_JAR` value was missed.
3. `./gradlew generateResourceOwners`, then diff `src/generated/resource-owners.json`.
4. `./gradlew checkJarPartition` - no jar carries another's package tree, and every split
   file's per-jar variant is present. This is the step the `contentPackages` map exists to
   keep honest.
5. The five boot configurations, each a real server boot with a real datapack load.
6. `./gradlew runGameTestServer` - green as of 2026-07-28 and expected to stay green;
   nothing here touches genome or creative tab membership.

## Risks

**The silent half of `checkJarPartition`.** Addressed by `contentPackages`, but it is worth
stating plainly: if that map is wrong or incomplete, the core-side leak check reports a
clean jar rather than failing. The step 4 output naming three jars and a nonzero split
variant count is the evidence that it ran against real package names.

**Ownership regenerated from a stale source.** Addressed by the fixed regeneration order
above.

**Farms and mail booting alone for the first time.** This is a risk being deliberately
taken on rather than avoided. If either has a latent ownership bug, this is the change that
surfaces it, and surfacing it during a rename is cheaper than surfacing it after release.
