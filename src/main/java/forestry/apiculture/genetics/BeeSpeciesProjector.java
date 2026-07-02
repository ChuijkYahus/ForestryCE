package forestry.apiculture.genetics;

import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;

import forestry.Forestry;
import forestry.api.apiculture.IBeeJubilance;
import forestry.api.apiculture.genetics.IBeeSpeciesType;
import forestry.api.genetics.IGenome;
import forestry.api.genetics.alleles.Allele;
import forestry.api.genetics.alleles.IChromosome;
import forestry.api.genetics.alleles.IKaryotype;
import forestry.api.plugin.IGenomeBuilder;
import forestry.apiculture.BeeSpecies;
import forestry.apiimpl.plugin.SpeciesRegistration;

/**
 * Projects a pure-data {@link BeeSpeciesDefinition} into a runtime {@link BeeSpecies}, without ever touching the
 * {@code BeeSpecies}/{@code Species} constructors directly: the definition is wrapped in a read-only
 * {@link DefinitionBeeSpeciesBuilder} adapter and driven through the same
 * {@link SpeciesRegistration#createDefaultGenomeBuilder} path the code-registered species use.
 * <p>
 * Fails soft: any failure (unknown jubilance, unknown chromosome, exception) is logged and yields {@code null}
 * rather than crashing species loading.
 */
public final class BeeSpeciesProjector {
	private BeeSpeciesProjector() {
	}

	/**
	 * Applies the definition's sparse genome overrides onto a genome builder, dispatching reference vs data
	 * chromosomes exactly as the code-built path does: reference chromosomes (non-null {@link IChromosome#resolver()})
	 * use the {@code ResourceLocation} overload so dominance is resolved from the reference; data chromosomes use
	 * the plain {@code Allele} overload.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void applyOverrides(IGenomeBuilder builder, IKaryotype karyotype, Map<ResourceLocation, Allele<?>> overrides) {
		for (Map.Entry<ResourceLocation, Allele<?>> e : overrides.entrySet()) {
			IChromosome<?> chromosome = karyotype.getChromosome(e.getKey());
			if (chromosome == null) {
				Forestry.LOGGER.warn("Skipping unknown chromosome {} in bee species genome override", e.getKey());
				continue;
			}
			Allele<?> allele = e.getValue();
			if (chromosome.resolver() != null) {
				builder.set((IChromosome<ResourceLocation>) chromosome, (ResourceLocation) allele.value());
			} else {
				builder.set((IChromosome) chromosome, allele);
			}
		}
	}

	/**
	 * @return The runtime {@link BeeSpecies} for the given definition, or {@code null} if projection failed
	 * (logged).
	 */
	@Nullable
	public static BeeSpecies project(IBeeSpeciesType type, ResourceLocation id, BeeSpeciesDefinition def) {
		try {
			IBeeJubilance jubilance = type.getJubilanceSafe(def.jubilance());
			if (jubilance == null) {
				Forestry.LOGGER.warn("Skipping bee species {}: unknown jubilance {}", id, def.jubilance());
				return null;
			}
			IKaryotype karyotype = type.getKaryotype();
			IGenomeBuilder gb = SpeciesRegistration.createDefaultGenomeBuilder(karyotype, id, def.genus(), def.dominant());
			applyOverrides(gb, karyotype, def.genome());
			IGenome genome = gb.build();
			return new BeeSpecies(id, type, genome, new DefinitionBeeSpeciesBuilder(def, jubilance));
		} catch (Exception e) {
			Forestry.LOGGER.error("Failed to project bee species {}", id, e);
			return null;
		}
	}
}
