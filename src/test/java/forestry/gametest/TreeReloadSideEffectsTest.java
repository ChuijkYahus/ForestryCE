package forestry.gametest;

import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.arboriculture.genetics.ITreeSpeciesType;
import forestry.core.genetics.SpeciesType;
import forestry.core.utils.SpeciesUtil;

/**
 * Exercises {@code TreeSpeciesType}'s overridden {@code setSpecies} directly, without going through
 * {@code GeneticsReloadHandler.rebuildTreeSpecies}/{@code TreeSpeciesProvider} (Task 8, not yet implemented).
 * <p>
 * Re-applies the CURRENT full live species map back through {@code setSpecies} (same objects, no identity churn),
 * so the live {@code TREE_TYPE} state is unchanged after this test runs and no restore is needed. This still
 * exercises the override's side effects (vanilla-membership rebuild + {@code ForestryLeafType} rewiring) and proves
 * {@code leafTickHandlers} survive the swap untouched, which is what the butterfly-registered {@code ButterflySpawner}
 * depends on across a real reload.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class TreeReloadSideEffectsTest {
	@GameTest(template = "empty")
	@SuppressWarnings("unchecked")
	public static void sideEffectsSurviveReload(GameTestHelper helper) {
		ITreeSpeciesType type = SpeciesUtil.TREE_TYPE.get();
		int handlersBefore = type.getLeafTickHandlers().size();

		ImmutableMap<ResourceLocation, ITreeSpecies> full = ImmutableMap.copyOf(
			type.getAllSpeciesIds().stream().collect(Collectors.toMap(id -> id, type::getSpecies))
		);

		((SpeciesType<ITreeSpecies, ?>) type).setSpecies(full);

		// vanilla membership rebuilt: find a species with vanilla leaf states and confirm the map is populated.
		// If none has vanilla states, there's nothing to assert for this half - skip it (still succeed).
		for (ITreeSpecies species : type.getAllSpecies()) {
			if (!species.getVanillaLeafStates().isEmpty()) {
				if (type.getVanillaIndividual(species.getVanillaLeafStates().get(0)) == null) {
					helper.fail("Expected vanilla-membership map to be rebuilt after setSpecies for species " + species.getSpeciesName());
					return;
				}
				break;
			}
		}

		// leaf-tick handlers untouched by the swap
		if (type.getLeafTickHandlers().size() != handlersBefore) {
			helper.fail("Expected leaf-tick handlers to survive a tree species setSpecies swap (was " + handlersBefore + ", now " + type.getLeafTickHandlers().size() + ")");
			return;
		}

		helper.succeed();
	}
}
