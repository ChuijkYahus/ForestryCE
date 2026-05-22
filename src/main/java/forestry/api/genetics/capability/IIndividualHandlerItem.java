package forestry.api.genetics.capability;

import forestry.api.genetics.IGenome;
import forestry.api.genetics.IIndividual;
import forestry.api.genetics.ILifeStage;
import forestry.api.genetics.ISpecies;
import forestry.api.genetics.ISpeciesType;
import forestry.core.genetics.ItemGE;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Stack helpers for genetic individuals stored in data components.
 * As of 1.21.1, items no longer use capabilities, but Data Components instead.
 */
public interface IIndividualHandlerItem {
	static void ifPresent(ItemStack stack, BiConsumer<IIndividual, ILifeStage> action) {
		ItemGE.ifPresent(stack, action);
	}

	static void ifPresent(ItemStack stack, Consumer<IIndividual> action) {
		ItemGE.ifPresent(stack, action);
	}

	/**
	 * @return Whether the given item has an individual capability. (Vanilla saplings have a capability too)
	 */
	static boolean isIndividual(ItemStack stack) {
		return ItemGE.isIndividual(stack);
	}

	static boolean hasIndividual(ItemStack stack) {
		return ItemGE.hasIndividual(stack);
	}

	/**
	 * Checks if the individual in this stack is present and if it matches some predicate.
	 *
	 * @param stack     The item to retrieve the individual from.
	 * @param predicate The predicate to test on the individual.
	 * @return {@code true} if the individual was present and the predicate returned true, false otherwise.
	 */
	static boolean filter(ItemStack stack, Predicate<IIndividual> predicate) {
		return ItemGE.filter(stack, predicate);
	}

	static boolean filter(ItemStack stack, BiPredicate<IIndividual, ILifeStage> predicate) {
		return ItemGE.filter(stack, predicate);
	}

	@Nullable
	static IIndividual getIndividual(ItemStack stack) {
		return ItemGE.getIndividual(stack);
	}

	@Nullable
	static IGenome getGenome(ItemStack stack) {
		return ItemGE.getGenome(stack);
	}

	@Nullable
	static ILifeStage getLifeStage(ItemStack stack) {
		return ItemGE.getLifeStage(stack);
	}

	@Nullable
	static ISpeciesType<?, ?> getSpeciesType(ItemStack stack) {
		return ItemGE.getSpeciesType(stack);
	}

	/**
	 * Gets the species of the current item stack, or returns the default species for the species type.
	 */
	static <S extends ISpecies<?>> S getSpecies(ItemStack stack, ISpeciesType<S, ?> type) {
		return ItemGE.getSpecies(stack, type);
	}
}
