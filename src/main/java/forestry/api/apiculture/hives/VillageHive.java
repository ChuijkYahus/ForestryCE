package forestry.api.apiculture.hives;

import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.IChromosome;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public record VillageHive(ResourceLocation speciesId, Map<IChromosome<?>, Allele<?>> alleles) {
}
