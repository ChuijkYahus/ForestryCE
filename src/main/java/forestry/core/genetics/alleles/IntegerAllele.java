package forestry.core.genetics.alleles;

import forestry.api.ForestryConstants;
import forestry.api.genetics.alleles.IIntegerAllele;
import net.minecraft.resources.ResourceLocation;

record IntegerAllele(ResourceLocation alleleId, int value, boolean dominant) implements IIntegerAllele {
	IntegerAllele(int value, boolean dominant) {
		this(createId(value, dominant), value, dominant);
	}

	private static ResourceLocation createId(int value, boolean dominant) {
		return ForestryConstants.forestry(value + (dominant ? "id" : "i"));
	}
}
