package forestry.core.genetics;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.Forestry;
import forestry.api.genetics.*;
import forestry.core.features.CoreDataComponents;
import forestry.core.utils.SpeciesUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.Optional;

public abstract class Individual<S extends ISpecies<I>, I extends IIndividual, T extends ISpeciesType<S, I>> implements IIndividual {
	protected final S species;
	protected final S inactiveSpecies;
	protected final IGenome genome;

	@Nullable
	protected IGenome mate;
	protected boolean analyzed;

	protected Individual(IGenome genome) {
		this.species = genome.getActiveSpecies();
		this.inactiveSpecies = genome.getInactiveSpecies();
		this.genome = genome;
	}

	// For codec
	protected Individual(IGenome genome, Optional<IGenome> mate, boolean analyzed) {
		this(genome);

		this.mate = mate.orElse(null);
		this.analyzed = analyzed;
	}

	// For "inheritance" in codecs
	protected static <I extends IIndividual> Products.P3<RecordCodecBuilder.Mu<I>, IGenome, Optional<IGenome>, Boolean> fields(RecordCodecBuilder.Instance<I> instance, Codec<IGenome> genomeCodec) {
		return instance.group(
			genomeCodec.fieldOf("genome").forGetter(I::getGenome),
			genomeCodec.optionalFieldOf("mate").forGetter(I::getMateOptional),
			Codec.BOOL.fieldOf("analyzed").forGetter(I::isAnalyzed)
		);
	}

	@Override
	public void setMate(@Nullable IGenome mate) {
		if (mate == null || this.genome.getKaryotype() == mate.getKaryotype()) {
			this.mate = mate;
		}
	}

	@Nullable
	@Override
	public IGenome getMate() {
		return this.mate;
	}

	public Optional<IGenome> getMateOptional() {
		return Optional.ofNullable(this.mate);
	}

	@Override
	public IGenome getGenome() {
		return this.genome;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T getType() {
		return (T) this.species.getType();
	}

	@Override
	public S getSpecies() {
		return this.species;
	}

	@Override
	public S getInactiveSpecies() {
		return this.inactiveSpecies;
	}

	@Override
	public boolean isAnalyzed() {
		return this.analyzed;
	}

	@Override
	public boolean analyze() {
		if (this.analyzed) {
			return false;
		}

		this.analyzed = true;
		return true;
	}

	@Override
	public I copy() {
		return copyWithGenome(this.genome);
	}

	@Override
	public I copyWithGenome(IGenome newGenome) {
		I individual = this.species.createIndividual(newGenome);
		copyPropertiesTo(individual);
		return individual;
	}

	@OverridingMethodsMustInvokeSuper
	protected void copyPropertiesTo(I other) {
	}

	@Override
	public void saveToStack(ItemStack stack) {
		Tag individual = SpeciesUtil.serializeIndividual(this);

		if (individual instanceof CompoundTag tag) {
			stack.set(CoreDataComponents.INDIVIDUAL, tag);
		} else if (individual != null) {
			// Every IIndividual codec is built via RecordCodecBuilder, so the encoded shape must
			// be a CompoundTag. A non-compound result here means a codec returned the wrong shape
			// and the genome would silently drop on save — log loudly so it gets fixed.
			Forestry.LOGGER.error(
					"Refusing to save genome data for {}: codec produced {} ({}) instead of CompoundTag. Stack: {}",
					this.species.id(), individual.getClass().getName(), individual, stack);
		}
	}

	@Override
	public ItemStack createStack(ILifeStage stage) {
		ItemStack stack = new ItemStack(stage.getItemForm());
		saveToStack(stack);
		return stack;
	}
}
