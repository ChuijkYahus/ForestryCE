package forestry.apiculture.genetics;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;

import forestry.api.apiculture.IBeeJubilance;
import forestry.api.apiculture.genetics.IBeeSpecies;
import forestry.api.apiculture.genetics.IBeeSpeciesType;
import forestry.api.core.HumidityType;
import forestry.api.core.IProduct;
import forestry.api.core.TemperatureType;
import forestry.api.genetics.IGenome;
import forestry.api.plugin.IBeeSpeciesBuilder;
import forestry.api.plugin.IGenomeBuilder;
import forestry.api.plugin.ISpeciesBuilder;

/**
 * Read-only {@link IBeeSpeciesBuilder} adapter over a {@link BeeSpeciesDefinition}: every getter the
 * {@code BeeSpecies}/{@code Species} constructors read is answered from the definition (or the resolved
 * {@link IBeeJubilance}); every mutator throws, since datapack species are immutable data, not code-built.
 *
 * @see BeeSpeciesProjector
 */
public class DefinitionBeeSpeciesBuilder implements IBeeSpeciesBuilder {
	private static final String READ_ONLY_MESSAGE = "datapack species builder is read-only";

	private final BeeSpeciesDefinition def;
	private final IBeeJubilance jubilance;

	public DefinitionBeeSpeciesBuilder(BeeSpeciesDefinition def, IBeeJubilance jubilance) {
		this.def = def;
		this.jubilance = jubilance;
	}

	@Override
	public String getGenus() {
		return def.genus();
	}

	@Override
	public String getSpecies() {
		return def.species();
	}

	@Override
	public boolean isDominant() {
		return def.dominant();
	}

	@Override
	public boolean hasGlint() {
		return def.glint();
	}

	@Override
	public boolean isSecret() {
		return def.secret();
	}

	@Override
	public int getComplexity() {
		return def.complexity();
	}

	@Override
	public String getAuthority() {
		return def.authority();
	}

	@Override
	public int getEscritoireColor() {
		return def.escritoireColor();
	}

	@Override
	public TemperatureType getTemperature() {
		return def.temperature();
	}

	@Override
	public HumidityType getHumidity() {
		return def.humidity();
	}

	@Override
	public List<IProduct> buildProducts() {
		return List.copyOf(def.products());
	}

	@Override
	public List<IProduct> buildSpecialties() {
		return List.copyOf(def.specialties());
	}

	@Override
	public int getBody() {
		return def.body();
	}

	@Override
	public int getStripes() {
		return def.stripes();
	}

	@Override
	public int getOutline() {
		return def.outline();
	}

	@Override
	public IBeeJubilance getJubilance() {
		return this.jubilance;
	}

	@Override
	public IBeeSpeciesBuilder addProduct(IProduct product) {
		throw new UnsupportedOperationException(READ_ONLY_MESSAGE);
	}

	@Override
	public IBeeSpeciesBuilder addProduct(ItemStack stack, float chance) {
		throw new UnsupportedOperationException(READ_ONLY_MESSAGE);
	}

	@Override
	public IBeeSpeciesBuilder addSpecialty(IProduct specialty) {
		throw new UnsupportedOperationException(READ_ONLY_MESSAGE);
	}

	@Override
	public IBeeSpeciesBuilder addSpecialty(ItemStack stack, float chance) {
		throw new UnsupportedOperationException(READ_ONLY_MESSAGE);
	}

	@Override
	public IBeeSpeciesBuilder setBody(TextColor color) {
		throw new UnsupportedOperationException(READ_ONLY_MESSAGE);
	}

	@Override
	public IBeeSpeciesBuilder setStripes(TextColor color) {
		throw new UnsupportedOperationException(READ_ONLY_MESSAGE);
	}

	@Override
	public IBeeSpeciesBuilder setOutline(TextColor color) {
		throw new UnsupportedOperationException(READ_ONLY_MESSAGE);
	}

	@Override
	public IBeeSpeciesBuilder setJubilance(IBeeJubilance jubilance) {
		throw new UnsupportedOperationException(READ_ONLY_MESSAGE);
	}

	@Override
	public IBeeSpeciesBuilder setDominant(boolean dominant) {
		throw new UnsupportedOperationException(READ_ONLY_MESSAGE);
	}

	@Override
	public IBeeSpeciesBuilder setGenome(Consumer<IGenomeBuilder> genome) {
		throw new UnsupportedOperationException(READ_ONLY_MESSAGE);
	}

	@Override
	public IBeeSpeciesBuilder setGlint(boolean glint) {
		throw new UnsupportedOperationException(READ_ONLY_MESSAGE);
	}

	@Override
	public IBeeSpeciesBuilder setTemperature(TemperatureType temperature) {
		throw new UnsupportedOperationException(READ_ONLY_MESSAGE);
	}

	@Override
	public IBeeSpeciesBuilder setHumidity(HumidityType humidity) {
		throw new UnsupportedOperationException(READ_ONLY_MESSAGE);
	}

	@Override
	public IBeeSpeciesBuilder setComplexity(int complexity) {
		throw new UnsupportedOperationException(READ_ONLY_MESSAGE);
	}

	@Override
	public IBeeSpeciesBuilder setEscritoireColor(TextColor color) {
		throw new UnsupportedOperationException(READ_ONLY_MESSAGE);
	}

	@Override
	public IBeeSpeciesBuilder setSecret(boolean secret) {
		throw new UnsupportedOperationException(READ_ONLY_MESSAGE);
	}

	@Override
	public IBeeSpeciesBuilder setAuthority(String authority) {
		throw new UnsupportedOperationException(READ_ONLY_MESSAGE);
	}

	@Override
	public IBeeSpeciesBuilder setFactory(ISpeciesBuilder.ISpeciesFactory<IBeeSpeciesType, IBeeSpecies, IBeeSpeciesBuilder> factory) {
		throw new UnsupportedOperationException(READ_ONLY_MESSAGE);
	}

	@Override
	public IGenome buildGenome(IGenomeBuilder builder) {
		throw new UnsupportedOperationException(READ_ONLY_MESSAGE);
	}

	@Override
	public ISpeciesBuilder.ISpeciesFactory<IBeeSpeciesType, IBeeSpecies, IBeeSpeciesBuilder> createSpeciesFactory() {
		throw new UnsupportedOperationException(READ_ONLY_MESSAGE);
	}
}
