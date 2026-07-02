package forestry.apiculture.genetics;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;

import forestry.Forestry;
import forestry.api.apiculture.IBeeJubilance;
import forestry.api.apiculture.genetics.IBeeSpeciesType;
import forestry.api.genetics.IGenome;
import forestry.api.genetics.alleles.IKaryotype;
import forestry.api.plugin.IGenomeBuilder;
import forestry.apiculture.BeeSpecies;
import forestry.apiimpl.plugin.SpeciesRegistration;
import forestry.core.genetics.GenomeProjection;

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
			GenomeProjection.applyOverrides(gb, karyotype, def.genome());
			IGenome genome = gb.build();
			return new BeeSpecies(id, type, genome, new DefinitionBeeSpeciesBuilder(def, jubilance));
		} catch (Exception e) {
			Forestry.LOGGER.error("Failed to project bee species {}", id, e);
			return null;
		}
	}
}
