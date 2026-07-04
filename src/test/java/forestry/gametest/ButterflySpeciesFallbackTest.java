package forestry.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.genetics.alleles.ButterflyChromosomes;
import forestry.api.genetics.alleles.IChromosome;
import forestry.api.lepidopterology.genetics.IButterflySpecies;
import forestry.api.lepidopterology.genetics.IButterflySpeciesType;
import forestry.core.utils.SpeciesUtil;

/**
 * Behavioral oracle for {@link ButterflyChromosomes#SPECIES}'s fail-soft resolver: a saved individual can reference a
 * butterfly species id that a datapack has since removed, and resolving it must fall back to the default species
 * instead of throwing (mirrors {@code TreeSpeciesFallbackTest}'s resolver coverage).
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class ButterflySpeciesFallbackTest {
	@GameTest(template = "empty")
	@SuppressWarnings("unchecked")
	public static void speciesResolverFallsSoft(GameTestHelper helper) {
		IButterflySpeciesType type = SpeciesUtil.BUTTERFLY_TYPE.get();

		IChromosome.IReferenceResolver<IButterflySpecies> resolver =
			(IChromosome.IReferenceResolver<IButterflySpecies>) ButterflyChromosomes.SPECIES.resolver();
		ResourceLocation unknownId = ForestryConstants.forestry("does_not_exist");
		IButterflySpecies resolved = resolver.get(unknownId);

		if (resolved != type.getDefaultSpecies()) {
			helper.fail("Expected unknown butterfly species id to resolve to the default species");
			return;
		}
		helper.succeed();
	}
}
