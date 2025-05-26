package forestry.core.genetics.alleles;

import forestry.api.genetics.alleles.IValueAllele;
import net.minecraft.resources.ResourceLocation;

public record ValueAllele<V>(ResourceLocation alleleId, V value, boolean dominant) implements IValueAllele<V> {
}
