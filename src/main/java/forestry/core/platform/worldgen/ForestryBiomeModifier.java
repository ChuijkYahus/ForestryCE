package forestry.core.platform.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.IForestryApi;
import java.util.Optional;

import forestry.api.apiculture.hives.IHive;
import forestry.api.core.climate.IClimateManager;
import forestry.api.core.HumidityType;
import forestry.api.core.TemperatureType;
import forestry.core.platform.config.ForestryConfig;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

// Pass in the feature holders from the codec.
//
// Every field is optional because each jar ships its own modifier naming only the features it owns:
// core the ores, apiculture the hive, arboriculture the tree. A single file naming all four cannot
// work once the jars are optional - a placed feature from an absent jar is an unbound registry value
// and fails world load outright, before any of the guards below get a chance to run.
public record ForestryBiomeModifier(Optional<Holder<PlacedFeature>> hive, Optional<Holder<PlacedFeature>> tree,
									Optional<Holder<PlacedFeature>> apatiteOre,
									Optional<Holder<PlacedFeature>> tinOre) implements BiomeModifier {
	public static final MapCodec<ForestryBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		PlacedFeature.CODEC.optionalFieldOf("hive").forGetter(ForestryBiomeModifier::hive),
		PlacedFeature.CODEC.optionalFieldOf("tree").forGetter(ForestryBiomeModifier::tree),
		PlacedFeature.CODEC.optionalFieldOf("apatite_ore").forGetter(ForestryBiomeModifier::apatiteOre),
		PlacedFeature.CODEC.optionalFieldOf("tin_ore").forGetter(ForestryBiomeModifier::tinOre)
	).apply(instance, ForestryBiomeModifier::new));

	@Override
	public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
		if (phase == Phase.ADD) {
			// server configs are loaded, so ores can be added
			if (biome.is(BiomeTags.IS_OVERWORLD)) {
				if (ForestryConfig.SERVER.spawnTinOre.get()) {
					this.tinOre.ifPresent(feature -> builder.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, feature));
				}
				if (ForestryConfig.SERVER.spawnApatiteOre.get()) {
					this.apatiteOre.ifPresent(feature -> builder.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, feature));
				}
			}

			IClimateManager climates = IForestryApi.INSTANCE.getClimateManager();
			TemperatureType temperature = climates.getTemperature(biome);
			HumidityType humidity = climates.getHumidity(biome);

			this.tree.ifPresent(feature -> builder.getGenerationSettings().addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, feature));

			if (this.hive.isPresent()) {
				for (IHive hive : IForestryApi.INSTANCE.getHiveManager().getHives()) {
					if (hive.isGoodBiome(biome) && hive.isGoodTemperature(temperature) && hive.isGoodHumidity(humidity)) {
						builder.getGenerationSettings().addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, this.hive.get());
						return;
					}
				}
			}
		}
	}

	@Override
	public MapCodec<? extends BiomeModifier> codec() {
		return CODEC;
	}
}
