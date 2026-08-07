# Migrating to the restructured ForestryCE (1.21.1)

This branch reorganizes the whole source tree and splits the mod into four jars. It touches 2594 files
and renames 1122 Java files. Nothing about the game changed: registry ids, tag ids, packet wire ids,
lang keys, creative tab ids and NBT are all byte-identical to before. What changed is where classes
live, which artifact ships them, and a handful of API signatures.

This document is written to be read by a person or fed to an LLM that is rebasing an addon or an open
PR onto the new tree. Work through it top to bottom. Sections 1 through 4 are what breaks; section 5
is the mechanical rename table that resolves most conflicts; section 8 is the step-by-step rebase.

Generate a fresh view of the diff at any time with the triage script in the repo root:

```
./diff-triage.py                                        # bucket summary
./diff-triage.py --list moved                           # pure relocations, free for rebasers
./diff-triage.py --list semantic ':!*.md' ':!*.txt'     # files with real code changes
./diff-triage.py --patch                                # the semantic diff, import churn filtered out
```

---

## 1. What did NOT change

Read this first. The most common mistake when rebasing this is "fixing" something that was never
broken.

| Thing | Status |
|---|---|
| Registry namespace | Still `forestry:` for every block, item, fluid, entity, menu, tile and recipe type, including content that now ships in `forestrybutterflies`, `forestryfarms` and `forestrymail`. A jar registering under a namespace other than its own mod id is deliberate and NeoForge 21.1.230 permits it. |
| Tag ids | Unchanged. `forestry:larch_logs`, `forestry:fireproof_larch_logs` and friends all still resolve. |
| Packet wire ids | Unchanged. The id-holder classes were split per module but every `type("...")` path string is the same, so a new client talks to an old server's protocol shape. |
| Lang keys | Unchanged. `en_us.json` is now assembled from a hand-written file plus a generated one and ships in the base jar alone, but no key was renamed. |
| Creative tab ids | Unchanged: `forestry:forestry`, `forestry:storage`, `forestry:apiculture`, `forestry:arboriculture`, `forestry:agriculture`, `forestry:mail`, `forestry:lepidopterology`. Tab *contents* are pinned by a golden-master GameTest. |
| Module ids (`ForestryModuleIds`) | Unchanged. Still `forestry:farming`, `forestry:cultivation`, `forestry:lepidopterology`, etc. The module ids did not follow the jar rename. |
| Java package roots for content | `forestry.lepidopterology`, `forestry.agriculture` and `forestry.mail` keep their package names even though their jars are named butterflies/farms/mail. The source set name and the package name deliberately diverge. |
| Save data / NBT / data components | Unchanged. |
| Maven coordinates of the published artifact | Unchanged. The `jar`, `sourcesJar`, `javadocJar` and `apiJar` publication is exactly as before. |

## 2. Build and packaging changes

### 2.1 One mod became four

| Jar | Mod id | Source set | Contents |
|---|---|---|---|
| `forestry` | `forestry` | `src/main` | Everything else: core, apiculture, arboriculture, factory/machines, energy, storage/backpacks, sorting, worktable, and the whole `forestry.api` package |
| `forestrybutterflies` | `forestrybutterflies` | `src/butterflies` | Butterflies, cocoons, the lepidopterist chain |
| `forestryfarms` | `forestryfarms` | `src/farms` | Multifarms, planters, farm logic |
| `forestrymail` | `forestrymail` | `src/mail` | Letters, post office, trade stations |
| (test only) | `forestrygametest` | `src/test` | The GameTest suite, its own mod id so partial-install boots can leave it out |

Each content jar declares a `REQUIRED` dependency on `forestry`. The three content jars are optional
at runtime: a jar loads as a mod only if it carries `neoforge.mods.toml`, so omitting the file omits
the mod, and its `IForestryPlugin` is simply never on the service path.

Note the mod ids carry no underscore: `forestryfarms`, not `forestry_farms`.

### 2.2 Consequences for an addon build script

* **Compiling against core is unchanged.** `forestry.api` still ships in the base jar and the base jar
  is still the published Maven artifact.
* **The three content jars are build artifacts, not Maven artifacts.** They are produced by the
  `butterfliesJar`, `farmsJar` and `mailJar` tasks but are not attached to `components.java`, so they
  are not in the publication. An addon that needs to compile against, say, `forestry.agriculture`
  internals has to consume the jar from a build directory or a distribution page, not from
  `modmaven.dev`. Prefer coding against `forestry.api.agriculture` in the base jar instead.
* **`forestry.api.agriculture` lives in the base jar, not the farms jar.** So does
  `forestry.api.lepidopterology` and `forestry.api.mail`. The API is undivided. Only implementations
  moved out.
* **A soft dependency on a content jar is now meaningful.** If your addon only cares about farming,
  depend on `forestryfarms` in your `neoforge.mods.toml` and guard with
  `IForestryApi.INSTANCE.getFarmingManager().isLoaded()` (see 3.3).

### 2.3 Source sets, for anyone with an open PR against the repo itself

```
src/main/java          -> forestry (core) jar; contains forestry.api
src/butterflies/java   -> forestrybutterflies jar; package forestry.lepidopterology
src/farms/java         -> forestryfarms jar;      package forestry.agriculture
src/mail/java          -> forestrymail jar;       package forestry.mail
src/test/java          -> forestrygametest
```

Each content source set has `sourceSets.main.output` on its compile classpath and nothing else. There
are no cross-content edges: butterflies cannot see farms, farms cannot see mail. **The compiler now
enforces the module graph**; the old import-scanning gradle gates that stood in for it are gone. If
your PR made a core class reference a content class, it will now fail to compile rather than fail a
check task.

Four verification tasks run under `check` and will reject a sloppy rebase:

| Task | Rejects |
|---|---|
| `checkApiBoundary` | Any `import forestry.<not api>` inside `src/main/java/forestry/api` |
| `checkCoreLayers` | `forestry.core.engine` importing `forestry.core.content` (the platform half is reported but not gated) |
| `checkResourceFqcn` | A resource file (Patchouli template, `kubejs.plugins.txt`, `META-INF/services/*`) naming a Java class that does not exist |
| `checkJarPartition` | A built jar carrying another jar's classes, referencing ModKit, stranding a `.mcmeta`, or shipping a second `en_us.json` |

### 2.4 Generated resources moved

```
src/generated/resources                -> the forestry jar   (unchanged path)
src/generated/resources_butterflies    -> forestrybutterflies
src/generated/resources_farms          -> forestryfarms
src/generated/resources_mail           -> forestrymail
```

567 generated files moved out of `src/generated/resources`. A PR that regenerated data will conflict
here; regenerate rather than merge (`./gradlew runData`).

Hand-written resources followed the same split: 95 files moved from `src/main/resources` into the
three content resource roots.

---

## 3. API breaking changes

These are the changes that will not be fixed by a package rename. Every one of them is a compile
error on the old code.

### 3.1 Flower types moved out of apiculture

Bees and butterflies both carry a `flower_type` chromosome, so a butterfly must resolve flower types
with no apiculture code present. Flower types are now a base-level pollination concept.

Removed from `IBeeSpeciesType`:

```java
IFlowerType getFlowerType(ResourceLocation id);
IFlowerType getFlowerTypeSafe(ResourceLocation id);   // was @Nullable
```

Replaced by a new manager, reachable from `IForestryApi`:

```java
// before
IFlowerType t = SpeciesUtil.BEE_TYPE.get().getFlowerType(id);

// after
IFlowerTypeManager mgr = IForestryApi.INSTANCE.getFlowerTypeManager();
IFlowerType t = mgr.getFlowerType(id);        // @Nullable
IFlowerType safe = mgr.getFlowerTypeSafe(id); // falls back to the vanilla flower type, never null
Map<ResourceLocation, IFlowerType> all = mgr.getAllFlowerTypes();
```

Note the nullability flipped: the old `getFlowerType` was non-null and `getFlowerTypeSafe` was
`@Nullable`; on `IFlowerTypeManager` it is the other way round.

Registration moved too:

```java
// before
void IApicultureRegistration.registerFlowerType(ResourceLocation id, IFlowerType type);

// after
void IGeneticRegistration.registerFlowerType(ResourceLocation id, IFlowerType type);
```

`IApicultureRegistration.registerFlowerType` no longer exists. `FlowerTypeTypes` moved from
`forestry.apiculture.genetics` to `forestry.core.engine.genetics`, and the `TagFlowerType`,
`WaterTagFlowerType` and `PhotosynthesisFlowerType` implementations moved from `forestry.apiculture`
to `forestry.core.engine.genetics.flowers`. Their datapack ids are unchanged.

### 3.2 Genetic items must implement `IIndividualItem`

`IIndividualHandlerItem` moved package **and** changed behavior. It used to delegate every static
helper to `forestry.core.genetics.ItemGE`, an impl class; it now dispatches through a new API
interface, so a third-party genetic item is finally possible.

```
forestry.api.genetics.capability.IIndividualHandlerItem
  -> forestry.api.core.genetics.capability.IIndividualHandlerItem
```

New interface, which your item class must implement for the helpers to see it:

```java
package forestry.api.core.genetics;

public interface IIndividualItem {
    @Nullable IIndividual getIndividualFromComponent(ItemStack stack);
    ILifeStage getLifeStage();
    ISpeciesType<?, ?> getSpeciesType();
}
```

`IIndividualHandlerItem.getIndividual`, `getLifeStage`, `getSpeciesType`, `ifPresent`, `filter`,
`isIndividual` and `getSpecies` all now return nothing for a stack whose item does not implement
`IIndividualItem`. If you subclassed `ItemGE` you get this for free; if you wrote your own item and
relied on the capability, you must implement `IIndividualItem`.

`hasIndividual(ItemStack)` now has a documented meaning distinct from `isIndividual`: it tests only
for the presence of the genome data component, which a wildcard villager-trade bee has without
resolving to an individual.

`getGenome(ItemStack)` reads the component directly, via a new API accessor:

```java
DataComponentType<IGenome> IGeneticManager.genomeComponent();
```

### 3.3 `IFarmingManager.isLoaded()`

The farms jar is optional, so base ships `FakeFarmingManager` and `IForestryApi.getFarmingManager()`
never returns null. Guard on:

```java
if (IForestryApi.INSTANCE.getFarmingManager().isLoaded()) { ... }
```

`isLoaded()` defaults to `true`, so any other manager implementation you may have keeps compiling.
This is the only manager with the method: every other manager comes from base and is always real.

### 3.4 Life stage enums resolve their item by id

`BeeLifeStage`, `TreeLifeStage` and `ButterflyLifeStage` used to hold an `ItemLike` handed in by the
module's item registry. That is a class-init cycle once the modules are separate jars, so they now
hold a `ResourceLocation` and resolve through `BuiltInRegistries.ITEM` on demand.

```java
// constructor changed from (ItemLike) to (ResourceLocation) -- affects nobody outside the enum
// new accessor:
ResourceLocation itemId();
// unchanged:
Item getItemForm();   // now BuiltInRegistries.ITEM.get(itemId())
```

The item ids are the pre-existing registry ids, so nothing at runtime changed:
`forestry:drone_bee`, `princess_bee`, `queen_bee`, `larvae_bee`, `tree_sapling`, `tree_pollen`,
`butterfly`, `butterfly_serum`, `caterpillar`, `cocoon`.

If your code called `getItemForm()` during mod construction it now returns air instead of the item;
call it after registration.

### 3.5 Bee hive particles go through the client manager

```java
// before
ParticleRender.addBeeHiveFX(housing, genome, flowerPositions);

// after
IForestryClientApi.INSTANCE.getBeeManager().addBeeHiveParticles(housing, genome, flowerPositions);
```

New method on `IBeeClientManager`:

```java
void addBeeHiveParticles(IBeeHousing housing, IGenome genome, List<BlockPos> flowerPositions);
```

`forestry.core.render.ParticleRender` still exists (as `forestry.core.platform.render.ParticleRender`)
but the bee-specific entry point left it. This is the default implementation of
`IBeeEffect.doFX`, so an addon bee effect that did not override `doFX` needs no change.

### 3.6 The 84 wood tag aliases are gone from `ForestryTags`

Every `ForestryTags.Blocks.<WOOD>_LOGS` and `ForestryTags.Items.<WOOD>_LOGS` constant was deleted (42
of each). They were aliases for `ForestryWoodType.X.blockTag`, and `ForestryWoodType` is an
implementation class that the API may no longer name.

Replacements, in order of preference:

```java
// 1. through the API, if you have an IWoodType
ITreeManager mgr = IForestryApi.INSTANCE.getTreeManager();
TagKey<Block> logs = mgr.getLogBlockTag(woodType, false);
TagKey<Item>  item = mgr.getLogItemTag(woodType, false);

// 2. by tag id, which is unchanged
TagKey<Block> larch = ForestryTags.blockTag("larch_logs");
TagKey<Item>  larchItem = ForestryTags.itemTag("larch_logs");
```

Three of the deleted constants did **not** match their tag path, and a mechanical
`LARCH_LOGS -> "larch_logs"` rewrite gets them wrong:

| Deleted constant | Actual tag |
|---|---|
| `ACACIA_DESERT_LOGS` | `forestry:camelthorn_logs` |
| `CITRUS_LOGS` | `forestry:lemon_logs` |
| `GIGANTEUM_LOGS` | `forestry:giant_sequoia_logs` |

### 3.7 Two new tags in `ForestryTags`

Both exist so a jar can name content it cannot import.

```java
ForestryTags.Blocks.TREE_SAPLINGS   // forestry:tree_saplings
ForestryTags.Items.GENETIC_SAMPLES  // forestry:genetic_samples
```

`TREE_SAPLINGS` is what the Arboretum farm logic (farms jar) now matches instead of naming the
arboriculture sapling block. Without the arboriculture content the tag resolves empty and the
Arboretum farms only vanilla saplings. `GENETIC_SAMPLES` is what the genetic filter names instead of
naming a bee or butterfly item.

### 3.8 New default methods on `IForestryModule`

All defaulted, so an existing module keeps compiling. They exist because a module now has to own the
wiring that a single monolithic plugin used to do for it.

```java
default void registerReloadListeners(AddReloadListenerEvent event) {}
default void syncDatapack(OnDatapackSyncEvent event) {}
default void installManagers() {}
default void installClientManagers(IClientRegistration registration) {}
```

All four are called in module load order, so a module may depend on anything it names in
`getModuleDependencies`.

### 3.9 Types promoted into the API

These were implementation classes that the API referenced or that a content jar needed. They are new
API surface; if you were referencing the impl class, switch to the API one.

| Was | Now |
|---|---|
| `forestry.core.inventory.IInventoryAdapter` | `forestry.api.core.IInventoryAdapter` |
| `forestry.core.tiles.IFilterSlotDelegate` | `forestry.api.core.IFilterSlotDelegate` |
| `forestry.apiculture.genetics.IGeneticTooltipProvider` | `forestry.api.core.genetics.alyzer.IGeneticTooltipProvider` |
| `forestry.mail.IWatchable` | `forestry.api.mail.IWatchable` |
| `forestry.apiculture.VillageHive` | `forestry.api.apiculture.hives.VillageHive` |
| `forestry.core.genetics.alleles.Chromosome` | `forestry.api.core.genetics.alleles.Chromosome` |
| `forestry.core.genetics.alleles.ChromosomeFactory` | `forestry.api.core.genetics.alleles.ChromosomeFactory` |

`GeneticTranslationKeys` is new in `forestry.api.core.genetics`; it holds the
`createTranslationKey(String, ResourceLocation, ResourceLocation)` logic that used to sit in core.
`ILeafSprite.MISSING` is a new constant for the no-sprite-registered case.

### 3.10 Chromosome holders no longer route through `SpeciesUtil`

`BeeChromosomes`, `TreeChromosomes` and `ButterflyChromosomes` are API classes and could no longer
name `forestry.core.utils.SpeciesUtil`. They now memoize their own `Lazy<...SpeciesType>` through
`IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(...)`. Behavior is identical. `SpeciesUtil`
itself still exists at `forestry.core.platform.util.SpeciesUtil` for core code.

`BeeChromosomes.FLOWER_TYPE` now resolves through `IFlowerTypeManager` rather than the bee species
type -- see 3.1. It is the same chromosome object `ButterflyChromosomes.FLOWER_TYPE` refers to.

---

## 4. Classes that were removed or absorbed

| Removed | What to use |
|---|---|
| `forestry.plugin.DefaultForestryPlugin` | Split into `forestry.core.plugin.DefaultForestryPlugin`, `forestry.apiculture.plugin.ApicultureForestryPlugin`, `forestry.arboriculture.plugin.ArboricultureForestryPlugin`, `forestry.agriculture.plugin.AgricultureForestryPlugin` (farms) and `forestry.lepidopterology.plugin.LepidopterologyForestryPlugin` (butterflies) |
| `forestry.plugin.client.DefaultForestryClientRegistration` | Renamed to `forestry.arboriculture.client.plugin.ArboricultureClientRegistration`; the bee and butterfly halves moved to `ApicultureClientRegistration` and `LepidopterologyClientRegistration` |
| `forestry.core.EventHandlerCore` | Renamed to `forestry.apiculture.EventHandlerApiculture`; the residual core half is `forestry.core.platform.EventHandlerCore` |
| `forestry.core.genetics.GeneticsReloadHandler` | Split: `forestry.core.engine.genetics.GeneticsReloadHandler` plus per-module `ApicultureReloadHandler`, `ArboricultureReloadHandler`, `LepidopterologyReloadHandler` |
| `forestry.core.network.PacketIdClient` | Core's half is `forestry.core.platform.network.PacketIdClient`; the rest moved to `ApiculturePacketIds`, `ArboriculturePacketIds`, `LepidopterologyPacketIds`, `MailPacketIds`. Wire ids unchanged. |
| `forestry.core.tab.ForestryCreativeTabs` | Core's half is `forestry.core.platform.tab.ForestryCreativeTabs`; the rest is `ApicultureCreativeTab`, `ArboricultureCreativeTab`, `AgricultureCreativeTab`, `MailCreativeTab`, `LepidopterologyCreativeTab`. Use `ForestryCreativeTabs.tabKey(String)` to reference another module's tab for ordering. |
| `forestry.apiculture.genetics.AlyzerManager` | `forestry.apiculture.bees.genetics.AlyzerManager` |
| `forestry.apiculture.gui.IContainerBeeHousing` | `forestry.apiculture.bees.IContainerBeeHousing` |
| `forestry.core.tiles.IPowerHandler` | `forestry.core.platform.tile.IPowerHandler` |
| `forestry.core.worldgen.ForestryBiomeModifier` | `forestry.core.platform.worldgen.ForestryBiomeModifier` (still one biome modifier, still registered as `forestry:forestry`) |
| `forestry.farming.multiblock.IFarmControllerInternal` | `forestry.agriculture.multifarm.multiblock.IFarmControllerInternal` (farms jar) |
| `forestry.farming.features.FarmingMenuTypes`, `forestry.cultivation.features.CultivationMenuTypes` | Both under `forestry.agriculture.features` (farms jar) |
| `FactoryBlocks`, `EnergyBlocks`, `SortingBlocks`, `SortingMenuTypes`, `WorktableBlocks`, `WorktableMenus` | Moved under `forestry.core.content.<machines\|energy\|sorting\|worktable>.features` |
| `TileMill` / `RenderMill` | `forestry.core.content.machines.TileMill` / `RenderMill` |
| `forestry.compat.*` | Dissolved into `forestry.core.platform.compat.*` |

A new `forestry.apiimpl.fake` package holds the no-op implementations: `FakeFarmingManager` and
`FakeBeekeepingLogic`.

---

## 5. Package relocation rules

1094 classes were renamed. 724 of them are covered by the prefix rewrites below, with no per-class
lookup needed. The remaining 370 sit in packages that fanned out to more than one destination and are
listed individually in section 6. The "Jar" column says which artifact the class ends up in.

### 5.1 Packages that moved

| Old package | New package | Jar |
|---|---|---|
| `forestry.api.circuits` | `forestry.api.core.circuits` | core |
| `forestry.api.climate` | `forestry.api.core.climate` | core |
| `forestry.api.farming` | `forestry.api.agriculture` | core |
| `forestry.api.fuels` | `forestry.api.core.machines.fuels` | core |
| `forestry.api.genetics` | `forestry.api.core.genetics` | core |
| `forestry.api.genetics.alleles` | `forestry.api.core.genetics.alleles` | core |
| `forestry.api.genetics.alyzer` | `forestry.api.core.genetics.alyzer` | core |
| `forestry.api.genetics.filter` | `forestry.api.core.genetics.filter` | core |
| `forestry.api.genetics.pollen` | `forestry.api.core.genetics.pollen` | core |
| `forestry.api.multiblock` | `forestry.api.core.multiblock` | core |
| `forestry.api.recipes` | `forestry.api.core.machines` | core |
| `forestry.api.storage` | `forestry.api.core.backpacks` | core |
| `forestry.api.util` | `forestry.api.core.util` | core |
| `forestry.apiculture.genetics.effects` | `forestry.apiculture.bees.genetics.effects` | core |
| `forestry.apiculture.multiblock` | `forestry.apiculture.alveary.multiblock` | core |
| `forestry.apiculture.villagers` | `forestry.apiculture.apiarist.villagers` | core |
| `forestry.apiimpl.client.plugin` | `forestry.arboriculture.client.plugin` | core |
| `forestry.arboriculture.capabilities` | `forestry.core.platform.capabilities` | core |
| `forestry.arboriculture.genetics` | `forestry.arboriculture.trees.genetics` | core |
| `forestry.compat` | `forestry.core.platform.compat` | core |
| `forestry.compat.curios` | `forestry.core.platform.compat.curios` | core |
| `forestry.compat.curios.client` | `forestry.core.platform.compat.curios.client` | core |
| `forestry.compat.jei` | `forestry.core.platform.compat.jei` | core |
| `forestry.compat.kubejs` | `forestry.core.platform.compat.kubejs` | core |
| `forestry.compat.kubejs.apiculture` | `forestry.core.platform.compat.kubejs.apiculture` | core |
| `forestry.compat.kubejs.event` | `forestry.core.platform.compat.kubejs.event` | core |
| `forestry.compat.patchouli.component` | `forestry.core.platform.compat.patchouli.component` | core |
| `forestry.compat.patchouli.processor` | `forestry.core.platform.compat.patchouli.processor` | core |
| `forestry.core.circuits` | `forestry.core.engine.circuits` | core |
| `forestry.core.client` | `forestry.core.platform.client` | core |
| `forestry.core.client.compat` | `forestry.core.platform.client.compat` | core |
| `forestry.core.climate` | `forestry.core.engine.climate` | core |
| `forestry.core.commands` | `forestry.core.platform.commands` | core |
| `forestry.core.config` | `forestry.core.platform.config` | core |
| `forestry.core.damage` | `forestry.core.platform.damage` | core |
| `forestry.core.data` | `forestry.lepidopterology.data` | butterflies |
| `forestry.core.entities` | `forestry.core.platform.entities` | core |
| `forestry.core.errors` | `forestry.core.platform.errors` | core |
| `forestry.core.fluids` | `forestry.core.platform.fluids` | core |
| `forestry.core.genetics` | `forestry.core.engine.genetics` | core |
| `forestry.core.genetics.alleles` | `forestry.api.core.genetics.alleles` | core |
| `forestry.core.genetics.mutations` | `forestry.core.engine.genetics.mutations` | core |
| `forestry.core.genetics.root` | `forestry.core.engine.genetics.root` | core |
| `forestry.core.gui.buttons` | `forestry.core.platform.gui.buttons` | core |
| `forestry.core.gui.ledgers` | `forestry.core.platform.gui.ledgers` | core |
| `forestry.core.gui.slots` | `forestry.core.platform.gui.slots` | core |
| `forestry.core.gui.widgets` | `forestry.core.platform.gui.widgets` | core |
| `forestry.core.inventory` | `forestry.core.platform.inventory` | core |
| `forestry.core.inventory.watchers` | `forestry.core.platform.inventory.watchers` | core |
| `forestry.core.inventory.wrappers` | `forestry.core.platform.inventory.wrappers` | core |
| `forestry.core.loot` | `forestry.core.platform.loot` | core |
| `forestry.core.models` | `forestry.core.platform.models` | core |
| `forestry.core.models.baker` | `forestry.core.platform.models.baker` | core |
| `forestry.core.multiblock` | `forestry.core.platform.multiblock` | core |
| `forestry.core.multiblock.pattern` | `forestry.core.platform.multiblock.pattern` | core |
| `forestry.core.network` | `forestry.core.platform.network` | core |
| `forestry.core.owner` | `forestry.core.platform.owner` | core |
| `forestry.core.particles` | `forestry.core.platform.particles` | core |
| `forestry.core.recipes` | `forestry.core.platform.recipes` | core |
| `forestry.core.recipes.jei` | `forestry.core.platform.recipes.jei` | core |
| `forestry.core.registration` | `forestry.core.platform.villager` | core |
| `forestry.core.utils.datastructures` | `forestry.core.platform.util.datastructures` | core |
| `forestry.cultivation.blocks` | `forestry.agriculture.planter.blocks` | farms |
| `forestry.cultivation.features` | `forestry.agriculture.features` | farms |
| `forestry.cultivation.gui` | `forestry.agriculture.planter.gui` | farms |
| `forestry.cultivation.gui.widgets` | `forestry.agriculture.planter.gui.widgets` | farms |
| `forestry.cultivation.inventory` | `forestry.agriculture.planter.inventory` | farms |
| `forestry.cultivation.items` | `forestry.agriculture.planter.items` | farms |
| `forestry.cultivation.proxy` | `forestry.agriculture.client` | farms |
| `forestry.cultivation.tiles` | `forestry.agriculture.planter.tiles` | farms |
| `forestry.energy` | `forestry.core.content.energy` | core |
| `forestry.energy.blocks` | `forestry.core.content.energy.blocks` | core |
| `forestry.energy.client` | `forestry.core.content.energy.client` | core |
| `forestry.energy.features` | `forestry.core.content.energy.features` | core |
| `forestry.energy.inventory` | `forestry.core.content.energy.inventory` | core |
| `forestry.energy.menu` | `forestry.core.content.energy.menu` | core |
| `forestry.energy.screen` | `forestry.core.content.energy.screen` | core |
| `forestry.energy.tiles` | `forestry.core.content.energy.tiles` | core |
| `forestry.factory` | `forestry.core.content.machines` | core |
| `forestry.factory.blocks` | `forestry.core.content.machines.blocks` | core |
| `forestry.factory.circuits` | `forestry.core.content.machines.circuits` | core |
| `forestry.factory.client` | `forestry.core.content.machines.client` | core |
| `forestry.factory.features` | `forestry.core.content.machines.features` | core |
| `forestry.factory.gui` | `forestry.core.content.machines.gui` | core |
| `forestry.factory.inventory` | `forestry.core.content.machines.inventory` | core |
| `forestry.factory.network.packets` | `forestry.core.content.machines.network.packets` | core |
| `forestry.factory.recipes` | `forestry.core.content.machines.recipes` | core |
| `forestry.factory.recipes.jei` | `forestry.core.content.machines.recipes.jei` | core |
| `forestry.factory.recipes.jei.carpenter` | `forestry.core.content.machines.recipes.jei.carpenter` | core |
| `forestry.factory.recipes.jei.centrifuge` | `forestry.core.content.machines.recipes.jei.centrifuge` | core |
| `forestry.factory.recipes.jei.fabricator` | `forestry.core.content.machines.recipes.jei.fabricator` | core |
| `forestry.factory.recipes.jei.fermenter` | `forestry.core.content.machines.recipes.jei.fermenter` | core |
| `forestry.factory.recipes.jei.moistener` | `forestry.core.content.machines.recipes.jei.moistener` | core |
| `forestry.factory.recipes.jei.rainmaker` | `forestry.core.content.machines.recipes.jei.rainmaker` | core |
| `forestry.factory.recipes.jei.squeezer` | `forestry.core.content.machines.recipes.jei.squeezer` | core |
| `forestry.factory.recipes.jei.still` | `forestry.core.content.machines.recipes.jei.still` | core |
| `forestry.factory.tiles` | `forestry.core.content.machines.tiles` | core |
| `forestry.farming.blocks` | `forestry.agriculture.multifarm.blocks` | farms |
| `forestry.farming.circuits` | `forestry.agriculture.multifarm.circuits` | farms |
| `forestry.farming.client` | `forestry.agriculture.client` | farms |
| `forestry.farming.compat` | `forestry.agriculture.compat` | farms |
| `forestry.farming.features` | `forestry.agriculture.features` | farms |
| `forestry.farming.gui` | `forestry.agriculture.multifarm.gui` | farms |
| `forestry.farming.items` | `forestry.agriculture.multifarm.items` | farms |
| `forestry.farming.logic` | `forestry.agriculture.farmlogic` | farms |
| `forestry.farming.logic.crops` | `forestry.agriculture.farmlogic.crops` | farms |
| `forestry.farming.logic.farmables` | `forestry.agriculture.farmlogic.farmables` | farms |
| `forestry.farming.multiblock` | `forestry.agriculture.multifarm.multiblock` | farms |
| `forestry.farming.tiles` | `forestry.agriculture.multifarm.tiles` | farms |
| `forestry.lepidopterology.blocks` | `forestry.lepidopterology.cocoons` | butterflies |
| `forestry.lepidopterology.genetics` | `forestry.lepidopterology.butterflies.genetics` | butterflies |
| `forestry.lepidopterology.items` | `forestry.lepidopterology.butterflies` | butterflies |
| `forestry.lepidopterology.tiles` | `forestry.lepidopterology.cocoons` | butterflies |
| `forestry.mail.postalstates` | `forestry.mail.letters` | mail |
| `forestry.modules.features` | `forestry.core.platform.registration` | core |
| `forestry.sorting` | `forestry.core.content.sorting` | core |
| `forestry.sorting.blocks` | `forestry.core.content.sorting.blocks` | core |
| `forestry.sorting.client` | `forestry.core.content.sorting.client` | core |
| `forestry.sorting.features` | `forestry.core.content.sorting.features` | core |
| `forestry.sorting.gui` | `forestry.core.content.sorting.gui` | core |
| `forestry.sorting.gui.widgets` | `forestry.core.content.sorting.gui.widgets` | core |
| `forestry.sorting.inventory` | `forestry.core.content.sorting.inventory` | core |
| `forestry.sorting.network.packets` | `forestry.core.content.sorting.network.packets` | core |
| `forestry.sorting.tiles` | `forestry.core.content.sorting.tiles` | core |
| `forestry.storage` | `forestry.core.content.backpacks` | core |
| `forestry.storage.client` | `forestry.core.content.backpacks.client` | core |
| `forestry.storage.compat` | `forestry.core.content.backpacks.compat` | core |
| `forestry.storage.features` | `forestry.core.content.backpacks.features` | core |
| `forestry.storage.gui` | `forestry.core.content.backpacks.gui` | core |
| `forestry.storage.inventory` | `forestry.core.content.backpacks.inventory` | core |
| `forestry.storage.items` | `forestry.core.content.backpacks.items` | core |
| `forestry.worktable` | `forestry.core.content.worktable` | core |
| `forestry.worktable.blocks` | `forestry.core.content.worktable.blocks` | core |
| `forestry.worktable.client` | `forestry.core.content.worktable.client` | core |
| `forestry.worktable.compat` | `forestry.core.content.worktable.compat` | core |
| `forestry.worktable.features` | `forestry.core.content.worktable.features` | core |
| `forestry.worktable.inventory` | `forestry.core.content.worktable.inventory` | core |
| `forestry.worktable.network.packets` | `forestry.core.content.worktable.network.packets` | core |
| `forestry.worktable.recipes` | `forestry.core.content.worktable.recipes` | core |
| `forestry.worktable.screens` | `forestry.core.content.worktable.screens` | core |
| `forestry.worktable.screens.widgets` | `forestry.core.content.worktable.screens.widgets` | core |
| `forestry.worktable.tiles` | `forestry.core.content.worktable.tiles` | core |

One caveat: `forestry.apiimpl.client.plugin` is a partial move. Only `ClientHelper` went to
`forestry.arboriculture.client.plugin`; `ClientRegistration` stayed put.

### 5.2 Packages that kept their name but changed jar

These need no import rewrite at all. They matter only if you have an open PR against the repo: the
file now lives under a different source root, so `git` will report a rename and the class can no
longer see anything outside `src/main`.

| Package | Jar |
|---|---|
| `forestry.lepidopterology.commands` | butterflies |
| `forestry.lepidopterology.compat` | butterflies |
| `forestry.lepidopterology.entities` | butterflies |
| `forestry.lepidopterology.features` | butterflies |
| `forestry.lepidopterology.proxy` | butterflies |
| `forestry.lepidopterology.recipe` | butterflies |
| `forestry.lepidopterology.render` | butterflies |
| `forestry.mail.blocks` | mail |
| `forestry.mail.carriers` | mail |
| `forestry.mail.carriers.players` | mail |
| `forestry.mail.carriers.trading` | mail |
| `forestry.mail.client` | mail |
| `forestry.mail.commands` | mail |
| `forestry.mail.compat` | mail |
| `forestry.mail.features` | mail |
| `forestry.mail.gui` | mail |
| `forestry.mail.inventory` | mail |
| `forestry.mail.network.packets` | mail |

---

## 6. Packages that split

These 31 old packages fanned out to more than one destination, so a prefix rewrite gets them wrong.
370 classes, listed individually. This is where the reorg carries meaning: `forestry.apiculture` was
one bag of everything bee-related and is now split by feature into `bees`, `apiary`, `alveary`,
`beehouse`, `hives` and `apiarist`, and the pieces that were never about bees (the scoop, the wax
items, the naturalist chest, flower types) left apiculture entirely.

**`forestry.apiculture`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `AlvearyBeeModifier` | `forestry.apiculture.alveary.AlvearyBeeModifier` | core |
| `ApiaristAI` | `forestry.apiculture.apiarist.ApiaristAI` | core |
| `ApiaryBeeListener` | `forestry.apiculture.apiary.ApiaryBeeListener` | core |
| `ApiaryBeeModifier` | `forestry.apiculture.apiary.ApiaryBeeModifier` | core |
| `ApicultureFilterRule` | `forestry.apiculture.bees.ApicultureFilterRule` | core |
| `ApicultureFilterRuleType` | `forestry.apiculture.bees.ApicultureFilterRuleType` | core |
| `ArmorApiaristHelper` | `forestry.apiculture.apiarist.ArmorApiaristHelper` | core |
| `BeeHousingListener` | `forestry.apiculture.bees.BeeHousingListener` | core |
| `BeeHousingModifier` | `forestry.apiculture.bees.BeeHousingModifier` | core |
| `BeeSpecies` | `forestry.apiculture.bees.BeeSpecies` | core |
| `BeehouseBeeModifier` | `forestry.apiculture.beehouse.BeehouseBeeModifier` | core |
| `BeekeepingLogic` | `forestry.apiculture.bees.BeekeepingLogic` | core |
| `CathemeralActivityType` | `forestry.apiculture.bees.CathemeralActivityType` | core |
| `CrepuscularActivityType` | `forestry.apiculture.bees.CrepuscularActivityType` | core |
| `FakeBeekeepingLogic` | `forestry.apiimpl.fake.FakeBeekeepingLogic` | core |
| `HasFlowersCache` | `forestry.apiculture.bees.HasFlowersCache` | core |
| `IApiary` | `forestry.apiculture.apiary.IApiary` | core |
| `InventoryBeeHousing` | `forestry.apiculture.bees.InventoryBeeHousing` | core |
| `PhotosynthesisFlowerType` | `forestry.core.engine.genetics.flowers.PhotosynthesisFlowerType` | core |
| `SingleActivityType` | `forestry.apiculture.bees.SingleActivityType` | core |
| `TagFlowerType` | `forestry.core.engine.genetics.flowers.TagFlowerType` | core |
| `VillageHive` | `forestry.api.apiculture.hives.VillageHive` | core |
| `WaterTagFlowerType` | `forestry.core.engine.genetics.flowers.WaterTagFlowerType` | core |
| `WorldgenBeekeepingLogic` | `forestry.apiculture.hives.WorldgenBeekeepingLogic` | core |

**`forestry.apiculture.blocks`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `BlockAlveary` | `forestry.apiculture.alveary.BlockAlveary` | core |
| `BlockApiculture` | `forestry.apiculture.apiary.BlockApiculture` | core |
| `BlockBeeHive` | `forestry.apiculture.hives.BlockBeeHive` | core |
| `BlockHiveType` | `forestry.apiculture.hives.BlockHiveType` | core |
| `BlockHoneyComb` | `forestry.apiculture.bees.BlockHoneyComb` | core |
| `BlockTypeApiculture` | `forestry.apiculture.apiary.BlockTypeApiculture` | core |
| `NaturalistChestBlockType` | `forestry.core.platform.block.NaturalistChestBlockType` | core |

**`forestry.apiculture.genetics`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `ApiaristTracker` | `forestry.apiculture.bees.genetics.ApiaristTracker` | core |
| `Bee` | `forestry.apiculture.bees.genetics.Bee` | core |
| `BeeEffectManager` | `forestry.apiculture.bees.genetics.BeeEffectManager` | core |
| `BeeSpeciesDefinition` | `forestry.apiculture.bees.genetics.BeeSpeciesDefinition` | core |
| `BeeSpeciesManager` | `forestry.apiculture.bees.genetics.BeeSpeciesManager` | core |
| `BeeSpeciesProjector` | `forestry.apiculture.bees.genetics.BeeSpeciesProjector` | core |
| `BeeSpeciesType` | `forestry.apiculture.bees.genetics.BeeSpeciesType` | core |
| `DefaultBeeJubilance` | `forestry.apiculture.bees.genetics.DefaultBeeJubilance` | core |
| `DefinitionBeeSpeciesBuilder` | `forestry.apiculture.bees.genetics.DefinitionBeeSpeciesBuilder` | core |
| `FireworkProduct` | `forestry.apiculture.bees.genetics.FireworkProduct` | core |
| `FlowerTypeManager` | `forestry.core.engine.genetics.FlowerTypeManager` | core |
| `FlowerTypeTypes` | `forestry.core.engine.genetics.FlowerTypeTypes` | core |
| `HermitBeeJubilance` | `forestry.apiculture.bees.genetics.HermitBeeJubilance` | core |
| `HiveDrop` | `forestry.apiculture.bees.genetics.HiveDrop` | core |
| `IGeneticTooltipProvider` | `forestry.api.core.genetics.alyzer.IGeneticTooltipProvider` | core |
| `JubilanceFactory` | `forestry.apiculture.bees.genetics.JubilanceFactory` | core |
| `RequiresResourceBeeJubilance` | `forestry.apiculture.bees.genetics.RequiresResourceBeeJubilance` | core |
| `TaxonManager` | `forestry.core.engine.genetics.TaxonManager` | core |

**`forestry.apiculture.gui`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `ContainerAlveary` | `forestry.apiculture.alveary.ContainerAlveary` | core |
| `ContainerAlvearyHygroregulator` | `forestry.apiculture.alveary.ContainerAlvearyHygroregulator` | core |
| `ContainerAlvearySieve` | `forestry.apiculture.alveary.ContainerAlvearySieve` | core |
| `ContainerAlvearySwarmer` | `forestry.apiculture.alveary.ContainerAlvearySwarmer` | core |
| `ContainerBeeHelper` | `forestry.apiculture.bees.ContainerBeeHelper` | core |
| `ContainerBeeHousing` | `forestry.apiculture.bees.ContainerBeeHousing` | core |
| `GuiAlveary` | `forestry.apiculture.alveary.GuiAlveary` | core |
| `GuiAlvearyHygroregulator` | `forestry.apiculture.alveary.GuiAlvearyHygroregulator` | core |
| `GuiAlvearySieve` | `forestry.apiculture.alveary.GuiAlvearySieve` | core |
| `GuiAlvearySwarmer` | `forestry.apiculture.alveary.GuiAlvearySwarmer` | core |
| `GuiBeeHousing` | `forestry.apiculture.bees.GuiBeeHousing` | core |
| `IGuiBeeHousingDelegate` | `forestry.apiculture.bees.IGuiBeeHousingDelegate` | core |

**`forestry.apiculture.inventory`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `IApiaryInventory` | `forestry.apiculture.apiary.IApiaryInventory` | core |
| `InventoryAlvearySieve` | `forestry.apiculture.alveary.InventoryAlvearySieve` | core |
| `InventoryApiary` | `forestry.apiculture.apiary.InventoryApiary` | core |
| `InventoryHygroregulator` | `forestry.apiculture.alveary.InventoryHygroregulator` | core |
| `InventorySwarmer` | `forestry.apiculture.alveary.InventorySwarmer` | core |

**`forestry.apiculture.items`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `EnumHoneyComb` | `forestry.apiculture.bees.EnumHoneyComb` | core |
| `EnumPollenCluster` | `forestry.apiculture.bees.EnumPollenCluster` | core |
| `EnumPropolis` | `forestry.apiculture.bees.EnumPropolis` | core |
| `ItemAmbrosia` | `forestry.apiculture.bees.ItemAmbrosia` | core |
| `ItemArmorApiarist` | `forestry.apiculture.apiarist.ItemArmorApiarist` | core |
| `ItemBeeGE` | `forestry.apiculture.bees.ItemBeeGE` | core |
| `ItemBeesWax` | `forestry.core.content.resources.ItemBeesWax` | core |
| `ItemBlockHoneyComb` | `forestry.apiculture.bees.ItemBlockHoneyComb` | core |
| `ItemCreativeHiveFrame` | `forestry.apiculture.apiary.ItemCreativeHiveFrame` | core |
| `ItemHiveFrame` | `forestry.apiculture.apiary.ItemHiveFrame` | core |
| `ItemHoneyComb` | `forestry.apiculture.bees.ItemHoneyComb` | core |
| `ItemPollenCluster` | `forestry.apiculture.bees.ItemPollenCluster` | core |
| `ItemPropolis` | `forestry.apiculture.bees.ItemPropolis` | core |
| `ItemRefractoryWax` | `forestry.core.content.resources.ItemRefractoryWax` | core |
| `ItemScoop` | `forestry.core.content.tools.ItemScoop` | core |
| `ItemSmoker` | `forestry.apiculture.apiarist.ItemSmoker` | core |

**`forestry.apiculture.tiles`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `FakeBeeHousingInventory` | `forestry.apiculture.bees.FakeBeeHousingInventory` | core |
| `HiveBeeHousingInventory` | `forestry.apiculture.hives.HiveBeeHousingInventory` | core |
| `TileApiary` | `forestry.apiculture.apiary.TileApiary` | core |
| `TileBeeHouse` | `forestry.apiculture.beehouse.TileBeeHouse` | core |
| `TileBeeHousingBase` | `forestry.apiculture.bees.TileBeeHousingBase` | core |
| `TileHive` | `forestry.apiculture.hives.TileHive` | core |

**`forestry.apiimpl.client`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `BeeClientManager` | `forestry.apiculture.client.BeeClientManager` | core |
| `TreeClientManager` | `forestry.arboriculture.client.TreeClientManager` | core |

**`forestry.apiimpl.plugin`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `ApicultureRegistration` | `forestry.apiculture.plugin.ApicultureRegistration` | core |
| `ArboricultureRegistration` | `forestry.arboriculture.plugin.ArboricultureRegistration` | core |
| `BeeSpeciesBuilder` | `forestry.apiculture.plugin.BeeSpeciesBuilder` | core |
| `ButterflySpeciesBuilder` | `forestry.lepidopterology.plugin.ButterflySpeciesBuilder` | butterflies |
| `FarmTypeBuilder` | `forestry.agriculture.plugin.FarmTypeBuilder` | farms |
| `FarmingRegistration` | `forestry.agriculture.plugin.FarmingRegistration` | farms |
| `HiveBuilder` | `forestry.apiculture.plugin.HiveBuilder` | core |
| `LepidopterologyRegistration` | `forestry.lepidopterology.plugin.LepidopterologyRegistration` | butterflies |
| `TreeSpeciesBuilder` | `forestry.arboriculture.plugin.TreeSpeciesBuilder` | core |
| `WindfallFarmableBuilder` | `forestry.agriculture.plugin.WindfallFarmableBuilder` | farms |

**`forestry.arboriculture`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `ArboricultureFilterRuleType` | `forestry.arboriculture.trees.ArboricultureFilterRuleType` | core |
| `DummyFruit` | `forestry.arboriculture.fruit.DummyFruit` | core |
| `ForestryWoodType` | `forestry.arboriculture.wood.ForestryWoodType` | core |
| `Fruit` | `forestry.arboriculture.fruit.Fruit` | core |
| `IWoodTyped` | `forestry.arboriculture.wood.IWoodTyped` | core |
| `PodFruit` | `forestry.arboriculture.fruit.PodFruit` | core |
| `RipeningFruit` | `forestry.arboriculture.fruit.RipeningFruit` | core |
| `TreeManager` | `forestry.arboriculture.trees.TreeManager` | core |
| `TreeSpecies` | `forestry.arboriculture.trees.TreeSpecies` | core |
| `VanillaWoodType` | `forestry.arboriculture.wood.VanillaWoodType` | core |
| `WoodAccess` | `forestry.arboriculture.wood.WoodAccess` | core |
| `WoodHelper` | `forestry.arboriculture.wood.WoodHelper` | core |

**`forestry.arboriculture.blocks`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `BlockAbstractLeaves` | `forestry.arboriculture.leaves.BlockAbstractLeaves` | core |
| `BlockAsh` | `forestry.arboriculture.charcoal.BlockAsh` | core |
| `BlockCharcoal` | `forestry.arboriculture.charcoal.BlockCharcoal` | core |
| `BlockDecorativeLeaves` | `forestry.arboriculture.leaves.BlockDecorativeLeaves` | core |
| `BlockDefaultLeaves` | `forestry.arboriculture.leaves.BlockDefaultLeaves` | core |
| `BlockDefaultLeavesFruit` | `forestry.arboriculture.leaves.BlockDefaultLeavesFruit` | core |
| `BlockExtendedLeaves` | `forestry.arboriculture.leaves.BlockExtendedLeaves` | core |
| `BlockForestryButton` | `forestry.arboriculture.wood.BlockForestryButton` | core |
| `BlockForestryDoor` | `forestry.arboriculture.wood.BlockForestryDoor` | core |
| `BlockForestryFence` | `forestry.arboriculture.wood.BlockForestryFence` | core |
| `BlockForestryFenceGate` | `forestry.arboriculture.wood.BlockForestryFenceGate` | core |
| `BlockForestryHangingSign` | `forestry.arboriculture.wood.BlockForestryHangingSign` | core |
| `BlockForestryLeaves` | `forestry.arboriculture.leaves.BlockForestryLeaves` | core |
| `BlockForestryLog` | `forestry.arboriculture.wood.BlockForestryLog` | core |
| `BlockForestryPlank` | `forestry.arboriculture.wood.BlockForestryPlank` | core |
| `BlockForestryPressurePlate` | `forestry.arboriculture.wood.BlockForestryPressurePlate` | core |
| `BlockForestrySlab` | `forestry.arboriculture.wood.BlockForestrySlab` | core |
| `BlockForestryStairs` | `forestry.arboriculture.wood.BlockForestryStairs` | core |
| `BlockForestryStandingSign` | `forestry.arboriculture.wood.BlockForestryStandingSign` | core |
| `BlockForestryTrapdoor` | `forestry.arboriculture.wood.BlockForestryTrapdoor` | core |
| `BlockForestryWallHangingSign` | `forestry.arboriculture.wood.BlockForestryWallHangingSign` | core |
| `BlockForestryWallSign` | `forestry.arboriculture.wood.BlockForestryWallSign` | core |
| `BlockFruitPod` | `forestry.arboriculture.fruit.BlockFruitPod` | core |
| `BlockSapling` | `forestry.arboriculture.sapling.BlockSapling` | core |
| `DecorativeLogPileBlock` | `forestry.arboriculture.charcoal.DecorativeLogPileBlock` | core |
| `ForestryLeafType` | `forestry.arboriculture.leaves.ForestryLeafType` | core |
| `ForestryPodType` | `forestry.arboriculture.fruit.ForestryPodType` | core |
| `ILeafTypeBlock` | `forestry.arboriculture.leaves.ILeafTypeBlock` | core |
| `LogPileBlock` | `forestry.arboriculture.charcoal.LogPileBlock` | core |

**`forestry.arboriculture.items`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `ForestryBoatDispenserBehavior` | `forestry.arboriculture.wood.ForestryBoatDispenserBehavior` | core |
| `GrafterItem` | `forestry.arboriculture.trees.GrafterItem` | core |
| `ItemBlockDecorativeLeaves` | `forestry.arboriculture.leaves.ItemBlockDecorativeLeaves` | core |
| `ItemBlockDefaultLeaves` | `forestry.arboriculture.leaves.ItemBlockDefaultLeaves` | core |
| `ItemBlockHangingSign` | `forestry.arboriculture.wood.ItemBlockHangingSign` | core |
| `ItemBlockLeaves` | `forestry.arboriculture.leaves.ItemBlockLeaves` | core |
| `ItemBlockSign` | `forestry.arboriculture.wood.ItemBlockSign` | core |
| `ItemBlockWood` | `forestry.arboriculture.wood.ItemBlockWood` | core |
| `ItemBlockWoodDoor` | `forestry.arboriculture.wood.ItemBlockWoodDoor` | core |
| `ItemBlockWoodSlab` | `forestry.arboriculture.wood.ItemBlockWoodSlab` | core |
| `ItemForestryBoat` | `forestry.arboriculture.wood.ItemForestryBoat` | core |
| `TreeItem` | `forestry.arboriculture.trees.TreeItem` | core |

**`forestry.arboriculture.tiles`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `TileFruitPod` | `forestry.arboriculture.fruit.TileFruitPod` | core |
| `TileLeaves` | `forestry.arboriculture.leaves.TileLeaves` | core |
| `TileSapling` | `forestry.arboriculture.sapling.TileSapling` | core |
| `TileTreeContainer` | `forestry.arboriculture.trees.TileTreeContainer` | core |

**`forestry.core`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `ClientsideCode` | `forestry.core.platform.client.ClientsideCode` | core |
| `EventHandlerCore` | `forestry.apiculture.EventHandlerApiculture` | core |
| `FluidProductTypes` | `forestry.core.platform.fluids.FluidProductTypes` | core |
| `ForestryColors` | `forestry.core.platform.client.ForestryColors` | core |
| `PickupHandlerCore` | `forestry.core.platform.PickupHandlerCore` | core |
| `TranslationKeys` | `forestry.core.platform.util.TranslationKeys` | core |

**`forestry.core.blocks`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `BlockBase` | `forestry.core.platform.block.BlockBase` | core |
| `BlockBogEarth` | `forestry.core.content.soil.BlockBogEarth` | core |
| `BlockCore` | `forestry.core.platform.block.BlockCore` | core |
| `BlockForestry` | `forestry.core.platform.block.BlockForestry` | core |
| `BlockHumus` | `forestry.core.content.soil.BlockHumus` | core |
| `BlockResourceStorage` | `forestry.core.content.resources.BlockResourceStorage` | core |
| `BlockStructure` | `forestry.core.platform.block.BlockStructure` | core |
| `BlockTesr` | `forestry.core.platform.block.BlockTesr` | core |
| `BlockTypeCoreTesr` | `forestry.core.platform.block.BlockTypeCoreTesr` | core |
| `EnumResourceType` | `forestry.core.content.resources.EnumResourceType` | core |
| `IBlockType` | `forestry.core.platform.block.IBlockType` | core |
| `IColoredBlock` | `forestry.core.platform.block.IColoredBlock` | core |
| `IMachineProperties` | `forestry.core.platform.block.IMachineProperties` | core |
| `IShapeProvider` | `forestry.core.platform.block.IShapeProvider` | core |
| `ISimpleShapeProvider` | `forestry.core.platform.block.ISimpleShapeProvider` | core |
| `MachineProperties` | `forestry.core.platform.block.MachineProperties` | core |
| `TileStreamUpdateTracker` | `forestry.core.platform.block.TileStreamUpdateTracker` | core |

**`forestry.core.gui`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `ContainerAnalyzer` | `forestry.core.content.analyzer.ContainerAnalyzer` | core |
| `ContainerEscritoire` | `forestry.core.content.escritoire.ContainerEscritoire` | core |
| `ContainerForestry` | `forestry.core.platform.gui.ContainerForestry` | core |
| `ContainerItemInventory` | `forestry.core.platform.gui.ContainerItemInventory` | core |
| `ContainerLiquidTanks` | `forestry.core.platform.gui.ContainerLiquidTanks` | core |
| `ContainerLiquidTanksHelper` | `forestry.core.platform.gui.ContainerLiquidTanksHelper` | core |
| `ContainerLiquidTanksSocketed` | `forestry.core.platform.gui.ContainerLiquidTanksSocketed` | core |
| `ContainerNaturalistInventory` | `forestry.core.platform.gui.ContainerNaturalistInventory` | core |
| `ContainerSocketed` | `forestry.core.platform.gui.ContainerSocketed` | core |
| `ContainerSocketedHelper` | `forestry.core.platform.gui.ContainerSocketedHelper` | core |
| `ContainerTile` | `forestry.core.platform.gui.ContainerTile` | core |
| `CycleTimer` | `forestry.core.platform.gui.CycleTimer` | core |
| `Drawable` | `forestry.core.platform.gui.Drawable` | core |
| `DummyMenu` | `forestry.core.platform.gui.DummyMenu` | core |
| `GuiAnalyzer` | `forestry.core.content.analyzer.GuiAnalyzer` | core |
| `GuiEscritoire` | `forestry.core.content.escritoire.GuiEscritoire` | core |
| `GuiForestry` | `forestry.core.platform.gui.GuiForestry` | core |
| `GuiForestryTitled` | `forestry.core.platform.gui.GuiForestryTitled` | core |
| `GuiNaturalistInventory` | `forestry.core.platform.gui.GuiNaturalistInventory` | core |
| `GuiTextBox` | `forestry.core.platform.gui.GuiTextBox` | core |
| `GuiUtil` | `forestry.core.platform.gui.GuiUtil` | core |
| `IContainerCrafting` | `forestry.core.platform.gui.IContainerCrafting` | core |
| `IContainerLiquidTanks` | `forestry.core.platform.gui.IContainerLiquidTanks` | core |
| `IContainerSocketed` | `forestry.core.platform.gui.IContainerSocketed` | core |
| `IGuiSelectable` | `forestry.core.platform.gui.IGuiSelectable` | core |
| `IGuiSizable` | `forestry.core.platform.gui.IGuiSizable` | core |
| `INaturalistMenu` | `forestry.core.platform.gui.INaturalistMenu` | core |
| `IPagedInventory` | `forestry.core.platform.gui.IPagedInventory` | core |
| `PortableAnalyzerMenu` | `forestry.core.content.analyzer.PortableAnalyzerMenu` | core |
| `PortableAnalyzerScreen` | `forestry.core.content.analyzer.PortableAnalyzerScreen` | core |
| `TextLayoutHelper` | `forestry.core.platform.gui.TextLayoutHelper` | core |

**`forestry.core.items`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `ForestersManualItem` | `forestry.core.content.tools.ForestersManualItem` | core |
| `HasRemnants` | `forestry.core.platform.item.HasRemnants` | core |
| `ItemAssemblyKit` | `forestry.core.content.resources.ItemAssemblyKit` | core |
| `ItemBlockForestry` | `forestry.core.platform.item.ItemBlockForestry` | core |
| `ItemBlockTesr` | `forestry.core.platform.item.ItemBlockTesr` | core |
| `ItemCraftingMaterial` | `forestry.core.content.resources.ItemCraftingMaterial` | core |
| `ItemElectronTube` | `forestry.core.content.resources.ItemElectronTube` | core |
| `ItemFertilizer` | `forestry.core.content.resources.ItemFertilizer` | core |
| `ItemFluidContainerForestry` | `forestry.core.platform.item.ItemFluidContainerForestry` | core |
| `ItemForestry` | `forestry.core.platform.item.ItemForestry` | core |
| `ItemForestryFood` | `forestry.core.platform.item.ItemForestryFood` | core |
| `ItemFruit` | `forestry.core.platform.item.ItemFruit` | core |
| `ItemOverlay` | `forestry.core.platform.item.ItemOverlay` | core |
| `ItemPipette` | `forestry.core.content.tools.ItemPipette` | core |
| `ItemSpectacles` | `forestry.core.content.tools.ItemSpectacles` | core |
| `ItemWrench` | `forestry.core.content.tools.ItemWrench` | core |
| `PortableAnalyzerItem` | `forestry.core.content.analyzer.PortableAnalyzerItem` | core |
| `SolderingIronItem` | `forestry.core.content.tools.SolderingIronItem` | core |
| `WithScreenItem` | `forestry.core.platform.item.WithScreenItem` | core |

**`forestry.core.items.definitions`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `DrinkProperties` | `forestry.core.platform.item.DrinkProperties` | core |
| `EnumContainerType` | `forestry.core.platform.item.EnumContainerType` | core |
| `EnumCraftingMaterial` | `forestry.core.content.resources.EnumCraftingMaterial` | core |
| `EnumElectronTube` | `forestry.core.content.resources.EnumElectronTube` | core |
| `FluidHandlerItemForestry` | `forestry.core.platform.item.FluidHandlerItemForestry` | core |
| `IColoredItem` | `forestry.core.platform.item.IColoredItem` | core |
| `ICraftingPlan` | `forestry.core.platform.item.ICraftingPlan` | core |
| `ToolTier` | `forestry.core.content.tools.ToolTier` | core |

**`forestry.core.network.packets`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `BeeEffectSyncPacket` | `forestry.apiculture.network.packets.BeeEffectSyncPacket` | core |
| `BeeSpeciesSyncPacket` | `forestry.apiculture.network.packets.BeeSpeciesSyncPacket` | core |
| `ButterflySpeciesSyncPacket` | `forestry.lepidopterology.network.ButterflySpeciesSyncPacket` | butterflies |
| `FlowerTypeSyncPacket` | `forestry.core.platform.network.packets.FlowerTypeSyncPacket` | core |
| `PacketActiveUpdate` | `forestry.core.platform.network.packets.PacketActiveUpdate` | core |
| `PacketChipsetClick` | `forestry.core.platform.network.packets.PacketChipsetClick` | core |
| `PacketErrorUpdate` | `forestry.core.platform.network.packets.PacketErrorUpdate` | core |
| `PacketGenomeTrackerSync` | `forestry.core.platform.network.packets.PacketGenomeTrackerSync` | core |
| `PacketGuiEnergy` | `forestry.core.platform.network.packets.PacketGuiEnergy` | core |
| `PacketGuiLayoutSelect` | `forestry.core.platform.network.packets.PacketGuiLayoutSelect` | core |
| `PacketGuiSelectRequest` | `forestry.core.platform.network.packets.PacketGuiSelectRequest` | core |
| `PacketGuiStream` | `forestry.core.platform.network.packets.PacketGuiStream` | core |
| `PacketItemStackDisplay` | `forestry.core.platform.network.packets.PacketItemStackDisplay` | core |
| `PacketPipetteClick` | `forestry.core.platform.network.packets.PacketPipetteClick` | core |
| `PacketRefractoryWax` | `forestry.core.platform.network.packets.PacketRefractoryWax` | core |
| `PacketSocketUpdate` | `forestry.core.platform.network.packets.PacketSocketUpdate` | core |
| `PacketSolderingIronClick` | `forestry.core.platform.network.packets.PacketSolderingIronClick` | core |
| `PacketTankLevelUpdate` | `forestry.core.platform.network.packets.PacketTankLevelUpdate` | core |
| `PacketTileStream` | `forestry.core.platform.network.packets.PacketTileStream` | core |
| `RecipeCachePacket` | `forestry.core.platform.network.packets.RecipeCachePacket` | core |
| `TaxonSyncPacket` | `forestry.core.platform.network.packets.TaxonSyncPacket` | core |
| `TreeSpeciesSyncPacket` | `forestry.arboriculture.network.TreeSpeciesSyncPacket` | core |

**`forestry.core.render`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `ColourProperties` | `forestry.core.platform.render.ColourProperties` | core |
| `EnumTankLevel` | `forestry.core.platform.render.EnumTankLevel` | core |
| `ForestryBewlr` | `forestry.core.platform.render.ForestryBewlr` | core |
| `ForestryModelLayers` | `forestry.core.platform.render.ForestryModelLayers` | core |
| `ForestrySpriteUploader` | `forestry.core.platform.render.ForestrySpriteUploader` | core |
| `ForestryTextureManager` | `forestry.core.platform.render.ForestryTextureManager` | core |
| `ParticleRender` | `forestry.core.platform.render.ParticleRender` | core |
| `RenderAnalyzer` | `forestry.core.platform.render.RenderAnalyzer` | core |
| `RenderEngine` | `forestry.core.platform.render.RenderEngine` | core |
| `RenderEscritoire` | `forestry.core.platform.render.RenderEscritoire` | core |
| `RenderMachine` | `forestry.core.platform.render.RenderMachine` | core |
| `RenderMill` | `forestry.core.content.machines.RenderMill` | core |
| `RenderNaturalistChest` | `forestry.core.platform.render.RenderNaturalistChest` | core |
| `TankRenderInfo` | `forestry.core.platform.render.TankRenderInfo` | core |

**`forestry.core.tiles`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `AdjacentTileCache` | `forestry.core.platform.tile.AdjacentTileCache` | core |
| `EscritoireGame` | `forestry.core.content.escritoire.EscritoireGame` | core |
| `EscritoireGameBoard` | `forestry.core.content.escritoire.EscritoireGameBoard` | core |
| `EscritoireGameToken` | `forestry.core.content.escritoire.EscritoireGameToken` | core |
| `EscritoireTextSource` | `forestry.core.content.escritoire.EscritoireTextSource` | core |
| `IActivatable` | `forestry.core.platform.tile.IActivatable` | core |
| `IFilterSlotDelegate` | `forestry.api.core.IFilterSlotDelegate` | core |
| `IForestryTicker` | `forestry.core.platform.tile.IForestryTicker` | core |
| `IItemStackDisplay` | `forestry.core.platform.tile.IItemStackDisplay` | core |
| `ILiquidTankTile` | `forestry.core.platform.tile.ILiquidTankTile` | core |
| `IRenderableTile` | `forestry.core.platform.tile.IRenderableTile` | core |
| `ITitled` | `forestry.core.platform.tile.ITitled` | core |
| `TemperatureState` | `forestry.core.platform.tile.TemperatureState` | core |
| `TileAnalyzer` | `forestry.core.content.analyzer.TileAnalyzer` | core |
| `TileApiaristChest` | `forestry.core.platform.tile.TileApiaristChest` | core |
| `TileArboristChest` | `forestry.core.platform.tile.TileArboristChest` | core |
| `TileBase` | `forestry.core.platform.tile.TileBase` | core |
| `TileEscritoire` | `forestry.core.content.escritoire.TileEscritoire` | core |
| `TileForestry` | `forestry.core.platform.tile.TileForestry` | core |
| `TileLepidopteristChest` | `forestry.core.platform.tile.TileLepidopteristChest` | core |
| `TileMill` | `forestry.core.content.machines.TileMill` | core |
| `TileNaturalistChest` | `forestry.core.platform.tile.TileNaturalistChest` | core |
| `TilePowered` | `forestry.core.platform.tile.TilePowered` | core |
| `TileUtil` | `forestry.core.platform.tile.TileUtil` | core |

**`forestry.core.utils`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `BlockUtil` | `forestry.core.platform.util.BlockUtil` | core |
| `ColourUtil` | `forestry.core.platform.util.ColourUtil` | core |
| `DayMonth` | `forestry.core.platform.util.DayMonth` | core |
| `EntityUtil` | `forestry.core.platform.util.EntityUtil` | core |
| `FieldsAreNonnullByDefault` | `forestry.core.platform.util.FieldsAreNonnullByDefault` | core |
| `GeneticsUtil` | `forestry.core.platform.util.GeneticsUtil` | core |
| `InventoryUtil` | `forestry.core.platform.util.InventoryUtil` | core |
| `ItemStackUtil` | `forestry.core.platform.util.ItemStackUtil` | core |
| `ItemTooltipUtil` | `forestry.core.platform.util.ItemTooltipUtil` | core |
| `JeiUtil` | `forestry.core.platform.util.JeiUtil` | core |
| `JsonUtil` | `forestry.core.platform.util.JsonUtil` | core |
| `ModUtil` | `forestry.core.platform.util.ModUtil` | core |
| `NBTUtilForestry` | `forestry.core.platform.util.NBTUtilForestry` | core |
| `NetworkUtil` | `forestry.core.platform.util.NetworkUtil` | core |
| `PlayerUtil` | `forestry.core.platform.util.PlayerUtil` | core |
| `RecipeUtils` | `forestry.core.platform.util.RecipeUtils` | core |
| `RenderUtil` | `forestry.core.platform.util.RenderUtil` | core |
| `ResourceUtil` | `forestry.core.platform.util.ResourceUtil` | core |
| `SlotUtil` | `forestry.core.platform.util.SlotUtil` | core |
| `SoundUtil` | `forestry.core.platform.util.SoundUtil` | core |
| `SpeciesUtil` | `forestry.core.platform.util.SpeciesUtil` | core |
| `StringUtil` | `forestry.core.platform.util.StringUtil` | core |
| `TagUtil` | `forestry.core.platform.util.TagUtil` | core |
| `Translator` | `forestry.core.platform.util.Translator` | core |
| `TreeUtil` | `forestry.arboriculture.trees.TreeUtil` | core |
| `VecUtil` | `forestry.core.platform.util.VecUtil` | core |

**`forestry.core.worldgen`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `ApiaristPoolElement` | `forestry.apiculture.worldgen.ApiaristPoolElement` | core |
| `FeatureBase` | `forestry.core.platform.worldgen.FeatureBase` | core |
| `FeatureHelper` | `forestry.arboriculture.worldgen.FeatureHelper` | core |
| `VillagerJigsaw` | `forestry.apiculture.worldgen.VillagerJigsaw` | core |

**`forestry.cultivation`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `IFarmHousingInternal` | `forestry.agriculture.farmlogic.IFarmHousingInternal` | farms |
| `ModuleCultivation` | `forestry.agriculture.ModuleCultivation` | farms |

**`forestry.farming`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `FarmHelper` | `forestry.agriculture.farmlogic.FarmHelper` | farms |
| `FarmManager` | `forestry.agriculture.farmlogic.FarmManager` | farms |
| `FarmTarget` | `forestry.agriculture.farmlogic.FarmTarget` | farms |
| `FarmWorkStatus` | `forestry.agriculture.farmlogic.FarmWorkStatus` | farms |
| `FarmingManager` | `forestry.agriculture.farmlogic.FarmingManager` | farms |
| `FarmingStage` | `forestry.agriculture.farmlogic.FarmingStage` | farms |
| `ModuleFarming` | `forestry.agriculture.ModuleFarming` | farms |

**`forestry.lepidopterology`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `ButterflySpawner` | `forestry.lepidopterology.butterflies.ButterflySpawner` | butterflies |
| `ButterflySpecies` | `forestry.lepidopterology.butterflies.ButterflySpecies` | butterflies |
| `DummyButterflyEffect` | `forestry.lepidopterology.butterflies.DummyButterflyEffect` | butterflies |
| `LepidopterologyFilterRule` | `forestry.lepidopterology.butterflies.LepidopterologyFilterRule` | butterflies |
| `LepidopterologyFilterRuleType` | `forestry.lepidopterology.butterflies.LepidopterologyFilterRuleType` | butterflies |
| `ModuleLepidopterology` | `forestry.lepidopterology.ModuleLepidopterology` | butterflies |

**`forestry.mail`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `IWatchable` | `forestry.api.mail.IWatchable` | core |
| `Letter` | `forestry.mail.letters.Letter` | mail |
| `LetterProperties` | `forestry.mail.letters.LetterProperties` | mail |
| `LetterUtils` | `forestry.mail.letters.LetterUtils` | mail |
| `MailAddress` | `forestry.mail.letters.MailAddress` | mail |
| `ModuleMail` | `forestry.mail.ModuleMail` | mail |
| `PostOffice` | `forestry.mail.postoffice.PostOffice` | mail |

**`forestry.mail.items`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `CatalogueItem` | `forestry.mail.tradestation.CatalogueItem` | mail |
| `EnumStampDefinition` | `forestry.mail.letters.EnumStampDefinition` | mail |
| `ItemStamp` | `forestry.mail.letters.ItemStamp` | mail |
| `LetterItem` | `forestry.mail.letters.LetterItem` | mail |

**`forestry.mail.tiles`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `TileMailbox` | `forestry.mail.postoffice.TileMailbox` | mail |
| `TileStampCollector` | `forestry.mail.postoffice.TileStampCollector` | mail |
| `TileTrader` | `forestry.mail.tradestation.TileTrader` | mail |

**`forestry.plugin`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `BeeTaxonomy` | `forestry.core.data.taxonomy.BeeTaxonomy` | core |
| `ButterflyTaxonomy` | `forestry.lepidopterology.data.ButterflyTaxonomy` | butterflies |
| `DefaultBeeSpecies` | `forestry.apiculture.plugin.DefaultBeeSpecies` | core |
| `DefaultButterflySpecies` | `forestry.lepidopterology.plugin.DefaultButterflySpecies` | butterflies |
| `DefaultFarms` | `forestry.agriculture.plugin.DefaultFarms` | farms |
| `DefaultTreeSpecies` | `forestry.arboriculture.plugin.DefaultTreeSpecies` | core |
| `DefaultWoods` | `forestry.arboriculture.plugin.DefaultWoods` | core |
| `ForestryTaxonomy` | `forestry.core.data.taxonomy.ForestryTaxonomy` | core |
| `TreeTaxonomy` | `forestry.core.data.taxonomy.TreeTaxonomy` | core |

**`forestry.plugin.client`** splits into:

| Class | New FQN | Jar |
|---|---|---|
| `BeeAnalyzerPlugin` | `forestry.apiculture.client.plugin.BeeAnalyzerPlugin` | core |
| `ButterflyAnalyzerPlugin` | `forestry.lepidopterology.client.plugin.ButterflyAnalyzerPlugin` | butterflies |
| `DefaultForestryClientRegistration` | `forestry.arboriculture.client.plugin.ArboricultureClientRegistration` | core |
| `TreeAnalyzerPlugin` | `forestry.arboriculture.client.plugin.TreeAnalyzerPlugin` | core |

---

## 7. How to decide where a new class goes

If your PR adds a file, the old "one top-level package per module" answer is gone. The rule is now
**what can this file see**, not what it is about.

### 7.1 Which jar

Follow the references. A class goes in the jar whose compile classpath can satisfy every type it
names. Since content jars see only `src/main`, anything referenced from two content jars has to move
to base or be inverted through an API interface.

### 7.2 Which layer, inside base

```
forestry/api/            No forestry import outside forestry.api. Gated by checkApiBoundary.
forestry/core/
  platform/              No game content, no genetics. Blocks, tiles, items, inventories,
                         GUI, network, fluids, the multiblock framework, registration,
                         owner/errors/config/commands/loot/particles, render/models/client,
                         util, and the shared compat plumbing.
  engine/                Genetics (genome, karyotype, species, alleles, mutations, roots,
                         breeding trackers), climate, circuits. Must not import content.
                         Gated by checkCoreLayers.
  content/               The actual blocks and items: machines (was factory), energy,
                         backpacks (was storage), sorting, worktable, escritoire, analyzer,
                         soil, tools, resources.
  data/                  Datagen. taxonomy/ holds the four Taxonomy classes.
  features/              The core registration holders.
  plugin/                Base's own IForestryPlugin.
forestry/apiculture/     bees, apiary, alveary, beehouse, hives, apiarist, particles, worldgen
forestry/arboriculture/  trees, wood, leaves, fruit, sapling, charcoal, worldgen
forestry/apiimpl/        API implementations, the plugin manager, and fake/ for the no-ops
forestry/modules/        The module framework
```

`platform` is documented as forbidden from importing `content`, but that half is **not** gated: 26
files violate it today and `checkCoreLayers` only prints the count. Do not treat an existing
`platform -> content` import as a bug you introduced.

### 7.3 Datagen

Datagen has no source set of its own. Each jar's providers live in that jar's source set, beside the
content they generate for, so a farms provider physically cannot name a butterflies type. A content
jar attaches its providers through a service:

```java
// META-INF/services/forestry.core.data.IForestryDataProvider  (in your own source set)
public interface IForestryDataProvider {
    void gather(GatherDataEvent event);
    Set<ResourceLocation> moduleIds();   // declaring a module here is what makes this jar own it
}
```

Write to your jar's own root via `DataRoots.of(event, DataRoots.FARMS)` (or `MAIL`, `BUTTERFLIES`,
`CORE`). The data run's output folder is the parent of all four roots, which is what lets `HashCache`
purge a file that stopped being generated.

The data providers are the only thing in the repo that touches ModKit, a dev-only dependency. Every
jar task excludes those packages and `checkJarPartition` proves the exclude held.

---

## 8. Rebase recipe

For an **addon**, only sections 1 to 4 matter: fix the API breaks, then apply the section 5 and 6
rewrites to your imports. You are done.

For an **open PR against the repo**, work in this order. Doing it out of order produces conflicts
that look semantic but are not.

1. **Rebase onto the new base and take theirs for pure moves.** 623 files are byte-identical
   relocations and 1217 changed nothing but their import block. Do not hand-merge those.

   ```
   git rebase 1.21.1-restructure
   # for each conflict, first ask whether the file is in the moved/imports bucket:
   ./diff-triage.py --list moved | grep <path>
   ./diff-triage.py --list imports | grep <path>
   ```

2. **Re-apply your changes onto the file's new path.** Find where each file you touched went:

   ```
   git diff -M -C -l0 --name-status --diff-filter=R \
       $(git merge-base 1.21.1 HEAD) HEAD -- '*.java' | grep YourClassName
   # or search the tables in sections 5 and 6 of this document
   ```

3. **Rewrite your imports** with the section 5 prefix rules, then check every one of your files
   against section 6 in case it sat in a split package.

4. **Fix the API breaks** from section 3. The compiler will find all of them.

5. **Check the file is still in a jar that can see what it names.** If a base file you wrote now
   references a content class, that is a compile error, not a warning. Either move the file into the
   content jar or invert the reference through an API interface.

6. **Regenerate data rather than merging it.** `./gradlew runData`. A generated-resource conflict is
   almost never worth resolving by hand, and the output now splits across four roots.

7. **Run the gates.**

   ```
   ./gradlew check                 # checkApiBoundary, checkCoreLayers, checkResourceFqcn, checkJarPartition
   ./gradlew runGameTestServer     # the golden-master suites: creative tabs, life stage items,
                                   # data components, manager loading
   ```

   `CreativeTabBaselineTest` is a golden master over creative tab contents. If your PR adds an item to
   a tab, that baseline has to be updated deliberately, not silently.

8. **If you renamed a species,** the checklist is unchanged: lang keys in every locale, regenerate the
   genome baseline, `runData`.

### 8.1 Conflict triage cheat sheet

| Symptom | Cause | Fix |
|---|---|---|
| Conflict is entirely in the import block | The file moved packages under you | Take theirs, re-apply your import additions with the new names |
| `cannot find symbol` on a `forestry.*` type | Package rename | Section 5, then section 6 |
| `package forestry.X is not visible` / symbol not found from a content source set | You are in a content jar naming another content jar, or naming base's `content` layer from `engine` | Move the file or invert through the API |
| `registerFlowerType` not found on `IApicultureRegistration` | 3.1 | `IGeneticRegistration.registerFlowerType` |
| `getFlowerType` not found on `IBeeSpeciesType` | 3.1 | `IForestryApi.INSTANCE.getFlowerTypeManager()` |
| `IIndividualHandlerItem` helpers return null on your item | 3.2 | Implement `IIndividualItem` |
| `ForestryTags.Blocks.<WOOD>_LOGS` not found | 3.6 | `ITreeManager.getLogBlockTag` or `ForestryTags.blockTag("<wood>_logs")`, minding the three mismatched names |
| `ParticleRender.addBeeHiveFX` not found | 3.5 | `IForestryClientApi.INSTANCE.getBeeManager().addBeeHiveParticles` |
| `checkJarPartition` fails with "carries another jar's classes" | A class landed in the wrong source set | Move it; the package prefix must match `contentPackages` in `build.gradle` |
| `checkResourceFqcn` fails | A `META-INF/services` file, Patchouli template or `kubejs.plugins.txt` still names an old FQN | Update the resource, not just the Java |
