package forestry.core.data;

import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * How a content jar attaches its own data providers to core's gather event. Core loads these through
 * {@link java.util.ServiceLoader}, the same way {@code IForestryPlugin} is loaded, so core never names a
 * content jar's types and the compile classpath keeps enforcing what each jar can see.
 *
 * <p>Implementations are listed in
 * {@code META-INF/services/forestry.core.data.IForestryDataProvider} in their own jar.
 */
public interface IForestryDataProvider {
	/**
	 * Called once during core's gather event, after core has registered its own providers.
	 *
	 * @param event The gather event to register providers with
	 */
	void gather(GatherDataEvent event);
}
