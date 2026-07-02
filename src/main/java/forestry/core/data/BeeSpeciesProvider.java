package forestry.core.data;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import forestry.api.apiculture.ForestryBeeJubilances;
import forestry.api.apiculture.IBeeJubilance;
import forestry.api.apiculture.genetics.IBeeSpeciesType;
import forestry.api.core.IProduct;
import forestry.api.core.Product;
import forestry.api.plugin.IBeeSpeciesBuilder;
import forestry.apiculture.genetics.BeeSpeciesDefinition;
import forestry.apiculture.genetics.DefaultBeeJubilance;
import forestry.apiculture.genetics.HermitBeeJubilance;
import forestry.apiimpl.plugin.ApicultureRegistration;
import forestry.core.utils.SpeciesUtil;
import forestry.plugin.DefaultBeeSpecies;

/**
 * Generates {@code data/forestry/bee_species/*.json} for every built-in bee, read directly from the
 * {@code DefaultBeeSpecies} builders via {@link ApicultureRegistration#forEachSpeciesBuilder} - the same builders the
 * code-registration path uses, so the generated definitions are a faithful parallel artifact of the code-built
 * species (proven by {@code BeeSpeciesEquivalenceTest}).
 * <p>
 * This is a plain {@link DataProvider} rather than a {@code RecipeOutput}-based one: bee species are not recipes, and
 * ModKit's {@code DataHelper} has no generic JSON sink.
 */
public class BeeSpeciesProvider implements DataProvider {
	private final PackOutput.PathProvider pathProvider;
	private final CompletableFuture<HolderLookup.Provider> lookupProvider;

	public BeeSpeciesProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
		this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "bee_species");
		this.lookupProvider = lookupProvider;
	}

	@Override
	public CompletableFuture<?> run(CachedOutput cache) {
		return this.lookupProvider.thenCompose(provider -> {
			RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, provider);

			IBeeSpeciesType type = SpeciesUtil.BEE_TYPE.get();
			ApicultureRegistration reg = new ApicultureRegistration(type);
			// DefaultBeeSpecies.register never calls registerBeeJubilance itself (it sets IBeeJubilance
			// *instances* directly on each builder), so this fresh registration needs the same companion
			// registrations DefaultForestryPlugin#registerApiculture makes, to invert instance -> id below.
			reg.registerBeeJubilance(ForestryBeeJubilances.DEFAULT, DefaultBeeJubilance.INSTANCE);
			reg.registerBeeJubilance(ForestryBeeJubilances.HERMIT, HermitBeeJubilance.INSTANCE);
			DefaultBeeSpecies.register(reg);

			Map<IBeeJubilance, ResourceLocation> jubilanceIds = new IdentityHashMap<>();
			reg.getJubilances().forEach((id, instance) -> jubilanceIds.put(instance, id));

			List<CompletableFuture<?>> futures = new ArrayList<>();
			reg.forEachSpeciesBuilder((id, builder) -> futures.add(saveSpecies(cache, ops, jubilanceIds, id, builder)));
			return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
		});
	}

	private CompletableFuture<?> saveSpecies(CachedOutput cache, RegistryOps<JsonElement> ops, Map<IBeeJubilance, ResourceLocation> jubilanceIds, ResourceLocation id, IBeeSpeciesBuilder builder) {
		RecordingGenomeBuilder rec = new RecordingGenomeBuilder();
		builder.buildGenome(rec);

		ResourceLocation jubilanceId = jubilanceIds.getOrDefault(builder.getJubilance(), ForestryBeeJubilances.DEFAULT);

		BeeSpeciesDefinition def = new BeeSpeciesDefinition(
			builder.getGenus(),
			builder.getSpecies(),
			builder.isDominant(),
			builder.hasGlint(),
			builder.isSecret(),
			builder.getComplexity(),
			builder.getAuthority(),
			builder.getEscritoireColor(),
			builder.getTemperature(),
			builder.getHumidity(),
			builder.getBody(),
			builder.getStripes(),
			builder.getOutline(),
			toProducts(builder.buildProducts()),
			toProducts(builder.buildSpecialties()),
			jubilanceId,
			rec.overrides
		);

		JsonElement json = BeeSpeciesDefinition.codec().encodeStart(ops, def).getOrThrow();
		return DataProvider.saveStable(cache, json, this.pathProvider.json(id));
	}

	private static List<Product> toProducts(List<IProduct> products) {
		List<Product> result = new ArrayList<>(products.size());
		for (IProduct product : products) {
			result.add(toProduct(product));
		}
		return result;
	}

	/**
	 * Converts an arbitrary {@link IProduct} into the static {@link Product} record the data-driven definition can
	 * express. Most products already are {@link Product} instances; the sole exception in the built-ins is
	 * {@code FireworkProduct} (the secret Patriotic bee), whose {@code createRandomStack} produces a randomized
	 * firework at runtime. Its static snapshot - {@link IProduct#createStack()}, which is what both the definition
	 * and {@code FireworkProduct} itself fall back to whenever randomness isn't in play - is captured instead;
	 * only the random-variant behavior is out of scope for a static data definition.
	 */
	private static Product toProduct(IProduct product) {
		if (product instanceof Product p) {
			return p;
		}
		ItemStack stack = product.createStack();
		return new Product(stack.getItem(), stack.getCount(), stack.getComponentsPatch(), product.chance());
	}

	@Override
	public String getName() {
		return "Forestry Bee Species";
	}
}
