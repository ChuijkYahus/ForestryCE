package forestry.core.platform.render;

import com.mojang.blaze3d.vertex.PoseStack;
import forestry.core.platform.block.NaturalistChestBlockType;
import forestry.core.platform.block.BlockBase;
import forestry.core.platform.block.BlockTypeCoreTesr;
import forestry.core.features.CoreBlocks;
import forestry.core.platform.tile.TileApiaristChest;
import forestry.core.platform.tile.TileArboristChest;
import forestry.core.platform.tile.TileLepidopteristChest;
import forestry.core.platform.tile.*;
import forestry.core.content.machines.*;
import forestry.core.content.escritoire.*;
import forestry.core.content.analyzer.*;
import forestry.core.content.energy.blocks.EngineBlockType;
import forestry.core.content.energy.features.EnergyBlocks;
import forestry.core.content.energy.tiles.BiogasEngineBlockEntity;
import forestry.core.content.energy.tiles.ClockworkEngineBlockEntity;
import forestry.core.content.energy.tiles.CombustionEngineBlockEntity;
import forestry.core.content.energy.tiles.PeatEngineBlockEntity;
import forestry.core.content.energy.tiles.SolarEngineBlockEntity;
import forestry.core.content.machines.blocks.BlockTypeFactoryTesr;
import forestry.core.content.machines.features.FactoryBlocks;
import forestry.core.content.machines.tiles.*;
import forestry.core.platform.registration.FeatureBlock;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.IdentityHashMap;
import java.util.function.BiFunction;

public class ForestryBewlr extends BlockEntityWithoutLevelRenderer {
	private final BlockEntityRenderDispatcher dispatcher;
	private final IdentityHashMap<Item, BlockEntity> tiles;

	public ForestryBewlr(BlockEntityRenderDispatcher dispatcher) {
		super(dispatcher, null);

		this.dispatcher = dispatcher;

		IdentityHashMap<Item, BlockEntity> tiles = new IdentityHashMap<>();

		addTile(tiles, CoreBlocks.BASE.get(BlockTypeCoreTesr.ESCRITOIRE), TileEscritoire::new);
		addTile(tiles, CoreBlocks.BASE.get(BlockTypeCoreTesr.ANALYZER), TileAnalyzer::new);

		addTile(tiles, CoreBlocks.NATURALIST_CHEST.get(NaturalistChestBlockType.APIARIST_CHEST), TileApiaristChest::new);
		addTile(tiles, CoreBlocks.NATURALIST_CHEST.get(NaturalistChestBlockType.ARBORIST_CHEST), TileArboristChest::new);
		addTile(tiles, CoreBlocks.NATURALIST_CHEST.get(NaturalistChestBlockType.LEPIDOPTERIST_CHEST), TileLepidopteristChest::new);

		addTile(tiles, FactoryBlocks.TESR.get(BlockTypeFactoryTesr.BOTTLER), TileBottler::new);
		addTile(tiles, FactoryBlocks.TESR.get(BlockTypeFactoryTesr.CARPENTER), TileCarpenter::new);
		addTile(tiles, FactoryBlocks.TESR.get(BlockTypeFactoryTesr.CENTRIFUGE), TileCentrifuge::new);
		addTile(tiles, FactoryBlocks.TESR.get(BlockTypeFactoryTesr.FERMENTER), TileFermenter::new);
		addTile(tiles, FactoryBlocks.TESR.get(BlockTypeFactoryTesr.MOISTENER), TileMoistener::new);
		addTile(tiles, FactoryBlocks.TESR.get(BlockTypeFactoryTesr.SQUEEZER), TileSqueezer::new);
		addTile(tiles, FactoryBlocks.TESR.get(BlockTypeFactoryTesr.STILL), TileStill::new);
		addTile(tiles, FactoryBlocks.TESR.get(BlockTypeFactoryTesr.RAINMAKER), TileMillRainmaker::new);

		addTile(tiles, EnergyBlocks.ENGINES.get(EngineBlockType.PEAT), PeatEngineBlockEntity::new);
		addTile(tiles, EnergyBlocks.ENGINES.get(EngineBlockType.BIOGAS), BiogasEngineBlockEntity::new);
		addTile(tiles, EnergyBlocks.ENGINES.get(EngineBlockType.CLOCKWORK), ClockworkEngineBlockEntity::new);
		addTile(tiles, EnergyBlocks.ENGINES.get(EngineBlockType.COMBUSTION), CombustionEngineBlockEntity::new);
		addTile(tiles, EnergyBlocks.ENGINES.get(EngineBlockType.SOLAR), SolarEngineBlockEntity::new);

		this.tiles = tiles;
	}

	private static void addTile(IdentityHashMap<Item, BlockEntity> map, FeatureBlock<?, ?> block, BiFunction<BlockPos, BlockState, BlockEntity> factory) {
		BlockState state = block.defaultState();
		if (state.hasProperty(BlockBase.FACING)) {
			state = state.setValue(BlockBase.FACING, Direction.SOUTH);
		}
		map.put(block.item(), factory.apply(BlockPos.ZERO, state));
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
		Item item = stack.getItem();
		BlockEntity blockEntity = this.tiles.get(item);

		if (blockEntity != null) {
			this.dispatcher.renderItem(blockEntity, poseStack, buffers, light, overlay);
		}
	}
}
