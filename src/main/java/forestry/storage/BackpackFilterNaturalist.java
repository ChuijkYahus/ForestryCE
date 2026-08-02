package forestry.storage;

import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public class BackpackFilterNaturalist implements Predicate<ItemStack> {
	private final ResourceLocation speciesRootUid;

	public BackpackFilterNaturalist(ResourceLocation speciesType) {
		this.speciesRootUid = speciesType;
	}

	@Override
	public boolean test(ItemStack stack) {
		return IIndividualHandlerItem.filter(stack, individual -> this.speciesRootUid.equals(individual.getType().id()));
	}
}
