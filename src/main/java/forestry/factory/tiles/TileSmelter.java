package forestry.factory.tiles;

import forestry.Forestry;
import forestry.api.IForestryApi;
import forestry.api.circuits.ForestryCircuitSocketTypes;
import forestry.api.circuits.ICircuitBoard;
import forestry.api.core.ForestryError;
import forestry.api.core.IErrorLogic;
import forestry.api.recipes.*;
import forestry.core.circuits.ISocketable;
import forestry.core.config.Constants;
import forestry.core.fluids.FilteredTank;
import forestry.core.fluids.FluidHelper;
import forestry.core.fluids.FluidRecipeFilter;
import forestry.core.fluids.TankManager;
import forestry.core.inventory.IInventoryAdapter;
import forestry.core.inventory.InventoryAdapter;
import forestry.core.inventory.InventoryAdapterTile;
import forestry.core.inventory.InventoryGhostCrafting;
import forestry.core.inventory.wrappers.InventoryMapper;
import forestry.core.render.TankRenderInfo;
import forestry.core.tiles.IItemStackDisplay;
import forestry.core.tiles.ILiquidTankTile;
import forestry.core.tiles.TilePowered;
import forestry.core.utils.InventoryUtil;
import forestry.core.utils.RecipeUtils;
import forestry.energy.EnergyHelper;
import forestry.factory.blocks.BlockFactoryPlain;
import forestry.factory.features.FactoryTiles;
import forestry.factory.gui.ContainerCarpenter;
import forestry.factory.gui.ContainerCentrifuge;
import forestry.factory.gui.ContainerSmelter;
import forestry.factory.inventory.*;
import forestry.factory.recipes.SmelterRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TileSmelter extends TilePowered implements WorldlyContainer, ISocketable, IItemStackDisplay {


	private final InventoryAdapter sockets = new InventoryAdapter(1, "sockets");
	private final ResultContainer craftPreviewInventory;
	public static final int MAX_HEAT = 5000;
	private int heat = 0;
	private int meltingPoint = 0;
	private static final int TICKS_PER_RECIPE_TIME = 1;
	private static final int ENERGY_PER_WORK_CYCLE = 0; //Machine only uses power to maintain temperature.
	private static final int ENERGY_PER_RECIPE_TIME = ENERGY_PER_WORK_CYCLE / 10;
	private ISmelterRecipe currentRecipe;
	private final InventorySmelter inventory;


	public TileSmelter(BlockPos pos, BlockState state) {
		super(FactoryTiles.SMELTER.tileType(), pos, state, 1100, Constants.MACHINE_MAX_ENERGY);
		this.craftPreviewInventory = new ResultContainer();
        currentRecipe = null;
		this.inventory = new InventorySmelter(this);
		setInternalInventory(this.inventory);
    }

	/* ISocketable */
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
		return ForestryCircuitSocketTypes.MACHINE;
	}



	/* LOADING & SAVING */
	@Override
	public void saveAdditional(CompoundTag compoundNBT) {
		super.saveAdditional(compoundNBT);
		this.sockets.write(compoundNBT);
		compoundNBT.putInt("Heat", this.heat);
	}

	@Override
	public void load(CompoundTag compoundNBT) {
		super.load(compoundNBT);
		this.sockets.read(compoundNBT);

		ItemStack chip = this.sockets.getItem(0);
		if (!chip.isEmpty()) {
			ICircuitBoard chipset = IForestryApi.INSTANCE.getCircuitManager().getCircuitBoard(chip);
			if (chipset != null) {
				chipset.onLoad(this);
			}
		}

		this.heat = compoundNBT.getInt("Heat");
	}

	@Override
	public void writeData(FriendlyByteBuf data) {
		super.writeData(data);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void readData(FriendlyByteBuf data) {
		super.readData(data);
	}

	@Override
	public void writeGuiData(FriendlyByteBuf data) {
		super.writeGuiData(data);
		this.sockets.writeData(data);
		data.writeVarInt(this.heat);
		data.writeVarInt(this.meltingPoint);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void readGuiData(FriendlyByteBuf data) {
		super.readGuiData(data);
		this.sockets.readData(data);
		this.heat = data.readVarInt();
		this.meltingPoint = data.readVarInt();
	}


	/* WORKING */

	@Override
	public void serverTick(Level level, BlockPos pos, BlockState state) {
		//Forestry.LOGGER.info("Ticking smelter");
		super.serverTick(level, pos, state);
		//Forestry.LOGGER.info("Smelter heat: " + this.heat);

		if (!updateOnInterval(WORK_TICK_INTERVAL)) {
			return;
		}

		boolean hasPower = this.getEnergyManager().getEnergyStored() > 0;
		int heatPowerConsumption;
		int heatProduction;

		//If the machine is sufficiently warmed, greatly reduce the amount of energy going into warming up
		if (this.currentRecipe != null){
			//Machine is not at temperature
			if (this.heat < this.currentRecipe.getTemperature()) {
				heatPowerConsumption = 100; //Use more power to warm it up
				heatProduction = 5;
			}
			//Machine is at temperature.
			else {
				heatPowerConsumption = 40; //Use less power to generate less heat.
				heatProduction = 2;
			}
			//If there is power connected, warm the machine up
			if (hasPower) {
				this.heat = Math.min(this.heat + heatProduction, MAX_HEAT);

			}
			this.getEnergyManager().drainEnergy(heatPowerConsumption);
		}


		if (this.currentRecipe == null || !hasPower) {
			//If there is no recipe or no power, reduce heat
			if (this.heat > MAX_HEAT / 2) {
				this.heat = Math.max(this.heat - 10, 0);
			} else if (this.heat > 0) {
				this.heat = Math.max(this.heat - 5, 0);
			}
		}


		//A squeezer squeezing honey uses 2000RF per item (in bursts of 200RF)
		//A squeezer stores 40000RF and can squeeze 20 items from full. Let's apply that to here.

	}

	//Called by super.serverTick()
	@Override
	public boolean hasWork(){

		//Forestry.LOGGER.info("Checking for work");
		checkRecipe();

		boolean hasResources = this.inventory.hasResources();
		boolean hasRecipe = true;
		boolean canAdd = true;
		boolean atTemperature = true;

		if (hasResources) {
			hasRecipe = this.currentRecipe != null;
			if (hasRecipe) {
				if (!this.currentRecipe.getOutput().isEmpty()) {
					canAdd = this.inventory.addResult(this.currentRecipe.getOutput(), false);
				}
				if (this.heat < this.currentRecipe.getTemperature()) atTemperature = false;
			}
		}

		IErrorLogic errorLogic = getErrorLogic();
		errorLogic.setCondition(!hasResources, ForestryError.NO_RESOURCE);
		errorLogic.setCondition(!hasRecipe, ForestryError.NO_RECIPE);
		errorLogic.setCondition(!canAdd, ForestryError.NO_SPACE_INVENTORY);
		errorLogic.setCondition(!atTemperature, ForestryError.NOT_WARM_ENOUGH);

		return hasResources && hasRecipe && canAdd && atTemperature;
	}

	public boolean checkRecipe(){
		//Forestry.LOGGER.info("Checking recipe");
		RecipeManager manager = RecipeUtils.getRecipeManager();
		ISmelterRecipe sameRec = null;

		//Look for a recipe if the manager exists
		if (manager != null) {
			sameRec = RecipeUtils.getSmelterRecipe(manager, getResources());
			//if (sameRec != null) Forestry.LOGGER.debug("Found a recipe.");

			//If there is a disparity between the current recipe and the found recipe, update it
			if (this.currentRecipe != sameRec) {
				//Forestry.LOGGER.debug("Recipes do not match");
				this.currentRecipe = sameRec; //This accounts for sameRec being null.

				//If the updated recipe is NOT null, update everything
				if (this.currentRecipe != null) {
					handleItemStackForDisplay(currentRecipe.getOutput());

					this.meltingPoint = currentRecipe.getTemperature();

					int recipeTime = this.currentRecipe.getProcessingTime();
					setTicksPerWorkCycle(recipeTime * TICKS_PER_RECIPE_TIME);
					setEnergyPerWorkCycle(recipeTime * ENERGY_PER_RECIPE_TIME);

					//Forestry.LOGGER.debug("Updating to recipe: " + this.currentRecipe);
				}
				//If the updated recipe IS null, clear everything.
				else {
					this.craftPreviewInventory.clearContent();
					this.meltingPoint = 0;
					setTicksPerWorkCycle(0);
					//Forestry.LOGGER.debug("Removed a recipe.");
				}
				//resetRecipe();
			}
			else {
				//Forestry.LOGGER.info("Recipes match");
			}
		}
		else {
			//Forestry.LOGGER.info("No manager found.");
		}

		getErrorLogic().setCondition(this.currentRecipe == null, ForestryError.NO_RECIPE);
		return this.currentRecipe != null;
	}

	@Override
	protected boolean workCycle() {
		//Forestry.LOGGER.info("Cycling work");

		if (this.currentRecipe == null) {
			return false;
		}

		if (!this.inventory.removeResources(this.currentRecipe.getInputs())) {
			return false;
		}

		this.heat = Math.max(this.heat - 50, 0);
		this.inventory.addResult(this.currentRecipe.getOutput(), true);
		return true;
	}


	public Container getCraftPreviewInventory() {
		return this.craftPreviewInventory;
	}

	@Override
	public void handleItemStackForDisplay(ItemStack itemStack) {
		this.craftPreviewInventory.setItem(0, itemStack);
	}
	
	private List<ItemStack> getResources(){
		IInventoryAdapter inventory = this.getInternalInventory();
		return InventoryUtil.getStacks(inventory, InventorySmelter.SLOT_INPUT_1, InventorySmelter.SLOT_INPUT_COUNT);
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
		return new ContainerSmelter(windowId, player.getInventory(), this);
	}

	public int getHeatScaled(int i) {
		return this.heat * i / MAX_HEAT;
	}

	public int getMeltingPointScaled(int i) {
		int meltingPoint = getMeltingPoint();

		if (meltingPoint <= 0) {
			return 0;
		} else {
			return meltingPoint * i / MAX_HEAT;
		}
	}

	public int getHeat() {
		return this.heat;
	}

	public int getMeltingPoint() {
		return this.meltingPoint;
	}
	public void getGUINetworkData(int i, int j) {
		if (i == 0) {
			this.heat = j;
		} else if (i == 1) {
			this.meltingPoint = j;
		}
	}

	public void sendGUINetworkData(AbstractContainerMenu container, ContainerListener iCrafting) {
		iCrafting.dataChanged(container, 0, this.heat);
		iCrafting.dataChanged(container, 1, getMeltingPoint());
	}
}
