package forestry;

import forestry.api.ForestryConstants;
import forestry.api.IForestryApi;
import forestry.apiimpl.plugin.PluginManager;
import forestry.core.EventHandlerCore;
import forestry.core.config.ForestryConfig;
import forestry.modules.ForestryModuleManager;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ForestryConstants.MOD_ID)
public class Forestry {
	public static final boolean DEBUG = ModList.get().isLoaded("modkit");
	public static final Logger LOGGER = LogManager.getLogger(ForestryConstants.MOD_ID);

	public Forestry(ModContainer modContainer) {
		// IForestryModule - registration of game objects
		ForestryModuleManager moduleManager = (ForestryModuleManager) IForestryApi.INSTANCE.getModuleManager();
		moduleManager.init();
		NeoForge.EVENT_BUS.register(EventHandlerCore.class);

		// IForestryPlugin - registration of Forestry data
		PluginManager.loadPlugins();
		PluginManager.registerErrors();

		ForestryConfig.register(modContainer);

		NeoForgeMod.enableMilkFluid();

		// The recipe-test integration shim is only compiled when the kit jar is in run/mods/
		// (see build.gradle). Reflective dispatch keeps Forestry.java compilable on machines
		// without the kit — the class is also gated by ModList so it only loads at runtime
		// when the kit mod is actually present.
		if (ModList.get().isLoaded("recipe_test")) {
			try {
				Class.forName("forestry.compat.recipetest.ForestryRecipeTestIntegration")
						.getMethod("register").invoke(null);
			} catch (ReflectiveOperationException e) {
				LOGGER.warn("recipe_test mod loaded but Forestry adapter shim missing on classpath", e);
			}
		}
	}
}
