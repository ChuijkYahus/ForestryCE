package forestry.api.genetics.alleles;

import net.minecraft.resources.ResourceLocation;

import forestry.Forestry;
import forestry.api.core.ToleranceType;
import forestry.api.genetics.ForestrySpeciesTypes;
import forestry.api.lepidopterology.IButterflyCocoon;
import forestry.api.lepidopterology.IButterflyEffect;
import forestry.api.lepidopterology.genetics.IButterflySpecies;
import forestry.api.lepidopterology.genetics.IButterflySpeciesType;
import forestry.core.genetics.alleles.ChromosomeFactory;
import forestry.core.utils.SpeciesUtil;

import static forestry.api.ForestryConstants.forestry;

public class ButterflyChromosomes {
	/**
	 * The species of a butterfly. The genome stores the species' ID.
	 */
	public static final IChromosome<ResourceLocation> SPECIES = ChromosomeFactory.referenceChromosome(ForestrySpeciesTypes.BUTTERFLY, ButterflyChromosomes::resolveSpeciesOrDefault, IButterflySpecies::isDominant);

	/**
	 * Resolves a butterfly species id stored in a genome to its species, falling back to the default species (instead
	 * of throwing) if a datapack has since removed it. Backs every SPECIES chromosome read (tooltips, analyzer,
	 * spawning, saved items), so a removed id must never crash those paths.
	 */
	private static IButterflySpecies resolveSpeciesOrDefault(ResourceLocation id) {
		IButterflySpeciesType type = SpeciesUtil.BUTTERFLY_TYPE.get();
		IButterflySpecies species = type.getSpeciesSafe(id);
		if (species != null) {
			return species;
		}
		Forestry.LOGGER.warn("Butterfly species {} not found (removed by a datapack?); falling back to the default species", id);
		return type.getDefaultSpecies();
	}
	/**
	 * Determines physical size of a butterfly.
	 */
	public static final IChromosome<Float> SIZE = ChromosomeFactory.floatChromosome(forestry("size"));
	/**
	 * Determines the flight speed of a butterfly.
	 */
	public static final IChromosome<Float> SPEED = BeeChromosomes.SPEED;
	/**
	 * Determines how long this butterfly will live.
	 */
	public static final IChromosome<Integer> LIFESPAN = ChromosomeFactory.intChromosome(forestry("butterfly_lifespan"));
	/**
	 * Determines the rate at which caterpillars destroy leaves and influences cocoon drops.
	 */
	public static final IChromosome<Integer> METABOLISM = ChromosomeFactory.intChromosome(forestry("metabolism"));
	/**
	 * Determines how likely this butterfly is to mate as well as how fast its nurseries and cocoons mature.
	 */
	public static final IChromosome<Integer> FERTILITY = BeeChromosomes.FERTILITY;
	/**
	 * Determines the acceptable range of temperatures from a butterfly's ideal temperature.
	 */
	public static final IChromosome<ToleranceType> TEMPERATURE_TOLERANCE = BeeChromosomes.TEMPERATURE_TOLERANCE;
	/**
	 * Determines the acceptable range of humidities from a butterfly's ideal humidity.
	 */
	public static final IChromosome<ToleranceType> HUMIDITY_TOLERANCE = BeeChromosomes.HUMIDITY_TOLERANCE;
	/**
	 * Whether diurnal butterflies can work during the night, or nocturnal butterflies (moths) can work during the day.
	 */
	public static final IChromosome<Boolean> NEVER_SLEEPS = ChromosomeFactory.booleanChromosome(forestry("never_sleeps"));
	/**
	 * Whether this butterfly can spawn or fly while it is raining.
	 */
	public static final IChromosome<Boolean> TOLERATES_RAIN = BeeChromosomes.TOLERATES_RAIN;
	/**
	 * Whether this butterfly is immune to fire/lava damage.
	 */
	public static final IChromosome<Boolean> FIREPROOF = ChromosomeFactory.booleanChromosome(forestry("fireproof"));
	/**
	 * Unimplemented.
	 */
	public static final IChromosome<ResourceLocation> FLOWER_TYPE = BeeChromosomes.FLOWER_TYPE;
	/**
	 * Unimplemented.
	 */
	public static final IChromosome<ResourceLocation> EFFECT = ChromosomeFactory.referenceChromosome(forestry("butterfly_effect"), id -> SpeciesUtil.BUTTERFLY_TYPE.get().getButterflyEffect(id), IButterflyEffect::isDominant);
	/**
	 * Used for silk moths (Bombyx Mori) to affect cocoon drops.
	 */
	public static final IChromosome<ResourceLocation> COCOON = ChromosomeFactory.referenceChromosome(forestry("cocoon"), id -> SpeciesUtil.BUTTERFLY_TYPE.get().getCocoon(id), IButterflyCocoon::isDominant);
}
