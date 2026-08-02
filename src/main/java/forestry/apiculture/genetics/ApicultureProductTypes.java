package forestry.apiculture.genetics;

import forestry.api.ForestryConstants;
import forestry.core.engine.genetics.ProductTypes;

/**
 * Registers apiculture's product types into the core {@link ProductTypes} registry. Core owns the
 * registry map and its dispatch codec; the module that owns a product registers it.
 */
public final class ApicultureProductTypes {
	private static boolean builtinsRegistered = false;

	/**
	 * Registers the apiculture product types under the {@code forestry} namespace.
	 * <p>
	 * Must be called before any datapack parse or network sync that can carry a bee product.
	 * Idempotent: repeated calls are no-ops.
	 */
	public static synchronized void registerBuiltins() {
		if (builtinsRegistered) {
			return;
		}
		builtinsRegistered = true;

		ProductTypes.register(ForestryConstants.forestry("firework"), FireworkProduct.TYPE);
	}

	private ApicultureProductTypes() {}
}
