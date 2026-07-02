package forestry.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.arboriculture.genetics.ITreeSpeciesType;
import forestry.api.genetics.alleles.IChromosome;
import forestry.api.genetics.alleles.TreeChromosomes;
import forestry.core.utils.SpeciesUtil;

/**
 * Behavioral oracle for {@link TreeChromosomes#SPECIES}'s fail-soft resolver: a saved individual can reference a
 * tree species id that a datapack has since removed, and resolving it must fall back to the default species
 * instead of throwing (mirrors {@code SpeciesFallbackTest}'s bee coverage).
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class TreeSpeciesFallbackTest {
	@GameTest(template = "empty")
	@SuppressWarnings("unchecked")
	public static void unknownSpeciesIdResolvesToDefault(GameTestHelper helper) {
		ITreeSpeciesType type = SpeciesUtil.TREE_TYPE.get();

		IChromosome.IReferenceResolver<ITreeSpecies> resolver =
			(IChromosome.IReferenceResolver<ITreeSpecies>) TreeChromosomes.SPECIES.resolver();
		ResourceLocation unknownId = ForestryConstants.forestry("does_not_exist");
		ITreeSpecies resolved = resolver.get(unknownId);

		if (resolved != type.getDefaultSpecies()) {
			helper.fail("Expected unknown tree species id to resolve to the default species");
			return;
		}
		helper.succeed();
	}
}
