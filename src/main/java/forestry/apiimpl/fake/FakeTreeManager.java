package forestry.apiimpl.fake;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import forestry.api.ForestryConstants;
import forestry.api.arboriculture.ICharcoalManager;
import forestry.api.arboriculture.ITreeManager;
import forestry.api.arboriculture.IWoodType;
import forestry.api.arboriculture.WoodBlockKind;

/**
 * The tree manager used when the arboriculture module is absent. Nothing is waxable and no wood
 * type is registered, so ItemRefractoryWax finds no target and passes the interaction through.
 */
public enum FakeTreeManager implements ITreeManager {
	INSTANCE;

	// An id no datapack defines. Vanilla resolves an undefined tag as empty rather than erroring,
	// so these are safe to query and match nothing
	private static final TagKey<Block> EMPTY_BLOCK_TAG = TagKey.create(Registries.BLOCK, ForestryConstants.forestry("empty"));
	private static final TagKey<Item> EMPTY_ITEM_TAG = TagKey.create(Registries.ITEM, ForestryConstants.forestry("empty"));

	@Override
	public boolean isLoaded() {
		return false;
	}

	@Nullable
	@Override
	public Block getRefractoryWaxed(Block block) {
		return null;
	}

	@Override
	public ICharcoalManager getCharcoalManager() {
		return FakeCharcoalManager.INSTANCE;
	}

	@Override
	public ItemStack getStack(IWoodType woodType, WoodBlockKind kind, boolean fireproof) {
		return ItemStack.EMPTY;
	}

	@Override
	public BlockState getBlock(IWoodType woodType, WoodBlockKind kind, boolean fireproof) {
		return Blocks.AIR.defaultBlockState();
	}

	@Override
	public TagKey<Block> getLogBlockTag(IWoodType kind, boolean fireproof) {
		return EMPTY_BLOCK_TAG;
	}

	@Override
	public TagKey<Item> getLogItemTag(IWoodType kind, boolean fireproof) {
		return EMPTY_ITEM_TAG;
	}

	@Override
	public List<IWoodType> getRegisteredWoodTypes() {
		return List.of();
	}

	@Override
	public void register(IWoodType woodType, WoodBlockKind woodBlockKind, boolean fireproof, BlockState blockState, Supplier<Item> itemStack) {
	}

	@Override
	public void registerLogTag(IWoodType woodType, boolean fireproof, TagKey<Block> logBlockTag, TagKey<Item> logItemTag) {
	}
}
