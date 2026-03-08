package forestry.core.data;

import forestry.api.ForestryConstants;
import forestry.api.apiculture.ForestryBeeSpecies;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.arboriculture.ForestryTreeSpecies;
import forestry.api.arboriculture.genetics.TreeLifeStage;
import forestry.apiculture.blocks.BlockAlvearyType;
import forestry.apiculture.blocks.BlockTypeApiculture;
import forestry.apiculture.features.ApicultureBlocks;
import forestry.apiculture.features.ApicultureItems;
import forestry.apiculture.items.EnumHoneyComb;
import forestry.arboriculture.features.ArboricultureItems;
import forestry.arboriculture.features.CharcoalBlocks;
import forestry.core.advancements.DiscoverSpeciesTrigger;
import forestry.core.blocks.BlockTypeCoreTesr;
import forestry.core.features.CoreBlocks;
import forestry.core.features.CoreItems;
import forestry.core.features.FluidsItems;
import forestry.core.items.definitions.EnumContainerType;
import forestry.core.utils.SpeciesUtil;
import forestry.cultivation.blocks.BlockTypePlanter;
import forestry.cultivation.features.CultivationBlocks;
import forestry.energy.blocks.EngineBlockType;
import forestry.energy.features.EnergyBlocks;
import forestry.factory.blocks.BlockTypeFactoryPlain;
import forestry.factory.blocks.BlockTypeFactoryTesr;
import forestry.factory.features.FactoryBlocks;
import forestry.farming.blocks.EnumFarmBlockType;
import forestry.farming.blocks.EnumFarmMaterial;
import forestry.farming.features.FarmingBlocks;
import forestry.worktable.features.WorktableBlocks;
import net.minecraft.advancements.*;
import net.minecraft.advancements.critereon.*;
import net.minecraft.commands.CommandFunction;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.ForgeAdvancementProvider;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static net.minecraft.advancements.Advancement.Builder.advancement;

public class ForestryAdvancementProvider extends ForgeAdvancementProvider {
	public ForestryAdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper existingFileHelper) {
		super(output, registries, existingFileHelper, List.of(new CoreAdvancements()));
	}


	private static class CoreAdvancements implements AdvancementGenerator {
		

		@Override
		public void generate(HolderLookup.Provider registries, Consumer<Advancement> writer, ExistingFileHelper existingFileHelper) {
			ItemStack icon = SpeciesUtil.BEE_TYPE.get().createStack(ForestryBeeSpecies.FOREST, BeeLifeStage.QUEEN);

			//Forestry
			Advancement root = advancement()
				.display(
					icon,
					Component.translatable("advancements.forestry.root.title"),
					Component.translatable("advancements.forestry.root.description"),
					ResourceLocation.tryParse("textures/block/honeycomb_block.png"),
					FrameType.TASK,
					false,
					false,
					false
				)
				.addCriterion("used_forestry", PlayerTrigger.TriggerInstance.located(LocationPredicate.ANY))
				.rewards(new AdvancementRewards(0, new ResourceLocation[]{ForestryConstants.forestry("grant_guide")}, new ResourceLocation[0], CommandFunction.CacheableFunction.NONE))
				.save(writer, ForestryConstants.MOD_ID + ":root");

			//Flame Grilled
			Advancement wood_pile = makeSimpleAdvancement(
				"get_ash",
				CharcoalBlocks.LOG_PILE.stack(),
				InventoryChangeTrigger.TriggerInstance.hasItems(CoreItems.ASH.get()),
				root,
				writer);

			//I Can, Can You?
			Advancement cans = makeSimpleAdvancement(
				"get_cans",
				FluidsItems.CONTAINERS.get(EnumContainerType.CAN).stack(),
				InventoryChangeTrigger.TriggerInstance.hasItems(FluidsItems.CONTAINERS.get(EnumContainerType.CAN)),
				root,
				writer);

			//Here's the scoop!
			//Like what newspaper people say
			Advancement scooped = makeSimpleAdvancement(
					"get_scoop",
					ApicultureItems.SCOOP.stack(),
					InventoryChangeTrigger.TriggerInstance.hasItems(ApicultureItems.SCOOP.get()),
					root,
					writer);

				//Ol' Reliable
				//A reference to Spongebob's special jellyfishing net
				Advancement get_proven_scoop = makeSimpleAdvancement(
					"get_proven_scoop",
					ApicultureItems.SCOOP_PROVEN.stack(),
					InventoryChangeTrigger.TriggerInstance.hasItems(ApicultureItems.SCOOP_PROVEN.get()),
					scooped,
					writer, FrameType.CHALLENGE, true, true, false);

				//Zonked Out
				//To be zonked is to be like, really tired, or really high. Which is kinda what the smoker does.
				Advancement smoked = makeSimpleAdvancement(
					"get_smoker",
					ApicultureItems.SMOKER.stack(),
					InventoryChangeTrigger.TriggerInstance.hasItems(ApicultureItems.SMOKER.get()),
					scooped,
					writer);


				//The Beekeeper
				//A reference to the Jason Statham movie of the same name
				Advancement apiarists_armor = advancement()
					.parent(smoked)
					.display(
						ApicultureItems.APIARIST_HELMET.stack(),
						Component.translatable("advancements.forestry.get_apiarists_armor.title"),
						Component.translatable("advancements.forestry.get_apiarists_armor.description"),
						null,
						FrameType.TASK,
						true,
						true,
						false
					)
					.addCriterion("has_apiarist_helmet", InventoryChangeTrigger.TriggerInstance.hasItems(ApicultureItems.APIARIST_HELMET.get()))
					.addCriterion("has_apiarist_chestplate", InventoryChangeTrigger.TriggerInstance.hasItems(ApicultureItems.APIARIST_CHEST.get()))
					.addCriterion("has_apiarist_leggings", InventoryChangeTrigger.TriggerInstance.hasItems(ApicultureItems.APIARIST_LEGS.get()))
					.addCriterion("has_apiarist_boots", InventoryChangeTrigger.TriggerInstance.hasItems(ApicultureItems.APIARIST_BOOTS.get()))
					.requirements(new String[][] {
						{ "has_apiarist_helmet" },
						{ "has_apiarist_chestplate" },
						{ "has_apiarist_leggings" },
						{ "has_apiarist_boots" }
					})
					.save(writer, ResourceLocation.fromNamespaceAndPath(ForestryConstants.MOD_ID, "get_apiarists_armor").toString());

					//Sting Operation
					//Cannot figure this one out yet.
					Advancement theBeekeeper = advancement()
						.parent(apiarists_armor)
						.display(
							ApicultureItems.APIARIST_CHEST.stack(),
							Component.translatable("advancements.forestry.sting_operation.title"),
							Component.translatable("advancements.forestry.sting_operation.description"),
							null,
							FrameType.CHALLENGE,
							true,
							true,
							false
				)
				.addCriterion(
					ResourceLocation.fromNamespaceAndPath(ForestryConstants.MOD_ID, "sting_operation_raid_complete").toString(), PlayerTrigger.TriggerInstance.raidWon())
				.save(writer, ResourceLocation.fromNamespaceAndPath(ForestryConstants.MOD_ID, "sting_operation").toString());
				//TODO: Make this trigger when the player has Apiarists Armor on


				//This Is Where The Series Ends
				//A reference to the Yogscast, and their infamous Site Bee series
				Advancement bee = makeSimpleAdvancement(
					"get_a_bee",
					SpeciesUtil.BEE_TYPE.get().createStack(ForestryBeeSpecies.MEADOWS, BeeLifeStage.DRONE),
					InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(
						ApicultureItems.BEE_DRONE.get(),
						ApicultureItems.BEE_PRINCESS.get(),
						ApicultureItems.BEE_QUEEN.get()
					).build()),
					scooped,
					writer);

					//iPad Kid
					//A reference to kids and they phones
					Advancement analyser = makeSimpleAdvancement(
						"get_analyser",
						CoreItems.PORTABLE_ALYZER.stack(),
						InventoryChangeTrigger.TriggerInstance.hasItems(CoreItems.PORTABLE_ALYZER.get()),
						bee,
						writer);

					/*
					 * Advancements for finding specific bee species begin here
					 */

					//I'm Different!
					//A reference to Portal 2
					Advancement valiant_bee = makeSimpleAdvancement(
						"get_valiant_drone",
						SpeciesUtil.BEE_TYPE.get().createStack(ForestryBeeSpecies.VALIANT, BeeLifeStage.DRONE),
						DiscoverSpeciesTrigger.TriggerInstance.checkDiscovered(ForestryBeeSpecies.VALIANT),
						bee,
						writer, FrameType.GOAL, true, true, false);

					//Dungeon Flyer
					//A pun on Dungeon Crawler
					Advancement steadfast_bee = makeSimpleAdvancement(
						"get_steadfast_drone",
						SpeciesUtil.BEE_TYPE.get().createStack(ForestryBeeSpecies.STEADFAST, BeeLifeStage.DRONE),
						DiscoverSpeciesTrigger.TriggerInstance.checkDiscovered(ForestryBeeSpecies.STEADFAST),
						bee,
						writer, FrameType.GOAL, true, true, false);

					//What a deal!
					Advancement monastic_bee = makeSimpleAdvancement(
						"get_monastic_drone",
						SpeciesUtil.BEE_TYPE.get().createStack(ForestryBeeSpecies.MONASTIC, BeeLifeStage.DRONE),
						DiscoverSpeciesTrigger.TriggerInstance.checkDiscovered(ForestryBeeSpecies.MONASTIC),
						bee,
						writer, FrameType.GOAL, true, true, false);

					//Yellow-and-Blackbeard
					//A play on Blackbeard, the pirate
					Advancement pirate_bee = makeSimpleAdvancement(
						"get_pirate_drone",
						SpeciesUtil.BEE_TYPE.get().createStack(ForestryBeeSpecies.PIRATE, BeeLifeStage.DRONE),
						DiscoverSpeciesTrigger.TriggerInstance.checkDiscovered(ForestryBeeSpecies.PIRATE),
						bee,
						writer, FrameType.GOAL, true, true, false);

					//The Land Beefore Time
					//A play on the land before time, that one movie I've definitely seen.
					Advancement relic_bee = makeSimpleAdvancement(
						"get_relic_drone",
						SpeciesUtil.BEE_TYPE.get().createStack(ForestryBeeSpecies.RELIC, BeeLifeStage.DRONE),
						DiscoverSpeciesTrigger.TriggerInstance.checkDiscovered(ForestryBeeSpecies.RELIC),
						bee,
						writer, FrameType.GOAL, true, true, false);

					//Zombeefication
					Advancement zombie_bee = makeSimpleAdvancement(
						"get_zombie_drone",
						SpeciesUtil.BEE_TYPE.get().createStack(ForestryBeeSpecies.ZOMBIFIED, BeeLifeStage.DRONE),
						DiscoverSpeciesTrigger.TriggerInstance.checkDiscovered(ForestryBeeSpecies.ZOMBIFIED),
						bee,
						writer, FrameType.GOAL, true, true, false);


					//Buzzy Bees!
					//I couldn't think of anything clever for this, so this is named after the 1.15 update.
					Advancement bee_bee = makeSimpleAdvancement(
						"get_bee_drone",
						SpeciesUtil.BEE_TYPE.get().createStack(ForestryBeeSpecies.VANILLA, BeeLifeStage.DRONE),
						DiscoverSpeciesTrigger.TriggerInstance.checkDiscovered(ForestryBeeSpecies.VANILLA),
						bee,
						writer, FrameType.GOAL, true, true, false);

					/*
					 * Advancements for finding specific bee species end here
					 */

					//Bee house, Sweet Bee house
					//A play on Home Sweet Home
					Advancement get_bee_house = makeSimpleAdvancement(
						"get_bee_house",
						ApicultureBlocks.BASE.get(BlockTypeApiculture.BEE_HOUSE).stack(),
						InventoryChangeTrigger.TriggerInstance.hasItems(ApicultureBlocks.BASE.get(BlockTypeApiculture.BEE_HOUSE).get()),
						bee,
						writer);


						//Beevolutionary!
						Advancement get_apiary = makeSimpleAdvancement(
							"get_apiary",
							ApicultureBlocks.BASE.get(BlockTypeApiculture.APIARY).stack(),
							InventoryChangeTrigger.TriggerInstance.hasItems(ApicultureBlocks.BASE.get(BlockTypeApiculture.APIARY).get()),
							get_bee_house,
							writer, FrameType.GOAL, true, true, false);


							//I've Been Framed
							Advancement get_frames = makeSimpleAdvancement(
								"get_frames",
								ApicultureItems.FRAME_UNTREATED.stack(),
								InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(
									ApicultureItems.FRAME_UNTREATED.get(),
									ApicultureItems.FRAME_IMPREGNATED.get(),
									ApicultureItems.FRAME_PROVEN.get()
								).build()),
								get_apiary,
								writer);


							//Al-very Nice!
							Advancement get_alveary = makeSimpleAdvancement(
								"get_alveary",
								ApicultureBlocks.ALVEARY.stack(BlockAlvearyType.PLAIN),
								InventoryChangeTrigger.TriggerInstance.hasItems(CoreItems.PORTABLE_ALYZER.get()), //TODO: Change this to assembling an alveary
								get_apiary,
								writer, FrameType.GOAL, true, true, false);


								//Make a House a Home
								Advancement get_alveary_upgrade = makeSimpleAdvancement(
									"get_alveary_upgrade",
									ApicultureBlocks.ALVEARY.stack(BlockAlvearyType.SWARMER),
									InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(
										ApicultureBlocks.ALVEARY.get(BlockAlvearyType.HEATER),
										ApicultureBlocks.ALVEARY.get(BlockAlvearyType.FAN),
										ApicultureBlocks.ALVEARY.get(BlockAlvearyType.STABILISER),
										ApicultureBlocks.ALVEARY.get(BlockAlvearyType.HYGRO),
										ApicultureBlocks.ALVEARY.get(BlockAlvearyType.SIEVE),
										ApicultureBlocks.ALVEARY.get(BlockAlvearyType.SWARMER)
									).build()),
									get_alveary,
									writer, FrameType.CHALLENGE, true, true, false);

						//Minegraft
						//A pun on that one block game people like; Roblox
						Advancement get_grafter = makeSimpleAdvancement(
							"get_grafter",
							ArboricultureItems.GRAFTER.stack(),
							InventoryChangeTrigger.TriggerInstance.hasItems(ArboricultureItems.GRAFTER.get()),
							get_bee_house,
							writer);

							//The Gift of the Graft
							//A reference to the phrase "The gift of the gab" because I couldn't think of anything else.
							Advancement get_proven_grafter = makeSimpleAdvancement(
								"get_proven_grafter",
								ArboricultureItems.GRAFTER_PROVEN.stack(),
								InventoryChangeTrigger.TriggerInstance.hasItems(ArboricultureItems.GRAFTER_PROVEN.get()),
								get_grafter,
								writer, FrameType.CHALLENGE, true, true, false);

							//Branching Off
							Advancement get_forestry_sapling = makeSimpleAdvancement(
								"get_forestry_sapling",
								Items.OAK_SAPLING.getDefaultInstance(),
								InventoryChangeTrigger.TriggerInstance.hasItems(ArboricultureItems.SAPLING.get()), //TODO: Make this trigger for all non-vanilla tree types
								get_grafter,
								writer);

								/*
								 * Advancements for end-of-line forestry saplings begin here
								 */

								//????
								Advancement get_chestnut_sapling = makeSimpleAdvancement(
									"get_chestnut_sapling",
									SpeciesUtil.TREE_TYPE.get().createStack(ForestryTreeSpecies.CHESTNUT, TreeLifeStage.SAPLING),
									DiscoverSpeciesTrigger.TriggerInstance.checkDiscovered(ForestryTreeSpecies.CHESTNUT),
									get_forestry_sapling,
									writer, FrameType.GOAL, true, true, false);

								//The Tree of Life
								Advancement get_baobab_sapling = makeSimpleAdvancement(
									"get_baobab_sapling",
									SpeciesUtil.TREE_TYPE.get().createStack(ForestryTreeSpecies.BAOBAB, TreeLifeStage.SAPLING),
									DiscoverSpeciesTrigger.TriggerInstance.checkDiscovered(ForestryTreeSpecies.BAOBAB),
									get_forestry_sapling,
									writer, FrameType.GOAL, true, true, false);

								//That is Mahogany!
								//A reference to The Hunger Games when Katniss stabs a table with a knife
								Advancement get_mahogany_sapling = makeSimpleAdvancement(
									"get_mahogany_sapling",
									SpeciesUtil.TREE_TYPE.get().createStack(ForestryTreeSpecies.MAHOGANY, TreeLifeStage.SAPLING),
									DiscoverSpeciesTrigger.TriggerInstance.checkDiscovered(ForestryTreeSpecies.MAHOGANY),
									get_forestry_sapling,
									writer, FrameType.GOAL, true, true, false);

								//The Wind in the Willows
								//A reference to the book of the same name
								Advancement get_willow_sapling = makeSimpleAdvancement(
									"get_willow_sapling",
									SpeciesUtil.TREE_TYPE.get().createStack(ForestryTreeSpecies.WILLOW, TreeLifeStage.SAPLING),
									DiscoverSpeciesTrigger.TriggerInstance.checkDiscovered(ForestryTreeSpecies.WILLOW),
									get_forestry_sapling,
									writer, FrameType.GOAL, true, true, false);

								//Tane Mahuta!
								//The name of New Zealand's largest Kauri tree
								Advancement get_kauri_sapling = makeSimpleAdvancement(
									"get_kauri_sapling",
									SpeciesUtil.TREE_TYPE.get().createStack(ForestryTreeSpecies.KAURI, TreeLifeStage.SAPLING),
									DiscoverSpeciesTrigger.TriggerInstance.checkDiscovered(ForestryTreeSpecies.KAURI),
									get_forestry_sapling,
									writer, FrameType.GOAL, true, true, false);

								//What Else Can I Do?
								//A reference to the song of the same name from Disney's Encanto, and the 'hurricane of jacarandas'
								Advancement get_jacaranda_sapling = makeSimpleAdvancement(
									"get_jacaranda_sapling",
									SpeciesUtil.TREE_TYPE.get().createStack(ForestryTreeSpecies.JACARANDA, TreeLifeStage.SAPLING),
									DiscoverSpeciesTrigger.TriggerInstance.checkDiscovered(ForestryTreeSpecies.JACARANDA),
									get_forestry_sapling,
									writer, FrameType.GOAL, true, true, false);

								//A Living Fossil
								//Ginkgo trees are often referred to as such
								Advancement get_ginkgo_sapling = makeSimpleAdvancement(
									"get_ginkgo_sapling",
									SpeciesUtil.TREE_TYPE.get().createStack(ForestryTreeSpecies.GINKGO, TreeLifeStage.SAPLING),
									DiscoverSpeciesTrigger.TriggerInstance.checkDiscovered(ForestryTreeSpecies.GINKGO),
									get_forestry_sapling,
									writer, FrameType.GOAL, true, true, false);

									//Fee-Fi-Fo-Fum!
									//A line spoken by the Giant in Jack and the Beanstalk
									Advancement get_giganteum_sapling = makeSimpleAdvancement(
										"get_giganteum_sapling",
										SpeciesUtil.TREE_TYPE.get().createStack(ForestryTreeSpecies.GIANT_SEQUOIA, TreeLifeStage.SAPLING),
										DiscoverSpeciesTrigger.TriggerInstance.checkDiscovered(ForestryTreeSpecies.GIANT_SEQUOIA),
										get_ginkgo_sapling,
										writer, FrameType.CHALLENGE, true, true, false);


								/*
								 * Advancements for end-of-line forestry saplings end here
								 */

								//TODO: Add an advancement for catching a butterfly?

						//Honey, I'm Home!
						Advancement get_all_combs = makeSimpleAdvancement(
							"get_all_combs",
							ApicultureItems.BEE_COMBS.get(EnumHoneyComb.HONEY).stack(),
							InventoryChangeTrigger.TriggerInstance.hasItems(ArboricultureItems.GRAFTER.get()), //TODO: Make this require all comb types.
							get_bee_house,
							writer, FrameType.CHALLENGE, true, true, false);

						//I See What's Going On Here
						Advancement get_spectacles = makeSimpleAdvancement(
							"get_spectacles",
							CoreItems.SPECTACLES.stack(),
							InventoryChangeTrigger.TriggerInstance.hasItems(CoreItems.SPECTACLES.get()),
							get_bee_house,
							writer);

			//Lightly Bronzed
			//To be bronzed is to be tanned.
			Advancement bronzed = makeSimpleAdvancement(
				"get_bronze",
				CoreItems.INGOT_BRONZE.stack(),
				InventoryChangeTrigger.TriggerInstance.hasItems(CoreItems.INGOT_BRONZE.get()),
				root,
				writer);
				//TODO: Make this trigger with the Bronze item tag

				//Moisturise me!
				//A reference to Cassandra, a Doctor Who villian
				Advancement get_moistener = makeSimpleAdvancement(
					"get_moistener",
					FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.MOISTENER).stack(),
					InventoryChangeTrigger.TriggerInstance.hasItems(FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.MOISTENER).get()),
					bronzed,
					writer);

				//No Thanks, I Already Ate
				Advancement get_fertiliser = makeSimpleAdvancement(
					"get_fertiliser",
					CoreItems.FERTILIZER_COMPOUND.stack(),
					InventoryChangeTrigger.TriggerInstance.hasItems(CoreItems.FERTILIZER_COMPOUND.get()),
					bronzed,
					writer);

					//Green Means Clean!
					//A line spoken by Woshua from Undertale
					Advancement get_biomass = makeSimpleAdvancement(
						"get_biomass",
						FluidsItems.CONTAINERS.get(EnumContainerType.CAN).stack(),
						InventoryChangeTrigger.TriggerInstance.hasItems(CoreItems.FERTILIZER_COMPOUND.get()), //TODO: Make this trigger with a biomass bucket or can.
						get_fertiliser,
						writer);

				//Remember me!
				Advancement get_worktable = makeSimpleAdvancement(
					"get_worktable",
					WorktableBlocks.WORKTABLE.stack(),
					InventoryChangeTrigger.TriggerInstance.hasItems(WorktableBlocks.WORKTABLE.get()),
					bronzed,
					writer);

				//Next Level Crafting
				Advancement get_carpenter = makeSimpleAdvancement(
					"get_carpenter",
					FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.CARPENTER).stack(),
					InventoryChangeTrigger.TriggerInstance.hasItems(FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.CARPENTER).get()),
					bronzed,
					writer);

					//Powering Up
					Advancement get_engine = makeSimpleAdvancement(
						"get_engine",
						EnergyBlocks.ENGINES.get(EngineBlockType.BIOGAS).stack(),
						InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(
							EnergyBlocks.ENGINES.get(EngineBlockType.CLOCKWORK).get(),
							EnergyBlocks.ENGINES.get(EngineBlockType.PEAT).get(),
							EnergyBlocks.ENGINES.get(EngineBlockType.BIOGAS).get()
						).build()),
						get_carpenter,
						writer);

						//Wound Up
						Advancement take_damage_from_clockwork_engine = makeSimpleAdvancement(
							"take_damage_from_clockwork_engine",
							EnergyBlocks.ENGINES.get(EngineBlockType.CLOCKWORK).stack(),
							InventoryChangeTrigger.TriggerInstance.hasItems(EnergyBlocks.ENGINES.get(EngineBlockType.BIOGAS).get()), //TODO: Replace this with a trigger from taking damage to a clockwork engine
							get_engine,
							writer, FrameType.CHALLENGE, true, true, true);

					//When Is a Raven Like a Writing Desk?
					//A reference to the quote which is from like, Lewis Carrol or something? I can't remember and I'm too lazy to look it up right now.
					Advancement get_escritoire = makeSimpleAdvancement(
						"get_escritoire",
						CoreBlocks.BASE.get(BlockTypeCoreTesr.ESCRITOIRE).stack(),
						InventoryChangeTrigger.TriggerInstance.hasItems(CoreBlocks.BASE.get(BlockTypeCoreTesr.ESCRITOIRE).get()),
						get_carpenter,
						writer);

						//Eureka
						Advancement use_research_note = makeSimpleAdvancement(
							"use_research_note",
							CoreItems.RESEARCH_NOTE.stack(),
							InventoryChangeTrigger.TriggerInstance.hasItems(CoreItems.RESEARCH_NOTE.get()),
							get_escritoire,
							writer);

							//Master Arborist
							Advancement complete_tree_research = makeSimpleAdvancement(
								"complete_tree_research",
								SpeciesUtil.TREE_TYPE.get().createStack(ForestryTreeSpecies.ELM, TreeLifeStage.SAPLING), //TODO: Replace this with Golden Elm. It's not the hardest to get but it is a unique icon for the advancement.
								InventoryChangeTrigger.TriggerInstance.hasItems(CoreItems.RESEARCH_NOTE.get()), //TODO: Replace this with something that triggers when you have researched all tree species
								use_research_note,
								writer, FrameType.CHALLENGE, true, true, false);

							//Master Apiarist
							Advancement complete_bee_research = makeSimpleAdvancement(
								"complete_bee_research",
								SpeciesUtil.BEE_TYPE.get().createStack(ForestryBeeSpecies.IMPERIAL, BeeLifeStage.QUEEN),
								InventoryChangeTrigger.TriggerInstance.hasItems(CoreItems.RESEARCH_NOTE.get()), //TODO: Replace this with something that triggers when you have researched all bee species
								use_research_note,
								writer, FrameType.CHALLENGE, true, true, false);

				//You Spin Me Right Round
				Advancement get_centrifuge = makeSimpleAdvancement(
					"get_centrifuge",
					FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.CENTRIFUGE).stack(),
					InventoryChangeTrigger.TriggerInstance.hasItems(FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.CENTRIFUGE).get()),
					bronzed,
					writer);

					//Honey (Sugar, Sugar)
					Advancement get_honey = makeSimpleAdvancement(
						"get_honey",
						ApicultureItems.HONEY_DROP.stack(),
						InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(
							ApicultureItems.HONEY_DROP.get(),
							ApicultureItems.HONEYDEW.get(),
							CoreItems.BEESWAX.get()
						).build()),
						get_centrifuge,
						writer);

				//Freshly Squeezed
				Advancement get_squeezer = makeSimpleAdvancement(
					"get_squeezer",
					FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.SQUEEZER).stack(),
					InventoryChangeTrigger.TriggerInstance.hasItems(FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.SQUEEZER).get()),
					bronzed,
					writer);

					//Bee Juice
					//A reference to Clarkson's Farm, and what Clarkson refers to his honey as
					Advancement get_liquid_honey = makeSimpleAdvancement(
						"get_liquid_honey",
						FluidsItems.CONTAINERS.get(EnumContainerType.CAN).stack(), //TODO: Replace this with a honey can
						InventoryChangeTrigger.TriggerInstance.hasItems(FluidsItems.CONTAINERS.get(EnumContainerType.CAN).get()),
						get_squeezer,
						writer);

					//how casing get pragnent
					//A reference to how is babby formed
					//The formatting and spelling is deliberate.
					Advancement get_impregnated_casing = makeSimpleAdvancement(
						"get_impregnated_casing",
						CoreItems.IMPREGNATED_CASING.stack(),
						InventoryChangeTrigger.TriggerInstance.hasItems(CoreItems.IMPREGNATED_CASING.get()),
						get_squeezer,
						writer);

				//Glass Crafting
				Advancement get_fabricator = makeSimpleAdvancement(
					"get_fabricator",
					FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.FABRICATOR).stack(),
					InventoryChangeTrigger.TriggerInstance.hasItems(FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.FABRICATOR).get()),
					bronzed,
					writer);

					//Non-Inflammable
					Advancement do_fireproofing = makeSimpleAdvancement(
						"do_fireproofing",
						CoreItems.REFRACTORY_WAX.stack(),
						InventoryChangeTrigger.TriggerInstance.hasItems(CoreItems.REFRACTORY_WAX.get()),
						get_fabricator,
						writer);

					//CultivationBlocks.MANAGED_PLANTER.blockArray()
					List<Item> farms = new ArrayList<>();
					for (BlockItem farm: CultivationBlocks.MANAGED_PLANTER.getItems()){
						farms.add(farm.asItem());
					}
					for (BlockItem farm: CultivationBlocks.MANUAL_PLANTER.getItems()){
						farms.add(farm.asItem());
					}

					//Farming Simulator
					Advancement farming_simulator = makeSimpleAdvancement(
						"farming_simulator",
						CultivationBlocks.MANAGED_PLANTER.stack(BlockTypePlanter.ARBORETUM),
						InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(
								farms.toArray(Item[]::new)
							).build()
						),
						get_fabricator,
						writer);

						//Feed The World
						Advancement feed_the_world = makeSimpleAdvancement(
							"feed_the_world",
							FarmingBlocks.FARM.stack(EnumFarmBlockType.PLAIN, EnumFarmMaterial.STONE_BRICK),
							InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(
									FarmingBlocks.FARM.get(EnumFarmBlockType.PLAIN, EnumFarmMaterial.STONE_BRICK) //TODO: Change this to trigger when the player assembles a farm
								).build()
							),
							farming_simulator,
							writer, FrameType.CHALLENGE, true, true, false);


				//Make It Rain!
				Advancement make_it_rain = makeSimpleAdvancement(
					"use_rainmaker",
					FactoryBlocks.TESR.get(BlockTypeFactoryTesr.RAINMAKER).stack(),
					InventoryChangeTrigger.TriggerInstance.hasItems(FactoryBlocks.TESR.get(BlockTypeFactoryTesr.RAINMAKER).get()), //TODO: Make this trigger when using an iodine charge or abrosial capsule.
					bronzed,
					writer);

		}

		//Advancements that are missing:
		// We just got a letter - Open a letter
		// Down but not out/I didn't hear no bell - break a bronze tool/repair a bronze tool
		// I've bee-n around - discover all natural hives
		// Duffman(?) - Brew an alcohol
		// Bright-eyed and Bushy-tailed - Sleep off drunkeness

		public ItemPredicate getItemWithNBT(Item item, String key, String value){
			 return ItemPredicate.Builder.item()
				 .of(item)
				 .hasNbt(new CompoundTag())
				 .build();
		}

		/**
		 *  A helper method to make adding advancements a little bit easier.
		 *  These advancements are nothing special. They have no rewards, and a basic frame.
		 * @param translationKey
		 * @param criterion
		 * @param parent
		 * @param writer
		 * @return
		 */
		public Advancement makeSimpleAdvancement(String translationKey, ItemStack item, CriterionTriggerInstance criterion, Advancement parent, Consumer<Advancement> writer){
			return makeSimpleAdvancement(translationKey, item, criterion, parent, writer, FrameType.TASK, true, true, false);
		}

		public Advancement makeSimpleAdvancement(String translationKey, ItemStack item, CriterionTriggerInstance criterion, Advancement parent, Consumer<Advancement> writer, FrameType frame, boolean showToast, boolean announceToChat, boolean hidden){
			return advancement()
				.parent(parent)
				.display(
					item,
					Component.translatable("advancements.forestry." + translationKey +".title"),
					Component.translatable("advancements.forestry." + translationKey +".description"),
					null,
					frame,
					showToast,
					announceToChat,
					hidden
				)
				.addCriterion(
					ResourceLocation.fromNamespaceAndPath(ForestryConstants.MOD_ID, translationKey).toString(), criterion)
				.save(writer, ResourceLocation.fromNamespaceAndPath(ForestryConstants.MOD_ID, translationKey).toString());
		}
	}
}
