package forestry.core.data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonElement;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;

import com.mojang.serialization.JsonOps;

import forestry.api.apiculture.ForestryBeeEffects;
import forestry.api.apiculture.genetics.IBeeEffect;
import forestry.apiculture.genetics.effects.AgingBeeEffect;
import forestry.apiculture.genetics.effects.DamageBeeEffect;
import forestry.apiculture.genetics.effects.PotionBeeEffect;
import forestry.apiculture.genetics.effects.ResurrectionBeeEffect;
import forestry.apiculture.genetics.effects.ThrottleSettings;
import forestry.core.damage.CoreDamageTypes;
import forestry.core.genetics.GeneticsReloadHandler;

/**
 * Generates {@code data/forestry/bee_effect/*.json} for the built-in bee effects that are expressible through the
 * data-driven effect primitives. Mirrors {@link FlowerTypeProvider}: this provider is the single source of truth for
 * the effects it emits &mdash; they are no longer code-registered at runtime (see {@code DefaultForestryPlugin}), so a
 * bee that references one resolves it from this generated JSON, loaded by {@code BeeEffectManager} on datapack
 * (re)load. Encoding needs no registry access: the only registry-backed field is the mob effect, which lives in the
 * static {@code BuiltInRegistries.MOB_EFFECT} and encodes to a plain resource location.
 * <p>
 * Addon mods generate their own effects by subclassing and overriding {@link #addEffects()}, mirroring
 * {@link MutationProvider}.
 */
public class BeeEffectProvider implements DataProvider {
	private final PackOutput.PathProvider path;
	private final Map<ResourceLocation, IBeeEffect> pending = new LinkedHashMap<>();

	public BeeEffectProvider(PackOutput output) {
		this.path = output.createPathProvider(PackOutput.Target.DATA_PACK, "bee_effect");
	}

	// Collector used by seedLiveBeeEffectsForDatagen: gathers the built-ins via addEffects() without needing a
	// PackOutput to write to (it never runs the provider). Never call this to write JSON - path is null.
	private BeeEffectProvider() {
		this.path = null;
	}

	/**
	 * Add your effects here. Make sure NOT to call the super constructor in your mod. Values must match the effect's
	 * former code registration exactly so that migrating it from code to datapack is behaviour-preserving.
	 */
	protected void addEffects() {
		// The plain PotionBeeEffect builtins, expressed through the forestry:apply_potion primitive.
		add(ForestryBeeEffects.BEATIFIC, new PotionBeeEffect(false, MobEffects.REGENERATION, 100));
		add(ForestryBeeEffects.MIASMIC, new PotionBeeEffect(false, MobEffects.POISON, 600, 100, 0.1f));
		add(ForestryBeeEffects.DRUNKARD, new PotionBeeEffect(false, MobEffects.CONFUSION, 100));
		add(ForestryBeeEffects.DARKNESS, new PotionBeeEffect(false, MobEffects.DARKNESS, 150));
		// The two "resurrect item drops into mobs" builtins, sharing one forestry:resurrect primitive and differing
		// only by their item->mob table.
		add(ForestryBeeEffects.REANIMATION, new ResurrectionBeeEffect(true, 40, ResurrectionBeeEffect.getReanimationList()));
		add(ForestryBeeEffects.RESURRECTION, new ResurrectionBeeEffect(true, 40, ResurrectionBeeEffect.getResurrectionList()));
		// The two queen-aging builtins, sharing one forestry:aging primitive and differing only by the aging flag.
		add(ForestryBeeEffects.REJUVENATION, new AgingBeeEffect(false, false));
		add(ForestryBeeEffects.CHRONOPHAGE, new AgingBeeEffect(false, true));
		// The area-damage builtins, expressed through the forestry:damage_entities primitive, differing only by damage,
		// armor scaling, damage type and target filter.
		add(ForestryBeeEffects.AGGRESSIVE, new DamageBeeEffect(new ThrottleSettings(true, 40, false, false), 4f, true, 1.0f, CoreDamageTypes.AGGRESSIVE, DamageBeeEffect.Target.Builtin.ALL));
		add(ForestryBeeEffects.MISANTHROPE, new DamageBeeEffect(new ThrottleSettings(true, 20, false, false), 4f, true, 1.0f, CoreDamageTypes.MISANTHROPE, DamageBeeEffect.Target.Builtin.PLAYERS));
		add(ForestryBeeEffects.HEROIC, new DamageBeeEffect(new ThrottleSettings(false, 40, true, false), 2f, false, 1.0f, CoreDamageTypes.HEROIC, DamageBeeEffect.Target.Builtin.MONSTERS));
	}

	protected void add(ResourceLocation id, IBeeEffect effect) {
		this.pending.put(id, effect);
	}

	/**
	 * Populates the live effect map directly from {@link #addEffects()}, bypassing the datapack JSON round trip. Only
	 * for use by the standalone data generator ({@code Data#preDataGen}): a data-generator invocation never fires the
	 * datapack-reload cycle that loads effects at real server start, but karyotype default-allele resolution for the
	 * {@code bee_effect} chromosome (while seeding live bee species for datagen) requires these builtins to already be
	 * resolvable. Mirrors {@link FlowerTypeProvider#seedLiveFlowerTypesForDatagen}. Merges onto the code-registered
	 * effects already set by module setup, so it must run after {@code setupApi()} and before bee species are seeded.
	 */
	public static void seedLiveBeeEffectsForDatagen() {
		BeeEffectProvider collector = new BeeEffectProvider();
		collector.addEffects();
		GeneticsReloadHandler.rebuildBeeEffects(collector.pending);
	}

	@Override
	public CompletableFuture<?> run(CachedOutput output) {
		this.pending.clear();
		addEffects();
		var futures = this.pending.entrySet().stream().map(entry -> {
			JsonElement json = IBeeEffect.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue()).getOrThrow();
			return DataProvider.saveStable(output, json, this.path.json(entry.getKey()));
		}).toArray(CompletableFuture[]::new);
		return CompletableFuture.allOf(futures);
	}

	@Override
	public String getName() {
		return "Forestry Bee Effects";
	}
}
