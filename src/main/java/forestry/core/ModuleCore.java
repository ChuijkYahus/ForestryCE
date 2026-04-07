package forestry.core;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import forestry.api.ForestryConstants;
import forestry.api.ForestryCapabilities;
import forestry.api.IForestryApi;
import forestry.api.client.IClientModuleHandler;
import forestry.api.core.ISpectacleVision;
import forestry.api.modules.ForestryModule;
import forestry.api.modules.ForestryModuleIds;
import forestry.api.modules.IForestryModule;
import forestry.api.modules.IPacketRegistry;
import forestry.apiculture.features.ApicultureItems;
import forestry.apiculture.items.ItemPollenCluster;
import forestry.apiimpl.plugin.PluginManager;
import forestry.arboriculture.features.ArboricultureBlocks;
import forestry.arboriculture.features.ArboricultureItems;
import forestry.arboriculture.loot.GrafterLootModifier;
import forestry.core.blocks.TileStreamUpdateTracker;
import forestry.core.client.CoreClientHandler;
import forestry.core.climate.ForestryClimateManager;
import forestry.core.commands.DiagnosticsCommand;
import forestry.core.commands.DumpCommand;
import forestry.core.features.CoreItems;
import forestry.core.features.CoreTiles;
import forestry.core.items.ItemPipette;
import forestry.core.items.ItemSpectacles;
import forestry.core.items.definitions.EnumCraftingMaterial;
import forestry.core.loot.ConditionLootModifier;
import forestry.core.network.PacketIdClient;
import forestry.core.network.PacketIdServer;
import forestry.core.network.packets.*;
import forestry.core.owner.GameProfileDataSerializer;
import forestry.core.recipes.RecipeManagers;
import forestry.core.utils.NetworkUtil;
import forestry.lepidopterology.features.LepidopterologyItems;
import forestry.modules.BlankForestryModule;
import forestry.modules.ForestryModuleManager;
import forestry.modules.ModuleUtil;
import forestry.modules.features.FeatureItem;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.ComposterBlock;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.function.Consumer;

@ForestryModule
public class ModuleCore extends BlankForestryModule {
	@Override
	public ResourceLocation getId() {
		return ForestryModuleIds.CORE;
	}

	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(ModuleCore::onCommonSetup);
		modBus.addListener(ModuleCore::registerCapabilities);
		modBus.addListener(ModuleCore::registerGlobalLootModifiers);

		ModuleUtil.loadFeatureProviders();
		NeoForge.EVENT_BUS.addListener(ModuleCore::onItemPickup);
		NeoForge.EVENT_BUS.addListener(ModuleCore::onLevelTick);
		NeoForge.EVENT_BUS.addListener(ModuleCore::onTagsUpdated);
		NeoForge.EVENT_BUS.addListener(ModuleCore::registerReloadListeners);
		NeoForge.EVENT_BUS.addListener(ModuleCore::registerCommands);

		PluginManager.registerAsyncException(modBus);
	}

	private static void onCommonSetup(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			((ForestryModuleManager) IForestryApi.INSTANCE.getModuleManager()).setupApi();
			PluginManager.registerCircuits();
			postItemRegistry();
			EntityDataSerializers.registerSerializer(GameProfileDataSerializer.INSTANCE);
			registerComposts();
		});
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerItem(ForestryCapabilities.SPECTACLE_VISION, (stack, context) -> ItemSpectacles.SPECTACLE_VISION, CoreItems.SPECTACLES.item());
		event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> ((ItemPipette) stack.getItem()).createFluidHandler(stack), CoreItems.PIPETTE.item());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CoreTiles.ANALYZER.tileType(), (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CoreTiles.ESCRITOIRE.tileType(), (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CoreTiles.APIARIST_CHEST.tileType(), (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CoreTiles.ARBORIST_CHEST.tileType(), (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CoreTiles.LEPIDOPTERIST_CHEST.tileType(), (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CoreTiles.ANALYZER.tileType(), (tile, side) -> tile.getEnergyHandler(side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CoreTiles.ANALYZER.tileType(), (tile, side) -> tile.getTankManager());
	}

	private static void registerComposts() {
		// cast avoids stupid typos (IItemLike can be different than Item, then composter will not work)
		@SuppressWarnings({"unchecked", "rawtypes"})
		Object2FloatMap<Item> composts = ((Object2FloatMap) ComposterBlock.COMPOSTABLES);

		for (FeatureItem<?> fruit : CoreItems.FRUITS.getFeatures()) {
			composts.put(fruit.item(), 0.65f);
		}
		composts.put(CoreItems.MOULDY_WHEAT.item(), 0.65f);
		composts.put(CoreItems.DECAYING_WHEAT.item(), 0.65f);
		composts.put(CoreItems.MULCH.item(), 0.65f);
		composts.put(CoreItems.ASH.item(), 0.65f);
		composts.put(CoreItems.CRAFTING_MATERIALS.item(EnumCraftingMaterial.WOOD_PULP), 0.65f);
		composts.put(CoreItems.PEAT.item(), 0.75f);
		composts.put(CoreItems.COMPOST.item(), 1f);
		for (ItemPollenCluster pollen : ApicultureItems.POLLEN_CLUSTER.getItems()) {
			composts.put(pollen, 0.3f);
		}
		composts.put(ArboricultureItems.SAPLING.item(), 0.3f);
		composts.put(ArboricultureItems.POLLEN_FERTILE.item(), 0.3f);
		for (BlockItem leaves : ArboricultureBlocks.LEAVES_DECORATIVE.getItems()) {
			composts.put(leaves, 0.3f);
		}
		composts.put(LepidopterologyItems.COCOON_GE.item(), 0.3f);
	}

	private static void registerGlobalLootModifiers(RegisterEvent event) {
		event.register(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, helper -> {
			helper.register(ForestryConstants.forestry("condition_modifier"), ConditionLootModifier.CODEC);
			helper.register(ForestryConstants.forestry("grafter_modifier"), GrafterLootModifier.CODEC);
		});
	}

	private static void postItemRegistry() {
		PluginManager.registerGenetics();
		PluginManager.registerFarming();
		PluginManager.registerPollen();
	}

	private static void onItemPickup(ItemEntityPickupEvent event) {
		PickupHandlerCore.onItemPickup(event.getPlayer(), event.getItemEntity());
	}

	private static void onLevelTick(LevelTickEvent.Post event) {
		TileStreamUpdateTracker.syncVisualUpdates();
	}

	private static void onTagsUpdated(TagsUpdatedEvent event) {
		if (event.shouldUpdateStaticData()) {
			event.getRegistryAccess().registry(Registries.BIOME).ifPresent(registry -> ((ForestryClimateManager) IForestryApi.INSTANCE.getClimateManager()).onBiomesReloaded(registry));
		}
	}

	private static void registerReloadListeners(AddReloadListenerEvent event) {
		event.addListener((prepBarrier, resourceManager, prepProfiler, reloadProfiler, backgroundExecutor, gameExecutor) -> {
			return prepBarrier.wait(Unit.INSTANCE).thenRunAsync(() -> {
				RecipeManagers.invalidateCaches();
				NetworkUtil.sendToAllPlayers(new RecipeCachePacket());
			});
		});
	}

	private static void registerCommands(RegisterCommandsEvent event) {
		LiteralArgumentBuilder<CommandSourceStack> forestryCommand = LiteralArgumentBuilder.literal("forestry");

		forestryCommand.then(DiagnosticsCommand.register());
		forestryCommand.then(DumpCommand.register());

		for (IForestryModule module : IForestryApi.INSTANCE.getModuleManager().getModulesForMod(ForestryConstants.MOD_ID)) {
			if (module instanceof BlankForestryModule forestryModule) {
				forestryModule.addToRootCommand(forestryCommand);
			}
		}

		event.getDispatcher().register(forestryCommand);
	}

	@Override
	public boolean isCore() {
		return true;
	}

	@Override
	public List<ResourceLocation> getModuleDependencies() {
		return List.of();
	}

	@Override
	public void registerPackets(IPacketRegistry registry) {
		registry.serverbound(PacketIdServer.GUI_SELECTION_REQUEST, PacketGuiSelectRequest::encode, PacketGuiSelectRequest::decode, PacketGuiSelectRequest::handle);
		registry.serverbound(PacketIdServer.PIPETTE_CLICK, PacketPipetteClick::encode, PacketPipetteClick::decode, PacketPipetteClick::handle);
		registry.serverbound(PacketIdServer.CHIPSET_CLICK, PacketChipsetClick::encode, PacketChipsetClick::decode, PacketChipsetClick::handle);
		registry.serverbound(PacketIdServer.SOLDERING_IRON_CLICK, PacketSolderingIronClick::encode, PacketSolderingIronClick::decode, PacketSolderingIronClick::handle);

		registry.clientbound(PacketIdClient.ERROR_UPDATE, PacketErrorUpdate::encode, PacketErrorUpdate::decode, PacketErrorUpdate::handle);
		registry.clientbound(PacketIdClient.GUI_UPDATE, PacketGuiStream::encode, PacketGuiStream::decode, PacketGuiStream::handle);
		registry.clientbound(PacketIdClient.GUI_LAYOUT_SELECT, PacketGuiLayoutSelect::encode, PacketGuiLayoutSelect::decode, PacketGuiLayoutSelect::handle);
		registry.clientbound(PacketIdClient.GUI_ENERGY, PacketGuiEnergy::encode, PacketGuiEnergy::decode, PacketGuiEnergy::handle);
		registry.clientbound(PacketIdClient.SOCKET_UPDATE, PacketSocketUpdate::encode, PacketSocketUpdate::decode, PacketSocketUpdate::handle);
		registry.clientbound(PacketIdClient.TILE_FORESTRY_UPDATE, PacketTileStream::encode, PacketTileStream::decode, PacketTileStream::handle);
		registry.clientbound(PacketIdClient.TILE_FORESTRY_ACTIVE, PacketActiveUpdate::encode, PacketActiveUpdate::decode, PacketActiveUpdate::handle);
		registry.clientbound(PacketIdClient.ITEMSTACK_DISPLAY, PacketItemStackDisplay::encode, PacketItemStackDisplay::decode, PacketItemStackDisplay::handle);
		registry.clientbound(PacketIdClient.GENOME_TRACKER_UPDATE, PacketGenomeTrackerSync::encode, PacketGenomeTrackerSync::decode, PacketGenomeTrackerSync::handle);
		registry.clientbound(PacketIdClient.TANK_LEVEL_UPDATE, PacketTankLevelUpdate::encode, PacketTankLevelUpdate::decode, PacketTankLevelUpdate::handle);
		registry.clientbound(PacketIdClient.RECIPE_CACHE, RecipeCachePacket::encode, RecipeCachePacket::decode, RecipeCachePacket::handle);
		registry.clientbound(PacketIdClient.REFRACTORY_WAX_ON, PacketRefractoryWax::encode, PacketRefractoryWax::decode, PacketRefractoryWax::handle);
	}

	@Override
	public void registerClientHandler(Consumer<IClientModuleHandler> registrar) {
		registrar.accept(new CoreClientHandler());
	}
}
