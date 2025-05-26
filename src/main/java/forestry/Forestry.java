package forestry;

import forestry.api.ForestryConstants;
import forestry.api.IForestryApi;
import forestry.apiimpl.plugin.PluginManager;
import forestry.core.EventHandlerCore;
import forestry.core.config.ForestryConfig;
import forestry.core.network.NetworkHandler;
import forestry.modules.ForestryModuleManager;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Forestry Minecraft Mod
 *
 * @author SirSengir
 */
@Mod(ForestryConstants.MOD_ID)
public class Forestry {
	public static final boolean DEBUG = ModList.get().isLoaded("modkit");
	public static final Logger LOGGER = LogManager.getLogger(ForestryConstants.MOD_ID);

	public Forestry() {
		ForestryModuleManager moduleManager = (ForestryModuleManager) IForestryApi.INSTANCE.getModuleManager();
		moduleManager.init();
		NetworkHandler.register();
		MinecraftForge.EVENT_BUS.register(EventHandlerCore.class);

		PluginManager.loadPlugins();
		PluginManager.registerErrors();

		ForestryConfig.register(ModLoadingContext.get());

		ForgeMod.enableMilkFluid();
	}
}
