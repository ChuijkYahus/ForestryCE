package forestry.arboriculture.genetics;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import forestry.api.arboriculture.ITreeGenData;
import forestry.api.arboriculture.ITreeGenerator;
import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.arboriculture.IWoodType;
import forestry.api.arboriculture.genetics.ITreeSpeciesType;
import forestry.api.core.HumidityType;
import forestry.api.core.TemperatureType;
import forestry.api.genetics.IGenome;
import forestry.api.plugin.IGenomeBuilder;
import forestry.api.plugin.ISpeciesBuilder;
import forestry.api.plugin.ITreeSpeciesBuilder;

/**
 * Read-only {@link ITreeSpeciesBuilder} adapter over a {@link TreeSpeciesDefinition} + its code-side
 * {@link TreeBlockBindings}: every getter the {@code TreeSpecies}/{@code Species} constructors read is answered from
 * the definition or the bindings; every mutator throws, since datapack species are immutable data.
 *
 * @see TreeSpeciesProjector
 */
public class DefinitionTreeSpeciesBuilder implements ITreeSpeciesBuilder {
	private static final String READ_ONLY_MESSAGE = "datapack species builder is read-only";

	private final TreeSpeciesDefinition def;
	private final TreeBlockBindings bindings;

	public DefinitionTreeSpeciesBuilder(TreeSpeciesDefinition def, TreeBlockBindings bindings) {
		this.def = def;
		this.bindings = bindings;
	}

	// --- genetics getters (from the definition) ---
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
	@Override public float getRarity() { return def.rarity(); }

	// --- block/worldgen getters (from the code-side bindings) ---
	@Override public ITreeGenerator getGenerator() { return bindings.generator(); }
	@Override public List<BlockState> getVanillaLeafStates() { return bindings.vanillaLeafStates(); }
	@Override public List<Item> getVanillaSaplingItems() { return bindings.vanillaSaplingItems(); }
	@Override public ItemStack getDecorativeLeaves() { return bindings.decorativeLeaves(); }

	// --- mutators / factory (all throw) ---
	@Override public ITreeSpeciesBuilder setDominant(boolean dominant) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public ITreeSpeciesBuilder setGenome(Consumer<IGenomeBuilder> genome) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public ITreeSpeciesBuilder setGlint(boolean glint) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public ITreeSpeciesBuilder setTemperature(TemperatureType temperature) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public ITreeSpeciesBuilder setHumidity(HumidityType humidity) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public ITreeSpeciesBuilder setComplexity(int complexity) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public ITreeSpeciesBuilder setEscritoireColor(TextColor color) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public ITreeSpeciesBuilder setSecret(boolean secret) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public ITreeSpeciesBuilder setAuthority(String authority) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public ITreeSpeciesBuilder setFactory(ISpeciesBuilder.ISpeciesFactory<ITreeSpeciesType, ITreeSpecies, ITreeSpeciesBuilder> factory) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public ITreeSpeciesBuilder setTreeFeature(Function<ITreeGenData, Feature<NoneFeatureConfiguration>> factory) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public ITreeSpeciesBuilder setGenerator(ITreeGenerator generator) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public ITreeSpeciesBuilder addVanillaStates(Collection<BlockState> states) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public ITreeSpeciesBuilder addVanillaSapling(Item sapling) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public ITreeSpeciesBuilder setDecorativeLeaves(ItemStack stack) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public ITreeSpeciesBuilder setWoodType(IWoodType woodType) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public ITreeSpeciesBuilder setRarity(float rarity) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public IGenome buildGenome(IGenomeBuilder builder) { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
	@Override public ISpeciesBuilder.ISpeciesFactory<ITreeSpeciesType, ITreeSpecies, ITreeSpeciesBuilder> createSpeciesFactory() { throw new UnsupportedOperationException(READ_ONLY_MESSAGE); }
}
