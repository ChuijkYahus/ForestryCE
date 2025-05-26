package forestry.api.genetics.alyzer;

import forestry.api.genetics.IIndividual;
import forestry.api.genetics.ILifeStage;
import forestry.apiculture.genetics.IGeneticTooltipProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Predicate;

public interface IAlleleDisplayHelper {
	void addTooltip(IGeneticTooltipProvider<? extends IIndividual> provider, ResourceLocation id, int orderingInfo);

	void addTooltip(IGeneticTooltipProvider<? extends IIndividual> provider, ResourceLocation id, int orderingInfo, Predicate<ILifeStage> typeFilter);

	void addAlyzer(IGeneticTooltipProvider<?> provider, ResourceLocation id, int orderingInfo);

}
