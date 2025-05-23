package forestry.arboriculture.worldgen.feature;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.genetics.IGenome;
import forestry.arboriculture.commands.TreeGenHelper;

public class ForestryTreeFeature extends Feature<ForestryTreeFeatureConfig> {
	public ForestryTreeFeature() {
		super(ForestryTreeFeatureConfig.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<ForestryTreeFeatureConfig> context) {
		IGenome genome = context.config().genome();
		ITreeSpecies species = genome.getActiveSpecies().cast();

		return TreeGenHelper.generateTree(species, genome, context.level(), context.random(), context.origin());
	}
}
