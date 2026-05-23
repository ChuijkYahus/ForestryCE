package forestry.compat.jei;

import forestry.api.genetics.IIndividual;
import forestry.api.genetics.capability.IIndividualHandlerItem;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class IndividualSubtypeInterpreter implements ISubtypeInterpreter<ItemStack> {
	@Nullable
	@Override
	public Object getSubtypeData(ItemStack ingredient, UidContext context) {
		return getLegacyStringSubtypeInfo(ingredient, context);
	}

	@Override
	public String getLegacyStringSubtypeInfo(ItemStack ingredient, UidContext context) {
		IIndividual individual = IIndividualHandlerItem.getIndividual(ingredient);
		return individual != null ? individual.getGenome().getActiveSpecies().getBinomial() : "";
	}
}
