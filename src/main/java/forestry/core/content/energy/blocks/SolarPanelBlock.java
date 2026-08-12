package forestry.core.content.energy.blocks;

import forestry.core.content.energy.tiles.SolarEngineTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class SolarPanelBlock extends Block {
	public static final BooleanProperty CONNECTED = BlockStateProperties.ATTACHED;
	public static final BooleanProperty IN_DAYLIGHT = BlockStateProperties.LIT;
	protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D);

	public SolarPanelBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.defaultBlockState().setValue(CONNECTED, false).setValue(IN_DAYLIGHT, false));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(IN_DAYLIGHT, context.getLevel().canSeeSky(context.getClickedPos()));
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		if(oldState.getBlock()==this)
			return;
		for(Direction dir: SolarEngineTileEntity.HORIZONTAL_DIRECTOINS){
			BlockState newState=level.getBlockState(pos.relative(dir));
			if(newState.getBlock()==this && newState.getValue(CONNECTED)){
				for(int x = SectionPos.blockToSectionCoord(pos.getX())-1;x<=SectionPos.blockToSectionCoord(pos.getX())+1;x++){
					for(int z=SectionPos.blockToSectionCoord(pos.getZ())-1;z<=SectionPos.blockToSectionCoord(pos.getZ())+1;z++){
						if(level.hasChunk(x,z)){
							for(Map.Entry<BlockPos, BlockEntity> entry:level.getChunk(x,z).getBlockEntities().entrySet()){
								BlockPos targetPos=entry.getKey();
								//max range 16 and correct y level
								if(entry.getValue() instanceof SolarEngineTileEntity tile && targetPos.getY()==pos.getY()-1 && (targetPos.getX()-pos.getX())*(targetPos.getX()-pos.getX())<=256 && (targetPos.getZ()-pos.getZ())*(targetPos.getZ()-pos.getZ())<=256){
									if(tile.attachNewPanel(pos,level,state))
										return;
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if(newState.getBlock()==this)
			return;
		if(state.getValue(CONNECTED)){
			for(int x = SectionPos.blockToSectionCoord(pos.getX())-1;x<=SectionPos.blockToSectionCoord(pos.getX())+1;x++){
				for(int z=SectionPos.blockToSectionCoord(pos.getZ())-1;z<=SectionPos.blockToSectionCoord(pos.getZ())+1;z++){
					if(level.hasChunk(x,z)){
						for(Map.Entry<BlockPos, BlockEntity> entry:level.getChunk(x,z).getBlockEntities().entrySet()){
							if(entry.getValue() instanceof SolarEngineTileEntity tile){
								if(tile.clearPanels(pos))
									return;
							}
						}
					}
				}
			}
		}
	}

	// IN_DAYLIGHT used to be refreshed here from randomTick, which for any one block averages 4096 / randomTickSpeed
	// ticks — over a minute at the default speed — so an array kept paying out long after it had been roofed over.
	// SolarEngineTileEntity now sweeps its whole array on its own interval instead, which bounds the delay and lets
	// it recount from scratch rather than trusting a running total.

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CONNECTED,IN_DAYLIGHT);
	}
}
