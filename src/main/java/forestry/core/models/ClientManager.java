package forestry.core.models;

import forestry.Forestry;
import forestry.core.blocks.IBlockType;
import forestry.core.blocks.IColoredBlock;
import forestry.core.blocks.MachineProperties;
import forestry.core.features.CoreBlocks;
import forestry.core.fluids.TankManager;
import forestry.core.inventory.IInventoryAdapter;
import forestry.core.items.definitions.IColoredItem;
import forestry.core.tiles.ILiquidTankTile;
import forestry.core.tiles.IRenderableTile;
import forestry.core.tiles.TilePowered;
import forestry.core.utils.ModUtil;
import forestry.core.utils.RenderUtil;
import forestry.core.utils.ResourceUtil;
import forestry.factory.tiles.*;
import forestry.modules.features.FeatureBlock;
import forestry.modules.features.FeatureGroup;
import forestry.modules.features.FeatureItem;
import forestry.modules.features.FeatureTable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ColorResolverManager;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.fluids.FluidStack;
import org.apache.logging.log4j.core.Core;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public enum ClientManager {
	INSTANCE;

	public static final ItemColor FORESTRY_ITEM_COLOR = (stack, tintIndex) -> {
		Item item = stack.getItem();
		if (item instanceof IColoredItem coloredItem) {
			return coloredItem.getColorFromItemStack(stack, tintIndex);
		}
		else if (
			stack.is(CoreBlocks.TURF_BLOCK.item()) || stack.is(CoreBlocks.TURF.item())) {
			return Minecraft.getInstance().getItemColors().getColor(Items.GRASS_BLOCK.getDefaultInstance(), 0);

		}
		return 0xffffff;
	};
	public static final BlockColor FORESTRY_BLOCK_COLOR = (state, level, pos, tintIndex) -> {
		Block block = state.getBlock();
		if (level != null && pos != null) {
			if (block instanceof IColoredBlock coloredBlock)
				return coloredBlock.colorMultiplier(state, level, pos, tintIndex);
			else if (level.getBlockEntity(pos) != null){
				//Special handling for machine
				BlockEntity bm = level.getBlockEntity(pos);

				if (bm instanceof IRenderableTile tile) {
					if (tintIndex == 0){
						FluidStack fs = tile.getResourceTankInfo().getFluidStack();
						if (fs.getFluid() != null) {
							return RenderUtil.getFluidColor(fs.getFluid());
						}
					}
					else if (tintIndex == 1){
						FluidStack fs = tile.getProductTankInfo().getFluidStack();
						if (fs.getFluid() != null) {
							return RenderUtil.getFluidColor(fs.getFluid());
						}
					}
					return 0x000000;
				}
			}
			else if (
				block.equals(CoreBlocks.TURF_BLOCK.block()) ||
					block.equals(CoreBlocks.TURF.block())) {

				return BiomeColors.getAverageGrassColor(level, pos);

			}
		}
		return 0xffffff;
	};

	/* CUSTOM MODELS*/
	private final List<BlockModelEntry> customBlockModels = new ArrayList<>();
	private final List<ModelEntry> customModels = new ArrayList<>();
	/* DEFAULT ITEM AND BLOCK MODEL STATES*/
	@Nullable
	private ModelState defaultBlockState;

	public ModelState getDefaultBlockState() {
		if (this.defaultBlockState == null) {
            this.defaultBlockState = ResourceUtil.loadTransform(new ResourceLocation("block/block"));
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
        this.customBlockModels.add(new BlockModelEntry(model, block, item, states));
	}

	public void registerModel(BakedModel model, Item item) {
        this.customModels.add(new ModelEntry(new ModelResourceLocation(ModUtil.getRegistryName(item), "inventory"), model));
	}

	public void onBakeModels(ModelEvent.ModifyBakingResult event) {
		//register custom models
		Map<ResourceLocation, BakedModel> registry = event.getModels();
		for (final BlockModelEntry entry : this.customBlockModels) {
			for (BlockState state : entry.states) {
				registry.put(BlockModelShaper.stateToModelLocation(state), entry.model);
			}
			if (entry.item != null) {
				ResourceLocation registryName = ModUtil.getRegistryName(entry.item);
				if (registryName == null) {
					continue;
				}
				registry.put(new ModelResourceLocation(registryName, "inventory"), entry.model);
			}
		}

		for (final ModelEntry entry : this.customModels) {
			registry.put(entry.modelLocation, entry.model);
		}
	}

	private record BlockModelEntry(BakedModel model, Block block, @Nullable BlockItem item,
								   Collection<BlockState> states) {
	}

	private record ModelEntry(ModelResourceLocation modelLocation, BakedModel model) {
	}
}
