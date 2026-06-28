package forestry.api.genetics.alleles;

import com.mojang.serialization.Codec;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;

import forestry.api.apiculture.IActivityType;
import forestry.api.apiculture.IFlowerType;
import forestry.api.apiculture.genetics.IBeeEffect;
import forestry.api.apiculture.genetics.IBeeSpecies;
import forestry.api.core.ToleranceType;
import forestry.api.genetics.ForestrySpeciesTypes;
import forestry.core.genetics.alleles.ChromosomeFactory;
import forestry.core.utils.SpeciesUtil;

import static forestry.api.ForestryConstants.forestry;

/**
 * All chromosomes of the Forestry bee species type.
 */
public class BeeChromosomes {
	static final Codec<ToleranceType> TOLERANCE_CODEC = Codec.STRING.xmap(ToleranceType::valueOf, Enum::name);
	static final Codec<Vec3i> VEC3I_CODEC = Vec3i.CODEC;

	/**
	 * The species of a bee. The genome stores the species' ID.
	 */
	public static final IChromosome<ResourceLocation> SPECIES = ChromosomeFactory.referenceChromosome(ForestrySpeciesTypes.BEE, id -> SpeciesUtil.BEE_TYPE.get().getSpecies(id), IBeeSpecies::isDominant);
	/**
	 * Determines a queen's production speed. Shows up as "worker" in the portable analyzer.
	 */
	public static final IChromosome<Float> SPEED = ChromosomeFactory.floatChromosome(forestry("speed"));
	/**
	 * Determines a queen's lifespan.
	 */
	public static final IChromosome<Integer> LIFESPAN = ChromosomeFactory.intChromosome(forestry("lifespan"));
	/**
	 * The number of drones given when a queen dies.
	 */
	public static final IChromosome<Integer> FERTILITY = ChromosomeFactory.intChromosome(forestry("fertility"));
	/**
	 * Determines the acceptable range of temperatures from a bee's ideal temperature. Reused by butterflies.
	 */
	public static final IChromosome<ToleranceType> TEMPERATURE_TOLERANCE = ChromosomeFactory.valueChromosome(forestry("temperature_tolerance"), TOLERANCE_CODEC, ToleranceType::name);
	/**
	 * Determines the acceptable range of humidities from a bee's ideal humidity. Reused by butterflies.
	 */
	public static final IChromosome<ToleranceType> HUMIDITY_TOLERANCE = ChromosomeFactory.valueChromosome(forestry("humidity_tolerance"), TOLERANCE_CODEC, ToleranceType::name);
	/**
	 * The activity type determines when this bee is awake. Builtin types are found in {@link forestry.api.apiculture.ForestryActivityTypes}.
	 */
	public static final IChromosome<ResourceLocation> ACTIVITY = ChromosomeFactory.referenceChromosome(forestry("activity"), id -> SpeciesUtil.BEE_TYPE.get().getActivityType(id), IActivityType::isDominant);
	/**
	 * Whether this bee can work when the sky above its housing is obstructed.
	 */
	public static final IChromosome<Boolean> CAVE_DWELLING = ChromosomeFactory.booleanChromosome(forestry("cave_dwelling"));
	/**
	 * Whether this bee can work while it is raining.
	 */
	public static final IChromosome<Boolean> TOLERATES_RAIN = ChromosomeFactory.booleanChromosome(forestry("tolerates_rain"));
	/**
	 * The type of flowers this bee needs to work. Also includes flowers that a bee can plant.
	 */
	public static final IChromosome<ResourceLocation> FLOWER_TYPE = ChromosomeFactory.referenceChromosome(forestry("flower_type"), id -> SpeciesUtil.BEE_TYPE.get().getFlowerType(id), IFlowerType::isDominant);
	/**
	 * Determines the effect of a bee species. Its range is determined by {@link #TERRITORY}.
	 */
	public static final IChromosome<ResourceLocation> EFFECT = ChromosomeFactory.referenceChromosome(forestry("bee_effect"), id -> SpeciesUtil.BEE_TYPE.get().getBeeEffect(id), IBeeEffect::isDominant);
	/**
	 * Determines how fast the hive can pollinate trees and plant flowers. Range is determined by {@link #TERRITORY}.
	 */
	public static final IChromosome<Integer> POLLINATION = ChromosomeFactory.intChromosome(forestry("pollination"));
	/**
	 * Determines the area in which a bee can pollinate trees, grow flowers, and use its special effect.
	 */
	public static final IChromosome<Vec3i> TERRITORY = ChromosomeFactory.valueChromosome(forestry("territory"), VEC3I_CODEC, v -> v.getX() + "_" + v.getY() + "_" + v.getZ());
}
