package forestry.core.data.models;

import forestry.arboriculture.wood.ForestryWoodType;
import forestry.arboriculture.wood.VanillaWoodType;
import forestry.arboriculture.wood.ForestryButtonBlock;
import forestry.arboriculture.wood.ForestryDoorBlock;
import forestry.arboriculture.wood.ForestryFenceBlock;
import forestry.arboriculture.wood.ForestryFenceGateBlock;
import forestry.arboriculture.wood.ForestryHangingSignBlock;
import forestry.arboriculture.wood.ForestryLogBlock;
import forestry.arboriculture.wood.ForestryPressurePlateBlock;
import forestry.arboriculture.wood.ForestrySlabBlock;
import forestry.arboriculture.wood.ForestryStairsBlock;
import forestry.arboriculture.wood.ForestryStandingSignBlock;
import forestry.arboriculture.wood.ForestryTrapdoorBlock;
import forestry.arboriculture.wood.ForestryWallHangingSignBlock;
import forestry.arboriculture.wood.ForestryWallSignBlock;
import forestry.arboriculture.features.ArboricultureBlocks;
import forestry.core.platform.registration.FeatureBlockGroup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

public class ForestryWoodModelProvider extends ForestryBlockStateProvider {
	public ForestryWoodModelProvider(PackOutput output, ExistingFileHelper exFileHelper) {
		super(output, exFileHelper);
	}

	@Override
	protected void registerStatesAndModels() {
		// Vanilla fireproof wood types
		for (VanillaWoodType woodType : VanillaWoodType.VALUES) {
			// planks
			Block planks = ArboricultureBlocks.PLANKS_VANILLA_FIREPROOF.get(woodType).block();
			String woodTypeName = woodType.getSerializedName();
			String planksName = woodTypeName + "_planks";
			ModelFile planksModel = existingMcBlock(planksName);
			simpleBlock(planks, planksModel);
			generic3d(planks, mcLoc(planksName));
			// log
			ForestryLogBlock log = ArboricultureBlocks.LOGS_VANILLA_FIREPROOF.get(woodType).block();
			String logName = woodTypeName + "_log";
			ModelFile logModel = existingMcBlock(logName);
			axisBlock(log, logModel, logModel);
			generic3d(log, mcLoc(logName));
			// wood
			ForestryLogBlock wood = ArboricultureBlocks.WOOD_VANILLA_FIREPROOF.get(woodType).block();
			String woodName = woodTypeName + "_wood";
			ModelFile woodModel = existingMcBlock(woodName);
			axisBlock(wood, woodModel, woodModel);
			generic3d(wood, mcLoc(woodName));
			// stripped log
			ForestryLogBlock strippedLog = ArboricultureBlocks.STRIPPED_LOGS_VANILLA_FIREPROOF.get(woodType).block();
			String strippedLogName = "stripped_" + woodTypeName + "_log";
			ModelFile strippedLogModel = existingMcBlock(strippedLogName);
			axisBlock(strippedLog, strippedLogModel, strippedLogModel);
			generic3d(strippedLog, mcLoc(strippedLogName));
			// stripped wood
			ForestryLogBlock strippedWood = ArboricultureBlocks.STRIPPED_WOOD_VANILLA_FIREPROOF.get(woodType).block();
			String strippedWoodName = "stripped_" + woodTypeName + "_wood";
			ModelFile strippedWoodModel = existingMcBlock(strippedWoodName);
			axisBlock(strippedWood, strippedWoodModel, strippedWoodModel);
			generic3d(strippedWood, mcLoc(strippedWoodName));
			// slab
			SlabBlock slab = ArboricultureBlocks.SLABS_VANILLA_FIREPROOF.get(woodType).block();
			String slabName = woodTypeName + "_slab";
			ModelFile bottomSlabModel = existingMcBlock(slabName);
			ModelFile topSlabModel = existingMcBlock(slabName + "_top");
			slabBlock(slab, bottomSlabModel, topSlabModel, planksModel);
			generic3d(slab, mcLoc(slabName));
			// stairs
			StairBlock stairs = ArboricultureBlocks.STAIRS_VANILLA_FIREPROOF.get(woodType).block();
			String stairsName = woodTypeName + "_stairs";
			ModelFile stairsModel = existingMcBlock(stairsName);
			ModelFile innerStairsModel = existingMcBlock(stairsName + "_inner");
			ModelFile outerStairsModel = existingMcBlock(stairsName + "_outer");
			stairsBlock(stairs, stairsModel, innerStairsModel, outerStairsModel);
			generic3d(stairs, mcLoc(stairsName));
			// fence
			ForestryFenceBlock fence = ArboricultureBlocks.FENCES_VANILLA_FIREPROOF.get(woodType).block();
			String fenceName = woodTypeName + "_fence";
			ModelFile fencePostModel = existingMcBlock(fenceName + "_post");
			ModelFile fenceSideModel = existingMcBlock(fenceName + "_side");
			ModelFile fenceInventoryModel = existingMcBlock(fenceName + "_inventory");
			fourWayBlock(fence, fencePostModel, fenceSideModel);
			itemModels().withExistingParent(path(fence), fenceInventoryModel.getLocation());
			// fence gate
			ForestryFenceGateBlock fenceGate = ArboricultureBlocks.FENCE_GATES_VANILLA_FIREPROOF.get(woodType).block();
			String fenceGateName = woodTypeName + "_fence_gate";
			ModelFile gateModel = existingMcBlock(fenceGateName);
			ModelFile gateOpenModel = existingMcBlock(fenceGateName + "_open");
			ModelFile gateWallModel = existingMcBlock(fenceGateName + "_wall");
			ModelFile gateWallOpenModel = existingMcBlock(fenceGateName + "_wall_open");
			fenceGateBlock(fenceGate, gateModel, gateOpenModel, gateWallModel, gateWallOpenModel);
			generic3d(fenceGate, mcLoc(fenceGateName));
		}

		// Forestry wood types
		for (ForestryWoodType woodType : ForestryWoodType.VALUES) {
			// Planks
			Block planks = ArboricultureBlocks.PLANKS.get(woodType).block();
			Block fireproofPlanks = ArboricultureBlocks.PLANKS_FIREPROOF.get(woodType).block();
			ModelFile planksModel = cubeAll(planks);

			simpleBlock(planks);
			simpleBlock(fireproofPlanks, planksModel);
			generic3d(planks);
			generic3d(fireproofPlanks, planks);

			// Logs, Wood, Stripped Logs, Stripped Wood
			ForestryLogBlock log = ArboricultureBlocks.LOGS.get(woodType).block();
			ResourceLocation logTexture = blockTexture(log);
			ResourceLocation strippedLogTexture = withPrefix("stripped_", logTexture);

			logLike(woodType, ArboricultureBlocks.LOGS, ArboricultureBlocks.LOGS_FIREPROOF, logTexture, withSuffix(logTexture, "_top"));
			logLike(woodType, ArboricultureBlocks.STRIPPED_LOGS, ArboricultureBlocks.STRIPPED_LOGS_FIREPROOF, strippedLogTexture, withPrefix("stripped_", withSuffix(logTexture, "_top")));
			logLike(woodType, ArboricultureBlocks.STRIPPED_WOOD, ArboricultureBlocks.STRIPPED_WOOD_FIREPROOF, strippedLogTexture, strippedLogTexture);
			logLike(woodType, ArboricultureBlocks.WOOD, ArboricultureBlocks.WOOD_FIREPROOF, logTexture, logTexture);

			// Slab
			ForestrySlabBlock slab = ArboricultureBlocks.SLABS.get(woodType).block();
			ForestrySlabBlock fireproofSlab = ArboricultureBlocks.SLABS_FIREPROOF.get(woodType).block();
			ResourceLocation planksLoc = blockTexture(planks);
			ModelFile bottomSlabModel = models().slab(path(slab), planksLoc, planksLoc, planksLoc);
			ModelFile topSlabModel = models().slabTop(path(slab) + "_top", planksLoc, planksLoc, planksLoc);
			slabBlock(slab, bottomSlabModel, topSlabModel, planksModel);
			slabBlock(fireproofSlab, bottomSlabModel, topSlabModel, planksModel);
			generic3d(slab);
			generic3d(fireproofSlab, slab);

			// Stairs
			ForestryStairsBlock stairs = ArboricultureBlocks.STAIRS.get(woodType).block();
			ForestryStairsBlock fireproofStairs = ArboricultureBlocks.STAIRS_FIREPROOF.get(woodType).block();
			ModelFile stairsModel = models().stairs(path(stairs), planksLoc, planksLoc, planksLoc);
			ModelFile innerStairsModel = models().stairsInner(path(stairs) + "_inner", planksLoc, planksLoc, planksLoc);
			ModelFile outerStairsModel = models().stairsOuter(path(stairs) + "_outer", planksLoc, planksLoc, planksLoc);
			stairsBlock(stairs, stairsModel, innerStairsModel, outerStairsModel);
			stairsBlock(fireproofStairs, stairsModel, innerStairsModel, outerStairsModel);
			generic3d(stairs);
			generic3d(fireproofStairs, stairs);

			// Fence
			ForestryFenceBlock fence = ArboricultureBlocks.FENCES.get(woodType).block();
			ForestryFenceBlock fireproofFence = ArboricultureBlocks.FENCES_FIREPROOF.get(woodType).block();
			ModelFile fencePostModel = models().fencePost(path(fence) + "_post", planksLoc);
			ModelFile fenceSideModel = models().fenceSide(path(fence) + "_side", planksLoc);
			ModelFile fenceInventoryModel = models().fenceInventory(path(fence) + "_inventory", planksLoc);
			fourWayBlock(fence, fencePostModel, fenceSideModel);
			fourWayBlock(fireproofFence, fencePostModel, fenceSideModel);
			itemModels().withExistingParent(path(fence), fenceInventoryModel.getLocation());
			itemModels().withExistingParent(path(fireproofFence), fenceInventoryModel.getLocation());

			// Fence Gate
			ForestryFenceGateBlock fenceGate = ArboricultureBlocks.FENCE_GATES.get(woodType).block();
			ForestryFenceGateBlock fireproofFenceGate = ArboricultureBlocks.FENCE_GATES_FIREPROOF.get(woodType).block();
			ModelFile gateModel = models().fenceGate(path(fenceGate), planksLoc);
			ModelFile gateOpenModel = models().fenceGateOpen(path(fenceGate) + "_open", planksLoc);
			ModelFile gateWallModel = models().fenceGateWall(path(fenceGate) + "_wall", planksLoc);
			ModelFile gateWallOpenModel = models().fenceGateWallOpen(path(fenceGate) + "_wall_open", planksLoc);
			fenceGateBlock(fenceGate, gateModel, gateOpenModel, gateWallModel, gateWallOpenModel);
			fenceGateBlock(fireproofFenceGate, gateModel, gateOpenModel, gateWallModel, gateWallOpenModel);
			generic3d(fenceGate);
			generic3d(fireproofFenceGate, fenceGate);

			// Door
			ForestryDoorBlock door = ArboricultureBlocks.DOORS.get(woodType).block();
			doorBlock(door, withSuffix(blockTexture(door), "_bottom"), withSuffix(blockTexture(door), "_top"));
			generic2d(door);

			// Trapdoor
			ForestryTrapdoorBlock trapdoor = ArboricultureBlocks.TRAPDOORS.get(woodType).block();
			trapdoorBlockWithRenderType(trapdoor, blockTexture(trapdoor), true, "cutout");
			itemModels().trapdoorBottom(path(trapdoor), blockTexture(trapdoor));

			// Sign
			ForestryStandingSignBlock sign = ArboricultureBlocks.SIGN.get(woodType).block();
			ForestryWallSignBlock wallSign = ArboricultureBlocks.WALL_SIGN.get(woodType).block();
			ModelFile signModel = particleOnly(this, path(sign), blockTexture(planks));
			singleModelBlock(this, sign, signModel);
			singleModelBlock(this, wallSign, signModel);
			generic2d(sign);

			// Hanging Sign
			ForestryHangingSignBlock hangingSign = ArboricultureBlocks.HANGING_SIGN.get(woodType).block();
			ForestryWallHangingSignBlock hangingWallSign = ArboricultureBlocks.WALL_HANGING_SIGN.get(woodType).block();
			ModelFile hangingSignModel = particleOnly(this, path(hangingSign), blockTexture(planks));
			singleModelBlock(this, hangingSign, hangingSignModel);
			singleModelBlock(this, hangingWallSign, hangingSignModel);
			generic2d(hangingSign);

			// Button
			ForestryButtonBlock button = ArboricultureBlocks.BUTTON.get(woodType).block();
			buttonBlock(button, planksLoc);
			ModelFile buttonInventoryModel = itemModels().buttonInventory(path(button) + "_inventory", planksLoc);
			itemModels().withExistingParent(path(button), buttonInventoryModel.getLocation());

			// Pressure plate
			ForestryPressurePlateBlock pressurePlate = ArboricultureBlocks.PRESSURE_PLATE.get(woodType).block();
			pressurePlateBlock(pressurePlate, planksLoc);
			generic3d(pressurePlate);
		}
	}

	private void logLike(ForestryWoodType woodType, FeatureBlockGroup<ForestryLogBlock, ForestryWoodType> logs, FeatureBlockGroup<ForestryLogBlock, ForestryWoodType> fireproofLogs, ResourceLocation sideTexture, ResourceLocation topTexture) {
		ForestryLogBlock wood = logs.get(woodType).block();
		ForestryLogBlock fireproofWood = fireproofLogs.get(woodType).block();
		ModelFile woodModel = models().cubeColumn(path(wood), sideTexture, topTexture);
		axisBlock(wood, woodModel, woodModel);
		axisBlock(fireproofWood, woodModel, woodModel);
		generic3d(wood);
		generic3d(fireproofWood, wood);
	}

	@Override
	public @NotNull String getName() {
		return "Wood Block States: Forestry";
	}
}
