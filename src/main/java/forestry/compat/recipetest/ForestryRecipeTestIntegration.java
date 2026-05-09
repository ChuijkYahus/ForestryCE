package forestry.compat.recipetest;

import dev.recipetest.core.RecipeAdapters;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry point that registers Forestry's {@link dev.recipetest.core.RecipeAdapter} implementations
 * with the {@link RecipeAdapters} registry.
 *
 * <p>This class lives in its own package and references the kit's classes directly, so it must
 * be class-loaded only when the kit mod is on the classpath. The caller (Forestry's main mod
 * class) gates the call behind a {@code ModList.isLoaded("recipe_test")} check; this class's
 * own bytecode then references the kit's types and the JVM resolves them lazily at the call
 * site, with no NPE risk if the kit is absent.
 *
 * <p>Adapters are inserted at the front of the registry by {@code RecipeAdapters.register},
 * so they take precedence over the kit's vanilla shaped/shapeless catch-alls and the
 * generic-recipe fallback. Each adapter's {@link dev.recipetest.core.RecipeAdapter#appliesTo}
 * narrows to its own {@code IFooRecipe} subtype.
 */
public final class ForestryRecipeTestIntegration {

	private static final Logger LOGGER = LogManager.getLogger("forestry/recipetest");

	private ForestryRecipeTestIntegration() {}

	/**
	 * Register every Forestry-specific {@link dev.recipetest.core.RecipeAdapter} with the
	 * Recipe Test Kit. Idempotent at the level of behaviour — re-registration just shadows
	 * the older instance with a newer one of the same class, which {@code findFor} resolves
	 * to the same type. Safe to invoke from {@code FMLCommonSetupEvent}.
	 */
	public static void register() {
		RecipeAdapters.register(new ForestryCarpenterAdapter());
		RecipeAdapters.register(new ForestrySqueezerAdapter());
		RecipeAdapters.register(new ForestryFermenterAdapter());
		RecipeAdapters.register(new ForestryStillAdapter());
		RecipeAdapters.register(new ForestryFabricatorAdapter());
		LOGGER.info("Registered 5 Forestry recipe-test adapters (carpenter, squeezer, fermenter, still, fabricator)");
	}
}
