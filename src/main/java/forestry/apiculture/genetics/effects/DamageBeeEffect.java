package forestry.apiculture.genetics.effects;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import forestry.api.apiculture.BeeManager;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.genetics.IBeeEffect;
import forestry.api.genetics.IEffectData;
import forestry.api.genetics.IGenome;
import forestry.core.damage.CoreDamageTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The {@code forestry:damage_entities} primitive: hurts living entities in range for a fixed amount, with
 * optional apiarist-armor scaling (each worn piece reduces the damage). Also expresses the built-in AGGRESSIVE
 * (all entities) and MISANTHROPE (players only) effects, which are {@code damage 4} + armor scaling differing
 * only by their {@code damage_type} and target filter. Covers the RADIOACTIVE effect's entity-harm
 * half from JSON (the base {@code radioactive} effect hardcodes damage and also destroys blocks, so this
 * primitive gives datapacks a configurable, block-safe alternative).
 */
public class DamageBeeEffect extends ThrottledBeeEffect {
	public static final MapCodec<DamageBeeEffect> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Codec.BOOL.optionalFieldOf("dominant", true).forGetter(IBeeEffect::isDominant),
		Codec.floatRange(0f, Float.MAX_VALUE).fieldOf("damage").forGetter(effect -> effect.damage),
		Codec.BOOL.optionalFieldOf("armor_scaling", true).forGetter(effect -> effect.armorScaling),
		Codec.INT.optionalFieldOf("throttle", 40).forGetter(ThrottledBeeEffect::getThrottle),
		Codec.floatRange(0f, 1f).optionalFieldOf("chance", 1.0f).forGetter(effect -> effect.chance),
		ResourceKey.codec(Registries.DAMAGE_TYPE).optionalFieldOf("damage_type", DamageTypes.GENERIC).forGetter(effect -> effect.damageType),
		Codec.BOOL.optionalFieldOf("players_only", false).forGetter(effect -> effect.playersOnly),
		Codec.BOOL.optionalFieldOf("combinable", true).forGetter(IBeeEffect::isCombinable)
	).apply(instance, DamageBeeEffect::new));

	private final float damage;
	private final boolean armorScaling;
	private final float chance;
	private final ResourceKey<DamageType> damageType;
	private final boolean playersOnly;

	public DamageBeeEffect(boolean dominant, float damage, boolean armorScaling, int throttle, float chance) {
		this(dominant, damage, armorScaling, throttle, chance, DamageTypes.GENERIC, false, true);
	}

	public DamageBeeEffect(boolean dominant, float damage, boolean armorScaling, int throttle, float chance, ResourceKey<DamageType> damageType, boolean playersOnly, boolean combinable) {
		super(dominant, throttle, false, combinable);
		this.damage = damage;
		this.armorScaling = armorScaling;
		this.chance = chance;
		this.damageType = damageType;
		this.playersOnly = playersOnly;
	}

	@Override
	public MapCodec<DamageBeeEffect> codec() {
		return MAP_CODEC;
	}

	@Override
	public IEffectData doEffectThrottled(IGenome genome, IEffectData storedData, IBeeHousing housing) {
		Level level = housing.getWorldObj();
		RandomSource rand = level.random;
		Class<? extends LivingEntity> targetClass = this.playersOnly ? Player.class : LivingEntity.class;
		List<? extends LivingEntity> entities = ThrottledBeeEffect.getEntitiesInRange(genome, housing, targetClass);
		DamageSource source = CoreDamageTypes.source(level, this.damageType);

		for (LivingEntity entity : entities) {
			// Skip the RNG draw entirely when chance is 1 so a guaranteed effect (e.g. AGGRESSIVE/MISANTHROPE)
			// does not perturb the shared world RNG state.
			if (this.chance < 1.0f && rand.nextFloat() >= this.chance) {
				continue;
			}

			float damage = this.damage;
			if (this.armorScaling) {
				// Entities wearing apiarist's armor take reduced (or no) damage.
				int count = BeeManager.armorApiaristHelper.wearsItems(entity, this, true);
				damage -= count * (this.damage / 4f);
			}
			if (damage <= 0f) {
				continue;
			}

			entity.hurt(source, damage);
		}

		return storedData;
	}
}
