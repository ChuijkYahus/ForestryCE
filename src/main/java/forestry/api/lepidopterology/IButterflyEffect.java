package forestry.api.lepidopterology;

import forestry.api.genetics.IEffectData;
import forestry.api.genetics.alleles.IRegistryAlleleValue;

/**
 * Unimplemented.
 */
public interface IButterflyEffect extends IRegistryAlleleValue {
	/**
	 * Used by butterflies to trigger effects in the world.
	 *
	 * @param butterfly {@link IEntityButterfly}
	 * @return {@link forestry.api.genetics.IEffectData} for the next cycle.
	 */
	IEffectData doEffect(IEntityButterfly butterfly, IEffectData storedData);
}
