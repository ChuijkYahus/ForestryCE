# Postage as a data map

Date: 2026-08-22
Branch: `1.21.1`
Status: design

## Problem

`IStamps` is an interface an item class implements to declare itself worth postage. Three things are
wrong with it.

It is implemented on the `Item` class, so the only way for another mod to add a stamp is to extend
or implement a Forestry type. That is a hard dependency on a jar that a pack may not install, for
what is one integer per item.

It returns `EnumPostage`, a closed set of nine constants. Postage cannot be any other value, and the
enum's `ordinal()` has leaked into storage: `PostOffice.collectedPostage` is an `int[]` indexed by it
and serialized as `CPS0`..`CPS8`, and `TradeStation.getPostage` returns an `int[]` on the same index
that `removeStamps` consumes positionally.

Because the enum is the key, two stamps of equal value are the same thing to the post office. The
stamp collector's filter slot inherits this: filtering by a 5n stamp yields any stamp worth 5.

`@Deprecated` and `// todo replace with capability` have been on the interface for some time. A
capability is the wrong replacement. `ItemCapability<IStamps, Void>` requires the `IStamps` class on
the other mod's classpath, so it is a larger dependency than what it replaces, and capabilities carry
behavior rather than values.

## Target

Postage is a NeoForge data map on `Registries.ITEM`. A third party declares stamp values by shipping
one JSON file and depends on nothing:

```json
// data/theirmod/data_maps/item/forestry/postage.json
{ "values": { "theirmod:fancy_stamp": 25, "#c:stamps/cheap": 3 } }
```

A data component was considered and rejected for the same reason as the capability. A
`DataComponentType` is a registry object, so setting a default component still requires either a
class reference to Forestry or an unchecked registry lookup whose result must already exist when the
other mod's items are constructed. Neither is a dependency-free path. No component override is part
of this design: every Forestry stamp is a distinct item, so per-stack postage has no use case.

The post office and the stamp collector stop reducing stamps to a number at all. They track the stamp
items themselves, which removes the ordinal indexing and the equal-value collision together.

## Data map and API surface

`forestry.api.ForestryDataMaps` is new in the core jar, a sibling of `ForestryCapabilities`, which
already holds constants for features the apiculture and core jars own:

```java
public static final DataMapType<Item, Integer> POSTAGE = DataMapType
        .builder(forestry("postage"), Registries.ITEM)
        .synced(ExtraCodecs.POSITIVE_INT, false)
        .build();
```

`ModuleMail.registerEvents` adds a `RegisterDataMapTypesEvent` listener next to the existing
capability listener.

`synced` is required, not optional. `TradeStationInventory.canPlaceItem` and
`StampCollectorInventory.canSlotAccept` run on the client for the GUI, so the client needs the
values. The sync is non-mandatory so a vanilla client can still connect.

Core never dereferences `POSTAGE`. NeoForge's loader iterates registered data map types looking for
their files rather than scanning directories, so when the mail jar is absent a third party's
`postage.json` is simply never read. Nothing to guard, and the optional-jar degradation invariant
holds.

`forestry.mail.letters.PostageUtil` holds the readers:

```java
static int getPostage(ItemStack stack)              // 0 when the item has no entry
static boolean isStamp(ItemStack stack)             // getPostage > 0
static int sumPostage(Iterable<ItemStack> stacks)   // postage * count, summed
```

`sumPostage` replaces three near-identical loops.

### Removed and changed

| Type | Change |
| --- | --- |
| `forestry.api.mail.IStamps` | deleted |
| `forestry.api.mail.EnumPostage` | deleted |
| `EnumStampDefinition` | `EnumPostage postage` becomes `int postage`; `POSTAGE_MAP` and `getFromPostage` deleted |
| `ItemStamp` | no longer implements `IStamps` |
| `IPostOffice` | the two `EnumPostage` overloads replaced, see below |

`ForestryTags.Items.STAMPS` is unchanged. It feeds only the catalogue recipe, and postage no longer
depends on tag membership.

### Datagen

A `MailDataMapProvider` registered through `jar.addServer(...)` in `MailData`, writing
`src/generated/resources_mail/data/forestry/data_maps/item/forestry/postage.json` from
`EnumStampDefinition.VALUES`.

## Post office

`PostOffice` stores `Object2IntOpenHashMap<Item> collectedStamps`. The old array already counted
stamps per denomination rather than summing values, so the shape carries over directly:
`collectPostage` becomes a `merge` on the item, still guarded by `isStamp` so a non-stamp cannot be
banked into a form nothing can withdraw.

The save format becomes a `collected` compound keyed by item id. Absence of that key triggers
migration from the old format: `CPS1`..`CPS7` map positionally onto `EnumStampDefinition.VALUES`,
because the old index was `EnumPostage.ordinal()` where `P_0` is 0 and `P_1`..`P_100` are 1 through
7. `CPS0` is always zero, and `CPS8` (`P_200`) never had a stamp item. Both are dropped, with a log
line when either is non-zero. An item id that no longer resolves on load is likewise dropped with a
warning, since there is nothing to hand back.

The API becomes:

```java
ItemStack getAnyStamp(int max);              // cheapest first, ties broken by item id
ItemStack getAnyStamp(Item stamp, int max);  // exact item
```

Cheapest-first preserves what the old ordinal walk did. The sort reads postage from the data map and
breaks ties on the registry id so it stays deterministic when two mods ship stamps of equal value.
The map holds one entry per stamp item currently banked, so sorting on demand is cheap enough. An
entry is removed once its count reaches zero, and zero counts are never serialized. `getAnyStamp`
returns `ItemStack.EMPTY` when nothing matches.

### Persistence bug

`getAnyStamp` mutates `collectedPostage` and never calls `setDirty()`. `lodgeLetter` calls it after
`collectPostage`, so deposits persist, but `StampCollectorBlockEntity` calls `getAnyStamp` directly
with nothing marking the data dirty afterward. Withdrawing stamps and then restarting leaves them
both in the vault and in the collector.

This is a duplication path independent of the refactor. The rework adds the `setDirty()` and a
GameTest pinning it, in the same manner as the multiblock conservation oracle.

## Stamp collector

`StampCollectorBlockEntity` calls `getAnyStamp(filter.getItem(), 1)` when the filter slot holds a
stamp. This is an intended behavior change: the filter now means this exact stamp rather than
anything worth this much.

The `ItemStack stamp = null` local and its `stamp == null` check become `ItemStack.EMPTY` and
`isEmpty()`.

`StampCollectorInventory.canSlotAccept` becomes `PostageUtil.isStamp(stack)`.

## Trade station

`TradeStation` builds a denomination snapshot of `(Item, postage, available)` sorted by postage
ascending. For a real station the snapshot comes from the stamp slots. For a virtual station it comes
from `EnumStampDefinition.VALUES` at 99 each.

That split is a fix. The old code applied `virtual ? 99 : getNumStamps(...)` to whatever denominations
existed, so once third parties can register stamps a virtual station would conjure them. A virtual
station produces only Forestry's own stamps.

`getPostage(int, boolean)` becomes `List<ItemStack> selectPostage(int, boolean)` running the same
three passes over the snapshot:

1. descending greedy, taking `min(available, floor(remaining / value))` of each
2. if postage remains, the smallest single stamp worth at least the full requirement, if one is held
3. if postage still remains, ceil-fill combining from the largest downward

The returned stacks drive both `mail.addStamps` and `removeStamps`, so the parallel-array coupling
between the two disappears. `removeStamps` walks the stamp slots matching on `ItemStack.isSameItem`.
The `i > 0` and `i = 1` bounds that skipped `P_0` are dropped: denominations built from real stamps
always have positive postage.

`getNumStamps(EnumPostage)` is folded into the snapshot and deleted.

`canPayPostage`, `Letter.isPostPaid`, and `TradeStationBlockEntity.hasPostageMin` become
`PostageUtil.sumPostage(...) >= required`.

`TradeStationInventory.canSlotAccept` becomes `PostageUtil.isStamp(stack)` for the stamp slot range.

## Testing

- GameTest: deposit stamps through `collectPostage`, withdraw through a stamp collector, assert the
  total count is conserved and survives a save and reload. This fails on the current `setDirty`
  defect and is the regression pin for it.
- GameTest: a stamp collector with a filter yields only that exact item, given two collected stamps
  of equal postage.
- Unit-level: a `PostOffice` constructed from a `CompoundTag` holding `CPS1`..`CPS8` produces the
  expected item counts and drops `CPS8`.
- `runData` regenerates `postage.json`. The existing suite must stay green.

## Not in scope

- Per-stack postage through a data component
- An optimal change-making solver. The three-pass greedy is preserved as-is
- Postage values in stamp tooltips
