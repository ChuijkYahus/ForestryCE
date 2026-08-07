package forestry.apiculture.worldgen;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import forestry.api.ForestryConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VillagerJigsaw {
	private static final ResourceKey<StructureProcessorList> EMPTY_PROCESSOR_LIST_KEY = ResourceKey.create(Registries.PROCESSOR_LIST, ResourceLocation.fromNamespaceAndPath("minecraft", "empty"));

	public static void init(Registry<StructureTemplatePool> pools, Registry<StructureProcessorList> processors) {
		addVillagerHouse(pools, processors, "plains", 15);
		addVillagerHouse(pools, processors, "snowy", 15);
		addVillagerHouse(pools, processors, "savanna", 15);
		addVillagerHouse(pools, processors, "desert", 15);
		addVillagerHouse(pools, processors, "taiga", 15);
	}

	private static void addVillagerHouse(Registry<StructureTemplatePool> pools, Registry<StructureProcessorList> processors, String biome, int weight) {
		addToJigsawPattern(pools, ResourceLocation.parse("village/" + biome + "/houses"), new ApiaristPoolElement(Either.left(ForestryConstants.forestry("village/apiarist_house_" + biome + "_1")), processors.getHolderOrThrow(EMPTY_PROCESSOR_LIST_KEY), StructureTemplatePool.Projection.RIGID, Optional.empty()), weight);
	}

	public static void addToJigsawPattern(Registry<StructureTemplatePool> pools, ResourceLocation pool, StructurePoolElement newPiece, int weight) {
		StructureTemplatePool oldPool = pools.get(pool);
		if (oldPool != null) {
			List<StructurePoolElement> jigsawPieces = oldPool.templates;

			for (int i = 0; i < weight; ++i) {
				jigsawPieces.add(newPiece);
			}

			List<Pair<StructurePoolElement, Integer>> listOfPieceEntries = new ArrayList<>(oldPool.rawTemplates);
			listOfPieceEntries.add(new Pair<>(newPiece, weight));
			oldPool.rawTemplates = listOfPieceEntries;
		}
	}
}
