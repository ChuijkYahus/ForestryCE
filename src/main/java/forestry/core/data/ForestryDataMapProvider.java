package forestry.core.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.neoforged.neoforge.registries.datamaps.builtin.RaidHeroGift;

import forestry.api.ForestryConstants;
import forestry.apiculture.features.ApicultureItems;
import forestry.apiculture.bees.ItemPollenCluster;
import forestry.arboriculture.features.ArboricultureBlocks;
import forestry.arboriculture.features.ArboricultureItems;
import forestry.arboriculture.features.CharcoalBlocks;
import forestry.core.features.CoreBlocks;
import forestry.core.features.CoreItems;
import forestry.core.platform.item.ItemFruit;
import forestry.core.content.resources.EnumCraftingMaterial;

/**
 * Generates the NeoForge built-in data maps for Forestry items and villagers:
 * <ul>
 *     <li>{@code neoforge:compostables} - composter chances (replaces the old imperative
 *     {@code ComposterBlock.COMPOSTABLES} mutation)</li>
 *     <li>{@code neoforge:furnace_fuels} - burn times for peat, charcoal, the log piles, plywood and cork
 *     (replacing the old per-item {@code ItemProperties.burnTime}), so they work in vanilla furnaces, Create
 *     blaze burners, etc.</li>
 *     <li>{@code neoforge:raid_hero_gifts} - Hero of the Village gifts for Forestry professions.</li>
 * </ul>
 */
public class ForestryDataMapProvider extends DataMapProvider {
	public ForestryDataMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
		super(packOutput, lookupProvider);
	}

	@Override
	protected void gather(HolderLookup.Provider provider) {
		gatherCompostables();
		gatherFurnaceFuels();
		gatherRaidHeroGifts();
	}

	private void gatherCompostables() {
		Builder<Compostable, Item> composts = builder(NeoForgeDataMaps.COMPOSTABLES);

		for (ItemFruit fruit : CoreItems.FRUITS.getItems()) {
			compostable(composts, fruit, 0.65f);
		}
		compostable(composts, CoreItems.MOULDY_WHEAT.item(), 0.65f);
		compostable(composts, CoreItems.DECAYING_WHEAT.item(), 0.65f);
		compostable(composts, CoreItems.MULCH.item(), 0.65f);
		compostable(composts, CoreItems.ASH.item(), 0.65f);
		compostable(composts, CoreItems.CRAFTING_MATERIALS.item(EnumCraftingMaterial.WOOD_PULP), 0.65f);
		compostable(composts, CoreItems.PEAT.item(), 0.75f);
		compostable(composts, CoreItems.COMPOST.item(), 1f);
		for (ItemPollenCluster pollen : ApicultureItems.POLLEN_CLUSTER.getItems()) {
			compostable(composts, pollen, 0.3f);
		}
		compostable(composts, ArboricultureItems.TREE_SAPLING.item(), 0.3f);
		compostable(composts, ArboricultureItems.TREE_POLLEN.item(), 0.3f);
		for (BlockItem leaves : ArboricultureBlocks.LEAVES_DECORATIVE.getItems()) {
			compostable(composts, leaves, 0.3f);
		}
		// The cocoon entry is added by the butterflies jar, which merges into this data map from its own file
	}

	private void gatherRaidHeroGifts() {
		Builder<RaidHeroGift, VillagerProfession> gifts = builder(NeoForgeDataMaps.RAID_HERO_GIFTS);
		gifts.add(ForestryConstants.forestry("beekeeper"), new RaidHeroGift(ForestryGiftLootTables.BEEKEEPER_GIFT), false);
		gifts.add(ForestryConstants.forestry("arborist"), new RaidHeroGift(ForestryGiftLootTables.ARBORIST_GIFT), false);
	}

	private void gatherFurnaceFuels() {
		Builder<FurnaceFuel, Item> fuels = builder(NeoForgeDataMaps.FURNACE_FUELS);

		furnaceFuel(fuels, CharcoalBlocks.CHARCOAL.item(), 16000);
		furnaceFuel(fuels, CoreItems.BITUMINOUS_PEAT.item(), 4200);
		furnaceFuel(fuels, CoreItems.PEAT.item(), 2000);
		furnaceFuel(fuels, CharcoalBlocks.LOG_PILE.item(), 1200);
		furnaceFuel(fuels, CharcoalBlocks.DECORATIVE_LOG_PILE.item(), 1200);
		furnaceFuel(fuels, CoreBlocks.PLYWOOD_BLOCK.item(), 300);
		furnaceFuel(fuels, CoreBlocks.CORK.item(), 300);
		furnaceFuel(fuels, CoreBlocks.PLYWOOD_SHEET.item(), 50);
	}

	private static void compostable(Builder<Compostable, Item> builder, ItemLike item, float chance) {
		builder.add(BuiltInRegistries.ITEM.getKey(item.asItem()), new Compostable(chance), false);
	}

	private static void furnaceFuel(Builder<FurnaceFuel, Item> builder, ItemLike item, int burnTime) {
		builder.add(BuiltInRegistries.ITEM.getKey(item.asItem()), new FurnaceFuel(burnTime), false);
	}

	@Override
	public String getName() {
		return "Forestry Data Maps";
	}
}
