# Allele Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace ForestryCE's interned singleton allele system with inline, self-describing value genomes — the foundation for later data-driven species stages — with **no change to built-in species behavior**.

**Architecture:** An allele becomes `record Allele<V>(V value, boolean dominant)` serialized directly into the genome (no global registry, no freeze). The five chromosome subtypes collapse into one generic `IChromosome<V>` carrying the value `Codec`/`StreamCodec`, default allele, weakly-inherited flag, a `String translationKey(V)` naming rule, and (for ID-reference types) a lazy `ResourceLocation ↔ V` resolver. Genome/karyotype codecs are built per-karyotype from chromosome value codecs. Value validity becomes fully permissive.

**Tech Stack:** Java 21, NeoForge 21.1 (MC 1.21.1), Mojang `Codec`/`StreamCodec` (DataFixerUpper), Forestry GameTest harness (`./gradlew runGameTestServer`).

**Spec:** `docs/superpowers/specs/2026-06-27-allele-foundation-design.md`

---

## ⚠️ Read before starting: this is atomic type-surgery

Swapping `IAllele` (a sealed hierarchy) for `Allele<V>` (a record) **breaks compilation across the
whole genetics package and its consumers until the swap is complete.** Therefore:

- **Phases B → C → D → E do not compile individually.** Do all of them, *then* run `./gradlew compileJava`
  and iterate to green. Treat the end of Phase E as the first compile gate.
- Commit at phase boundaries anyway (even non-compiling) **on this feature branch** so review is
  granular and `git bisect`/revert stays useful; clearly mark WIP commits. The *final* commits
  (Phase E onward) must compile.
- The **golden-master test (Phase A)** and the existing test suite are the behavioral safety net:
  built-in species' default genomes must be identical before and after.
- Branch: `allele-foundation` (already created off `1.21.1`). Do not work on `1.21.1` directly.

Build/verify commands used throughout:
- Compile: `./gradlew compileJava`
- Compile tests: `./gradlew compileTestJava`
- GameTests: `./gradlew runGameTestServer`

---

## File Structure

**New files**
- `src/main/java/forestry/api/genetics/alleles/Allele.java` — `record Allele<V>(V value, boolean dominant)`.
- `src/main/java/forestry/core/genetics/alleles/Chromosome.java` — the single generic `IChromosome<V>` impl.
- `src/main/java/forestry/core/genetics/alleles/ChromosomeFactory.java` — static factories replacing `IAlleleManager` chromosome creation (or fold into `Chromosome`).
- `src/test/java/forestry/gametest/GenomeBaselineTest.java` — golden-master dump + assert.
- `src/test/java/forestry/gametest/AlleleFoundationTest.java` — codec/dominance/inheritance gametests.
- `src/test/resources/forestry/gametest/genome-baseline.txt` — committed golden-master snapshot.

**Heavily rewritten (API — `forestry/api/genetics/alleles/`)**
- `IAllele.java` → delete (replaced by `Allele<V>`); `AllelePair.java` → generic; `IChromosome.java` → generic `<V>`; `IKaryotype.java` → trim.
- **Delete:** `IBooleanAllele`, `IFloatAllele`, `IIntegerAllele`, `IValueAllele`, `IRegistryAllele`, `IBooleanChromosome`, `IFloatChromosome`, `IIntegerChromosome`, `IValueChromosome`, `IRegistryChromosome`, `IRegistryAlleleValue`, `IAlleleManager`, `IAlleleNaming`.
- **Rewrite:** `ForestryAlleles.java`, `BeeChromosomes.java`, `TreeChromosomes.java`, `ButterflyChromosomes.java`.

**Heavily rewritten (impl — `forestry/core/genetics/`)**
- **Delete:** `alleles/AlleleManager`, `alleles/AbstractChromosome`, `alleles/BooleanAllele`, `alleles/FloatAllele`, `alleles/IntegerAllele`, `alleles/ValueAllele`, `alleles/RegistryAllele`, `alleles/BooleanChromosome`, `alleles/FloatChromosome`, `alleles/IntegerChromosome`, `alleles/ValueChromosome`, `alleles/RegistryChromosome`.
- **Rewrite:** `Karyotype.java`, `Genome.java` (+ `Genome.Builder`), `IGenome` (api) accessors.

**Modified (registration / plugin)**
- `api/plugin/IGenomeBuilder.java`, `api/plugin/IKaryotypeBuilder.java`, `api/plugin/IChromosomeBuilder.java`, `core/genetics/ChromosomeBuilder.java`, `apiimpl/plugin/SpeciesRegistration.java`, `apiimpl/plugin/PluginManager.java`, `apiimpl/ForestryApiImpl.java`, `api/IForestryApi.java`.

**Modified (marker drop, 8 interfaces)**
- `api/genetics/ISpecies.java`, `api/apiculture/IFlowerType.java`, `api/apiculture/genetics/IBeeEffect.java`, `api/apiculture/IActivityType.java`, `api/arboriculture/genetics/IFruit.java`, `api/arboriculture/genetics/ITreeEffect.java`, `api/lepidopterology/IButterflyCocoon.java`, `api/lepidopterology/IButterflyEffect.java`.

**Modified (serialization + commands + consumers — compiler-guided)**
- `core/CoreDataComponents.java`, `core/commands/DiagnosticsCommand.java`, `core/commands/ModifyGenomeCommand.java`, plus the consumer files enumerated in Phase E.

---

## Phase A — Golden-master safety net (runs on CURRENT code)

### Task A1: Capture the default-genome baseline

**Files:**
- Create: `src/test/java/forestry/gametest/GenomeBaselineTest.java`
- Create (generated): `src/test/resources/forestry/gametest/genome-baseline.txt`

A representation-agnostic dump of every built-in species' default genome, so we can prove the refactor
preserves genetics. The dump must render identically under both the old and new allele models, so it
uses **canonical value strings**, not allele IDs (except for ID-reference values, where the
ResourceLocation is the canonical value and equals today's allele ID).

- [ ] **Step 1: Write the dumper gametest** (against current API)

```java
package forestry.gametest;

// Canonical dump format, one line per (species, chromosome):
//   <speciesTypeId> <speciesId> <chromosomeId> = <active> | <inactive>
// where each allele renders as "<canonicalValue>:<dominant>".
// canonicalValue rules (MUST match in the Phase G new-API rewrite):
//   - float  -> Float.toString(v)
//   - int    -> Integer.toString(v)
//   - boolean-> Boolean.toString(v)
//   - enum   -> ((Enum<?>)v).name()
//   - Vec3i  -> v.getX()+","+v.getY()+","+v.getZ()
//   - ID-reference value (species, flower type, effect, activity, fruit, cocoon)
//            -> the allele's ResourceLocation (old: allele.alleleId(); new: stored id)
// Species sorted by id; chromosomes in karyotype order.
```

Implement it as a `@GameTest`-style entry (mirroring existing tests in `src/test/java/forestry/gametest/`)
that iterates `IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypes()`, and for each species in
`type.getAllSpeciesIds()` (sorted), reads `species.getDefaultGenome().getChromosomes()` and writes the
canonical lines. In **generation mode**, write to `run/genome-baseline.txt`; in **assert mode**, compare
against the committed resource and fail with a diff on mismatch. Use a system property
(`-Dforestry.genomeBaseline=generate`) to switch.

- [ ] **Step 2: Generate the baseline on current code**

Run: `./gradlew runGameTestServer -Dforestry.genomeBaseline=generate`
Expected: PASS; `run/genome-baseline.txt` produced with one block per species type.

- [ ] **Step 3: Move the generated file into resources and commit**

```bash
mkdir -p src/test/resources/forestry/gametest
cp run/genome-baseline.txt src/test/resources/forestry/gametest/genome-baseline.txt
git add src/test/java/forestry/gametest/GenomeBaselineTest.java src/test/resources/forestry/gametest/genome-baseline.txt
git commit -m "test: capture default-genome golden-master baseline (pre-refactor)"
```

- [ ] **Step 4: Verify assert-mode passes on current code**

Run: `./gradlew runGameTestServer` (default = assert mode)
Expected: PASS (dump matches committed baseline). This proves the oracle is stable before any change.

---

## Phase B — New core API types

> No compilation expected until end of Phase E. Commit WIP at the end of each task.

### Task B1: `Allele<V>` and generic `AllelePair<V>`

**Files:**
- Create: `src/main/java/forestry/api/genetics/alleles/Allele.java`
- Modify: `src/main/java/forestry/api/genetics/alleles/AllelePair.java`
- Delete: `src/main/java/forestry/api/genetics/alleles/IAllele.java`

- [ ] **Step 1: Create `Allele<V>`**

```java
package forestry.api.genetics.alleles;

/** An inline allele: a value plus its dominance. Replaces the old interned IAllele hierarchy. */
public record Allele<V>(V value, boolean dominant) {
    public static <V> Allele<V> dominant(V value) { return new Allele<>(value, true); }
    public static <V> Allele<V> recessive(V value) { return new Allele<>(value, false); }
}
```

- [ ] **Step 2: Rewrite `AllelePair` generic over the value type, preserving dominance ordering**

Preserve the exact semantics of the current `create`/`getActiveAllele`/`getInactiveAllele`/`inheritOther`/
`inheritHaploid`/`both`/`isSameAlleles`. Remove the static `CODEC` (it used the global `IAllele.CODEC`);
the per-chromosome codec moves to `IChromosome` (Task B2).

```java
package forestry.api.genetics.alleles;

import net.minecraft.util.RandomSource;

public record AllelePair<V>(Allele<V> active, Allele<V> inactive) {
    public static <V> AllelePair<V> both(Allele<V> allele) { return new AllelePair<>(allele, allele); }

    public AllelePair<V> inheritOther(RandomSource rand, AllelePair<V> other) {
        Allele<V> a = rand.nextBoolean() ? this.active : this.inactive;
        Allele<V> b = rand.nextBoolean() ? other.active : other.inactive;
        return rand.nextBoolean() ? create(a, b) : create(b, a);
    }
    public AllelePair<V> inheritHaploid(RandomSource rand) {
        Allele<V> c = rand.nextBoolean() ? this.active : this.inactive;
        return new AllelePair<>(c, c);
    }
    public boolean isSameAlleles() { return this.active.equals(this.inactive); }

    /** Orders by dominance: first dominant wins; both-recessive -> first; both-dominant -> second inactive. */
    public static <V> AllelePair<V> create(Allele<V> first, Allele<V> second) {
        return new AllelePair<>(activeOf(first, second), inactiveOf(first, second));
    }
    private static <V> Allele<V> activeOf(Allele<V> a, Allele<V> b) {
        if (a.dominant()) return a;
        if (b.dominant()) return b;
        return a;
    }
    private static <V> Allele<V> inactiveOf(Allele<V> a, Allele<V> b) {
        if (!b.dominant()) return b;
        if (!a.dominant()) return a;
        return b;
    }
}
```

> Note the `isSameAlleles` change from `==` (old, relied on interning) to `.equals` (records). This is
> required because alleles are no longer interned; covered by the inheritance gametest.

- [ ] **Step 3: Delete `IAllele.java`.** Commit WIP.

```bash
git rm src/main/java/forestry/api/genetics/alleles/IAllele.java
git add -A && git commit -m "wip: add Allele record, make AllelePair generic over value (non-compiling)"
```

### Task B2: Generic `IChromosome<V>` + delete subtypes

**Files:**
- Modify: `src/main/java/forestry/api/genetics/alleles/IChromosome.java`
- Delete: `IBooleanAllele`, `IFloatAllele`, `IIntegerAllele`, `IValueAllele`, `IRegistryAllele`, `IBooleanChromosome`, `IFloatChromosome`, `IIntegerChromosome`, `IValueChromosome`, `IRegistryChromosome`, `IRegistryAlleleValue`, `IAlleleManager`, `IAlleleNaming` (all in `api/genetics/alleles/`).

- [ ] **Step 1: Rewrite `IChromosome<V>`**

```java
package forestry.api.genetics.alleles;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import javax.annotation.Nullable;

public interface IChromosome<V> {
    ResourceLocation id();

    /** Value codec for inline serialization in genomes. For ID-reference types this encodes a ResourceLocation. */
    Codec<V> valueCodec();
    StreamCodec<RegistryFriendlyByteBuf, V> valueStreamCodec();

    /** Karyotype-level default allele (value + dominance). */
    Allele<V> defaultAllele();

    boolean weaklyInherited();

    /** Translation key for the chromosome itself (e.g. "chromosome.forestry.speed"). */
    String chromosomeTranslationKey();

    /** Naming RULE: translation key for a value. No Component is ever stored or returned here. */
    String translationKey(V value);

    /** Non-null only for ID-reference value types (species, flower_type, effect, activity, fruit, cocoon). */
    @Nullable IValueResolver<V> resolver();

    /** Lazy id<->value resolver backed by a code registry (populated after chromosome creation). */
    interface IValueResolver<V> {
        V get(ResourceLocation id);
        ResourceLocation getId(V value);
    }
}
```

- [ ] **Step 2: Delete the subtype interfaces and the manager/naming interfaces**

```bash
git rm src/main/java/forestry/api/genetics/alleles/IBooleanAllele.java \
       src/main/java/forestry/api/genetics/alleles/IFloatAllele.java \
       src/main/java/forestry/api/genetics/alleles/IIntegerAllele.java \
       src/main/java/forestry/api/genetics/alleles/IValueAllele.java \
       src/main/java/forestry/api/genetics/alleles/IRegistryAllele.java \
       src/main/java/forestry/api/genetics/alleles/IBooleanChromosome.java \
       src/main/java/forestry/api/genetics/alleles/IFloatChromosome.java \
       src/main/java/forestry/api/genetics/alleles/IIntegerChromosome.java \
       src/main/java/forestry/api/genetics/alleles/IValueChromosome.java \
       src/main/java/forestry/api/genetics/alleles/IRegistryChromosome.java \
       src/main/java/forestry/api/genetics/alleles/IRegistryAlleleValue.java \
       src/main/java/forestry/api/genetics/alleles/IAlleleManager.java \
       src/main/java/forestry/api/genetics/alleles/IAlleleNaming.java
git add -A && git commit -m "wip: collapse chromosome subtypes into generic IChromosome<V> (non-compiling)"
```

### Task B3: Trim `IKaryotype` and collapse `IGenome` accessors

**Files:**
- Modify: `src/main/java/forestry/api/genetics/alleles/IKaryotype.java`
- Modify: `src/main/java/forestry/api/genetics/IGenome.java`

- [ ] **Step 1: `IKaryotype`** — remove `getAlleles(IChromosome)` and the value-checking `isAlleleValid`
  overload. Keep `contains`, `getChromosomes`, `getSpeciesChromosome`, `getDefaultSpecies`,
  `getGenomeCodec`, `createGenomeBuilder`, `id`, `size`, `isWeaklyInherited`. Change
  `getDefaultAlleles()` to `ImmutableMap<IChromosome<?>, Allele<?>>`, `getDefaultAllele(IChromosome<V>)`
  to return `Allele<V>`. Add `StreamCodec<RegistryFriendlyByteBuf, IGenome> getGenomeStreamCodec()`.
  Keep `Codec<IKaryotype> CODEC` (ResourceLocation → species type's karyotype) unchanged.

- [ ] **Step 2: `IGenome`** — collapse the four typed `getActiveValue`/`getInactiveValue` overloads into
  one generic each; change `getActiveAllele` to return `Allele<V>`; remove the `getActiveName`/`getInactiveName`
  `Component` helpers (naming moves to the UI edge in Task E4).

```java
default <V> V getActiveValue(IChromosome<V> chromosome) { return getActiveAllele(chromosome).value(); }
default <V> V getInactiveValue(IChromosome<V> chromosome) { return getInactiveAllele(chromosome).value(); }
default <V> Allele<V> getActiveAllele(IChromosome<V> chromosome) { return getAllelePair(chromosome).active(); }
default <V> Allele<V> getInactiveAllele(IChromosome<V> chromosome) { return getAllelePair(chromosome).inactive(); }
<V> AllelePair<V> getAllelePair(IChromosome<V> chromosome);
ImmutableMap<IChromosome<?>, AllelePair<?>> getChromosomes();
default <S extends ISpecies<?>> S getActiveSpecies() { return (S) getActiveValue(getKaryotype().getSpeciesChromosome()); }
```

Update `copyWith(Map<IChromosome<?>, Allele<?>>)` accordingly (wrap each as `AllelePair.both`).

- [ ] **Step 3: Commit WIP.**

```bash
git add -A && git commit -m "wip: trim IKaryotype valid-allele API, collapse IGenome accessors (non-compiling)"
```

### Task B4: Builder API signatures

**Files:**
- Modify: `src/main/java/forestry/api/plugin/IGenomeBuilder.java`
- Modify: `src/main/java/forestry/api/plugin/IKaryotypeBuilder.java`
- Modify: `src/main/java/forestry/api/plugin/IChromosomeBuilder.java`

- [ ] **Step 1:** `IGenomeBuilder` — change `set/setActive/setInactive(IChromosome<A>, A)` to
  `set/setActive/setInactive(IChromosome<V>, Allele<V>)`. Keep `setUnchecked`, `isEmpty`,
  `setRemainingDefault`.
- [ ] **Step 2:** `IKaryotypeBuilder` — change `set(IChromosome<A>, A)` to `set(IChromosome<V>, Allele<V>)`;
  the `IRegistryChromosome` overloads (`set(chromosome, ResourceLocation)`) become ID-reference
  `set(IChromosome<V>, Allele<V>)` using `Allele.dominant(resolver.get(id))` helpers; **remove `addAlleles`**
  references from the builder contract. `setSpecies(IChromosome<? extends ISpecies<?>>, ResourceLocation)`
  stays (default species id).
- [ ] **Step 3:** `IChromosomeBuilder` — **remove `addAlleles`**; keep `setDefault` and `setWeaklyInherited`.
  Commit WIP.

```bash
git add -A && git commit -m "wip: update genome/karyotype/chromosome builder signatures to Allele<V> (non-compiling)"
```

---

## Phase C — New impl

### Task C1: Generic `Chromosome<V>` impl + factory; delete old impl classes

**Files:**
- Create: `src/main/java/forestry/core/genetics/alleles/Chromosome.java`
- Create: `src/main/java/forestry/core/genetics/alleles/ChromosomeFactory.java`
- Delete: `AlleleManager`, `AbstractChromosome`, `BooleanAllele`, `FloatAllele`, `IntegerAllele`, `ValueAllele`, `RegistryAllele`, `BooleanChromosome`, `FloatChromosome`, `IntegerChromosome`, `ValueChromosome`, `RegistryChromosome` (all in `core/genetics/alleles/`).

- [ ] **Step 1:** Implement `Chromosome<V>` (record or final class) implementing `IChromosome<V>` with
  fields for id, value codec, stream codec, default allele, weaklyInherited, chromosome translation key,
  a `Function<V,String>` naming rule, and a nullable resolver.
- [ ] **Step 2:** Implement `ChromosomeFactory` static helpers used by the chromosome holders:
  - `floatChromosome(ResourceLocation id, Allele<Float> def, NamingRule)` (codec `Codec.FLOAT`, stream `ByteBufCodecs.FLOAT`);
  - `intChromosome(...)`, `booleanChromosome(...)`;
  - `valueChromosome(ResourceLocation id, Codec<V>, StreamCodec, Allele<V> def, NamingRule)` for enums/Vec3i;
  - `referenceChromosome(ResourceLocation id, IValueResolver<V> resolver, Allele<V> def, NamingRule)` —
    value codec = `ResourceLocation.CODEC.xmap(resolver::get, resolver::getId)`, stream codec built from
    `ResourceLocation.STREAM_CODEC` likewise. Resolver is **lazy** (resolves against `IForestryApi` registries
    at call time, mirroring the old `RegistryAllele.value()` lazy lookup).
- [ ] **Step 3:** Default naming rules: numeric/enum → a `Function<V,String>` that maps Forestry presets to
  suffix keys and otherwise returns `chromosomeKeyBase + "." + canonical(value)` (canonical per Phase A
  rules, sanitized for key safety); reference → `value -> resolver.getId(value)` rendered as a key
  (e.g. species use the species' existing translation key).
- [ ] **Step 4:** Delete the old impl classes; commit WIP.

```bash
git rm src/main/java/forestry/core/genetics/alleles/AlleleManager.java \
       src/main/java/forestry/core/genetics/alleles/AbstractChromosome.java \
       src/main/java/forestry/core/genetics/alleles/BooleanAllele.java \
       src/main/java/forestry/core/genetics/alleles/FloatAllele.java \
       src/main/java/forestry/core/genetics/alleles/IntegerAllele.java \
       src/main/java/forestry/core/genetics/alleles/ValueAllele.java \
       src/main/java/forestry/core/genetics/alleles/RegistryAllele.java \
       src/main/java/forestry/core/genetics/alleles/BooleanChromosome.java \
       src/main/java/forestry/core/genetics/alleles/FloatChromosome.java \
       src/main/java/forestry/core/genetics/alleles/IntegerChromosome.java \
       src/main/java/forestry/core/genetics/alleles/ValueChromosome.java \
       src/main/java/forestry/core/genetics/alleles/RegistryChromosome.java
git add -A && git commit -m "wip: add generic Chromosome impl + factory, delete old allele/chromosome impls (non-compiling)"
```

### Task C2: `Karyotype` impl — inline genome codec, permissive validity

**Files:**
- Modify: `src/main/java/forestry/core/genetics/Karyotype.java`

- [ ] **Step 1:** Build the genome `Codec<IGenome>` dynamically from the karyotype's ordered chromosomes:
  for each chromosome, a map field keyed by `chromosome.id()` whose value is an `AllelePair` codec built
  from that chromosome's `valueCodec()`:

```java
// per chromosome:
Codec<Allele<V>> alleleCodec = RecordCodecBuilder.create(i -> i.group(
    chromosome.valueCodec().fieldOf("value").forGetter(Allele::value),
    Codec.BOOL.optionalFieldOf("dominant", false).forGetter(Allele::dominant)
).apply(i, Allele::new));
Codec<AllelePair<V>> pairCodec = RecordCodecBuilder.create(i -> i.group(
    alleleCodec.fieldOf("active").forGetter(AllelePair::active),
    alleleCodec.fieldOf("inactive").forGetter(AllelePair::inactive)
).apply(i, AllelePair::new));
```

  Combine via the existing `Codec.simpleMap(chromosomeKeyCodec, dispatchedPairCodec, keyable)` pattern, or
  a `MapCodec` that adds one field per chromosome; then `.xmap(map -> Genome.sanitizeAlleles(this, map),
  IGenome::getChromosomes)`. Preserve the `sanitizeAlleles` backfill behavior.
- [ ] **Step 2:** Build the matching `StreamCodec<RegistryFriendlyByteBuf, IGenome>` from chromosome
  `valueStreamCodec()`s (iterate chromosomes in order; encode active+dominant, inactive+dominant).
- [ ] **Step 3:** Replace `isAlleleValid(chromosome, allele)` with `contains(chromosome)` (membership only);
  delete the `validAlleles` field, its population, and `getAlleles`. `getDefaultAlleles()` returns
  `ImmutableMap<IChromosome<?>, Allele<?>>`. Commit WIP.

```bash
git add -A && git commit -m "wip: Karyotype inline genome codec + permissive validity (non-compiling)"
```

### Task C3: `Genome` + `Genome.Builder`

**Files:**
- Modify: `src/main/java/forestry/core/genetics/Genome.java`

- [ ] **Step 1:** `Genome` — `getAllelePair` returns `AllelePair<V>`; in `sanitizeAlleles` replace
  `speciesPair.active().<IRegistryAllele<ISpecies<?>>>cast().value()` with `speciesPair.active().value()`
  and the missing-species fallback `karyotype.getSpeciesChromosome().get(...)` with the species chromosome's
  resolver (`resolver().get(defaultSpeciesId)`).
- [ ] **Step 2:** `Genome.Builder` — change the `active`/`inactive` maps to `IdentityHashMap<IChromosome<?>, Allele<?>>`;
  `set/setActive/setInactive(IChromosome<V>, Allele<V>)`; **drop the `isAlleleValid` guards** (permissive — keep
  an optional `karyotype.contains(chromosome)` assert); `setRemainingDefault` reads `getDefaultAlleles()`
  (now `Allele<?>`); `build()` constructs `new AllelePair<>(active, inactive)`. Commit WIP.

```bash
git add -A && git commit -m "wip: Genome + Builder on Allele<V>, permissive set (non-compiling)"
```

### Task C4: Rewrite `ForestryAlleles` + chromosome holders

**Files:**
- Modify: `ForestryAlleles.java`, `BeeChromosomes.java`, `TreeChromosomes.java`, `ButterflyChromosomes.java` (all `api/genetics/alleles/`)

- [ ] **Step 1:** `ForestryAlleles` — every constant becomes an `Allele<V>` (e.g.
  `SPEED_FAST = Allele.dominant(1.2f)`, `SPEED_NORMAL = Allele.recessive(1.0f)`, `TRUE = Allele.dominant(true)`,
  `TOLERANCE_BOTH_1 = Allele.dominant(ToleranceType.BOTH_1)`, `ACTIVITY_DIURNAL = Allele.recessive(ForestryActivityTypes.DIURNAL...)`).
  For ID-reference values that previously created registry alleles by id, store the **value** (resolved
  lazily) or the id wrapped as `Allele` — match how the karyotype/species builders consume them.
  **Delete all `DEFAULT_*` aggregate lists** (`DEFAULT_SPEEDS`, etc.). Preserve every value + dominance
  exactly (cross-check against the current file; the golden master will catch drift).
- [ ] **Step 2:** `BeeChromosomes` / `TreeChromosomes` / `ButterflyChromosomes` — create each chromosome via
  `ChromosomeFactory` instead of `ForestryAlleles.REGISTRY.*Chromosome(...)`. Provide value codecs, naming
  rules, and (for SPECIES/FLOWER_TYPE/EFFECT/ACTIVITY/FRUIT/COCOON) lazy resolvers backed by the relevant
  registries (species map via `IForestryApi.getGeneticManager().getSpeciesType(...)`, flower types/effects/
  activity via the species-type registries populated in `handleSpeciesRegistration`). Commit WIP.

```bash
git add -A && git commit -m "wip: ForestryAlleles as Allele<V> constants, chromosome holders via factory (non-compiling)"
```

---

## Phase D — Registration & lifecycle

### Task D1: Species registration, plugin manager, API wiring

**Files:**
- Modify: `apiimpl/plugin/SpeciesRegistration.java`, `apiimpl/plugin/PluginManager.java`, `core/genetics/ChromosomeBuilder.java`, `apiimpl/ForestryApiImpl.java`, `api/IForestryApi.java`

- [ ] **Step 1:** `SpeciesRegistration.buildAll` — replace
  `defaultGenomeBuilder.setUnchecked(speciesChromosome, AllelePair.both(registryAllele(id, speciesChromosome)))`
  with setting the species **value**: `AllelePair.both(new Allele<>(species, dominance))` (the species value
  resolved via the species map being built, or deferred — keep the existing post-build species-chromosome
  population but store the species value, not a registry allele). Replace the `karyotype.isAlleleValid(...)`
  taxon-default guard with `karyotype.contains(chromosome)`.
- [ ] **Step 2:** `PluginManager.registerGenetics` — delete the two
  `alleleManager.setRegistrationState(...)` calls and the `AlleleManager` import/cast.
- [ ] **Step 3:** `ChromosomeBuilder` — drop `addAlleles` and any `validAlleles` collection; keep
  `setDefault`/`setWeaklyInherited`.
- [ ] **Step 4:** `IForestryApi` / `ForestryApiImpl` — remove `getAlleleManager()` / `setAlleleManager()`
  and the `AlleleManager` field. Commit WIP.

```bash
git add -A && git commit -m "wip: species registration sets species value, drop allele freeze + AlleleManager wiring (non-compiling)"
```

### Task D2: Drop `IRegistryAlleleValue` marker from 8 interfaces

**Files:**
- Modify: `ISpecies`, `IFlowerType`, `IBeeEffect`, `IActivityType`, `IFruit`, `ITreeEffect`, `IButterflyCocoon`, `IButterflyEffect`

- [ ] **Step 1:** Remove `extends IRegistryAlleleValue` from each. Where the value's default dominance was
  read via `isDominant()`, move that default into the value's definition / the species or registration
  builder (used only when constructing the default genome's `Allele`). Keep `isDominant()` only where a
  value genuinely needs a default-dominance hint; otherwise delete. Commit WIP.

```bash
git add -A && git commit -m "wip: drop IRegistryAlleleValue marker from value interfaces (non-compiling)"
```

---

## Phase E — Compiler-guided fixups to the first GREEN build

### Task E1: Serialization (data components + codecs)

**Files:**
- Modify: `src/main/java/forestry/core/CoreDataComponents.java` (genome component)
- Audit: any remaining references to `AllelePair.CODEC`, `IAllele.CODEC`, `alleleCodec()`, `chromosomeCodec()`.

- [ ] **Step 1:** Point the GENOME `DataComponentType` at `IGenome.CODEC` (persistent) and the new
  `getGenomeStreamCodec()` (network). Remove dead references to the deleted global codecs. (No separate test
  step — covered by Phase F round-trip + Phase G build.)

### Task E2: Migrate direct allele/chromosome consumers

**Files (from blast-radius inventory — fix what the compiler flags):**
- `getActiveAllele(...).value()` / `getInactiveAllele(...)` sites: `apiculture/ApicultureFilterRule.java`,
  `apiculture/BeeSpecies.java`, `arboriculture/genetics/TreeGrowthHelper.java`, `arboriculture/tiles/TileLeaves.java`,
  `arboriculture/TreeSpecies.java`, `plugin/DefaultFarms.java`, `apiimpl/client/genetics/AnalyzerScreenGraphics.java`.
- Typed-chromosome-variable declarations → `IChromosome<V>`: `apiculture/genetics/Bee.java`,
  `lepidopterology/genetics/Butterfly.java`, `core/genetics/IndividualLiving.java`, `core/genetics/Species.java`,
  `core/utils/SpeciesUtil.java`, `core/utils/JeiUtil.java`, `core/gui/GuiNaturalistInventory.java`,
  `api/client/genetics/IAnalyzerGraphics.java`, `apiimpl/client/genetics/AnalyzerScreenGraphics.java`.
- `AllelePair` generic-type sites: `apiculture/genetics/Bee.java`, `arboriculture/genetics/Tree.java`,
  `lepidopterology/genetics/Butterfly.java`, `core/genetics/mutations/Mutation.java`,
  `apiculture/compat/MutationRecipe.java`, `api/genetics/IMutation.java`, `api/genetics/ISpecies.java`,
  `api/genetics/filter/IFilterLogic.java`, `sorting/FilterLogic.java`, `apiculture/LepidopterologyFilterRule.java`
  (and `lepidopterology/LepidopterologyFilterRule.java`).

- [ ] **Step 1:** Run `./gradlew compileJava`; fix the first batch of errors; repeat. Prefer the stable
  `getActiveValue(IChromosome<V>)` accessor over `getActiveAllele().value()`. Replace `allele == other`
  identity checks with `.equals`. Replace `IRegistryAllele#value()` chains with `getActiveValue` on the
  reference chromosome.
- [ ] **Step 2:** When `compileJava` is green, commit.

```bash
./gradlew compileJava   # iterate until BUILD SUCCESSFUL
git add -A && git commit -m "refactor: migrate allele/chromosome/genome consumers to inline-value model"
```

### Task E3: Commands compute candidates from registered species

**Files:**
- Modify: `core/commands/DiagnosticsCommand.java` (line ~63), `core/commands/ModifyGenomeCommand.java` (lines ~56, ~103)

- [ ] **Step 1:** Replace `karyotype.getAlleles(chromosome)` with a helper that gathers distinct values
  from `speciesType.getAllSpecies()` default genomes for the chromosome (`getActiveValue` + `getInactiveValue`),
  de-duplicated and **sorted by value** (natural order for numbers; id/name for reference/enum). Use these
  for `DiagnosticsCommand` listing and `ModifyGenomeCommand` argument suggestions.
- [ ] **Step 2:** Replace the `isAlleleValid` guard in `ModifyGenomeCommand` with: accept any value the
  chromosome's `valueCodec` can parse (permissive); only require `karyotype.contains(chromosome)`.
- [ ] **Step 3:** `./gradlew compileJava` green; commit.

```bash
git add -A && git commit -m "refactor: commands compute allele candidates from registered species (permissive)"
```

### Task E4: Naming at the UI edge (no Component in the model)

**Files:**
- Modify: analyzer/tooltip consumers that used `IGenome.getActiveName`/`chromosome.getDisplayName`
  (`apiimpl/client/genetics/AnalyzerScreenGraphics.java`, `core/gui/GuiNaturalistInventory.java`, and any
  flagged by the compiler).

- [ ] **Step 1:** Build display `Component`s at the call site via
  `Component.translatable(chromosome.translationKey(genome.getActiveValue(chromosome)))`, with the raw-value
  fallback when the key is absent (use `Component.translatableWithFallback(key, rawString)`). No Component is
  added to the genetics API surface.
- [ ] **Step 2:** `./gradlew compileJava` && `./gradlew compileTestJava` green; commit.

```bash
git add -A && git commit -m "refactor: render allele names from translation keys at the UI edge"
```

---

## Phase F — New behavioral GameTests (TDD for the new model)

### Task F1: Codec round-trip + permissive value

**Files:**
- Create: `src/test/java/forestry/gametest/AlleleFoundationTest.java`

- [ ] **Step 1: Write tests, run, watch them pass** (the impl already exists; these lock behavior):
  - Persistent round-trip: build a non-default bee genome, encode with `IGenome.CODEC` (JsonOps/NbtOps),
    decode, assert `isSameAlleles`.
  - Network round-trip: encode/decode with `getGenomeStreamCodec()`; assert equal.
  - Permissive value: set a chromosome to an **off-preset** numeric value (e.g. speed `1.23f`), build,
    round-trip, and assert `getActiveValue` reads back `1.23f`.
- [ ] **Step 2:** Run `./gradlew runGameTestServer`; expected PASS. Commit.

### Task F2: Dominance, inheritance, default-genome construction

**Files:**
- Modify: `src/test/java/forestry/gametest/AlleleFoundationTest.java`

- [ ] **Step 1:** Tests:
  - `AllelePair.create` ordering for all four dominance combinations (matches old semantics).
  - `inheritOther`/`inheritHaploid` produce pairs drawn from parents (seeded `RandomSource`).
  - Default-genome construction: a freshly built species default genome equals the karyotype defaults +
    taxon defaults + species overrides (spot-check Forest bee chromosomes).
- [ ] **Step 2:** Run `./gradlew runGameTestServer`; expected PASS. Commit.

```bash
git add -A && git commit -m "test: allele foundation gametests (codec, dominance, inheritance, defaults)"
```

---

## Phase G — Verify golden master + full build

### Task G1: Prove built-in species are unchanged

**Files:**
- Modify: `src/test/java/forestry/gametest/GenomeBaselineTest.java` (rewrite the dumper for the new API,
  reproducing the canonical format from Task A1 exactly).

- [ ] **Step 1:** Update the dumper to read values via `getActiveValue`/`getInactiveValue` and dominance via
  `getActiveAllele().dominant()`, rendering the **same canonical strings** as Phase A (reference values →
  `chromosome.resolver().getId(value)`; primitives/enums/Vec3i as specified).
- [ ] **Step 2:** Run assert mode against the committed baseline.

Run: `./gradlew runGameTestServer`
Expected: PASS — every built-in species' default genome is byte-identical to the pre-refactor baseline.
If it fails: diff the report; a mismatch means a value/dominance was altered during the `ForestryAlleles`
rewrite — fix the constant, do **not** edit the baseline.

- [ ] **Step 3:** Full verification.

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL (compile main + tests, all gametests green). Commit.

```bash
git add -A && git commit -m "test: verify default genomes unchanged after allele foundation refactor"
```

---

## Phase H — Optional cleanup

### Task H1: Remove orphan Imprinter assets

**Files:**
- Delete: `src/main/resources/assets/forestry/models/item/imprinter.json` and the two `imprinter.png` textures (verify exact paths first).

- [ ] **Step 1:** `git rm` the orphan assets (no Java references exist). Run `./gradlew build`; expected
  SUCCESSFUL. Commit. Skip this task if you prefer to keep assets.

---

## Done criteria

- `./gradlew build` is green (main + test compile, all gametests pass).
- `GenomeBaselineTest` proves built-in default genomes are unchanged (clean break is serialization-format-only).
- No references remain to `IAllele`, `AlleleManager`, `IRegistryAllele(Value)`, the chromosome subtypes,
  `getAlleles`, `addAlleles`, or `AllelePair.CODEC`.
- Genomes serialize values inline; no global allele registry or freeze exists.
- Naming uses translation keys only; no `Component` in the genetics API surface.
