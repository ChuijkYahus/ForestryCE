package forestry.api.core.genetics.alyzer;

import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.ILifeStage;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Predicate;

public interface IAlleleDisplayHelper {
	void addTooltip(IGeneticTooltipProvider<? extends IIndividual> provider, ResourceLocation id, int orderingInfo);

	void addTooltip(IGeneticTooltipProvider<? extends IIndividual> provider, ResourceLocation id, int orderingInfo, Predicate<ILifeStage> typeFilter);

	void addAlyzer(IGeneticTooltipProvider<?> provider, ResourceLocation id, int orderingInfo);

}
