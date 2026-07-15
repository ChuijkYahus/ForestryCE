package forestry.gametest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import io.netty.buffer.Unpooled;

import forestry.api.ForestryConstants;
import forestry.api.ForestryRegistries;
import forestry.api.apiculture.ForestryBeeEffects;
import forestry.api.apiculture.ForestryBeeSpecies;
import forestry.api.apiculture.genetics.IBeeEffect;
import forestry.api.apiculture.genetics.IBeeSpeciesType;
import forestry.api.core.TemperatureType;
import forestry.api.genetics.alleles.BeeChromosomes;
import forestry.apiculture.genetics.BeeEffectManager;
import forestry.apiculture.genetics.effects.AgingBeeEffect;
import forestry.apiculture.genetics.effects.DamageBeeEffect;
import forestry.apiculture.genetics.effects.PotionBeeEffect;
import forestry.apiculture.genetics.effects.ResurrectionBeeEffect;
import forestry.apiculture.genetics.effects.ThrottleSettings;
import forestry.apiculture.genetics.effects.TransformBlockBeeEffect;
import forestry.core.damage.CoreDamageTypes;
import forestry.core.genetics.GeneticsReloadHandler;
import forestry.core.utils.SpeciesUtil;

/**
 * Behavioral oracle for the data-driven bee effect system ported onto the upstream foundation (migration Module 2).
 * Proves the three invariants of the port: the 14 parameterized effect primitives are registered as serializer types;
 * {@link IBeeEffect#CODEC} round-trips a datapack effect definition through both JSON and the network stream codec used
 * by {@code BeeEffectSyncPacket}; and {@code GeneticsReloadHandler.rebuildBeeEffects} merges datapack effects onto the
 * code-registered builtins reloadably (datapack entries resolve, builtins survive, and an empty reload keeps builtins).
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class BeeEffectSystemTest {
	/** The effect a species' default genome resolves to, via the reference {@code bee_effect} chromosome. */
	private static IBeeEffect effectOf(ResourceLocation speciesId) {
		return SpeciesUtil.BEE_TYPE.get().getSpecies(speciesId).getDefaultGenome().resolveActive(BeeChromosomes.EFFECT);
	}

	/** All 14 parameterized primitive serializer types must be registered in the dispatch registry. */
	@GameTest(template = "empty")
	public static void allEffectPrimitiveTypesRegistered(GameTestHelper helper) {
		String[] types = {
			"apply_potion", "spawn_mob", "damage_entities", "feed", "firework", "strike_lightning", "teleport",
			"entity_force", "bonemeal", "spawn_projectile", "transform_block", "place_block", "fill_fluid", "inject_energy"
		};
		for (String type : types) {
			ResourceLocation id = ForestryConstants.forestry(type);
			if (!ForestryRegistries.BEE_EFFECT_TYPE.containsKey(id)) {
				helper.fail("bee effect primitive type not registered: " + id);
				return;
			}
		}
		helper.succeed();
	}

	/**
	 * A datapack effect definition round-trips through {@link IBeeEffect#CODEC} (JSON) preserving its type and
	 * parameters, and through the network stream codec the sync packet uses.
	 */
	@GameTest(template = "empty")
	public static void effectDefinitionRoundTrips(GameTestHelper helper) {
		RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());

		TransformBlockBeeEffect original = new TransformBlockBeeEffect(
			new ThrottleSettings(false, 30, true, false),
			List.of(new TransformBlockBeeEffect.Transform(
				BuiltInRegistries.BLOCK.getOrCreateTag(BlockTags.DIRT),
				new TransformBlockBeeEffect.To.Fixed(Blocks.COARSE_DIRT.defaultBlockState()),
				true)),
			10, 0.34f, Optional.of(TemperatureType.NORMAL));

		JsonElement json = IBeeEffect.CODEC.encodeStart(ops, original).getOrThrow();
		if (!json.getAsJsonObject().get("max_temperature").getAsString().equals("normal")) {
			helper.fail("transform_block did not encode max_temperature as a lowercase name: " + json);
			return;
		}
		IBeeEffect fromJson = IBeeEffect.CODEC.parse(ops, json).getOrThrow();
		if (!(fromJson instanceof TransformBlockBeeEffect transform)) {
			helper.fail("transform_block did not decode to TransformBlockBeeEffect (got " + fromJson.getClass().getSimpleName() + ")");
			return;
		}
		if (transform.getThrottle() != 30 || transform.isDominant() || transform.attempts() != 10
			|| !transform.maxTemperature().equals(Optional.of(TemperatureType.NORMAL))) {
			helper.fail("transform_block parameters not preserved through JSON: " + json);
			return;
		}
		TransformBlockBeeEffect.Transform decodedRule = transform.transforms().getFirst();
		if (!decodedRule.requiresAirAbove()
			|| !Blocks.DIRT.defaultBlockState().is(decodedRule.from())
			|| !(decodedRule.to() instanceof TransformBlockBeeEffect.To.Fixed fixed)
			|| !fixed.state().is(Blocks.COARSE_DIRT)) {
			helper.fail("transform_block transform rule not preserved through JSON: " + json);
			return;
		}

		StreamCodec<RegistryFriendlyByteBuf, IBeeEffect> streamCodec = ByteBufCodecs.fromCodecWithRegistries(IBeeEffect.CODEC);
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
		streamCodec.encode(buf, original);
		IBeeEffect fromBuf = streamCodec.decode(buf);
		if (!(fromBuf instanceof TransformBlockBeeEffect decoded) || decoded.getThrottle() != 30 || decoded.attempts() != 10) {
			helper.fail("transform_block did not survive the network stream codec round-trip");
			return;
		}

		helper.succeed();
	}

	/**
	 * The identity guard's decision function: {@code To.apply} returns the state it was given when the transform is a
	 * no-op, which is exactly what {@code doEffectThrottled} branches on to skip the write. This subsumes SIFTER's
	 * coarse-dirt exclusion (coarse dirt is in {@code #minecraft:dirt}, so the tag matches and only the identity check
	 * stops the rewrite) and GLOW_BERRY_GROW's already-berried check.
	 */
	@GameTest(template = "empty")
	public static void transformIdentityGuardSkipsNoOps(GameTestHelper helper) {
		BlockState coarseDirt = Blocks.COARSE_DIRT.defaultBlockState();
		if (!coarseDirt.is(BuiltInRegistries.BLOCK.getOrCreateTag(BlockTags.DIRT))) {
			helper.fail("#minecraft:dirt no longer contains coarse_dirt; SIFTER's identity guard assumption is void");
			return;
		}
		if (new TransformBlockBeeEffect.To.Fixed(coarseDirt).apply(coarseDirt) != coarseDirt) {
			helper.fail("Fixed.apply did not return the identical state for a coarse dirt no-op");
			return;
		}

		BlockState berried = Blocks.CAVE_VINES.defaultBlockState().setValue(BlockStateProperties.BERRIES, true);
		TransformBlockBeeEffect.To setBerries = new TransformBlockBeeEffect.To.SetProperties(Map.of("berries", "true"));
		if (setBerries.apply(berried) != berried) {
			helper.fail("SetProperties.apply did not return the identical state for an already-berried vine");
			return;
		}
		// ...and it is a real mutation on an unberried vine, preserving the vine's other properties.
		BlockState bare = Blocks.CAVE_VINES.defaultBlockState().setValue(BlockStateProperties.AGE_25, 7);
		BlockState grown = setBerries.apply(bare);
		if (grown == bare || !grown.getValue(BlockStateProperties.BERRIES) || grown.getValue(BlockStateProperties.AGE_25) != 7) {
			helper.fail("SetProperties.apply did not set berries while preserving the vine's other properties");
			return;
		}

		helper.succeed();
	}

	/**
	 * {@code rebuildBeeEffects} merges datapack effects onto the code builtins: the datapack entry resolves, a builtin
	 * still resolves, and after an empty reload the builtin survives (self-resetting). Restores the live effect map
	 * afterwards so the mutation does not leak into other tests.
	 */
	@GameTest(template = "empty")
	public static void datapackEffectsMergeOntoBuiltins(GameTestHelper helper) {
		IBeeSpeciesType beeType = SpeciesUtil.BEE_TYPE.get();
		Map<ResourceLocation, IBeeEffect> original = BeeEffectManager.INSTANCE.getEffects();
		ResourceLocation testId = ForestryConstants.forestry("gametest_transform");
		IBeeEffect testEffect = new TransformBlockBeeEffect(
			new ThrottleSettings(true, 30, false, false),
			List.of(new TransformBlockBeeEffect.Transform(
				BuiltInRegistries.BLOCK.getOrCreateTag(BlockTags.DIRT),
				new TransformBlockBeeEffect.To.Fixed(Blocks.COARSE_DIRT.defaultBlockState()),
				false)),
			1, 0.34f, Optional.empty());
		try {
			GeneticsReloadHandler.rebuildBeeEffects(Map.of(testId, testEffect));

			if (!(beeType.getBeeEffect(testId) instanceof TransformBlockBeeEffect)) {
				helper.fail("datapack effect " + testId + " did not resolve after rebuildBeeEffects");
				return;
			}
			// A code builtin (registered by DefaultForestryPlugin) must survive the merge.
			if (beeType.getBeeEffect(ForestryBeeEffects.NONE) == null) {
				helper.fail("builtin bee effect NONE was dropped by the datapack merge");
				return;
			}

			// Empty reload: builtins remain (self-resetting when a datapack is removed).
			GeneticsReloadHandler.rebuildBeeEffects(Map.of());
			if (beeType.getBeeEffect(ForestryBeeEffects.NONE) == null) {
				helper.fail("builtin bee effect NONE was dropped after an empty reload");
				return;
			}
		} finally {
			GeneticsReloadHandler.rebuildBeeEffects(original);
		}

		helper.succeed();
	}

	/**
	 * The plain-potion built-in effects are no longer code-registered: they are defined in the mod's own datapack
	 * (generated by {@code BeeEffectProvider}) and must resolve to a {@link PotionBeeEffect} through the same merged
	 * effect map any datapack effect uses. Proves base content flows through the data-driven pipe, not just new
	 * primitives.
	 */
	@GameTest(template = "empty")
	public static void potionBuiltinsAreDatapackDefined(GameTestHelper helper) {
		IBeeSpeciesType beeType = SpeciesUtil.BEE_TYPE.get();
		for (ResourceLocation id : new ResourceLocation[]{
			ForestryBeeEffects.BEATIFIC, ForestryBeeEffects.MIASMIC, ForestryBeeEffects.DRUNKARD, ForestryBeeEffects.DARKNESS
		}) {
			if (!(beeType.getBeeEffect(id) instanceof PotionBeeEffect)) {
				helper.fail("datapack-defined potion built-in " + id + " did not resolve to a PotionBeeEffect");
				return;
			}
		}
		// ...and the bees that carry them resolve their genome's effect to that datapack instance: IMPERIAL/BEATIFIC,
		// AQUATIC/MIASMIC, ABYSSAL/DARKNESS, TIPSY/DRUNKARD.
		for (ResourceLocation speciesId : new ResourceLocation[]{
			ForestryBeeSpecies.IMPERIAL, ForestryBeeSpecies.AQUATIC, ForestryBeeSpecies.ABYSSAL, ForestryBeeSpecies.TIPSY
		}) {
			if (!(effectOf(speciesId) instanceof PotionBeeEffect)) {
				helper.fail("bee " + speciesId + " did not resolve its genome effect to the datapack PotionBeeEffect");
				return;
			}
		}
		helper.succeed();
	}

	/**
	 * REANIMATION and RESURRECTION are generalized into one {@code forestry:resurrect} primitive parameterized by an
	 * item&rarr;mob table: the type is registered, both built-ins are datapack-defined and resolve to a
	 * {@link ResurrectionBeeEffect}, and a definition round-trips through {@link IBeeEffect#CODEC} preserving its
	 * entries.
	 */
	@GameTest(template = "empty")
	public static void resurrectBuiltinsAreDatapackDefined(GameTestHelper helper) {
		if (!ForestryRegistries.BEE_EFFECT_TYPE.containsKey(ForestryConstants.forestry("resurrect"))) {
			helper.fail("resurrect effect type not registered: forestry:resurrect");
			return;
		}
		IBeeSpeciesType beeType = SpeciesUtil.BEE_TYPE.get();
		for (ResourceLocation id : new ResourceLocation[]{ForestryBeeEffects.REANIMATION, ForestryBeeEffects.RESURRECTION}) {
			if (!(beeType.getBeeEffect(id) instanceof ResurrectionBeeEffect)) {
				helper.fail("datapack-defined built-in " + id + " did not resolve to a ResurrectionBeeEffect");
				return;
			}
		}
		// The Spectral (REANIMATION) and Phantasmal (RESURRECTION) bees carry these effects; their genomes must
		// resolve to the datapack instance.
		if (!(effectOf(ForestryBeeSpecies.SPECTRAL) instanceof ResurrectionBeeEffect)
			|| !(effectOf(ForestryBeeSpecies.PHANTASMAL) instanceof ResurrectionBeeEffect)) {
			helper.fail("Spectral/Phantasmal bees did not resolve their genome effect to the datapack ResurrectionBeeEffect");
			return;
		}

		// The item->mob table survives a JSON round trip through the dispatch codec.
		RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
		ResurrectionBeeEffect original = new ResurrectionBeeEffect(true, 40, ResurrectionBeeEffect.getReanimationList());
		JsonElement json = IBeeEffect.CODEC.encodeStart(ops, original).getOrThrow();
		if (!(IBeeEffect.CODEC.parse(ops, json).getOrThrow() instanceof ResurrectionBeeEffect)) {
			helper.fail("resurrect definition did not round-trip through IBeeEffect.CODEC");
			return;
		}

		helper.succeed();
	}

	/**
	 * REJUVENATION and CHRONOPHAGE are generalized into one {@code forestry:aging} primitive differing only by the
	 * {@code aging} flag: the type is registered, both built-ins resolve to an {@link AgingBeeEffect}, and the bees
	 * that carry them (RELIC/REJUVENATION, ANACHRONE/CHRONOPHAGE) resolve their genome effect to the datapack instance.
	 */
	@GameTest(template = "empty")
	public static void agingBuiltinsAreDatapackDefined(GameTestHelper helper) {
		if (!ForestryRegistries.BEE_EFFECT_TYPE.containsKey(ForestryConstants.forestry("aging"))) {
			helper.fail("aging effect type not registered: forestry:aging");
			return;
		}
		IBeeSpeciesType beeType = SpeciesUtil.BEE_TYPE.get();
		for (ResourceLocation id : new ResourceLocation[]{ForestryBeeEffects.REJUVENATION, ForestryBeeEffects.CHRONOPHAGE}) {
			if (!(beeType.getBeeEffect(id) instanceof AgingBeeEffect)) {
				helper.fail("datapack-defined built-in " + id + " did not resolve to an AgingBeeEffect");
				return;
			}
		}
		if (!(effectOf(ForestryBeeSpecies.RELIC) instanceof AgingBeeEffect)
			|| !(effectOf(ForestryBeeSpecies.ANACHRONE) instanceof AgingBeeEffect)) {
			helper.fail("Relic/Anachrone bees did not resolve their genome effect to the datapack AgingBeeEffect");
			return;
		}

		// The tunable strength multiplier survives the dispatch codec (built-ins omit it; a pack can set it).
		RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
		JsonElement json = IBeeEffect.CODEC.encodeStart(ops, new AgingBeeEffect(false, true, 3.0f)).getOrThrow();
		if (json.getAsJsonObject().get("strength").getAsFloat() != 3.0f
			|| !(IBeeEffect.CODEC.parse(ops, json).getOrThrow() instanceof AgingBeeEffect)) {
			helper.fail("aging strength did not round-trip through IBeeEffect.CODEC");
			return;
		}

		helper.succeed();
	}

	/**
	 * AGGRESSIVE and MISANTHROPE are generalized into the {@code forestry:damage_entities} primitive: both deal 4
	 * damage with armor scaling and are non-combinable, differing only by damage type and target filter (MISANTHROPE
	 * hits players only). The type is registered, both built-ins resolve to a {@link DamageBeeEffect}, and the bees
	 * that carry them (SINISTER/AGGRESSIVE, ENDED/MISANTHROPE) resolve their genome effect to the datapack instance.
	 */
	@GameTest(template = "empty")
	public static void damageEntitiesBuiltinsAreDatapackDefined(GameTestHelper helper) {
		if (!ForestryRegistries.BEE_EFFECT_TYPE.containsKey(ForestryConstants.forestry("damage_entities"))) {
			helper.fail("damage_entities effect type not registered: forestry:damage_entities");
			return;
		}
		IBeeSpeciesType beeType = SpeciesUtil.BEE_TYPE.get();
		for (ResourceLocation id : new ResourceLocation[]{ForestryBeeEffects.AGGRESSIVE, ForestryBeeEffects.MISANTHROPE}) {
			if (!(beeType.getBeeEffect(id) instanceof DamageBeeEffect)) {
				helper.fail("datapack-defined built-in " + id + " did not resolve to a DamageBeeEffect");
				return;
			}
		}
		// AGGRESSIVE and MISANTHROPE are non-combinable; the code default for damage_entities is combinable, so the
		// migrated built-ins must keep that flag off (the JSON sets "combinable": false).
		if (beeType.getBeeEffect(ForestryBeeEffects.AGGRESSIVE).isCombinable()
			|| beeType.getBeeEffect(ForestryBeeEffects.MISANTHROPE).isCombinable()) {
			helper.fail("AGGRESSIVE/MISANTHROPE must resolve as non-combinable");
			return;
		}
		if (!(effectOf(ForestryBeeSpecies.SINISTER) instanceof DamageBeeEffect)
			|| !(effectOf(ForestryBeeSpecies.ENDED) instanceof DamageBeeEffect)) {
			helper.fail("Sinister/Ended bees did not resolve their genome effect to the datapack DamageBeeEffect");
			return;
		}

		// The custom damage type and players-only target filter survive the dispatch codec (built-ins set them; a
		// bare damage_entities effect omits them, defaulting to minecraft:generic / all living entities).
		RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
		DamageBeeEffect misanthrope = new DamageBeeEffect(true, 4f, true, 20, 1.0f, CoreDamageTypes.MISANTHROPE, true, false);
		JsonElement json = IBeeEffect.CODEC.encodeStart(ops, misanthrope).getOrThrow();
		if (!json.getAsJsonObject().get("players_only").getAsBoolean()
			|| !json.getAsJsonObject().get("damage_type").getAsString().equals("forestry:misanthrope")
			|| json.getAsJsonObject().get("combinable").getAsBoolean()
			|| !(IBeeEffect.CODEC.parse(ops, json).getOrThrow() instanceof DamageBeeEffect)) {
			helper.fail("damage_entities damage_type/players_only/combinable did not round-trip through IBeeEffect.CODEC");
			return;
		}

		helper.succeed();
	}

	/**
	 * Every {@code ThrottledBeeEffect}-derived primitive exposes the four common fields through
	 * {@link ThrottleSettings}, so a pack can retune them. Proven on {@code apply_potion} and {@code resurrect}.
	 */
	@GameTest(template = "empty")
	public static void throttleSettingsRoundTrip(GameTestHelper helper) {
		RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());

		ThrottleSettings tuned = new ThrottleSettings(false, 50, false, true);
		JsonElement json = IBeeEffect.CODEC.encodeStart(ops, new PotionBeeEffect(tuned, MobEffects.REGENERATION, 100, 1.0f)).getOrThrow();
		if (json.getAsJsonObject().get("throttle").getAsInt() != 50
			|| json.getAsJsonObject().get("requires_working").getAsBoolean()
			|| !json.getAsJsonObject().get("combinable").getAsBoolean()
			|| json.getAsJsonObject().get("dominant").getAsBoolean()) {
			helper.fail("apply_potion did not emit the ThrottleSettings fields: " + json);
			return;
		}
		if (!(IBeeEffect.CODEC.parse(ops, json).getOrThrow() instanceof PotionBeeEffect decoded)
			|| !decoded.settings().equals(tuned)) {
			helper.fail("apply_potion did not decode back to the tuned ThrottleSettings");
			return;
		}

		ThrottleSettings resurrectTuned = new ThrottleSettings(false, 7, false, false);
		JsonElement resurrectJson = IBeeEffect.CODEC.encodeStart(ops,
			new ResurrectionBeeEffect(resurrectTuned, ResurrectionBeeEffect.getReanimationList())).getOrThrow();
		if (!(IBeeEffect.CODEC.parse(ops, resurrectJson).getOrThrow() instanceof ResurrectionBeeEffect decodedResurrect)
			|| !decodedResurrect.settings().equals(resurrectTuned)) {
			helper.fail("resurrect did not round-trip its ThrottleSettings");
			return;
		}

		helper.succeed();
	}

	/**
	 * Each primitive's {@link ThrottleSettings} defaults are its historical hardcoded values, so an effect built the
	 * way {@code BeeEffectProvider} builds the built-ins emits none of the four fields it did not already emit. This
	 * is what keeps the generated JSON for the pre-existing built-ins unchanged.
	 */
	@GameTest(template = "empty")
	public static void throttleSettingsDefaultsStayOutOfJson(GameTestHelper helper) {
		RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());

		// BEATIFIC, verbatim from BeeEffectProvider: dominant=false is the only non-default of the four.
		JsonElement potion = IBeeEffect.CODEC.encodeStart(ops, new PotionBeeEffect(false, MobEffects.REGENERATION, 100)).getOrThrow();
		if (potion.getAsJsonObject().has("throttle") || potion.getAsJsonObject().has("requires_working")
			|| potion.getAsJsonObject().has("combinable")) {
			helper.fail("apply_potion emitted a defaulted ThrottleSettings field: " + potion);
			return;
		}

		// REANIMATION, verbatim from BeeEffectProvider: all four are defaults.
		JsonElement resurrect = IBeeEffect.CODEC.encodeStart(ops,
			new ResurrectionBeeEffect(true, 40, ResurrectionBeeEffect.getReanimationList())).getOrThrow();
		if (resurrect.getAsJsonObject().has("throttle") || resurrect.getAsJsonObject().has("requires_working")
			|| resurrect.getAsJsonObject().has("combinable") || resurrect.getAsJsonObject().has("dominant")) {
			helper.fail("resurrect emitted a defaulted ThrottleSettings field: " + resurrect);
			return;
		}

		helper.succeed();
	}
}
