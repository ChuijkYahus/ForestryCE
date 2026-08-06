package forestry.core.data.models;

import forestry.api.ForestryConstants;
import forestry.api.client.IForestryClientApi;
import forestry.apiculture.alveary.BlockAlveary;
import forestry.apiculture.hives.BlockBeeHive;
import forestry.apiculture.hives.BlockHiveType;
import forestry.apiculture.apiary.BlockTypeApiculture;
import forestry.apiculture.features.ApicultureBlocks;
import forestry.arboriculture.features.ArboricultureBlocks;
import forestry.arboriculture.leaves.ForestryLeafType;
import forestry.arboriculture.features.CharcoalBlocks;
import forestry.core.content.resources.EnumResourceType;
import forestry.core.features.CoreBlocks;
import forestry.core.features.CoreItems;
import forestry.core.platform.fluids.ForestryFluids;
import forestry.core.content.energy.features.EnergyBlocks;
import forestry.core.content.machines.blocks.BlockTypeFactoryPlain;
import forestry.core.content.machines.features.FactoryBlocks;
import forestry.core.platform.util.ModUtil;
import forestry.core.content.worktable.features.WorktableBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import forestry.core.platform.block.BlockBase;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ForestryBlockStateProvider extends BlockStateProvider {
	public ForestryBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
		super(output, ForestryConstants.MOD_ID, exFileHelper);
	}

	@Override
	protected void registerStatesAndModels() {
		// Resources
		simpleBlock(CoreBlocks.BOG_EARTH.block());
		simpleBlock(CoreBlocks.HUMUS.block());

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

		generic2d(CoreItems.RAW_TIN);
		generic2d(CoreItems.INGOT_TIN);
		generic2d(CoreItems.GEAR_TIN);
		generic2d(CoreItems.INGOT_BRONZE);
		generic2d(CoreItems.GEAR_BRONZE);
		generic2d(CoreItems.GEAR_COPPER);

		// Fluids (doesn't actually show in game, but silences the warning spam from Minecraft)
		for (ForestryFluids fluid : ForestryFluids.values()) {
			Block block = fluid.getFeature().fluidBlock().block();
			ModelFile blockModel = particleOnly(this, path(block), fluid.getFeature().properties().resources[0]);
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
		singleModelBlock(this, ArboricultureBlocks.LEAVES.block(), particleOnly(this, ArboricultureBlocks.LEAVES.getName(), blockTexture(Blocks.OAK_LEAVES)));

		for (BlockHiveType type : BlockHiveType.values()) {
			BlockBeeHive feature = ApicultureBlocks.BEEHIVE.get(type).block();
			String path = path(feature);

			ResourceLocation side = modBlock(this, "beehives/" + type.getSerializedName() + ".side");
			ResourceLocation top = modBlock(this, "beehives/" + type.getSerializedName() + ".top");
			ResourceLocation bottom = modBlock(this, "beehives/" + type.getSerializedName() + ".bottom");

			singleModelBlock(this, feature, models().cubeBottomTop(path, side, bottom, top));
			generic3d(feature);
		}

		// Single-variant blocks migrated from hand-authored blockstates. Each renders one existing
		// (hand-authored) model for all states, mirroring the old {"variants":{"":{"model":...}}}.
		// Item models for these blocks stay hand-authored (custom display transforms), so no generic3d here.
		for (Block block : CoreBlocks.BASE.blockArray()) existingModelBlock(block);            // analyzer, escritoire
		for (Block block : FactoryBlocks.TESR.blockArray()) existingModelBlock(block);          // bottler, carpenter, centrifuge, ...
		for (Block block : EnergyBlocks.ENGINES.blockArray()) existingModelBlock(block);        // biogas/clockwork/peat engine
		for (Block block : CoreBlocks.NATURALIST_CHEST.blockArray()) existingModelBlock(block); // apiarist/arborist/lepidopterist chest
		existingModelBlock(CharcoalBlocks.ASH.block());
		existingModelBlock(CharcoalBlocks.CHARCOAL.block());
		existingModelBlock(CharcoalBlocks.LOG_PILE.block());
		existingModelBlock(ArboricultureBlocks.SAPLING_GE.block());
		existingModelBlock(CoreBlocks.PEAT.block());

		// Comb blocks all share the block_bee_combs model.
		ModelFile combModel = models().getExistingFile(modBlock(this, "block_bee_combs"));
		for (Block block : ApicultureBlocks.BEE_COMB.blockArray()) singleModelBlock(this, block, combModel);

		// Resource storage blocks use block/storage/<type>.
		for (EnumResourceType type : EnumResourceType.values()) {
			existingModelBlock(CoreBlocks.RESOURCE_STORAGE.get(type).block(), "storage/" + type.getSerializedName());
		}

		// Alveary components (the single-variant subset of the alveary block group).
		existingModelBlock(ApicultureBlocks.ALVEARY.get(BlockAlveary.Type.HYGRO).block(), "apiculture/alveary_hygroregulator");
		existingModelBlock(ApicultureBlocks.ALVEARY.get(BlockAlveary.Type.STABILISER).block(), "apiculture/alveary_stabilizer");
		existingModelBlock(ApicultureBlocks.ALVEARY.get(BlockAlveary.Type.SIEVE).block(), "apiculture/alveary_sieve");

		// Horizontal-facing machines migrated from hand-authored blockstates + models. Each is a
		// block/cube with per-face textures <prefix>.<n>, rotated by BlockBase.FACING. Item models
		// stay hand-authored (custom display transforms), so no generic3d here.
		horizontalMachine(this, ApicultureBlocks.BASE.get(BlockTypeApiculture.APIARY).block(), "apiary", 0, 1, 2, 4, 4, 4, 4);
		horizontalMachine(this, ApicultureBlocks.BASE.get(BlockTypeApiculture.BEE_HOUSE).block(), "beehouse", 0, 1, 2, 4, 4, 4, 4);
		horizontalMachine(this, FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.FABRICATOR).block(), "thermionic_fabricator", 0, 1, 3, 2, 4, 4, 4);
		horizontalMachine(this, WorktableBlocks.WORKTABLE.block(), "worktable", 0, 1, 3, 2, 4, 4, 4);
	}

	// Builds a block/cube model whose faces map to textures block/<prefix>.<n>, then emits a
	// horizontal-facing blockstate for it. Face suffixes are given in JSON order:
	// down, up, north, south, east, west, particle.
	// Static and taking the provider, because a content jar's blocks are generated by that jar's own
	// provider rather than by this one. Every static below does the same and takes BlockStateProvider,
	// so a content jar's provider can call any of them through the one handle it already holds, without
	// checking which sibling wants a narrower type
	public static void horizontalMachine(BlockStateProvider states, Block block, String prefix, int down, int up, int north, int south, int east, int west, int particle) {
		ModelFile model = states.models().withExistingParent(path(block), states.mcLoc("block/cube"))
			.texture("particle", states.modLoc("block/" + prefix + "." + particle))
			.texture("down", states.modLoc("block/" + prefix + "." + down))
			.texture("up", states.modLoc("block/" + prefix + "." + up))
			.texture("north", states.modLoc("block/" + prefix + "." + north))
			.texture("east", states.modLoc("block/" + prefix + "." + east))
			.texture("south", states.modLoc("block/" + prefix + "." + south))
			.texture("west", states.modLoc("block/" + prefix + "." + west));
		horizontalForestryBlock(states, block, model);
	}

	// Emits a single "" variant pointing at an existing hand-authored model named after the block.
	private void existingModelBlock(Block block) {
		existingModelBlock(block, path(block));
	}

	// Emits a single "" variant pointing at the existing hand-authored model at block/<modelPath>.
	private void existingModelBlock(Block block, String modelPath) {
		singleModelBlock(this, block, models().getExistingFile(modBlock(this, modelPath)));
	}

	public static void singleModelBlock(BlockStateProvider states, Block defaultBlock, ModelFile file) {
		states.getVariantBuilder(defaultBlock).partialState().modelForState().modelFile(file).addModel();
	}

	public static ModelFile particleOnly(BlockStateProvider states, String path, ResourceLocation particleTexture) {
		return states.models().getBuilder(path).texture("particle", particleTexture);
	}

	// Makes a 3d cube of a block for item model
	public static void generic3d(BlockStateProvider states, Block block) {
		String path = path(block);
		states.itemModels().withExistingParent(path, states.modLoc("block/" + path));
	}

	/**
	 * BlockStateProvider#horizontalBlock keys off vanilla's BlockStateProperties.HORIZONTAL_FACING,
	 * which Forestry's BlockBase doesn't carry. Its FACING is a custom EnumProperty&lt;Direction&gt;
	 * (different identity, same name). Build the variant manually using BlockBase.FACING so the
	 * lookup succeeds.
	 */
	public static void horizontalForestryBlock(BlockStateProvider states, Block block, ModelFile model) {
		states.getVariantBuilder(block).forAllStates(state -> {
			Direction facing = state.getValue(BlockBase.FACING);
			int yRot = ((int) facing.toYRot() + 180) % 360;
			return ConfiguredModel.builder()
				.modelFile(model)
				.rotationY(yRot)
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
		itemModels().withExistingParent(path(block), ResourceLocation.fromNamespaceAndPath(otherParentId.getNamespace(), "block/" + otherParentId.getPath()));
	}

	protected ModelFile existingMcBlock(String path) {
		return models().getExistingFile(mcBlock(this, path));
	}

	// Everything below this line is boilerplate code adapted from https://github.com/thedarkcolour/ModKit
	public void generic3d(Block block) {
		generic3d(this, block);
	}

	public static String path(Block block) {
		return ModUtil.getRegistryName(block).getPath();
	}

	public static ModelFile.UncheckedModelFile file(ResourceLocation resourceLoc) {
		return new ModelFile.UncheckedModelFile(resourceLoc);
	}

	public static ResourceLocation modBlock(BlockStateProvider states, String name) {
		return states.modLoc("block/" + name);
	}

	public static ResourceLocation mcBlock(BlockStateProvider states, String name) {
		return states.mcLoc("block/" + name);
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
			.texture("layer0", ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), "item/" + path));
	}
}
