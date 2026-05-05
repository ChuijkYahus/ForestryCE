package forestry.arboriculture;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import forestry.api.arboriculture.TreeManager;
import forestry.api.arboriculture.genetics.ITree;
import forestry.api.arboriculture.genetics.ITreeSpeciesType;
import forestry.api.arboriculture.genetics.TreeLifeStage;
import forestry.api.ForestryCapabilities;
import forestry.api.client.IClientModuleHandler;
import forestry.api.modules.ForestryModule;
import forestry.api.modules.ForestryModuleIds;
import forestry.api.modules.IPacketRegistry;
import forestry.arboriculture.client.ArboricultureClientHandler;
import forestry.arboriculture.commands.CommandTree;
import forestry.arboriculture.features.ArboricultureEntities;
import forestry.arboriculture.features.ArboricultureItems;
import forestry.arboriculture.items.ForestryBoatDispenserBehavior;
import forestry.core.genetics.ItemGE;
import forestry.arboriculture.network.PacketRipeningUpdate;
import forestry.arboriculture.villagers.ArboricultureVillagers;
import forestry.core.genetics.capability.IndividualHandlerItem;
import forestry.core.network.PacketIdClient;
import forestry.core.utils.SpeciesUtil;
import forestry.modules.BlankForestryModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@ForestryModule
public class ModuleArboriculture extends BlankForestryModule {
	@Override
	public ResourceLocation getId() {
		return ForestryModuleIds.ARBORICULTURE;
	}

	@Override
	public void registerEvents(IEventBus modBus) {
		NeoForge.EVENT_BUS.addListener(ArboricultureVillagers::villagerTrades);

		modBus.addListener(ModuleArboriculture::registerCapabilities);
		modBus.addListener(ModuleArboriculture::commonSetup);
		NeoForge.EVENT_BUS.addListener(ModuleArboriculture::modifySnifferLoot);
	}

	private static void modifySnifferLoot(LootTableLoadEvent event) {
		if (event.getName().equals(BuiltInLootTables.SNIFFER_DIGGING)) {
			LootPool main = event.getTable().getPool("main");

			if (main != null) {
				List<LootPoolEntryContainer> entries = new ArrayList<>(main.entries);
				entries.add(LootItem.lootTableItem(ArboricultureItems.AMBER_SAPLING).build());
				main.entries = entries;
			}
		}
	}

	@Override
	public void setupApi() {
		TreeManager.woodAccess = WoodAccess.INSTANCE;
	}

	@Override
	public void addToRootCommand(LiteralArgumentBuilder<CommandSourceStack> command) {
		command.then(CommandTree.register());
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerItem(ForestryCapabilities.INDIVIDUAL_HANDLER_ITEM, (stack, context) -> {
			ITree individual = SpeciesUtil.TREE_TYPE.get().getVanillaIndividual(stack.getItem());
			return individual != null ? new IndividualHandlerItem(SpeciesUtil.TREE_TYPE.get(), stack, individual, TreeLifeStage.SAPLING) : null;
		},
			BuiltInRegistries.ITEM.stream().filter(item -> SpeciesUtil.TREE_TYPE.get().getVanillaIndividual(item) != null).toArray(net.minecraft.world.item.Item[]::new));
		event.registerItem(ForestryCapabilities.INDIVIDUAL_HANDLER_ITEM, (stack, context) -> ((ItemGE) stack.getItem()).createIndividualHandler(stack),
			ArboricultureItems.SAPLING.item(),
			ArboricultureItems.POLLEN_FERTILE.item());
		event.registerEntity(Capabilities.ItemHandler.ENTITY, ArboricultureEntities.CHEST_BOAT.entityType(), (boat, context) -> boat.isAlive() ? boat.getItemHandler() : null);
	}

	private static void commonSetup(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			for (ForestryWoodType type : ForestryWoodType.VALUES) {
				DispenserBlock.registerBehavior(ArboricultureItems.BOAT.item(type), new ForestryBoatDispenserBehavior(type, false));
				DispenserBlock.registerBehavior(ArboricultureItems.CHEST_BOAT.item(type), new ForestryBoatDispenserBehavior(type, true));
				WoodType.register(type.getWoodType());
			}
		});
	}

	@Override
	public void registerPackets(IPacketRegistry registry) {
		registry.clientbound(PacketIdClient.RIPENING_UPDATE, PacketRipeningUpdate::encode, PacketRipeningUpdate::decode, PacketRipeningUpdate::handle);
	}

	@Override
	public void registerClientHandler(Consumer<IClientModuleHandler> registrar) {
		registrar.accept(new ArboricultureClientHandler());
	}
}
