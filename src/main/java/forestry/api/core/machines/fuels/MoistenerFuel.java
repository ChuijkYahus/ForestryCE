package forestry.api.core.machines.fuels;

import com.google.common.base.Preconditions;
import net.minecraft.world.item.ItemStack;

/**
 * todo data driven
 *
 * @param resource       The item to use.
 * @param product        The item that leaves the moistener's working slot (i.e. mouldy wheat, decayed wheat, mulch)
 * @param stage          How much this item contributes to the final product of the moistener (i.e. mycelium)
 * @param moistenerValue What stage this product represents. Resources with lower stage value will be consumed first.
 */
public record MoistenerFuel(ItemStack resource, ItemStack product, int stage, int moistenerValue) {
	public MoistenerFuel {
		Preconditions.checkArgument(!resource.isEmpty());
		Preconditions.checkArgument(!product.isEmpty());
	}
}
