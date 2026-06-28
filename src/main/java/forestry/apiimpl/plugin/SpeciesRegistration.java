package forestry.apiimpl.plugin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import forestry.Forestry;
import forestry.api.IForestryApi;
import forestry.api.genetics.*;
import forestry.api.genetics.alleles.*;
import forestry.api.plugin.IGenomeBuilder;
import forestry.api.plugin.ISpeciesBuilder;
import forestry.core.genetics.MutationManager;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Base implementation of {@link ISpeciesBuilder} with common logic.
 *
 * @param <I> Interface type of the species builders used by this species registration.
 * @param <S> Interface type of the species registered by this species registration.
 * @param <B> The concrete type of the species builder used by this species registration.
 */
public abstract class SpeciesRegistration<I extends ISpeciesBuilder<? extends ISpeciesType<S, ?>, S, I>, S extends ISpecies<?>, B extends I> {
	@SuppressWarnings({"unchecked", "rawtypes"})
	private final ModifiableRegistrar<ResourceLocation, I, B> species = new ModifiableRegistrar(ISpeciesBuilder.class);

	protected final ISpeciesType<S, ?> type;

	public SpeciesRegistration(ISpeciesType<S, ?> type) {
		this.type = type;
	}

	protected abstract B createSpeciesBuilder(ResourceLocation id, String genus, String species, MutationsRegistration mutations);

	protected I register(ResourceLocation id, String genus, String species) {
		return this.species.create(id, createSpeciesBuilder(id, genus, species, new MutationsRegistration(id)));
	}

	public void modifySpecies(ResourceLocation id, Consumer<I> action) {
		this.species.modify(id, action);
	}

	// Creates the final map of species and the mutations manager. The species chromosome is a reference chromosome,
	// so it no longer needs to be populated separately; it is resolved on demand via the species type.
	@SuppressWarnings({"unchecked", "rawtypes"})
	public Pair<ImmutableMap<ResourceLocation, S>, IMutationManager<S>> buildAll() {
		IKaryotype karyotype = this.type.getKaryotype();
		IChromosome<ResourceLocation> speciesChromosome = karyotype.getSpeciesChromosome();

		ImmutableMap<ResourceLocation, S> allSpecies = this.species.build((id, builder) -> {
			// create default genome builder
			IGenomeBuilder defaultGenomeBuilder = karyotype.createGenomeBuilder();
			ITaxon[] ancestry = IForestryApi.INSTANCE.getGeneticManager().getParentTaxa(builder.getGenus());

			// apply default genomes from parent taxa
			for (ITaxon taxon : ancestry) {
				for (Map.Entry<IChromosome<?>, ITaxon.TaxonAllele> alleleEntry : taxon.alleles().entrySet()) {
					IChromosome<?> chromosome = alleleEntry.getKey();
					ITaxon.TaxonAllele taxonAllele = alleleEntry.getValue();

					if (!karyotype.contains(chromosome)) {
						// If a taxon is shared by different species types, don't throw errors for incompatible default alleles
						Forestry.LOGGER.warn("Default allele set by taxon {} skipped for species {} due to being invalid for its karyotype", taxon.name(), id);
						continue;
					}

					ResourceLocation reference = taxonAllele.reference();
					if (reference != null) {
						// reference chromosome: resolve the value's declared dominance lazily
						defaultGenomeBuilder.set((IChromosome<ResourceLocation>) chromosome, reference);
					} else {
						defaultGenomeBuilder.setUnchecked(chromosome, AllelePair.both(taxonAllele.allele()));
					}
				}
			}

			// set the species chromosome; dominance comes from the species' own builder (the resolver is not ready yet)
			defaultGenomeBuilder.setUnchecked(speciesChromosome, AllelePair.both(new Allele<>(id, builder.isDominant())));
			defaultGenomeBuilder.setRemainingDefault();
			IGenome defaultGenome = builder.buildGenome(defaultGenomeBuilder);

			return builder.createSpeciesFactory().create(id, this.type.cast(), defaultGenome, builder);
		});

		// build mutations once species are available
		ImmutableList.Builder<IMutation<S>> mutations = new ImmutableList.Builder<>();
		for (Map.Entry<ResourceLocation, B> entry : this.species.getValues().entrySet()) {
			mutations.addAll(entry.getValue().buildMutations(this.type, allSpecies));
		}

		return Pair.of(allSpecies, new MutationManager<>(mutations.build()));
	}
}
