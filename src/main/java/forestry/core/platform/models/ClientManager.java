package forestry.core.platform.models;

import forestry.core.platform.block.IColoredBlock;
import forestry.core.platform.item.IColoredItem;
import forestry.core.platform.util.ModUtil;
import forestry.core.platform.util.ResourceUtil;
import forestry.core.platform.registration.FeatureBlock;
import forestry.core.platform.registration.FeatureGroup;
import forestry.core.platform.registration.FeatureItem;
import forestry.core.platform.registration.FeatureTable;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public enum ClientManager {
	INSTANCE;

	public static final ItemColor FORESTRY_ITEM_COLOR = (stack, tintIndex) -> {
		Item item = stack.getItem();
		int color = 0xffffff;
		if (item instanceof IColoredItem coloredItem) {
			color = coloredItem.getColorFromItemStack(stack, tintIndex);
		}
		// 1.21 ItemRenderer reads ARGB; legacy IColoredItem returns 0xRRGGBB (alpha 0),
		// which renders as fully transparent. Force alpha to 0xff so opaque-tint multiply
		// passes the texture through unchanged.
		return color | 0xff000000;
	};
	public static final BlockColor FORESTRY_BLOCK_COLOR = (state, level, pos, tintIndex) -> {
		Block block = state.getBlock();
		if (level != null && pos != null && block instanceof IColoredBlock coloredBlock) {
			return coloredBlock.colorMultiplier(state, level, pos, tintIndex);
		}
		return 0xffffff;
	};

	/* CUSTOM MODELS*/
	// Keyed, not appended: models are registered on ModelEvent.RegisterGeometryLoaders, which is posted once per
	// resource reload, so registering the same block again must replace its entry instead of growing these
	private final Map<Block, BlockModelEntry> customBlockModels = new LinkedHashMap<>();
	private final Map<ModelResourceLocation, BakedModel> customModels = new LinkedHashMap<>();
	/* DEFAULT ITEM AND BLOCK MODEL STATES*/
	@Nullable
	private ModelState defaultBlockState;

	public ModelState getDefaultBlockState() {
		if (this.defaultBlockState == null) {
            this.defaultBlockState = ResourceUtil.loadTransform(ResourceLocation.parse("block/block"));
		}
		return this.defaultBlockState;
	}

	public void registerModel(BakedModel model, Object feature) {
		if (feature instanceof FeatureGroup<?, ?, ?> group) {
			group.getFeatures().forEach(f -> registerModel(model, f));
		} else if (feature instanceof FeatureTable<?, ?, ?, ?> group) {
			group.getFeatures().forEach(f -> registerModel(model, f));
		} else if (feature instanceof FeatureBlock<?, ?> block) {
			registerModel(model, block.block(), block.item());
		} else if (feature instanceof FeatureItem<?> item) {
			registerModel(model, item.item());
		}
	}

	public void registerModel(BakedModel model, Block block, @Nullable BlockItem item) {
		registerModel(model, block, item, block.getStateDefinition().getPossibleStates());
	}

	public void registerModel(BakedModel model, Block block, @Nullable BlockItem item, Collection<BlockState> states) {
        this.customBlockModels.put(block, new BlockModelEntry(model, item, states));
	}

	public void registerModel(BakedModel model, Item item) {
        this.customModels.put(new ModelResourceLocation(ModUtil.getRegistryName(item), "inventory"), model);
	}

	public void onBakeModels(ModelEvent.ModifyBakingResult event) {
		//register custom models
		Map<ModelResourceLocation, BakedModel> registry = event.getModels();
		for (final BlockModelEntry entry : this.customBlockModels.values()) {
			for (BlockState state : entry.states) {
				registry.put(BlockModelShaper.stateToModelLocation(state), entry.model);
			}
			if (entry.item != null) {
				ResourceLocation registryName = ModUtil.getRegistryName(entry.item);
				if (registryName == null) {
					continue;
				}
				registry.put(ModelResourceLocation.inventory(registryName), entry.model);
			}
		}

		registry.putAll(this.customModels);
	}

	private record BlockModelEntry(BakedModel model, @Nullable BlockItem item, Collection<BlockState> states) {
	}
}
