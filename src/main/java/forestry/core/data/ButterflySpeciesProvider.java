package forestry.core.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

import forestry.api.core.IProduct;
import forestry.api.core.Product;
import forestry.api.lepidopterology.genetics.IButterflySpeciesType;
import forestry.api.plugin.IButterflySpeciesBuilder;
import forestry.apiimpl.plugin.LepidopterologyRegistration;
import forestry.core.genetics.GeneticsReloadHandler;
import forestry.core.utils.SpeciesUtil;
import forestry.lepidopterology.genetics.ButterflySpeciesDefinition;
import forestry.plugin.DefaultButterflySpecies;

/**
 * Generates {@code data/forestry/butterfly_species/*.json} for every built-in butterfly/moth, read directly from the
 * {@code DefaultButterflySpecies} builders via {@link LepidopterologyRegistration#forEachSpeciesBuilder} - the same
 * builders the code-registration path uses, so the generated definitions are a faithful parallel artifact of the
 * code-built species (proven by {@code ButterflySpeciesEquivalenceTest}).
 * <p>
 * Like {@link BeeSpeciesProvider} (and unlike {@link TreeSpeciesProvider}), butterflies set some reference
 * chromosomes (cocoon, effect, flower type) via the id-based {@code IGenomeBuilder#set(IChromosome, ResourceLocation)}
 * overload, so {@link RecordingGenomeBuilder} already records them as {@code Allele.reference(id)}. Unlike bees,
 * none of the built-in butterflies register a cocoon/effect *instance* directly on the builder (only bee jubilance
 * does that), so no companion instance -&gt; id inversion map is needed here.
 */
public class ButterflySpeciesProvider implements DataProvider {
	private final PackOutput.PathProvider pathProvider;
	private final CompletableFuture<HolderLookup.Provider> lookupProvider;

	public ButterflySpeciesProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
		this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "butterfly_species");
		this.lookupProvider = lookupProvider;
	}

	@Override
	public CompletableFuture<?> run(CachedOutput cache) {
		return this.lookupProvider.thenCompose(provider -> {
			RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, provider);

			List<CompletableFuture<?>> futures = new ArrayList<>();
			buildDefinitions().forEach((id, def) -> futures.add(saveSpecies(cache, ops, id, def)));
			return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
		});
	}

	/**
	 * Builds every built-in butterfly species definition straight from the {@code DefaultButterflySpecies} builders -
	 * the same definitions {@link #run} serializes to {@code butterfly_species/*.json}. Needs no registry access
	 * (unlike {@link #run}, which only needs {@link RegistryOps} to encode the result to JSON), so it can also be
	 * used to seed the live species type - see {@link #seedLiveSpeciesForDatagen()}.
	 */
	public static Map<ResourceLocation, ButterflySpeciesDefinition> buildDefinitions() {
		IButterflySpeciesType type = SpeciesUtil.BUTTERFLY_TYPE.get();
		LepidopterologyRegistration reg = new LepidopterologyRegistration(type);
		DefaultButterflySpecies.register(reg);

		Map<ResourceLocation, ButterflySpeciesDefinition> definitions = new LinkedHashMap<>();
		reg.forEachSpeciesBuilder((id, builder) -> definitions.put(id, buildDefinition(builder)));
		return definitions;
	}

	/**
	 * Populates the live butterfly species type directly from {@link #buildDefinitions()}, bypassing the datapack
	 * JSON round trip. Only for use by the standalone data generator ({@code Data#preDataGen}): a data-generator
	 * invocation never fires the {@code AddReloadListenerEvent}/datapack-reload cycle that loads species at real
	 * server start. Species built here come from the identical {@code DefaultButterflySpecies} source the generated
	 * JSON itself is derived from, so this does not reintroduce a second, divergent species source.
	 */
	public static void seedLiveSpeciesForDatagen() {
		GeneticsReloadHandler.rebuildButterflySpecies(buildDefinitions());
	}

	private static ButterflySpeciesDefinition buildDefinition(IButterflySpeciesBuilder builder) {
		RecordingGenomeBuilder rec = new RecordingGenomeBuilder();
		builder.buildGenome(rec);

		return new ButterflySpeciesDefinition(
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
			builder.isNocturnal(),
			builder.isMoth(),
			builder.getRarity(),
			builder.getFlightDistance(),
			builder.getSerumColor(),
			Optional.ofNullable(builder.getSpawnBiomes()),
			toProducts(builder.buildProducts()),
			toProducts(builder.buildCaterpillarProducts()),
			rec.overrides
		);
	}

	private CompletableFuture<?> saveSpecies(CachedOutput cache, RegistryOps<JsonElement> ops, ResourceLocation id, ButterflySpeciesDefinition def) {
		JsonElement json = ButterflySpeciesDefinition.codec().encodeStart(ops, def).getOrThrow();
		return DataProvider.saveStable(cache, json, this.pathProvider.json(id));
	}

	private static List<IProduct> toProducts(List<IProduct> products) {
		List<IProduct> result = new ArrayList<>(products.size());
		for (IProduct product : products) {
			result.add(toProduct(product));
		}
		return result;
	}

	/**
	 * Converts an arbitrary {@link IProduct} into the static {@link Product} record the data-driven definition can
	 * express. No built-in butterfly currently authors any products/caterpillar products ({@code
	 * ButterflySpeciesBuilder#buildProducts}/{@code #buildCaterpillarProducts} are stubs returning {@code List.of()}),
	 * so this is a no-op today; kept for parity with {@link BeeSpeciesProvider} so loot support can be added later
	 * without revisiting this conversion.
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
		return "Forestry Butterfly Species";
	}
}
