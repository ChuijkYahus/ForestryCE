package forestry.core.genetics.alleles;

import forestry.api.genetics.alleles.IFloatAllele;
import forestry.api.genetics.alleles.IFloatChromosome;
import net.minecraft.resources.ResourceLocation;

public class FloatChromosome extends AbstractChromosome<IFloatAllele> implements IFloatChromosome {
	public FloatChromosome(ResourceLocation id) {
		super(id);
	}

	@Override
	public Class<?> valueClass() {
		return float.class;
	}
}
