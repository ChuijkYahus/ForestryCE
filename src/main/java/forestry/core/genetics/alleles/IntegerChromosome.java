package forestry.core.genetics.alleles;

import forestry.api.genetics.alleles.IIntegerAllele;
import forestry.api.genetics.alleles.IIntegerChromosome;
import net.minecraft.resources.ResourceLocation;

public class IntegerChromosome extends AbstractChromosome<IIntegerAllele> implements IIntegerChromosome {
	public IntegerChromosome(ResourceLocation id) {
		super(id);
	}

	@Override
	public Class<?> valueClass() {
		return int.class;
	}
}
