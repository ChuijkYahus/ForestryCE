package forestry.energy.tiles;

import com.google.common.collect.ImmutableSet;
import forestry.api.IForestryApi;
import forestry.api.circuits.ForestryCircuitSocketTypes;
import forestry.api.circuits.ICircuitBoard;
import forestry.api.genetics.alleles.IKaryotype;
import forestry.core.circuits.IEngineUpgradeable;
import forestry.core.circuits.ISocketable;
import forestry.core.config.Constants;
import forestry.core.config.ForestryConfig;
import forestry.core.inventory.InventoryAdapter;
import forestry.energy.blocks.SolarPanelBlock;
import forestry.energy.features.EnergyBlocks;
import forestry.energy.features.EnergyTiles;
import forestry.energy.menu.PeatEngineMenu;
import forestry.energy.menu.SolarEngineMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;

public class SolarEngineBlockEntity extends EngineBlockEntity implements WorldlyContainer, ISocketable, IEngineUpgradeable {

	private int activePanels;
	private int miliBuffer;
	private final HashSet<BlockPos> array;
	private final InventoryAdapter sockets = new InventoryAdapter(1, "sockets");

	//used for clientside only
	private int activeCount = 0;
	private int totalCount = 0;

	public static final Direction[] HORIZONTAL_DIRECTOINS=new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};

	public SolarEngineBlockEntity(BlockPos pos, BlockState state){
		super(EnergyTiles.SOLAR_ENGINE.tileType(), pos, state, "engine.tin", Constants.ENGINE_COPPER_HEAT_MAX, 2000);

		this.array=new HashSet<>();
	}

	@Override
	public void serverTick(Level level, BlockPos pos, BlockState state) {
		super.serverTick(level, pos, state);
		if (!updateOnInterval(20)) {
			return;
		}
		if(this.array.isEmpty()){
			activePanels = 0;
			attachPanel(this.array, pos.above(), level);
			setChanged();
		}
	}

	public void attachPanel(HashSet<BlockPos> array, BlockPos pos, Level level){
		BlockState state=level.getBlockState(pos);
		if(state.getBlock() == EnergyBlocks.SOLAR_PANEL.block() && !state.getValue(SolarPanelBlock.CONNECTED)){
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
				if(state.getBlock()==EnergyBlocks.SOLAR_PANEL.block())
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
			if(state.getBlock()==EnergyBlocks.SOLAR_PANEL.block())
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
				if(level.getSkyDarken()<=7){
					miliBuffer=(activePanels * ForestryConfig.SERVER.solarRF.get())>>level.getSkyDarken();
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
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new SolarEngineMenu(containerId, playerInventory, this);
	}

	@Override
	public void openGui(ServerPlayer player, InteractionHand hand, BlockPos pos) {
		//player.displayClientMessage(Component.literal(ChatFormatting.GREEN+"Solar Array Status: "+activePanels+"/"+array.size()+ChatFormatting.WHITE+" | "+ChatFormatting.DARK_RED+"Current Output: "+(isRedstoneActivated()?currentOutput:0)+"/"+activePanels*ForestryConfig.SERVER.solarRF.get()/1000+"RF/t"),true);
		super.openGui(player, hand, pos);
	}

	@Override
	public void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		this.sockets.write(nbt);
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
		this.sockets.read(nbt);
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
		super.writeGuiData(data);
		this.sockets.writeData(data);
		data.writeInt(this.activePanels);
		data.writeInt(this.array.size());
	}

	@Override
	public void readGuiData(FriendlyByteBuf data) {
		super.readGuiData(data);
		this.sockets.readData(data);
		this.activeCount = data.readInt();
		this.totalCount = data.readInt();

	}

	@Override
	public void applyEngineUpgrade(float outputBoost, float efficiencyMult, int heat) {

	}

	@Override
	public void removeEngineUpgrade(float outputBoost, float efficiencyMult, int heat) {

	}

	@Override
	public int getSocketCount() {
		return this.sockets.getContainerSize();
	}

	@Override
	public ItemStack getSocket(int slot) {
		return this.sockets.getItem(slot);
	}

	@Override
	public void setSocket(int slot, ItemStack stack) {
		if (!stack.isEmpty() && !IForestryApi.INSTANCE.getCircuitManager().isCircuitBoard(stack)) {
			return;
		}

		// Dispose correctly of old chipsets
		if (!this.sockets.getItem(slot).isEmpty()) {
			if (IForestryApi.INSTANCE.getCircuitManager().isCircuitBoard(this.sockets.getItem(slot))) {
				ICircuitBoard chipset = IForestryApi.INSTANCE.getCircuitManager().getCircuitBoard(this.sockets.getItem(slot));
				if (chipset != null) {
					chipset.onRemoval(this);
				}
			}
		}

		this.sockets.setItem(slot, stack);
		if (stack.isEmpty()) {
			return;
		}

		ICircuitBoard chipset = IForestryApi.INSTANCE.getCircuitManager().getCircuitBoard(stack);
		if (chipset != null) {
			chipset.onInsertion(this);
		}
	}

	@Override
	public ResourceLocation getSocketType() {
		return ForestryCircuitSocketTypes.ENGINE;
	}

	//WORKS CLIENTSIDE ONLY
	public int getActivePanelCount() {
		return this.activeCount;
	}
	//WORKS CLIENTSIDE ONLY
	public int getPanelCount() {
		return this.totalCount;
	}
}
