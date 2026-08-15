package forestry.energy.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.items.ItemBlockForestry;
import forestry.core.items.ItemBlockTesr;
import forestry.energy.blocks.EngineBlock;
import forestry.energy.blocks.EngineBlockType;
import forestry.energy.blocks.SolarPanelBlock;
import forestry.modules.features.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

@FeatureProvider
public class EnergyBlocks {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.ENERGY);

	public static final FeatureBlockGroup<EngineBlock, EngineBlockType> ENGINES = REGISTRY.blockGroup(EngineBlock::new, EngineBlockType.VALUES).item(ItemBlockTesr::new).identifier("engine").create();

	public static final FeatureBlock<Block, BlockItem> SOLAR_PANEL = REGISTRY.block(()->new SolarPanelBlock(BlockBehaviour.Properties.copy(Blocks.DAYLIGHT_DETECTOR).sound(SoundType.METAL).pushReaction(PushReaction.BLOCK)), ItemBlockForestry::new, "solar_panel");
}
