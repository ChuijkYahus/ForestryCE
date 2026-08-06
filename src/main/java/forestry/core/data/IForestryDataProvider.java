package forestry.core.data;

import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * How a content jar attaches its own data providers to core's gather event. Core loads these through
 * {@link java.util.ServiceLoader}, the same way {@code IForestryPlugin} is loaded, so core never names a
 * content jar's types directly. Each jar's datagen package lives in that jar's own source set, so the
 * compile classpath enforces what this indirection only asks of it.
 *
 * <p>Implementations are listed in
 * {@code META-INF/services/forestry.core.data.IForestryDataProvider}, in the source set the
 * implementation is compiled from. A jar naming a class it does not carry puts that class's package in
 * two modules and the module layer refuses to build.
 */
public interface IForestryDataProvider {
	/**
	 * Called once during core's gather event, after core has registered its own providers.
	 *
	 * @param event The gather event to register providers with
	 */
	void gather(GatherDataEvent event);
}
