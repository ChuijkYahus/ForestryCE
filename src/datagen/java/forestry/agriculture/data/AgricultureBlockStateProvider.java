package forestry.agriculture.data;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.CompositeModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import forestry.agriculture.features.CultivationBlocks;
import forestry.agriculture.features.FarmingBlocks;
import forestry.agriculture.multifarm.blocks.EnumFarmBlockType;
import forestry.agriculture.multifarm.blocks.EnumFarmMaterial;
import forestry.agriculture.multifarm.blocks.FarmBlock;
import forestry.agriculture.planter.blocks.BlockTypePlanter;
import forestry.api.ForestryConstants;
import forestry.core.data.models.ForestryBlockStateProvider;

import static forestry.core.data.models.ForestryBlockStateProvider.file;
import static forestry.core.data.models.ForestryBlockStateProvider.mcBlock;
import static forestry.core.data.models.ForestryBlockStateProvider.modBlock;
import static forestry.core.data.models.ForestryBlockStateProvider.path;

/**
 * Generates the blockstates, block models and item models for the farms jar. A farm block is a pillar
 * of its material carrying a farm overlay, and a planter renders one hand-authored model per planter
 * type.
 */
public class AgricultureBlockStateProvider extends BlockStateProvider {
	public AgricultureBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
		super(output, ForestryConstants.MOD_ID, existingFileHelper);
	}

	@Override
	protected void registerStatesAndModels() {
		for (FarmBlock block : FarmingBlocks.FARM.getBlocks()) {
			if (block.getType() == EnumFarmBlockType.PLAIN) {
				plainFarm(block);
			} else {
				singleFarm(block);
			}

			ForestryBlockStateProvider.generic3d(this, block);
		}

		for (BlockTypePlanter planter : BlockTypePlanter.values()) {
			ModelFile model = models().getExistingFile(modBlock(this, planter.getSerializedName()));
			Block managed = CultivationBlocks.MANAGED_PLANTER.get(planter).block();
			Block manual = CultivationBlocks.MANUAL_PLANTER.get(planter).block();

			ForestryBlockStateProvider.horizontalForestryBlock(this, managed, model);
			ForestryBlockStateProvider.horizontalForestryBlock(this, manual, model);

			planterItem(managed, planter);
			planterItem(manual, planter);
		}
	}

	private void singleFarm(FarmBlock block) {
		EnumFarmMaterial material = block.getFarmMaterial();
		Block base = material.getBase();
		ResourceLocation texture = modBlock(this, "farm/" + block.getType().getSerializedName());

		ForestryBlockStateProvider.singleModelBlock(this, block, farmPillar(path(block), base, texture, texture));
	}

	private void plainFarm(FarmBlock block) {
		EnumFarmMaterial material = block.getFarmMaterial();
		Block base = material.getBase();

		// todo need to use reverse texture
		getVariantBuilder(block)
			.partialState().with(FarmBlock.BAND, false)
			.modelForState().modelFile(farmPillar(path(block), base, modBlock(this, "farm/top"), modBlock(this, "farm/plain"))).addModel()
			.partialState().with(FarmBlock.BAND, true)
			.modelForState().modelFile(farmPillar(path(block) + "_band", base, modBlock(this, "farm/top"), modBlock(this, "farm/band"))).addModel();
	}

	private ModelFile farmPillar(String path, Block base, ResourceLocation top, ResourceLocation side) {
		ModelFile baseModel = file(blockTexture(base));

		return models().getBuilder(path).customLoader(CompositeModelBuilder::begin)
			.child("base", models().nested()
				.parent(baseModel)
				.renderType("solid"))
			.child("overlay", models().nested()
				.parent(file(mcBlock(this, "cube_column")))
				.texture("end", top)
				.texture("side", side)
				// should we use cutout_mipped?
				.renderType("cutout"))
			.itemRenderOrder("base", "overlay")
			.end()
			// reuse the particle
			.parent(baseModel);
	}

	// Every planter type shares one hand-authored block model, and its item renders that model
	private void planterItem(Block block, BlockTypePlanter planter) {
		itemModels().withExistingParent(path(block), modBlock(this, planter.getSerializedName()));
	}
}
