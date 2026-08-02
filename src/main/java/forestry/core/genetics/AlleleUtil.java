package forestry.core.genetics;

import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.ISpecies;
import forestry.api.core.genetics.ISpeciesType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class AlleleUtil {
	@Nullable
	public static <S extends ISpecies<I>, I extends IIndividual> S getSpecies(ISpeciesType<S, I> speciesType, CompoundTag nbt, String key) {
		String idString = nbt.getString(key);
		if (idString.isEmpty()) {
			return null;
		}
		ResourceLocation id = ResourceLocation.parse(idString);
		return speciesType.getSpecies(id);
	}
}
