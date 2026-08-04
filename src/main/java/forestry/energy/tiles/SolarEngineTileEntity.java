package forestry.energy.tiles;

import forestry.core.config.Constants;
import forestry.core.config.ForestryConfig;
import forestry.energy.blocks.SolarPanelBlock;
import forestry.energy.features.EnergyBlocks;
import forestry.energy.features.EnergyTiles;
import forestry.energy.menu.SolarEngineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;

public class SolarEngineTileEntity extends EngineBlockEntity{

	public int activePanels;
	private int miliBuffer;
	public int arraySize, darkening;
	private final HashSet<BlockPos> array;

	public static final Direction[] HORIZONTAL_DIRECTOINS=new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};

	public SolarEngineTileEntity(BlockPos pos, BlockState state){
		super(EnergyTiles.SOLAR_ENGINE.tileType(), pos, state, "engine.tin", Constants.ENGINE_COPPER_HEAT_MAX, 10000);

		array=new HashSet<>();
	}

	@Override
	public void serverTick(Level level, BlockPos pos, BlockState state) {
		super.serverTick(level, pos, state);
		if (!updateOnInterval(20)) {
			return;
		}
		if(array.isEmpty()){
			activePanels = 0;
			attachPanel(array, pos.above(), level);
			setChanged();
		}
	}

	public void attachPanel(HashSet<BlockPos> array, BlockPos pos, Level level){
		BlockState state=level.getBlockState(pos);
		if(state.getBlock() == EnergyBlocks.SOLAR_PANELS.block() && !state.getValue(SolarPanelBlock.CONNECTED)){
			if((worldPosition.getX()-pos.getX())*(worldPosition.getX()-pos.getX())<=256 && (worldPosition.getZ()-pos.getZ())*(worldPosition.getZ()-pos.getZ())<=256) {
				array.add(pos);
				if (state.getValue(SolarPanelBlock.IN_DAYLIGHT))
					activePanels++;
				level.setBlock(pos, state.setValue(SolarPanelBlock.CONNECTED, true), 3);
				attachPanel(array, pos.north(), level);
				attachPanel(array, pos.east(), level);
				attachPanel(array, pos.south(), level);
				attachPanel(array, pos.west(), level);
			}
		}
	}

	public boolean clearPanels(BlockPos pos){
		if(array.contains(pos)) {
			array.forEach(blockpos->{
				BlockState state=level.getBlockState(blockpos);
				if(state.getBlock()==EnergyBlocks.SOLAR_PANELS.block())
					level.setBlock(blockpos,state.setValue(SolarPanelBlock.CONNECTED,false),3);
			});
			array.clear();
			activePanels = 0;
			setChanged();
			return true;
		}
		return false;
	}

	public boolean attachNewPanel(BlockPos pos, Level level, BlockState state){
		for(Direction dir:HORIZONTAL_DIRECTOINS){
			if(array.contains(pos.relative(dir))){
				array.add(pos);
				if(state.getValue(SolarPanelBlock.IN_DAYLIGHT))
					activePanels++;
				level.setBlock(pos,state.setValue(SolarPanelBlock.CONNECTED,true),3);
				attachPanel(array,pos.north(),level);
				attachPanel(array,pos.east(),level);
				attachPanel(array,pos.south(),level);
				attachPanel(array,pos.west(),level);
				setChanged();
				return true;
			}
		}
		return false;
	}

	public boolean updatePanelExposure(BlockPos pos, boolean sun){
		if(array.contains(pos)){
			if(sun)
				activePanels++;
			else
				activePanels--;
			setChanged();
			return true;
		}
		return false;
	}

	@Override
	public void onDropContents(ServerLevel level) {
		array.forEach(pos-> {
			BlockState state=level.getBlockState(pos);
			if(state.getBlock()==EnergyBlocks.SOLAR_PANELS.block())
				level.setBlock(pos,state.setValue(SolarPanelBlock.CONNECTED,false),3);
		});
	}

	@Override
	protected void dissipateHeat() {

	}

	@Override
	protected void generateHeat() {
		heat=0;
		if(activePanels>=16)
			heat=4000;
		if(activePanels>=64)
			heat=6000;
		if(activePanels>=256)
			heat=8000;
		if(activePanels>=1024)
			heat=9000;
	}

	@Override
	protected void burn() {
		if(isRedstoneActivated()){
			if(level.dimension().location().toString().equals("twilightforest:twilight_forest")){
				miliBuffer=activePanels * ForestryConfig.SERVER.twilightSolarRF.get();
				currentOutput=miliBuffer/1000;
				miliBuffer=miliBuffer%1000;
				energyStorage.generateEnergy(currentOutput);
			}else{
				if(level.getSkyDarken()<7){
					miliBuffer+=(activePanels * ForestryConfig.SERVER.solarRF.get())>>level.getSkyDarken();
					currentOutput=miliBuffer/1000;
					miliBuffer=miliBuffer%1000;
					energyStorage.generateEnergy(currentOutput);
				}else
					currentOutput=0;
			}
		}
	}

	@Override
	protected boolean isBurning() {
		return mayBurn();
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
		return new SolarEngineMenu(i, inventory, this);
	}

	@Override
	public void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		nbt.putInt("active",activePanels);
		//it has to be this way, long array stalls the game
		nbt.putInt("array_size",array.size());
		int i=0;
		for(BlockPos pos:array){
			nbt.putLong("array_"+i,pos.asLong());
			i++;
		}
	}

	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		activePanels= nbt.getInt("active");
		int i=nbt.getInt("array_size");
		i--;
		while(i>=0){
			array.add(BlockPos.of(nbt.getLong("array_"+i)));
			i--;

		}
	}

	@Override
	public void writeGuiData(FriendlyByteBuf data) {
		super.writeData(data);
		data.writeInt(activePanels);
		data.writeInt(array.size());
		data.writeInt(level.getSkyDarken());
		data.writeInt(currentOutput);
	}

	@Override
	public void readGuiData(FriendlyByteBuf data) {
		super.readData(data);
		activePanels=data.readInt();
		arraySize=data.readInt();
		darkening=data.readInt();
		currentOutput=data.readInt();
	}
}
