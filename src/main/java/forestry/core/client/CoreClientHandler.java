package forestry.core.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Pair;
import forestry.api.ForestryConstants;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.apiculture.genetics.IBeeSpecies;
import forestry.api.client.IClientModuleHandler;
import forestry.api.client.IForestryClientApi;
import forestry.api.client.apiculture.IBeeClientManager;
import forestry.api.client.arboriculture.ITreeClientManager;
import forestry.api.core.ISpectacleBlock;
import forestry.apiculture.features.ApicultureBlocks;
import forestry.apiculture.features.ApicultureItems;
import forestry.apiimpl.client.ForestryClientApiImpl;
import forestry.apiimpl.plugin.PluginManager;
import forestry.arboriculture.features.ArboricultureBlocks;
import forestry.arboriculture.features.ArboricultureItems;
import forestry.core.blocks.BlockBurnBarrel;
import forestry.core.circuits.GuiSolderingIron;
import forestry.core.config.Constants;
import forestry.core.features.*;
import forestry.core.fluids.ForestryFluids;
import forestry.core.gui.*;
import forestry.core.models.ClientManager;
import forestry.core.models.FluidContainerModel;
import forestry.core.models.ModelBlockCached;
import forestry.core.particles.CoreParticles;
import forestry.core.render.*;
import forestry.core.utils.GeneticsUtil;
import forestry.core.utils.RenderUtil;
import forestry.core.utils.SpeciesUtil;
import forestry.energy.features.EnergyBlocks;
import forestry.energy.features.EnergyTiles;
import forestry.factory.features.FactoryBlocks;
import forestry.factory.features.FactoryTiles;
import forestry.lepidopterology.features.LepidopterologyItems;
import forestry.mail.features.MailItems;
import forestry.modules.ModuleUtil;
import forestry.storage.features.BackpackItems;
import forestry.storage.features.CrateItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.awt.*;
import java.util.Map;
import java.util.OptionalDouble;

public class CoreClientHandler implements IClientModuleHandler {
	public static BlockEntityWithoutLevelRenderer bewlr;
	// Copied from RenderStateShard.java (just LINES but with NO_DEPTH_TEST)
	public static final RenderType RENDER_TYPE_LINES_XRAY = RenderType.create("lines_xray", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 256, false, false, RenderType.CompositeState.builder()
		.setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
		.setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
		.setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
		.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
		.setOutputState(RenderStateShard.OUTLINE_TARGET)
		.setWriteMaskState(RenderStateShard.COLOR_WRITE)
		.setCullState(RenderStateShard.NO_CULL)
		.setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
		.createCompositeState(false));

	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(CoreClientHandler::onClientSetup);
		modBus.addListener(CoreClientHandler::registerModelLoaders);
		modBus.addListener(CoreClientHandler::additionalBakedModels);
		modBus.addListener(CoreClientHandler::bakeModels);
		modBus.addListener(CoreClientHandler::setupLayers);
		modBus.addListener(CoreClientHandler::clientSetupRenderers);
		modBus.addListener(CoreClientHandler::registerReloadListeners);
		modBus.addListener(CoreClientHandler::registerBlockColors);
		modBus.addListener(CoreClientHandler::registerItemColors);
		modBus.addListener(CoreClientHandler::registerParticleFactory);
		MinecraftForge.EVENT_BUS.addListener(CoreClientHandler::onClientTick);

		ModuleUtil.getModBus(ForestryConstants.MOD_ID).addListener(EventPriority.HIGHEST, ((ForestryClientApiImpl) IForestryClientApi.INSTANCE)::initializeTextureManager);
	}

	private static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			CoreBlocks.BASE.getBlocks().forEach((block) -> ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutoutMipped()));
			CoreBlocks.JUMBO_CANDLES.getBlocks().forEach(block -> ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutout()));
			CoreBlocks.BIG_CANDLES.getBlocks().forEach(block -> ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutout()));
			ItemBlockRenderTypes.setRenderLayer(CoreBlocks.BURN_BARREL.block(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(CoreBlocks.PLYWOOD_SHEET.block(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(EnergyBlocks.SOLAR_PANEL.block(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(CoreBlocks.PHOSPHOR_WALL_TORCH.block(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(CoreBlocks.PHOSPHOR_TORCH.block(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(CoreBlocks.TIN_CHAIN.block(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(CoreBlocks.PHOSPHOR_LANTERN.block(), RenderType.cutout());

			for (ForestryFluids fluid : ForestryFluids.values()) {
				ItemBlockRenderTypes.setRenderLayer(fluid.getFluid(), RenderType.translucent());
				ItemBlockRenderTypes.setRenderLayer(fluid.getFlowing(), RenderType.translucent());
			}

			MenuScreens.register(CoreMenuTypes.ALYZER.menuType(), PortableAnalyzerScreen::new);
			MenuScreens.register(CoreMenuTypes.ANALYZER.menuType(), GuiAnalyzer::new);
			MenuScreens.register(CoreMenuTypes.NATURALIST_INVENTORY.menuType(), GuiNaturalistInventory<ContainerNaturalistInventory>::new);
			MenuScreens.register(CoreMenuTypes.ESCRITOIRE.menuType(), GuiEscritoire::new);
			MenuScreens.register(CoreMenuTypes.SOLDERING_IRON.menuType(), GuiSolderingIron::new);
			MenuScreens.register(CoreMenuTypes.BURN_BARREL.menuType(), GuiBurnBarrel::new);
		});

		bewlr = new ForestryBewlr(Minecraft.getInstance().getBlockEntityRenderDispatcher());
	}

	private static void registerModelLoaders(ModelEvent.RegisterGeometryLoaders event) {
		event.register("fluid_container", FluidContainerModel.Loader.INSTANCE);

		PluginManager.registerClient();
	}

	private static void additionalBakedModels(ModelEvent.RegisterAdditional event) {
		IBeeClientManager beeManager = IForestryClientApi.INSTANCE.getBeeManager();

		for (BeeLifeStage stage : BeeLifeStage.values()) {
			Map<IBeeSpecies, ResourceLocation> models = beeManager.getBeeModels(stage);

			for (IBeeSpecies species : SpeciesUtil.getAllBeeSpecies()) {
				event.register(models.get(species));
			}
		}

		ITreeClientManager treeManager = IForestryClientApi.INSTANCE.getTreeManager();

		for (Pair<ResourceLocation, ResourceLocation> pair : treeManager.getAllSaplingModels()) {
			event.register(pair.getFirst());
			event.register(pair.getSecond());
		}
	}

	private static void bakeModels(ModelEvent.ModifyBakingResult event) {
		ClientManager.INSTANCE.onBakeModels(event);
	}

	private static void setupLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(ForestryModelLayers.NATURALIST_CHEST_LAYER, RenderNaturalistChest::createBodyLayer);
		event.registerLayerDefinition(ForestryModelLayers.MILL_LAYER, RenderMill::createBodyLayer);
		event.registerLayerDefinition(ForestryModelLayers.ENGINE_LAYER, RenderEngine::createBodyLayer);
	}

	private static void clientSetupRenderers(EntityRenderersEvent.RegisterRenderers event) {
		// Core
		event.registerBlockEntityRenderer(CoreTiles.ANALYZER.tileType(), RenderAnalyzer::new);
		event.registerBlockEntityRenderer(CoreTiles.ESCRITOIRE.tileType(), RenderEscritoire::new);
		// Apiculture/Arboriculture/Lepidopterology
		event.registerBlockEntityRenderer(CoreTiles.APIARIST_CHEST.tileType(), ctx -> new RenderNaturalistChest(ctx, "apiaristchest"));
		event.registerBlockEntityRenderer(CoreTiles.ARBORIST_CHEST.tileType(), ctx -> new RenderNaturalistChest(ctx, "arbchest"));
		event.registerBlockEntityRenderer(CoreTiles.LEPIDOPTERIST_CHEST.tileType(), ctx -> new RenderNaturalistChest(ctx, "lepichest"));
		// Engine
		event.registerBlockEntityRenderer(EnergyTiles.CLOCKWORK_ENGINE.tileType(), ctx -> new RenderEngine(ctx, Constants.TEXTURE_PATH_BLOCK + "/engine_clock_"));
		event.registerBlockEntityRenderer(EnergyTiles.BIOGAS_ENGINE.tileType(), ctx -> new RenderEngine(ctx, Constants.TEXTURE_PATH_BLOCK + "/engine_bronze_"));
		event.registerBlockEntityRenderer(EnergyTiles.PEAT_ENGINE.tileType(), ctx -> new RenderEngine(ctx, Constants.TEXTURE_PATH_BLOCK + "/engine_copper_"));
		event.registerBlockEntityRenderer(EnergyTiles.COMBUSTION_ENGINE.tileType(), ctx -> new RenderEngine(ctx, Constants.TEXTURE_PATH_BLOCK + "/engine_iron_"));
		event.registerBlockEntityRenderer(EnergyTiles.SOLAR_ENGINE.tileType(), ctx -> new RenderEngine(ctx, Constants.TEXTURE_PATH_BLOCK + "/engine_tin_"));

		//Rainmaker out here getting it's own render code (it's so cool and special <3)
		event.registerBlockEntityRenderer(FactoryTiles.RAINMAKER.tileType(), ctx -> new RenderMill(ctx, Constants.TEXTURE_PATH_BLOCK + "/rainmaker_"));
	}

	private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
		event.registerReloadListener(((ForestryTextureManager) IForestryClientApi.INSTANCE.getTextureManager()).getSpriteUploader());
		event.registerReloadListener(ColourProperties.INSTANCE);

		event.registerReloadListener((prepBarrier, resourceManager, prepProfiler, reloadProfiler, backgroundExecutor, gameExecutor) -> {
			return prepBarrier.wait(Unit.INSTANCE).thenRunAsync(ModelBlockCached::clear, gameExecutor);
		});
	}

	private static void registerParticleFactory(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(CoreParticles.REFRACTORY_WAX.get(), RefractoryWaxParticle::new);
	}

	private static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
		// Apiculture
		event.register(ClientManager.FORESTRY_BLOCK_COLOR, ApicultureBlocks.BEE_COMB.blockArray());
		// Arboriculture
		event.register(ClientManager.FORESTRY_BLOCK_COLOR, ArboricultureBlocks.LEAVES.block());
		event.register(ClientManager.FORESTRY_BLOCK_COLOR, ArboricultureBlocks.LEAVES_DEFAULT.blockArray());
		event.register(ClientManager.FORESTRY_BLOCK_COLOR, ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.blockArray());
		event.register(ClientManager.FORESTRY_BLOCK_COLOR, ArboricultureBlocks.LEAVES_DECORATIVE.blockArray());
		// Factory
		event.register(ClientManager.FORESTRY_BLOCK_COLOR, FactoryBlocks.PLAIN.blockArray());

		//Core
		event.register(ClientManager.FORESTRY_BLOCK_COLOR, CoreBlocks.TURF.block());
		event.register(ClientManager.FORESTRY_BLOCK_COLOR, CoreBlocks.TURF_BLOCK.block());
	}

	private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		// Core
		event.register(ClientManager.FORESTRY_ITEM_COLOR, CoreItems.ELECTRON_TUBES.itemArray());
		event.register(ClientManager.FORESTRY_ITEM_COLOR, CoreItems.CIRCUITBOARDS.itemArray());
		event.register(ClientManager.FORESTRY_ITEM_COLOR, FluidsItems.CONTAINERS.itemArray());
		event.register(ClientManager.FORESTRY_ITEM_COLOR, CoreItems.PIPETTE.item());
		event.register(ClientManager.FORESTRY_ITEM_COLOR, CoreBlocks.TURF.item());
		event.register(ClientManager.FORESTRY_ITEM_COLOR, CoreBlocks.TURF_BLOCK.item());

		// Apiculture
		event.register(ClientManager.FORESTRY_ITEM_COLOR, ApicultureBlocks.BEE_COMB.blockArray());
		event.register(ClientManager.FORESTRY_ITEM_COLOR,
			ApicultureItems.BEE_QUEEN.item(),
			ApicultureItems.BEE_DRONE.item(),
			ApicultureItems.BEE_PRINCESS.item(),
			ApicultureItems.BEE_LARVAE.item()
		);
		event.register(ClientManager.FORESTRY_ITEM_COLOR, ApicultureItems.HONEY_DROP.item());
		event.register(ClientManager.FORESTRY_ITEM_COLOR, ApicultureItems.PROPOLIS.itemArray());
		event.register(ClientManager.FORESTRY_ITEM_COLOR, ApicultureItems.POLLEN_CLUSTER.itemArray());
		event.register(ClientManager.FORESTRY_ITEM_COLOR, ApicultureItems.BEE_COMBS.itemArray());

		// Arboriculture
		event.register(ClientManager.FORESTRY_ITEM_COLOR, ArboricultureBlocks.LEAVES.block());
		event.register(ClientManager.FORESTRY_ITEM_COLOR, ArboricultureBlocks.LEAVES_DEFAULT.blockArray());
		event.register(ClientManager.FORESTRY_ITEM_COLOR, ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.blockArray());
		event.register(ClientManager.FORESTRY_ITEM_COLOR, ArboricultureBlocks.LEAVES_DECORATIVE.blockArray());
		event.register(ClientManager.FORESTRY_ITEM_COLOR, ArboricultureItems.SAPLING.item());
		event.register(ClientManager.FORESTRY_ITEM_COLOR, ArboricultureItems.POLLEN_FERTILE.item());

		// Lepidopterology
		event.register(ClientManager.FORESTRY_ITEM_COLOR, LepidopterologyItems.CATERPILLAR_GE.item());
		event.register(ClientManager.FORESTRY_ITEM_COLOR, LepidopterologyItems.SERUM_GE.item());

		// Backpacks
		event.register(ClientManager.FORESTRY_ITEM_COLOR,
			BackpackItems.APIARIST_BACKPACK.item(),
			BackpackItems.ARBORIST_BACKPACK.item(),
			BackpackItems.LEPIDOPTERIST_BACKPACK.item(),
			BackpackItems.MINER_BACKPACK.item(),
			BackpackItems.MINER_BACKPACK_T_2.item(),
			BackpackItems.DIGGER_BACKPACK.item(),
			BackpackItems.DIGGER_BACKPACK_T_2.item(),
			BackpackItems.FORESTER_BACKPACK.item(),
			BackpackItems.FORESTER_BACKPACK_T_2.item(),
			BackpackItems.HUNTER_BACKPACK.item(),
			BackpackItems.HUNTER_BACKPACK_T_2.item(),
			BackpackItems.ADVENTURER_BACKPACK.item(),
			BackpackItems.ADVENTURER_BACKPACK_T_2.item(),
			BackpackItems.BUILDER_BACKPACK.item(),
			BackpackItems.BUILDER_BACKPACK_T_2.item(),
			BackpackItems.BREWER_BACKPACK.item(),
			BackpackItems.BREWER_BACKPACK_T_2.item()
		);

		// Crates
		event.register(ClientManager.FORESTRY_ITEM_COLOR, CrateItems.CRATED_BEE_COMBS.itemArray());
		event.register(ClientManager.FORESTRY_ITEM_COLOR,
			CrateItems.CRATED_GRASS_BLOCK.item(),
			CrateItems.CRATED_POLLEN_CLUSTER_NORMAL.item(),
			CrateItems.CRATED_POLLEN_CLUSTER_CRYSTALLINE.item(),
			CrateItems.CRATED_PROPOLIS.item());

		// Mail
		event.register(ClientManager.FORESTRY_ITEM_COLOR, MailItems.STAMPS.itemArray());
	}

	private static void onClientTick(RenderLevelStageEvent event) {
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
			Minecraft minecraft = Minecraft.getInstance();
			Player player = minecraft.player;

			if (player != null) {
				if (GeneticsUtil.hasNaturalistEye(player)) {
					// Draw lines around pollinated leaves and wild hives
					PoseStack stack = event.getPoseStack();

					Vec3 cameraPos = event.getCamera().getPosition();

					RENDER_TYPE_LINES_XRAY.setupRenderState();

					MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
					VertexConsumer lines = buffers.getBuffer(RENDER_TYPE_LINES_XRAY);

					int renderDistance = minecraft.options.renderDistance().get();
					BlockPos playerPos = minecraft.player.blockPosition();
					ChunkPos playerChunkPos = new ChunkPos(playerPos);

					Color color = RenderUtil.getRainbowColor(minecraft.level.getGameTime(), event.getPartialTick());

					float r = color.getRed() / 255f;
					float g = color.getGreen() / 255f;
					float b = color.getBlue() / 255f;

					// Iterate through all chunks in render distance
					for (int chunkX = playerChunkPos.x - renderDistance; chunkX <= playerChunkPos.x + renderDistance; chunkX++) {
						for (int chunkZ = playerChunkPos.z - renderDistance; chunkZ <= playerChunkPos.z + renderDistance; chunkZ++) {
							LevelChunk chunk = minecraft.level.getChunk(chunkX, chunkZ);

							// Get all block entities in the chunk
							for (BlockEntity be : chunk.getBlockEntities().values()) {
								if (be instanceof ISpectacleBlock naturalist && naturalist.isHighlighted(player)) {
									BlockPos pos = be.getBlockPos();

									stack.pushPose();
									// Translate the matrix stack to avoid floating point precision errors
									stack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);

									// render at origin (inflate slightly to avoid weirdness with selection box)
									LevelRenderer.renderLineBox(stack, lines, -0.001, -0.001, -0.001, 1.001, 1.001, 1.001, r, g, b, 1.0F);

									stack.popPose();
								}
							}
						}
					}

					buffers.endBatch();
					RENDER_TYPE_LINES_XRAY.clearRenderState();
				}
			}
		}
	}
}
