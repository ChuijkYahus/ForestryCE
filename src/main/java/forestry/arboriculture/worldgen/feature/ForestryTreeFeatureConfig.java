package forestry.arboriculture.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.genetics.IGenome;
import forestry.core.utils.SpeciesUtil;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record ForestryTreeFeatureConfig(IGenome genome) implements FeatureConfiguration {
	public static final Codec<ForestryTreeFeatureConfig> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(builder -> builder.group(
		SpeciesUtil.TREE_TYPE.get().getKaryotype().getGenomeCodec().fieldOf("genome").forGetter(ForestryTreeFeatureConfig::genome)
	).apply(builder, ForestryTreeFeatureConfig::new)));
}
