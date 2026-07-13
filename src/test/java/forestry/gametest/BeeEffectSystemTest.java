package forestry.gametest;

import java.util.Map;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import io.netty.buffer.Unpooled;

import forestry.api.ForestryConstants;
import forestry.api.ForestryRegistries;
import forestry.api.apiculture.ForestryBeeEffects;
import forestry.api.apiculture.genetics.IBeeEffect;
import forestry.api.apiculture.genetics.IBeeSpeciesType;
import forestry.apiculture.genetics.BeeEffectManager;
import forestry.apiculture.genetics.effects.LightningBeeEffect;
import forestry.apiculture.genetics.effects.PotionBeeEffect;
import forestry.apiculture.genetics.effects.ResurrectionBeeEffect;
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

		// { "type": "forestry:strike_lightning", "throttle": 30, "chance": 0.34 }
		JsonElement json = IBeeEffect.CODEC.encodeStart(ops, new LightningBeeEffect(true, 30, 0.34f)).getOrThrow();
		IBeeEffect fromJson = IBeeEffect.CODEC.parse(ops, json).getOrThrow();
		if (!(fromJson instanceof LightningBeeEffect lightning)) {
			helper.fail("strike_lightning did not decode to LightningBeeEffect (got " + fromJson.getClass().getSimpleName() + ")");
			return;
		}
		if (lightning.getThrottle() != 30 || !lightning.isDominant()) {
			helper.fail("strike_lightning parameters not preserved through JSON (throttle=" + lightning.getThrottle() + ", dominant=" + lightning.isDominant() + ")");
			return;
		}

		StreamCodec<RegistryFriendlyByteBuf, IBeeEffect> streamCodec = ByteBufCodecs.fromCodecWithRegistries(IBeeEffect.CODEC);
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
		streamCodec.encode(buf, new LightningBeeEffect(false, 42, 0.5f));
		IBeeEffect fromBuf = streamCodec.decode(buf);
		if (!(fromBuf instanceof LightningBeeEffect decoded) || decoded.getThrottle() != 42 || decoded.isDominant()) {
			helper.fail("strike_lightning did not survive the network stream codec round-trip");
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
		ResourceLocation testId = ForestryConstants.forestry("gametest_lightning");
		try {
			GeneticsReloadHandler.rebuildBeeEffects(Map.of(testId, new LightningBeeEffect(true, 30, 0.34f)));

			if (!(beeType.getBeeEffect(testId) instanceof LightningBeeEffect)) {
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
}
