package forestry.core.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import forestry.api.genetics.ISpeciesType;
import forestry.api.genetics.alleles.Allele;
import net.minecraft.resources.ResourceLocation;

public record AlleleArgument(ISpeciesType<?, ?> type) implements ISpeciesArgumentType<Allele<?>> {
	@Override
	public Allele<?> parse(StringReader reader) throws CommandSyntaxException {
		ResourceLocation id = ResourceLocation.read(reader);
		// Alleles are no longer interned in a global registry, so there is nothing to look up by id here.
		// Wrap the parsed id as a reference allele; the chromosome-aware resolution/validation happens in the
		// consuming command (ModifyGenomeCommand), which knows the target chromosome.
		return Allele.reference(id);
	}
}
