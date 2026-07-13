package forestry.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.apiculture.ForestryFlowerTypes;
import forestry.api.apiculture.IFlowerType;
import forestry.api.apiculture.genetics.IBeeSpeciesType;
import forestry.apiculture.PhotosynthesisFlowerType;
import forestry.apiculture.TagFlowerType;
import forestry.apiculture.WaterTagFlowerType;
import forestry.core.utils.SpeciesUtil;

@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class FlowerTypeTest {
	private static final ResourceLocation[] ALL = {
		ForestryFlowerTypes.VANILLA, ForestryFlowerTypes.NETHER, ForestryFlowerTypes.CACTI,
		ForestryFlowerTypes.MUSHROOMS, ForestryFlowerTypes.END, ForestryFlowerTypes.JUNGLE,
		ForestryFlowerTypes.SNOW, ForestryFlowerTypes.WHEAT, ForestryFlowerTypes.GOURD,
		ForestryFlowerTypes.CAVE, ForestryFlowerTypes.PHOTOSYNTHESIS, ForestryFlowerTypes.ANCIENT,
		ForestryFlowerTypes.SEA, ForestryFlowerTypes.CORAL, ForestryFlowerTypes.SCULK,
	};

	@GameTest(template = "empty")
	public static void allBuiltinsResolve(GameTestHelper helper) {
		IBeeSpeciesType bees = SpeciesUtil.BEE_TYPE.get();
		for (ResourceLocation id : ALL) {
			IFlowerType type = bees.getFlowerTypeSafe(id);
			if (type == null) {
				helper.fail("Built-in flower type did not load from datapack: " + id);
				return;
			}
		}
		// Spot-check serializer classes + dominance survived the JSON round-trip.
		if (!(bees.getFlowerType(ForestryFlowerTypes.END) instanceof TagFlowerType end) || end.biomes() == null) {
			helper.fail("END should be a TagFlowerType with a biomes tag");
			return;
		}
		if (!(bees.getFlowerType(ForestryFlowerTypes.SEA) instanceof WaterTagFlowerType)) {
			helper.fail("SEA should be a WaterTagFlowerType");
			return;
		}
		if (!(bees.getFlowerType(ForestryFlowerTypes.PHOTOSYNTHESIS) instanceof PhotosynthesisFlowerType)) {
			helper.fail("PHOTOSYNTHESIS should be a PhotosynthesisFlowerType");
			return;
		}
		if (!bees.getFlowerType(ForestryFlowerTypes.VANILLA).isDominant()) {
			helper.fail("VANILLA flower type should be dominant");
			return;
		}
		helper.succeed();
	}
}
