package forestry.api.genetics.alleles;

import net.minecraft.resources.ResourceLocation;

/**
 * An allele is a single inline value of a chromosome, together with its dominance.
 * <p>
 * Alleles are plain data: they are not interned, have no ID, and are not stored in any registry. A genome stores
 * {@link AllelePair} of these directly, serialized inline via each chromosome's value codec. For "reference"
 * chromosomes (species, flower type, bee effect, activity, fruit, tree effect, cocoon, butterfly effect) the value
 * is the referenced object's {@link net.minecraft.resources.ResourceLocation}; the behavior object is resolved on
 * demand through {@link IChromosome#resolver()}.
 *
 * @param value    The value held by this allele.
 * @param dominant Whether this allele is dominant.
 * @param <V>      The type of value held by this allele.
 */
public record Allele<V>(V value, boolean dominant) {
	public static <V> Allele<V> dominant(V value) {
		return new Allele<>(value, true);
	}

	public static <V> Allele<V> recessive(V value) {
		return new Allele<>(value, false);
	}

	public static <V> Allele<V> of(V value, boolean dominant) {
		return new Allele<>(value, dominant);
	}

	/**
	 * Creates an allele for a reference chromosome from the referenced value's ID. The dominance is a placeholder:
	 * a reference value's dominance is intrinsic to the value, so it is resolved from the chromosome's resolver when
	 * the genome is materialized (e.g. in {@link IGenome#copyWith}). Use this for genome overrides built before
	 * registries are populated, such as hive drops or village bees.
	 */
	public static Allele<ResourceLocation> reference(ResourceLocation id) {
		return new Allele<>(id, false);
	}
}
