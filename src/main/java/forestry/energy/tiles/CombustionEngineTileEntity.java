package forestry.energy.tiles;

import forestry.api.IForestryApi;
import forestry.api.circuits.ForestryCircuitSocketTypes;
import forestry.api.circuits.ICircuitBoard;
import forestry.api.core.ForestryError;
import forestry.api.core.IErrorLogic;
import forestry.api.fuels.FuelManager;
import forestry.core.circuits.IEngineUpgradeable;
import forestry.core.circuits.ISocketable;
import forestry.core.config.Constants;
import forestry.core.fluids.*;
import forestry.core.inventory.InventoryAdapter;
import forestry.core.tiles.ILiquidTankTile;
import forestry.energy.features.EnergyTiles;
import forestry.energy.inventory.InventoryEngineBiogas;
import forestry.energy.inventory.InventoryEngineCombustion;
import forestry.energy.menu.CombustionEngineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import static net.minecraftforge.fluids.FluidType.BUCKET_VOLUME;

public class CombustionEngineTileEntity extends EngineBlockEntity implements WorldlyContainer, ILiquidTankTile, IEngineUpgradeable, ISocketable {
	private final StandardTank burnTank;
	private final StandardTank waterTank;
	private final FilteredTank fuelTank;
	private final FilteredTank coolantTank;
	private final TankManager tankManager;
	private float burnTime;
	private int coolantTime;
	//upgrades
	private final InventoryAdapter sockets = new InventoryAdapter(1, "sockets");
	private float outputBoost=1.0f;
	private float outputMult=1.0f;
	private float efficiencyMult=1.0f;
	private float burnRate=1.0f;

	private final LazyOptional<IFluidHandler> fluidCap;

	public CombustionEngineTileEntity(BlockPos pos, BlockState state){
		super(EnergyTiles.COMBUSTION_ENGINE.tileType(), pos, state, "engine_iron", Constants.ENGINE_COPPER_HEAT_MAX, 80000);

		setInternalInventory(new InventoryEngineCombustion(this));

		this.fuelTank = new FilteredTank(Constants.ENGINE_TANK_CAPACITY).setFilters(FuelManager.combustionEngineFuel.keySet());
		this.coolantTank = new FilteredTank(Constants.ENGINE_TANK_CAPACITY,true,false).setFilters(FuelManager.combustionEngineCoolant.keySet());
		this.burnTank = new StandardTank(BUCKET_VOLUME, false, false);
		this.waterTank = new StandardTank(BUCKET_VOLUME, false, false);
		this.tankManager = new TankManager(this,fuelTank,coolantTank,burnTank,waterTank);
		this.fluidCap = LazyOptional.of(()->tankManager);
	}

	@Override
	public void serverTick(Level level, BlockPos pos, BlockState state) {
		super.serverTick(level, pos, state);
		if (!updateOnInterval(20)) {
			return;
		}

		// Check if we have suitable items waiting in the item slot
		FluidHelper.drainContainers(this.tankManager, this, InventoryEngineBiogas.SLOT_CAN);

		IErrorLogic errorLogic = getErrorLogic();

		errorLogic.setCondition(coolantTank.isEmpty() && waterTank.isEmpty(), ForestryError.NO_COOLANT);

		errorLogic.setCondition(burnTank.isEmpty() && fuelTank.isEmpty(), ForestryError.NO_FUEL);
	}

	@Override
	protected void dissipateHeat() {
		if(!isBurning() && heat>0)
			heat--;
		int heatToCool=0;
		double heatLevel=getHeatLevel();
		if(heatLevel>0.2)
			heatToCool++;
		if(heatLevel>0.25)
			heatToCool++;
		if(heatLevel>0.45)
			heatToCool+=2;
		if(heatLevel>0.55)
			heatToCool+=2;
		if(heatLevel>0.65)
			heatToCool+=2;
		if(heatLevel>0.75)
			heatToCool+=2;
		if(heatLevel>0.85)
			heatToCool+=2;

		FluidStack water=waterTank.drainInternal(heatToCool, IFluidHandler.FluidAction.EXECUTE);
		heat-=water.getAmount();
		if(waterTank.isEmpty()){
			FluidStack fluidStack=coolantTank.drainInternal(BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
			if(!fluidStack.isEmpty()){
				fluidStack.setAmount(determineCoolantTime(fluidStack));
				waterTank.setCapacity(fluidStack.getAmount());
				waterTank.setFluid(fluidStack);
				if(water.getFluid()!=fluidStack.getFluid()) {
					removeEngineUpgrade(0,determineCoolantModifier(water)/100f,0);
					applyEngineUpgrade(0, determineCoolantModifier(fluidStack)/100f, 0);
				}
			}
		}
	}

	@Override
	protected void generateHeat() {
		if(isBurning()){
			addHeat((int) (2*outputMult));
		}
	}

	@Override
	protected void burn() {

		currentOutput=0;

		if(isRedstoneActivated()){
			if(burnTime>0 && !waterTank.isEmpty()){
				currentOutput= (int) (determineFuelValue(burnTank.getFluid())*outputMult);
				if(energyStorage.getMaxEnergyStored()-energyStorage.getEnergyStored()>=currentOutput){
					burnTime-=burnRate;
					setChanged();
					energyStorage.generateEnergy(currentOutput);
					FluidStack fuel=burnTank.getFluid();
					fuel.setAmount((int) burnTime);
					burnTank.setFluid(fuel);
					this.level.updateNeighbourForOutputSignal(this.worldPosition, getBlockState().getBlock());
				}else{
					currentOutput=0;
				}
			}else if(fuelTank.getFluidAmount() >= BUCKET_VOLUME){
				FluidStack fuel=fuelTank.drainInternal(BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE).copy();
				int time=determineBurnTime(fuel);
				if(!fuel.isEmpty()){
					fuel.setAmount(time);
					burnTime=time;
				}
				burnTank.setCapacity(time);
				burnTank.setFluid(fuel);
			}
		}

	}

	@Override
	protected boolean isBurning() {
		return mayBurn() && (level.isClientSide || (!burnTank.isEmpty() && currentOutput!=0));
	}

	/**
	 * Returns the fuel value (power per cycle) an item of the passed fluid
	 */
	private static int determineFuelValue(@javax.annotation.Nullable FluidStack fluidStack) {
		if (fluidStack != null) {
			Fluid fluid = fluidStack.getFluid();
			if (FuelManager.combustionEngineFuel.containsKey(fluid)) {
				return FuelManager.combustionEngineFuel.get(fluid).powerPerCycle();
			}
		}
		return 0;
	}

	/**
	 * @return Duration of burn cycle of one bucket
	 */
	private static int determineBurnTime(@javax.annotation.Nullable FluidStack fluidStack) {
		if (fluidStack != null) {
			Fluid fluid = fluidStack.getFluid();
			if (FuelManager.combustionEngineFuel.containsKey(fluid)) {
				return FuelManager.combustionEngineFuel.get(fluid).burnDuration();
			}
		}
		return 0;
	}

	private static int determineCoolantTime(@javax.annotation.Nullable FluidStack fluidStack) {
		if (fluidStack != null) {
			Fluid fluid = fluidStack.getFluid();
			if (FuelManager.combustionEngineCoolant.containsKey(fluid)) {
				return FuelManager.combustionEngineCoolant.get(fluid).burnDuration();
			}
		}
		return 0;
	}

	private static int determineCoolantModifier(@javax.annotation.Nullable FluidStack fluidStack) {
		if (fluidStack != null) {
			Fluid fluid = fluidStack.getFluid();
			if (FuelManager.combustionEngineCoolant.containsKey(fluid)) {
				return FuelManager.combustionEngineCoolant.get(fluid).dissipationMultiplier();
			}
		}
		return 0;
	}

	public boolean isCrushedIce(){
		return waterTank.getFluidType()==ForestryFluids.ICE.getFluid();
	}

	@Override
	public void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		tankManager.write(nbt);
		nbt.putFloat("burnTime",burnTime);
		nbt.putInt("coolantTime",coolantTime);
		sockets.write(nbt);
	}

	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		tankManager.read(nbt);
		burnTime=nbt.getFloat("burnTime");
		coolantTime=nbt.getInt("coolantTime");
		sockets.read(nbt);

		ItemStack chip = this.sockets.getItem(0);
		if (!chip.isEmpty()) {
			ICircuitBoard chipset = IForestryApi.INSTANCE.getCircuitManager().getCircuitBoard(chip);
			if (chipset != null) {
				chipset.onLoad(this);
			}
		}
		if(!waterTank.isEmpty())
			applyEngineUpgrade(0,determineCoolantModifier(waterTank.getFluid())/100f,0);
	}

	@Override
	public void writeData(FriendlyByteBuf data) {
		super.writeData(data);
		tankManager.writeData(data);
		sockets.writeData(data);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void readData(FriendlyByteBuf data) {
		super.readData(data);
		tankManager.readData(data);
		sockets.readData(data);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, @javax.annotation.Nullable Direction facing) {
		if (!this.remove && cap == ForgeCapabilities.FLUID_HANDLER) {
			return this.fluidCap.cast();
		}
		return super.getCapability(cap, facing);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		this.fluidCap.invalidate();
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
		return new CombustionEngineMenu(i, inventory, this);
	}

	@Override
	public ITankManager getTankManager() {
		return tankManager;
	}

	@Override
	public void applyEngineUpgrade(float outputBoost, float efficiencyMult, int heat) {
		//dont stack multiple chokes
		if(!(outputBoost<0&&this.outputBoost<0))
			this.efficiencyMult+=efficiencyMult;
		this.outputBoost+=outputBoost;
		this.outputMult=Math.max(0.5f,this.outputBoost);
		this.burnRate=this.outputMult/Math.min(1.6f,Math.max(this.efficiencyMult,isCrushedIce()?1.0f:0.1f));
	}

	@Override
	public void removeEngineUpgrade(float outputBoost, float efficiencyMult, int heat) {
		//dont unstack multiple chokes
		if(!(outputBoost<0&&this.outputBoost<outputBoost))
			this.efficiencyMult-=efficiencyMult;
		this.outputBoost-=outputBoost;
		this.outputMult=this.outputBoost;
		this.burnRate=this.outputBoost/Math.max(this.efficiencyMult,isCrushedIce()?1.0f:0.1f);
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
}
