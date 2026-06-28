# Mutations as Recipes — Design Spec (Stage 2)

**Status:** approved (design), pending spec review
**Branch:** `allele-foundation` (continues from the Stage 1 allele foundation)
**Date:** 2026-06-28

## Goal

Make genetics **mutations** (two parent species → offspring species, with a chance and conditions) data-driven by
modeling each mutation as a custom **`RecipeType`** loaded from datapack JSON. Datapack authors add/remove mutations
exactly like recipes; mutations gain datapack reload + automatic client sync for free; and JEI discovers them through
the recipe system instead of a bespoke iteration of `IMutationManager`.

Species themselves remain **code-registered** (Stage 1 unchanged) — only mutations become data here. Trees/butterflies
mutate the same way as bees. This is a **clean break**: existing worlds' breeding-research progress is preserved (it is
keyed by parent+result species ids, which are unchanged), but there is no save datafixer for any new data shapes.

## Locked decisions

1. **Conditions = dispatch-codec registry.** `IMutationCondition` gains a `MapCodec` + `StreamCodec`, dispatched on a
   `type` field via a registry (vanilla loot/recipe-condition style). The seven built-in conditions each get a codec and
   a registered type id; addons register their own. Semantics unchanged (each condition is a chance modifier).
2. **One `RecipeType` per species type** — `forestry:bee_mutation`, `forestry:tree_mutation`,
   `forestry:butterfly_mutation` — mirroring today's per-type `MutationManager`s and per-type JEI categories. The recipe
   class and serializer are shared/generic to avoid literal 3× duplication; the three `RecipeType`/`RecipeSerializer`
   instances stay distinct.
3. **Datagen, drop the runtime API.** Built-in mutations move to generated recipe JSON via a `MutationProvider` that
   reuses the familiar builder shape. The runtime `IMutationsRegistration` / `IMutationBuilder` /
   `ISpeciesBuilder.addMutations` API is removed (consistent with Stage 1's builders→datagen move).

## Background (current system, for reference)

- `IMutation<S>` holds `firstParent`/`secondParent`/`result` (`ISpecies`), `chance` (float),
  `List<IMutationCondition>`, and `ImmutableList<AllelePair<?>>` result-allele overrides
  (`core/genetics/mutations/Mutation.java`).
- `IMutationManager<S>` indexes mutations `from`/`into`/`combinations` for a species type
  (`core/genetics/MutationManager.java`); stored on the species type, accessed via `ISpeciesType.getMutations()`.
- Conditions: 7 implementations under `core/genetics/mutations/MutationCondition*.java`
  (temperature, humidity, biome tag, daytime, time-limited/date-range, requires-resource block, cave). Each implements
  `IMutationCondition.modifyChance(level, pos, mutation, g1, g2, climate, currentChance)` + `getDescription()`.
- Built-ins are registered today via `ISpeciesBuilder.addMutations(Consumer<IMutationsRegistration>)` →
  `MutationsRegistration` / `MutationBuilder` (`apiimpl/plugin/MutationsRegistration.java`); built in
  `SpeciesRegistration.buildAll()` → `MutationManager`, returned from `handleSpeciesRegistration` and stored via
  `onSpeciesRegistered`.
- Runtime: `SpeciesUtil.mutateSpecies` calls `speciesType.getMutations().getCombinationsShuffled(...)` then
  `Mutation.getChance(...)`; on success returns `mutation.getResultAlleles()`.
- **Result-allele overrides** (`addSpecialAllele`) have **no callers** in the codebase — kept as an optional, default-
  empty field for parity/extensibility, but absent from all built-in recipes.
- Research: `BreedingTracker` keys researched/discovered mutations by the string
  `"<firstParent>:<secondParent>:<result>"` (`getMutationString`), i.e. by species ids — **recipe-id-independent**, so
  it needs no change.
- JEI today (`apiculture/compat/MutationsRecipeCategory` + `MutationRecipe` POJO + `ApicultureJeiPlugin`) builds an
  ad-hoc, unregistered `RecipeType` per species type and feeds it by iterating `IMutationManager.getAllMutations()`.

## Architecture

### Component 1 — `MutationRecipe` (the recipe)

A single class `forestry.core.genetics.mutations.MutationRecipe implements Recipe<RecipeInput>` (package may differ;
keep near the existing mutation code). It is a **data holder**, never grid-crafted:

- Fields: `ResourceLocation speciesTypeId`, `ResourceLocation firstParentId`, `ResourceLocation secondParentId`,
  `ResourceLocation resultId`, `float chance`, `List<IMutationCondition> conditions`,
  `Map<ResourceLocation, Allele<?>> resultAlleles` (optional, default empty; keyed by chromosome id).
- `Recipe` overrides: `matches`/`assemble` return false/empty (never called); `canCraftInDimensions` false;
  `getResultItem` empty; `getType()` returns the `RecipeType` whose species type matches `speciesTypeId`;
  `getSerializer()` returns the shared serializer.
- A `toMutation(ISpeciesType<S,?> type, ImmutableMap<ResourceLocation, S> speciesLookup)` adapter builds a runtime
  `Mutation<S>` (reusing the existing `Mutation` class) so downstream breeding/JEI logic is unchanged. Resolution of
  parent/result species and result-allele chromosome lookup uses the species type's karyotype.

**Codec / StreamCodec** (in the serializer): `MapCodec`/`StreamCodec` via `RecordCodecBuilder` over the fields above.
`resultAlleles` uses a karyotype-aware codec (dispatched per chromosome, like the Stage 1 genome codec) resolved from
`speciesTypeId`; since no built-in uses it, it is `optionalFieldOf` with an empty default. Conditions use the condition
codec from Component 2.

### Component 2 — Condition codec registry

- `IMutationCondition` gains: `MapCodec<? extends IMutationCondition> codec()` (instance → its type codec) and a static
  dispatch `Codec<IMutationCondition>` + `StreamCodec` built from a registry of condition **types**.
- A condition-type registry (a vanilla `Registry` keyed by `ResourceLocation`, or a simple
  `Codec.dispatch`-backed `Map`), exposed for addons (e.g. via `IGeneticRegistration` or a dedicated registration hook).
  Built-in types registered under `forestry:temperature`, `forestry:humidity`, `forestry:biome`, `forestry:daytime`,
  `forestry:time_range`, `forestry:requires_resource`, `forestry:cave`.
- Each of the 7 `MutationCondition*` classes becomes a record (or gains a static `MapCodec`) encoding its parameters
  (temperature/humidity = two enum ordinals/names; biome = `TagKey<Biome>`; time_range = 4 ints; requires_resource =
  list of `BlockState`; daytime = boolean; cave = no fields). `StreamCodec`s mirror the map codecs.
- Existing `IMutationBuilder` convenience methods (`restrictTemperature`, …) are reproduced on the **datagen** builder
  (Component 4), constructing these condition records.

### Component 3 — Loading, indexing & reload

Mutations are decoupled from species registration:

- `ISpeciesType.handleSpeciesRegistration` returns **species only** (drop the mutations half of the `Pair`);
  `onSpeciesRegistered` drops its `IMutationManager` parameter. `buildAll()` stops building mutations.
- A new core **reload hook** rebuilds each species type's `MutationManager` from the recipe manager:
  `manager.byType(beeMutationType)` → `MutationRecipe.toMutation(...)` per recipe → `new MutationManager<>(...)` →
  assign to the species type via a new internal `SpeciesType.setMutations(IMutationManager)` (replaces the old
  registration-time assignment).
  - **Server:** rebuild on server start and on `/reload` (datapack reload). The recipe manager is reached via the
    server; the rebuild iterates the registered species types from `IGeneticManager`.
  - **Client:** rebuild on NeoForge `RecipesUpdatedEvent` (recipes already sync to the client automatically), so the
    Portable Analyzer and JEI see the current set.
- `ISpeciesType.getMutations()` returns an **empty** `MutationManager` before the first rebuild (instead of throwing),
  so early access is safe; by the time breeding/JEI run, recipes are loaded.
- Research keying is **unchanged** (parent+result species id string), so breeding-tracker state survives reloads and is
  independent of recipe ids.

### Component 4 — Datagen & API removal

- New `MutationProvider` (a `RecipeProvider` or standalone `DataProvider`) exposing a builder
  `add(speciesType, first, second, result, chance)` with `.temperature(...)`, `.humidity(...)`, `.biome(...)`,
  `.day()/.night()`, `.timeRange(...)`, `.requiresResource(...)`, `.condition(IMutationCondition)` — the same surface as
  today's `IMutationBuilder` — emitting `data/forestry/recipe/<bee|tree|butterfly>_mutation/<id>.json`. Recipe ids are
  derived from the result species + a disambiguating suffix.
- All built-in `.addMutations(...)` blocks in `DefaultBeeSpecies` / `DefaultTreeSpecies` / `DefaultButterflySpecies`
  move into `MutationProvider`.
- **Removed:** `IMutationsRegistration`, `IMutationBuilder`, `ISpeciesBuilder.addMutations`,
  `apiimpl/plugin/MutationsRegistration`, and the mutation-building paths in `SpeciesRegistration.buildAll` /
  `ISpeciesBuilder.buildMutations`.

### Component 5 — JEI

The three per-type JEI categories remain, but each is fed from its registered `RecipeType` via
`RecipeManager.byType(...)` rather than `IMutationManager.getAllMutations()`. The displayed object is the
`MutationRecipe` (adapted through `toMutation` for the existing display logic), removing the ad-hoc unregistered
`RecipeType` and the manual mutation iteration in `ApicultureJeiPlugin` / the tree & butterfly JEI plugins.

## Data flow

1. **Authoring:** datapack JSON under `data/<ns>/recipe/<type>_mutation/*.json` (built-ins emitted by `MutationProvider`).
2. **Load/reload (server):** `RecipeManager` parses recipes → reload hook builds per-type `MutationManager`s → assigned
   to species types.
3. **Sync:** vanilla recipe sync sends recipes to clients on login/reload → client `RecipesUpdatedEvent` rebuilds
   client-side `MutationManager`s.
4. **Breeding:** `SpeciesUtil.mutateSpecies` → `speciesType.getMutations().getCombinationsShuffled(...)` →
   `Mutation.getChance(...)` (conditions evaluated at location/time) → offspring.
5. **JEI/Analyzer:** read the per-type recipes (JEI) / `IMutationManager` (analyzer) — both now reflect the loaded set.

## Error handling & edge cases

- **Unknown species id** in a mutation recipe (typo or removed/unregistered species): skip that recipe during index
  rebuild with a logged warning (do not crash load); it simply does not contribute a mutation. (Mirrors Stage 1's
  permissive, fail-soft posture for data.)
- **Recipe references a species of the wrong species type** (e.g. a bee id in `tree_mutation`): skipped + warned.
- **Empty `MutationManager`** before recipes load: `getMutations()` returns an empty manager (no throw).
- **Condition codec for an unknown `type`:** datapack parse error surfaced by the recipe loader (standard vanilla
  behavior — the offending recipe is dropped with an error in the log).
- **`resultAlleles` referencing a chromosome not in the karyotype:** dropped with a warning (permissive).

## Testing

GameTests (extend the existing `forestry/gametest` harness; run via `runGameTestServer`):

- **Index built:** after load, each species type's `MutationManager` is non-empty and contains the expected built-in
  combinations (spot-check a known bee mutation, e.g. Common from Forest×Meadows).
- **Codec round-trip:** encode a `MutationRecipe` (including at least one of each condition type) via its
  `MapCodec`→JSON and `StreamCodec`→buffer, decode, assert equal (parents/result/chance/conditions).
- **Breeding outcome:** `SpeciesUtil.mutateSpecies` for a known parent pair under satisfying conditions yields the
  expected result species; under a failing condition (e.g. wrong temperature) the chance is 0.
- **Reload:** after a simulated datapack reload, the index reflects the (re)loaded recipes.
- The Stage 1 golden-master `GenomeBaselineTest` continues to guard default genomes (mutations do not affect them).

## Out of scope (future stages)

- Data-driven **species** (Stages 3–5) — species stay code-registered here.
- A save datafixer (clean break).
- New mutation condition kinds beyond the existing seven (addons may add their own via the registry).
