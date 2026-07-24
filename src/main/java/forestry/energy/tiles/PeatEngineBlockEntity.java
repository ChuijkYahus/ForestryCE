package forestry.energy.tiles;

import forestry.api.IForestryApi;
import forestry.api.circuits.ForestryCircuitSocketTypes;
import forestry.api.circuits.ICircuitBoard;
import forestry.api.core.ForestryError;
import forestry.api.fuels.FuelManager;
import forestry.core.circuits.IEngineUpgradeable;
import forestry.core.circuits.ISocketable;
import forestry.core.config.Constants;
import forestry.core.features.CoreItems;
import forestry.core.inventory.IInventoryAdapter;
import forestry.core.inventory.InventoryAdapter;
import forestry.energy.features.EnergyTiles;
import forestry.energy.inventory.InventoryEnginePeat;
import forestry.energy.menu.PeatEngineMenu;
import net.minecraft.core.BlockPos;
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

import javax.annotation.Nullable;

public class PeatEngineBlockEntity extends EngineBlockEntity implements WorldlyContainer, ISocketable, IEngineUpgradeable {
	private ItemStack fuel = ItemStack.EMPTY;
	private float burnTime;
	private int totalBurnTime;
	private int ashProduction;
	private final int ashForItem;
	private final InventoryAdapter sockets = new InventoryAdapter(1, "sockets");
	private float outputBoost=1.0f;
	private float efficiencyMult=1.0f;
	private float outputMultCap=1.0f;
	private float burnRate=1.0f;

	public PeatEngineBlockEntity(BlockPos pos, BlockState state) {
		super(EnergyTiles.PEAT_ENGINE.tileType(), pos, state, "engine.copper", Constants.ENGINE_COPPER_HEAT_MAX, 40000);

        this.ashForItem = Constants.ENGINE_COPPER_ASH_FOR_ITEM;
		setInternalInventory(new InventoryEnginePeat(this));
	}

	private int getFuelSlot() {
		IInventoryAdapter inventory = getInternalInventory();
		if (inventory.getItem(InventoryEnginePeat.SLOT_FUEL).isEmpty()) {
			return -1;
		}

		if (determineFuelValue(inventory.getItem(InventoryEnginePeat.SLOT_FUEL)) > 0) {
			return InventoryEnginePeat.SLOT_FUEL;
		}

		return -1;
	}

	private int getFreeWasteSlot() {
		IInventoryAdapter inventory = getInternalInventory();
		for (int i = InventoryEnginePeat.SLOT_WASTE_1; i <= InventoryEnginePeat.SLOT_WASTE_COUNT; i++) {
			ItemStack waste = inventory.getItem(i);
			if (waste.isEmpty()) {
				return i;
			}

			if (!CoreItems.ASH.itemEqual(waste)) {
				continue;
			}

			if (waste.getCount() < waste.getMaxStackSize()) {
				return i;
			}
		}

		return -1;
	}

	@Override
	public void serverTick(Level level, BlockPos pos, BlockState state) {
		super.serverTick(level, pos, state);

		if (updateOnInterval(40)) {
			int fuelSlot = getFuelSlot();
			boolean hasFuel = fuelSlot >= 0 && determineBurnDuration(getInternalInventory().getItem(fuelSlot)) > 0;
			getErrorLogic().setCondition(!hasFuel, ForestryError.NO_FUEL);
		}
	}

	@Override
	public void burn() {

        this.currentOutput = 0;

		if (this.burnTime > 0) {
            this.burnTime-=burnRate;
			addAsh((int) (this.outputMultCap+0.5f));

			if (isRedstoneActivated()) {
                this.currentOutput = (int) (determineFuelValue(this.fuel)*this.outputMultCap);
                this.energyStorage.generateEnergy(this.currentOutput);
                this.level.updateNeighbourForOutputSignal(this.worldPosition, getBlockState().getBlock());    //TODO - I thuink
			}
		} else if (isRedstoneActivated()) {
			int fuelSlot = getFuelSlot();
			int wasteSlot = getFreeWasteSlot();

			if (fuelSlot >= 0 && wasteSlot >= 0 && energyStorage.getEnergyStored() <= 0) {
				IInventoryAdapter inventory = getInternalInventory();
				ItemStack fuelStack = inventory.getItem(fuelSlot);
                this.burnTime = this.totalBurnTime = determineBurnDuration(fuelStack);
				if (this.burnTime > 0 && !fuelStack.isEmpty()) {
                    this.fuel = fuelStack.copy();
					removeItem(fuelSlot, 1);
				}
			}
		}
	}

	@Override
	public void dissipateHeat() {
		if (this.heat <= 0) {
			return;
		}

		int loss = 0;

		if (!isBurning()) {
			loss += 1;
		}

		final double scaledHeat = (double) heat / maxHeat;
		if(scaledHeat>0.2){
			loss++;
		}
		if(scaledHeat>0.45){
			loss++;
		}
		if(scaledHeat>0.65){
			loss++;
		}
		if(scaledHeat>0.75){
			loss++;
		}
		if(scaledHeat>0.85){
			loss++;
		}
		if(scaledHeat>0.95){
			loss++;
		}

        this.heat -= loss;
	}

	@Override
	public void generateHeat() {

		int heatToAdd = 0;

		if (isBurning()) {
			heatToAdd++;
			heatToAdd+= (int) this.outputMultCap * energyStorage.getEnergyStored()>=energyStorage.getMaxEnergyStored()?2:1;
		}

		addHeat(heatToAdd);
	}

	private void addAsh(int amount) {

        this.ashProduction += amount;
		if (this.ashProduction < this.ashForItem) {
			return;
		}

		// If we have reached the necessary amount, we need to add ash
		int wasteSlot = getFreeWasteSlot();
		if (wasteSlot >= 0) {
			IInventoryAdapter inventory = getInternalInventory();
			ItemStack wasteStack = inventory.getItem(wasteSlot);
			if (wasteStack.isEmpty()) {
				inventory.setItem(wasteSlot, CoreItems.ASH.stack());
			} else {
				wasteStack.grow(1);
			}
		}
		// Reset
        this.ashProduction = 0;
	}

	/**
	 * Returns the fuel value (power per cycle) an item of the passed ItemStack provides
	 */
	private static int determineFuelValue(ItemStack fuel) {
		if (FuelManager.peatEngineFuel.containsKey(fuel)) {
			return FuelManager.peatEngineFuel.get(fuel).powerPerCycle();
		} else {
			return 0;
		}
	}

	/**
	 * Returns the fuel value (power per cycle) an item of the passed ItemStack provides
	 */
	private static int determineBurnDuration(ItemStack fuel) {
		if (FuelManager.peatEngineFuel.containsKey(fuel)) {
			return FuelManager.peatEngineFuel.get(fuel).burnDuration();
		} else {
			return 0;
		}
	}

	// / STATE INFORMATION
	@Override
	public boolean isBurning() {
		return mayBurn() && this.burnTime > 0;
	}

	@Override
	public int getBurnTimeRemainingScaled(int i) {
		if (this.totalBurnTime == 0) {
			return 0;
		}

		return (int)(this.burnTime * i) / this.totalBurnTime;
	}

	@Override
	public boolean hasFuelMin(float percentage) {
		int fuelSlot = this.getFuelSlot();
		if (fuelSlot < 0) {
			return false;
		}

		IInventoryAdapter inventory = getInternalInventory();
		return (float) inventory.getItem(fuelSlot).getCount() / (float) inventory.getItem(fuelSlot).getMaxStackSize() > percentage;
	}

	// / LOADING AND SAVING
	@Override
	public void load(CompoundTag compoundNBT) {
		super.load(compoundNBT);

		this.sockets.read(compoundNBT);

		if (compoundNBT.contains("EngineFuelItemStack")) {
			CompoundTag fuelItemNbt = compoundNBT.getCompound("EngineFuelItemStack");
            this.fuel = ItemStack.of(fuelItemNbt);
		}

        this.burnTime = compoundNBT.getFloat("EngineBurnTime");
        this.totalBurnTime = compoundNBT.getInt("EngineTotalTime");
		if (compoundNBT.contains("AshProduction")) {
            this.ashProduction = compoundNBT.getInt("AshProduction");
		}

		ItemStack chip = this.sockets.getItem(0);
		if (!chip.isEmpty()) {
			ICircuitBoard chipset = IForestryApi.INSTANCE.getCircuitManager().getCircuitBoard(chip);
			if (chipset != null) {
				chipset.onLoad(this);
			}
		}
	}


	@Override
	public void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);

		this.sockets.write(nbt);

		if (!this.fuel.isEmpty()) {
			nbt.put("EngineFuelItemStack", this.fuel.serializeNBT());
		}

		nbt.putFloat("EngineBurnTime", this.burnTime);
		nbt.putInt("EngineTotalTime", this.totalBurnTime);
		nbt.putInt("AshProduction", this.ashProduction);
	}

	@Override
	public void writeGuiData(FriendlyByteBuf data) {
		super.writeGuiData(data);
		data.writeInt((int)this.burnTime);
		data.writeInt(this.totalBurnTime);
		this.sockets.writeData(data);
	}

	@Override
	public void readGuiData(FriendlyByteBuf data) {
		super.readGuiData(data);
        this.burnTime = data.readInt();
        this.totalBurnTime = data.readInt();
		this.sockets.readData(data);
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory pPlayerInventory, Player pPlayer) {
		return new PeatEngineMenu(windowId, pPlayerInventory, this);
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

	@Override
	public void applyEngineUpgrade(float outputBoost, float efficiencyMult, int heat) {
		//dont stack multiple chokes
		if(!(outputBoost<0&&this.outputBoost<0))
			this.efficiencyMult+=efficiencyMult;
		this.outputBoost+=outputBoost;
		this.outputMultCap=Math.max(0.5f,this.outputBoost);
		this.burnRate=this.outputMultCap/Math.min(1.6f,this.efficiencyMult);
	}

	@Override
	public void removeEngineUpgrade(float outputBoost, float efficiencyMult, int heat) {
		//dont unstack multiple chokes
		if(!(outputBoost<0&&this.outputBoost<outputBoost))
			this.efficiencyMult-=efficiencyMult;
		this.outputBoost-=outputBoost;
		this.outputMultCap=this.outputBoost;
		this.burnRate=this.outputBoost/this.efficiencyMult;
	}
}
