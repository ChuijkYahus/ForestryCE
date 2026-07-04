package forestry.lepidopterology.genetics;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.network.chat.TextColor;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import forestry.api.core.HumidityType;
import forestry.api.core.IProduct;
import forestry.api.core.TemperatureType;
import forestry.api.genetics.IGenome;
import forestry.api.lepidopterology.genetics.IButterflySpecies;
import forestry.api.lepidopterology.genetics.IButterflySpeciesType;
import forestry.api.plugin.IButterflySpeciesBuilder;
import forestry.api.plugin.IGenomeBuilder;
import forestry.api.plugin.ISpeciesBuilder;

/**
 * Read-only {@link IButterflySpeciesBuilder} adapter over a {@link ButterflySpeciesDefinition}: every getter the
 * {@code ButterflySpecies}/{@code Species} constructors read is answered from the definition; every mutator throws,
 * since datapack species are immutable data, not code-built. Butterflies have no code-side block/worldgen bindings
 * (unlike trees), so there is nothing else to adapt.
 *
 * @see ButterflySpeciesProjector
 */
public class DefinitionButterflySpeciesBuilder implements IButterflySpeciesBuilder {
	private static final String READ_ONLY_MESSAGE = "datapack species builder is read-only";

	private final ButterflySpeciesDefinition def;

	public DefinitionButterflySpeciesBuilder(ButterflySpeciesDefinition def) {
		this.def = def;
	}

	// --- genetics/metadata getters (from the definition) ---
	@Override public String getGenus() { return def.genus(); }
	@Override public String getSpecies() { return def.species(); }
	@Override public boolean isDominant() { return def.dominant(); }
	@Override public boolean hasGlint() { return def.glint(); }
	@Override public boolean isSecret() { return def.secret(); }
	@Override public int getComplexity() { return def.complexity(); }
	@Override public String getAuthority() { return def.authority(); }
	@Override public int getEscritoireColor() { return def.escritoireColor(); }
	@Override public TemperatureType getTemperature() { return def.temperature(); }
	@Override public HumidityType getHumidity() { return def.humidity(); }
	@Override public boolean isNocturnal() { return def.nocturnal(); }
	@Override public boolean isMoth() { return def.moth(); }
	@Override public float getRarity() { return def.rarity(); }
	@Override public float getFlightDistance() { return def.flightDistance(); }
	@Override public int getSerumColor() { return def.serumColor(); }

	@Nullable
	@Override
	public TagKey<Biome> getSpawnBiomes() {
		return def.spawnBiomes().orElse(null);
	}

	@Override
	public List<IProduct> buildProducts() {
		return List.copyOf(def.products());
	}

	@Override
	public List<IProduct> buildCaterpillarProducts() {
		return List.copyOf(def.caterpillarProducts());
	}

	// --- mutators / factory (all throw) ---
	@Override public IButterflySpeciesBuilder setSerumColor(TextColor color) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public IButterflySpeciesBuilder setFlightDistance(float flightDistance) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public IButterflySpeciesBuilder setNocturnal(boolean nocturnal) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public IButterflySpeciesBuilder setMoth(boolean moth) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public IButterflySpeciesBuilder setSpawnBiomes(TagKey<Biome> biomeTag) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public IButterflySpeciesBuilder setRarity(float rarity) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public IButterflySpeciesBuilder setDominant(boolean dominant) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public IButterflySpeciesBuilder setGenome(Consumer<IGenomeBuilder> genome) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public IButterflySpeciesBuilder setGlint(boolean glint) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public IButterflySpeciesBuilder setTemperature(TemperatureType temperature) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public IButterflySpeciesBuilder setHumidity(HumidityType humidity) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public IButterflySpeciesBuilder setComplexity(int complexity) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public IButterflySpeciesBuilder setEscritoireColor(TextColor color) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public IButterflySpeciesBuilder setSecret(boolean secret) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public IButterflySpeciesBuilder setAuthority(String authority) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public IButterflySpeciesBuilder setFactory(ISpeciesBuilder.ISpeciesFactory<IButterflySpeciesType, IButterflySpecies, IButterflySpeciesBuilder> factory) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public IGenome buildGenome(IGenomeBuilder builder) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public ISpeciesBuilder.ISpeciesFactory<IButterflySpeciesType, IButterflySpecies, IButterflySpeciesBuilder> createSpeciesFactory() { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
}
