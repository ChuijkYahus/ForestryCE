package forestry.core.data.models;

import forestry.api.ForestryConstants;
import forestry.api.client.IForestryClientApi;
import forestry.apiculture.blocks.BlockBeeHive;
import forestry.apiculture.blocks.BlockHiveType;
import forestry.apiculture.features.ApicultureBlocks;
import forestry.arboriculture.blocks.ForestryLeafType;
import forestry.arboriculture.features.ArboricultureBlocks;
import forestry.core.blocks.*;
import forestry.core.features.CoreBlocks;
import forestry.core.features.CoreItems;
import forestry.core.fluids.ForestryFluids;
import forestry.core.utils.ModUtil;
import forestry.cultivation.blocks.BlockTypePlanter;
import forestry.cultivation.features.CultivationBlocks;
import forestry.factory.blocks.BlockFactoryPlain;
import forestry.factory.blocks.BlockTypeFactoryPlain;
import forestry.factory.features.FactoryBlocks;
import forestry.farming.blocks.EnumFarmBlockType;
import forestry.farming.blocks.EnumFarmMaterial;
import forestry.farming.blocks.FarmBlock;
import forestry.farming.features.FarmingBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.*;
import net.minecraftforge.client.model.generators.*;
import net.minecraftforge.client.model.generators.loaders.CompositeModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;

public class ForestryBlockStateProvider extends BlockStateProvider {
	public ForestryBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
		super(output, ForestryConstants.MOD_ID, exFileHelper);
	}

	public enum TankLayout {
		NONE,
		RESOURCE,
		PRODUCT,
		BOTH
	}

	@Override
	protected void registerStatesAndModels() {
		// Farm blocks
		for (FarmBlock block : FarmingBlocks.FARM.getBlocks()) {
			if (block.getType() == EnumFarmBlockType.PLAIN) {
				plainFarm(block);
			} else {
				singleFarm(block);
			}

			generic3d(block);
		}

		for (BlockTypePlanter farmType : BlockTypePlanter.values()) {
			ModelFile file = models().getExistingFile(modBlock(farmType.getSerializedName()));
			horizontalBlock(CultivationBlocks.MANAGED_PLANTER.get(farmType).block(), file);
			horizontalBlock(CultivationBlocks.MANUAL_PLANTER.get(farmType).block(), file);
		}

		// Resources
		//simpleBlock(CoreBlocks.BOG_EARTH.block());
		//simpleBlock(CoreBlocks.HUMUS.block());

		simpleBlock(CoreBlocks.APATITE_ORE.block());
		simpleBlock(CoreBlocks.DEEPSLATE_APATITE_ORE.block());
		simpleBlock(CoreBlocks.TIN_ORE.block());
		simpleBlock(CoreBlocks.DEEPSLATE_TIN_ORE.block());
		simpleBlock(CoreBlocks.RAW_TIN_BLOCK.block());
		generic3d(CoreBlocks.APATITE_ORE.block());
		generic3d(CoreBlocks.DEEPSLATE_APATITE_ORE.block());
		generic3d(CoreBlocks.TIN_ORE.block());
		generic3d(CoreBlocks.DEEPSLATE_TIN_ORE.block());
		generic3d(CoreBlocks.RAW_TIN_BLOCK.block());



		for(BlockTypeMetalPlating type: BlockTypeMetalPlating.values()){
			BlockMetalPlating block = CoreBlocks.METAL_PLATING.get(type).block();
			simpleBlock(block);
			generic3d(block);
		}

		generic2d(CoreItems.RAW_TIN);
		generic2d(CoreItems.INGOT_TIN);
		generic2d(CoreItems.SILICON);
		generic2d(CoreItems.GEAR_TIN);
		generic2d(CoreItems.INGOT_BRONZE);
		generic2d(CoreItems.GEAR_BRONZE);
		generic2d(CoreItems.GEAR_COPPER);
		generic2d(CoreItems.GEAR_IRON);

		generic2d(CoreItems.NUGGET_TIN);
		generic2d(CoreItems.SOLAR_CELL);

		machineBlock(BlockTypeFactoryPlain.BOTTLER, TankLayout.RESOURCE);
		machineBlock(BlockTypeFactoryPlain.CARPENTER, TankLayout.RESOURCE);
		machineBlock(BlockTypeFactoryPlain.CENTRIFUGE, TankLayout.NONE);
		machineBlock(BlockTypeFactoryPlain.FERMENTER, TankLayout.BOTH);
		machineBlock(BlockTypeFactoryPlain.MOISTENER, TankLayout.RESOURCE);
		machineBlock(BlockTypeFactoryPlain.SMELTER, TankLayout.NONE);
		machineBlock(BlockTypeFactoryPlain.SQUEEZER, TankLayout.PRODUCT);
		machineBlock(BlockTypeFactoryPlain.STILL, TankLayout.BOTH);

		simpleBlock(CoreBlocks.CORK.block());
		generic3d(CoreBlocks.CORK.block());

		jumboCandles();
		bigCandles();
		vanillaCandle(CoreBlocks.REFRACTORY_CANDLE.block());
		vanillaCandle(CoreBlocks.RAINBOW_CANDLE.block());

		// Fluids (doesn't actually show in game, but silences the warning spam from Minecraft)
		for (ForestryFluids fluid : ForestryFluids.values()) {
			Block block = fluid.getFeature().fluidBlock().block();
			ModelFile blockModel = particleOnly(models(), path(block), fluid.getFeature().properties().resources[0]);
			singleModelBlock(this, block, blockModel);
		}

		// Leaves (same as with fluids)
		for (ForestryLeafType treeType : ForestryLeafType.values()) {
			Block defaultBlock = ArboricultureBlocks.LEAVES_DEFAULT.get(treeType).block();
			Block defaultFruitBlock = ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(treeType).block();
			Block decorativeBlock = ArboricultureBlocks.LEAVES_DECORATIVE.get(treeType).block();
			ResourceLocation particle = IForestryClientApi.INSTANCE.getTreeManager().getLeafSprite(treeType.getIndividual().getSpecies()).get(false, true);
			ModelFile file = models().getBuilder(path(defaultBlock)).texture("particle", particle);

			singleModelBlock(this, defaultBlock, file);
			singleModelBlock(this, defaultFruitBlock, file);
			singleModelBlock(this, decorativeBlock, file);

			generic3d(defaultBlock);
			generic3d(defaultFruitBlock, defaultBlock);
			generic3d(decorativeBlock, defaultBlock);
		}
		singleModelBlock(this, ArboricultureBlocks.LEAVES.block(), particleOnly(models(), ArboricultureBlocks.LEAVES.getName(), blockTexture(Blocks.OAK_LEAVES)));

		for (BlockHiveType type : BlockHiveType.values()) {
			BlockBeeHive feature = ApicultureBlocks.BEEHIVE.get(type).block();
			String path = path(feature);

			ResourceLocation side = modBlock("beehives/" + type.getSerializedName() + ".side");
			ResourceLocation top = modBlock("beehives/" + type.getSerializedName() + ".top");
			ResourceLocation bottom = modBlock("beehives/" + type.getSerializedName() + ".bottom");

			singleModelBlock(this, feature, models().cubeBottomTop(path, side, bottom, top));
			generic3d(feature);
		}

		simpleBlock(CoreBlocks.ASHEN_WAX_BLOCK.block());
		generic3d(CoreBlocks.ASHEN_WAX_BLOCK.block());
		simpleBlock(CoreBlocks.CRISPY_HONEY_BLOCK.block());
		generic3d(CoreBlocks.CRISPY_HONEY_BLOCK.block());

		stoneBlockSet(CoreBlocks.ASH_BRICKS.block(),
			CoreBlocks.ASH_BRICK_STAIRS.block(),
			CoreBlocks.ASH_BRICK_SLAB.block(),
			CoreBlocks.ASH_BRICK_WALL.block(),
			CoreBlocks.ASH_BRICKS_CHISELED.block(),
			modLoc("block/ash_bricks"));

		stoneBlockSet(CoreBlocks.WAX_BRICKS.block(),
			CoreBlocks.WAX_BRICK_STAIRS.block(),
			CoreBlocks.WAX_BRICK_SLAB.block(),
			CoreBlocks.WAX_BRICK_WALL.block(),
			CoreBlocks.WAX_BRICKS_CHISELED.block(),
			modLoc("block/wax_bricks"));

		stoneBlockSet(CoreBlocks.REFRACTORY_WAX_BRICKS.block(),
			CoreBlocks.REFRACTORY_WAX_BRICK_STAIRS.block(),
			CoreBlocks.REFRACTORY_WAX_BRICK_SLAB.block(),
			CoreBlocks.REFRACTORY_WAX_BRICK_WALL.block(),
			CoreBlocks.REFRACTORY_WAX_BRICKS_CHISELED.block(),
			modLoc("block/refractory_wax_bricks"));

		stoneBlockSet(CoreBlocks.WAXSTONE.block(),
			CoreBlocks.WAXSTONE_STAIRS.block(),
			CoreBlocks.WAXSTONE_SLAB.block(),
			CoreBlocks.WAXSTONE_WALL.block(),
			null, //Chiseled Waxstone has a special model
			modLoc("block/waxstone"));

		stoneBlockSet(CoreBlocks.COBBLED_WAXSTONE.block(),
			CoreBlocks.COBBLED_WAXSTONE_STAIRS.block(),
			CoreBlocks.COBBLED_WAXSTONE_SLAB.block(),
			CoreBlocks.COBBLED_WAXSTONE_WALL.block(),
			null,
			modLoc("block/cobbled_waxstone"));

		stoneBlockSet(CoreBlocks.POLISHED_WAXSTONE.block(),
			CoreBlocks.POLISHED_WAXSTONE_STAIRS.block(),
			CoreBlocks.POLISHED_WAXSTONE_SLAB.block(),
			CoreBlocks.POLISHED_WAXSTONE_WALL.block(),
			null,
			modLoc("block/polished_waxstone"));

		stoneBlockSet(CoreBlocks.WAXSTONE_BRICKS.block(),
			CoreBlocks.WAXSTONE_BRICK_STAIRS.block(),
			CoreBlocks.WAXSTONE_BRICK_SLAB.block(),
			CoreBlocks.WAXSTONE_BRICK_WALL.block(),
			null,
			modLoc("block/waxstone_bricks"));

		stoneBlockSet(CoreBlocks.REFRACTORY_WAXSTONE.block(),
			CoreBlocks.REFRACTORY_WAXSTONE_STAIRS.block(),
			CoreBlocks.REFRACTORY_WAXSTONE_SLAB.block(),
			CoreBlocks.REFRACTORY_WAXSTONE_WALL.block(),
			null, //Chiseled Refractory Waxstone has a special model
			modLoc("block/refractory_waxstone"));

		stoneBlockSet(CoreBlocks.COBBLED_REFRACTORY_WAXSTONE.block(),
			CoreBlocks.COBBLED_REFRACTORY_WAXSTONE_STAIRS.block(),
			CoreBlocks.COBBLED_REFRACTORY_WAXSTONE_SLAB.block(),
			CoreBlocks.COBBLED_REFRACTORY_WAXSTONE_WALL.block(),
			null,
			modLoc("block/cobbled_refractory_waxstone"));

		stoneBlockSet(CoreBlocks.POLISHED_REFRACTORY_WAXSTONE.block(),
			CoreBlocks.POLISHED_REFRACTORY_WAXSTONE_STAIRS.block(),
			CoreBlocks.POLISHED_REFRACTORY_WAXSTONE_SLAB.block(),
			CoreBlocks.POLISHED_REFRACTORY_WAXSTONE_WALL.block(),
			null,
			modLoc("block/polished_refractory_waxstone"));

		stoneBlockSet(CoreBlocks.REFRACTORY_WAXSTONE_BRICKS.block(),
			CoreBlocks.REFRACTORY_WAXSTONE_BRICK_STAIRS.block(),
			CoreBlocks.REFRACTORY_WAXSTONE_BRICK_SLAB.block(),
			CoreBlocks.REFRACTORY_WAXSTONE_BRICK_WALL.block(),
			null,
			modLoc("block/refractory_waxstone_bricks"));

		stoneBlockSet(CoreBlocks.HONEYSTONE.block(),
			CoreBlocks.HONEYSTONE_STAIRS.block(),
			CoreBlocks.HONEYSTONE_SLAB.block(),
			CoreBlocks.HONEYSTONE_WALL.block(),
			null, //Chiseled Honeystone has a special model
			modLoc("block/honeystone"));

		stoneBlockSet(CoreBlocks.COBBLED_HONEYSTONE.block(),
			CoreBlocks.COBBLED_HONEYSTONE_STAIRS.block(),
			CoreBlocks.COBBLED_HONEYSTONE_SLAB.block(),
			CoreBlocks.COBBLED_HONEYSTONE_WALL.block(),
			null,
			modLoc("block/cobbled_honeystone"));

		stoneBlockSet(CoreBlocks.POLISHED_HONEYSTONE.block(),
			CoreBlocks.POLISHED_HONEYSTONE_STAIRS.block(),
			CoreBlocks.POLISHED_HONEYSTONE_SLAB.block(),
			CoreBlocks.POLISHED_HONEYSTONE_WALL.block(),
			null,
			modLoc("block/polished_honeystone"));

		stoneBlockSet(CoreBlocks.HONEYSTONE_BRICKS.block(),
			CoreBlocks.HONEYSTONE_BRICK_STAIRS.block(),
			CoreBlocks.HONEYSTONE_BRICK_SLAB.block(),
			CoreBlocks.HONEYSTONE_BRICK_WALL.block(),
			null,
			modLoc("block/honeystone_bricks"));
	}

	public void stoneBlockSet(Block base, StairBlock stairs, SlabBlock slab, WallBlock wall, Block chiseled, ResourceLocation modelLocation){
		simpleBlock(base);
		generic3d(base);

		stairsBlock(stairs, modelLocation);
		generic3d(stairs);

		slabBlock(slab, modelLocation, modelLocation);
		generic3d(slab);

		wallBlock(wall, modelLocation);
		wallBlockItem(wall, modelLocation);

		if (chiseled != null){
			simpleBlock(chiseled);
			generic3d(chiseled);
		}
	}

	private void wallBlockItem(WallBlock block, ResourceLocation resourceLocation) {
		String name = path(block);
		models().wallInventory(name + "_inventory", resourceLocation);
		itemModels().withExistingParent(name, modLoc("block/" + name + "_inventory"));
	}

	public static void singleModelBlock(ForestryBlockStateProvider states, Block defaultBlock, ModelFile file) {
		states.getVariantBuilder(defaultBlock).partialState().modelForState().modelFile(file).addModel();
	}

	public static ModelFile particleOnly(BlockModelProvider models, String path, ResourceLocation particleTexture) {
		return models.getBuilder(path).texture("particle", particleTexture);
	}

	private void singleFarm(FarmBlock block) {
		EnumFarmMaterial material = block.getFarmMaterial();
		Block base = material.getBase();
		ResourceLocation texture = modLoc("block/farm/" + block.getType().getSerializedName());

		singleModelBlock(this, block, farmPillar(path(block), base, texture, texture));
	}

	private void plainFarm(FarmBlock block) {
		EnumFarmMaterial material = block.getFarmMaterial();
		Block base = material.getBase();

		// todo need to use reverse texture
		getVariantBuilder(block)
			.partialState().with(FarmBlock.BAND, false)
			.modelForState().modelFile(farmPillar(path(block), base, modLoc("block/farm/top"), modLoc("block/farm/plain"))).addModel()
			.partialState().with(FarmBlock.BAND, true)
			.modelForState().modelFile(farmPillar(path(block) + "_band", base, modLoc("block/farm/top"), modLoc("block/farm/band"))).addModel();
	}

	private ModelFile farmPillar(String path, Block base, ResourceLocation top, ResourceLocation side) {
		ModelFile baseModel = file(blockTexture(base));

		return models().getBuilder(path).customLoader(CompositeModelBuilder::begin)
			.child("base", models().nested()
				.parent(baseModel)
				.renderType("solid"))
			.child("overlay", models().nested()
				.parent(mcFile("cube_column"))
				.texture("end", top)
				.texture("side", side)
				// should we use cutout_mipped?
				.renderType("cutout"))
			.itemRenderOrder("base", "overlay")
			.end()
			// reuse the particle
			.parent(baseModel);
	}

	public void machineBlock (
		BlockTypeFactoryPlain block,
		TankLayout layout
	) {

		BlockFactoryPlain machine = FactoryBlocks.PLAIN.get(block).block();
		String name = block.name().toLowerCase();

		String baseTexture = "block/machines/" + name + "/base";
		String particleTexture = "block/machines/" + name + "/particles";

		switch (layout) {

			// ----------------------------------
			// 0 TANKS
			// ----------------------------------
			case NONE -> {

				models().withExistingParent(name,
						modLoc("block/machines/base_machine"))
					.renderType("cutout")
					.texture("base", modLoc(baseTexture))
					.texture("particle", modLoc(particleTexture));

				getVariantBuilder(machine)
					.forAllStates(state -> ConfiguredModel.builder()
						.modelFile(models().getExistingFile(
							modLoc("block/" + name)))
						.rotationY(rotationFromFacing(state.getValue(BlockBase.FACING)))
						.build());
			}

			// ----------------------------------
			// 1 TANK (RESOURCE)
			// ----------------------------------
			case RESOURCE -> {

				for (int level = 0; level <= 4; level++) {

					String modelName = name + "_res_" + level;

					models().withExistingParent(modelName,
							modLoc("block/machines/base_machine"))
						.renderType("cutout")
						.texture("base", modLoc(baseTexture))
						.texture("particle", modLoc(particleTexture))
						.texture("resource_tank", modLoc(
							"block/machines/" + name + "/tank_res_" + level));
				}

				getVariantBuilder(machine)
					.forAllStates(state -> {

						int level = state.getValue(BlockFactoryPlain.TANK_RESOURCE_LEVEL);

						return ConfiguredModel.builder()
							.modelFile(models().getExistingFile(
								modLoc("block/" + name + "_res_" + level)))
							.rotationY(rotationFromFacing(state.getValue(BlockBase.FACING)))
							.build();
					});
			}

			// ----------------------------------
			// 1 TANK (PRODUCT)
			// ----------------------------------
			case PRODUCT -> {

				for (int level = 0; level <= 4; level++) {

					String modelName = name + "_prod_" + level;

					models().withExistingParent(modelName,
							modLoc("block/machines/base_machine"))
						.renderType("cutout")
						.texture("base", modLoc(baseTexture))
						.texture("particle", modLoc(particleTexture))
						.texture("product_tank", modLoc(
							"block/machines/" + name + "/tank_prod_" + level));
				}

				getVariantBuilder(machine)
					.forAllStates(state -> {

						int level = state.getValue(BlockFactoryPlain.TANK_PRODUCT_LEVEL);

						return ConfiguredModel.builder()
							.modelFile(models().getExistingFile(
								modLoc("block/" + name + "_prod_" + level)))
							.rotationY(rotationFromFacing(state.getValue(BlockBase.FACING)))
							.build();
					});
			}

			// ----------------------------------
			// 2 TANKS
			// ----------------------------------
			case BOTH -> {

				for (int left = 0; left <= 4; left++) {
					for (int right = 0; right <= 4; right++) {

						String modelName = name + "_res_" + left + "_prod_" + right;

						models().withExistingParent(modelName,
								modLoc("block/machines/base_machine"))
							.renderType("cutout")
							.texture("base", modLoc(baseTexture))
							.texture("particle", modLoc(particleTexture))
							.texture("resource_tank", modLoc(
								"block/machines/" + name + "/tank_res_" + left))
							.texture("product_tank", modLoc(
								"block/machines/" + name + "/tank_prod_" + right));
					}
				}

				getVariantBuilder(machine)
					.forAllStates(state -> {

						int left = state.getValue(BlockFactoryPlain.TANK_RESOURCE_LEVEL);
						int right = state.getValue(BlockFactoryPlain.TANK_PRODUCT_LEVEL);

						String modelName = name + "_res_" + left + "_prod_" + right;

						return ConfiguredModel.builder()
							.modelFile(models().getExistingFile(
								modLoc("block/" + modelName)))
							.rotationY(rotationFromFacing(state.getValue(BlockBase.FACING)))
							.build();
					});
			}
		}
	}

	private int rotationFromFacing(Direction facing) {
		return switch (facing) {
            case SOUTH -> 180;
			case WEST  -> 270;
			case EAST  -> 90;
			default -> 0;
		};
	}

	private void jumboCandles(){

		for(BlockTypeJumboCandle type: BlockTypeJumboCandle.values()){

			Block candle = CoreBlocks.JUMBO_CANDLES.get(type).block();
			String modelName;
			String bottom;
			String side;
			String top;

			//Single
			modelName = "block/candles/" + type.getSerializedName() + "_jumbo_single";

			bottom = "block/candles/" + type.getSerializedName() + "_bottom";
			side = "block/candles/" + type.getSerializedName() + "_side";
			top = "block/candles/" + type.getSerializedName() + "_top";

			models().withExistingParent(modelName,
					modLoc("block/candles/jumbo_single"))
				.texture("bottom", modLoc(bottom))
				.texture("side", modLoc(side))
				.texture("top", modLoc(top))
				.texture("particle", modLoc(side));

			//Top
			modelName = "block/candles/" + type.getSerializedName() + "_jumbo_top";

			bottom = "block/candles/" + type.getSerializedName() + "_bottom_top";
			side = "block/candles/" + type.getSerializedName() + "_side_top";
			top = "block/candles/" + type.getSerializedName() + "_top";

			models().withExistingParent(modelName,
					modLoc("block/candles/jumbo_top"))
				.texture("bottom", modLoc(bottom))
				.texture("side", modLoc(side))
				.texture("top", modLoc(top))
				.texture("particle", modLoc(side));

			//Middle
			modelName = "block/candles/" + type.getSerializedName() + "_jumbo_middle";

			bottom = "block/candles/" + type.getSerializedName() + "_bottom_top";
			side = "block/candles/" + type.getSerializedName() + "_side_middle";
			top = "block/candles/" + type.getSerializedName() + "_middle_top";

			models().withExistingParent(modelName,
					modLoc("block/candles/jumbo_middle"))
				.texture("bottom", modLoc(bottom))
				.texture("side", modLoc(side))
				.texture("top", modLoc(top))
				.texture("particle", modLoc(side));

			//Bottom
			//It is only about here that I really found the phrase "Jumbo Bottom" incredibly funny.
			modelName = "block/candles/" + type.getSerializedName() + "_jumbo_bottom";

			bottom = "block/candles/" + type.getSerializedName() + "_bottom";
			side = "block/candles/" + type.getSerializedName() + "_side_bottom";
			top = "block/candles/" + type.getSerializedName() + "_bottom_top";

			models().withExistingParent(modelName,
					modLoc("block/candles/jumbo_bottom"))
				.texture("bottom", modLoc(bottom))
				.texture("side", modLoc(side))
				.texture("top", modLoc(top))
				.texture("particle", modLoc(side));


			getVariantBuilder(candle).forAllStates(state -> {

				BlockJumboCandle.CandleShape shape = state.getValue(BlockJumboCandle.SHAPE);

				String curModel = "block/candles/" + type.getSerializedName() + "_jumbo_" + shape.getSerializedName();

				return ConfiguredModel.builder()
					.modelFile(models().getExistingFile(modLoc(curModel)))
					.build();
			});

			//simpleBlockItem(candle, models().getExistingFile(ResourceLocation.parse("block/candles/" + type.getSerializedName() + "_jumbo_single")));
		}

	}

	private void bigCandles(){

		for(BlockTypeBigCandle type: BlockTypeBigCandle.values()){

			Block candle = CoreBlocks.BIG_CANDLES.get(type).block();
			String modelName;

			modelName = "block/candles/" + type.getSerializedName() + "_big";
			models().withExistingParent(modelName,
					modLoc("block/candles/big"))
				.texture("0", modLoc(modelName))
				.texture("particle", modLoc(modelName));

			getVariantBuilder(candle).forAllStates(state -> {

				String curModel = "block/candles/" + type.getSerializedName() + "_big";

				return ConfiguredModel.builder()
					.modelFile(models().getExistingFile(modLoc(curModel)))
					.build();
			});

			//simpleBlockItem(candle, models().getExistingFile(ResourceLocation.parse("block/candles/" + type.getSerializedName() + "_jumbo_single")));
		}

	}

	//This code was very graciously written by Artificial Intelligence.
	//Datagen is cool and all but it's even cooler when I don't have to do any of it because I hate writing it ever so slightly more than I hate AI.
	//I wrote some tho so that makes it okay!!!!
	private void vanillaCandle(Block block) {
		String name = ForgeRegistries.BLOCKS.getKey(block).getPath();

		ModelFile one = models()
			.withExistingParent("block/candles/" + name + "_one", mcLoc("block/template_candle"))
			.texture("all", modLoc("block/candles/" + name))
			.texture("particle", modLoc("block/candles/" + name));

		ModelFile oneLit = models()
			.withExistingParent("block/candles/" + name + "_one_lit", mcLoc("block/template_candle"))
			.texture("all", modLoc("block/candles/" + name + "_lit"))
			.texture("particle", modLoc("block/candles/" + name + "_lit"));;

		ModelFile two = models()
			.withExistingParent("block/candles/" + name + "_two", mcLoc("block/template_two_candles"))
			.texture("all", modLoc("block/candles/" + name))
			.texture("particle", modLoc("block/candles/" + name));


		ModelFile twoLit = models()
			.withExistingParent("block/candles/" + name + "_two_lit", mcLoc("block/template_two_candles"))
			.texture("all", modLoc("block/candles/" + name + "_lit"))
			.texture("particle", modLoc("block/candles/" + name + "_lit"));

		ModelFile three = models()
			.withExistingParent("block/candles/" + name + "_three", mcLoc("block/template_three_candles"))
			.texture("all", modLoc("block/candles/" + name))
			.texture("particle", modLoc("block/candles/" + name));


		ModelFile threeLit = models()
			.withExistingParent("block/candles/" + name + "_three_lit", mcLoc("block/template_three_candles"))
			.texture("all", modLoc("block/candles/" + name + "_lit"))
			.texture("particle", modLoc("block/candles/" + name + "_lit"));

		ModelFile four = models()
			.withExistingParent("block/candles/" + name + "_four", mcLoc("block/template_four_candles"))
			.texture("all", modLoc("block/candles/" + name))
			.texture("particle", modLoc("block/candles/" + name));

		ModelFile fourLit = models()
			.withExistingParent("block/candles/" + name + "_four_lit", mcLoc("block/template_four_candles"))
			.texture("all", modLoc("block/candles/" + name + "_lit"))
			.texture("particle", modLoc("block/candles/" + name + "_lit"));

		getVariantBuilder(block)
			.forAllStates(state -> {
				int count = state.getValue(CandleBlock.CANDLES);
				boolean lit = state.getValue(CandleBlock.LIT);

				ModelFile model = switch (count) {
					case 2 -> lit ? twoLit : two;
					case 3 -> lit ? threeLit : three;
					case 4 -> lit ? fourLit : four;
					default -> lit ? oneLit : one;
				};

				return ConfiguredModel.builder()
					.modelFile(model)
					.build();
			});
	}


	protected static ResourceLocation withSuffix(ResourceLocation loc, String suffix) {
		return loc.withSuffix(suffix);
	}

	protected static ResourceLocation withPrefix(String prefix, ResourceLocation loc) {
		String oldPath = loc.getPath();
		int slash = oldPath.lastIndexOf('/') + 1;

		if (slash != 0) {
			return loc.withPath(oldPath.substring(0, slash) + prefix + oldPath.substring(slash));
		}
		return loc.withPrefix(prefix);
	}

	public void generic3d(Block block, Block otherParent) {
		itemModels().withExistingParent(path(block), modLoc("block/" + path(otherParent)));
	}

	public void generic3d(Block block, ResourceLocation otherParentId) {
		itemModels().withExistingParent(path(block), new ResourceLocation(otherParentId.getNamespace(), "block/" + otherParentId.getPath()));
	}

	protected ModelFile existingMcBlock(String path) {
		return models().getExistingFile(mcBlock(path));
	}

	// Everything below this line is boilerplate code adapted from https://github.com/thedarkcolour/ModKit
	// Makes a 3d cube of a block for item model
	public void generic3d(Block block) {
		String path = path(block);
		itemModels().withExistingParent(path, modLoc("block/" + path));
	}

	public static String path(Block block) {
		return ModUtil.getRegistryName(block).getPath();
	}

	public static ModelFile.UncheckedModelFile file(ResourceLocation resourceLoc) {
		return new ModelFile.UncheckedModelFile(resourceLoc);
	}

	public ModelFile.UncheckedModelFile modFile(String path) {
		return file(this.modBlock(path));
	}

	public ModelFile.UncheckedModelFile mcFile(String path) {
		return file(this.mcBlock(path));
	}

	public ResourceLocation modBlock(String name) {
		return this.modLoc("block/" + name);
	}

	public ResourceLocation mcBlock(String name) {
		return this.mcLoc("block/" + name);
	}

	public void generic2d(ItemLike item) {
		generic2d(ModUtil.getRegistryName(item.asItem()));
	}

	/**
	 * Makes a 2d single layer item like hopper, gold ingot, or redstone dust item models
	 */
	public void generic2d(ResourceLocation itemId) {
		layer0(itemId, "item/generated");
	}

	public void layer0(ResourceLocation itemId, String parentName) {
		String path = itemId.getPath();

		itemModels().getBuilder(path)
			.parent(new ModelFile.UncheckedModelFile(parentName))
			.texture("layer0", new ResourceLocation(itemId.getNamespace(), "item/" + path));
	}
}
