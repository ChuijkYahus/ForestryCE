package forestry.apiculture.multiblock;

import forestry.api.climate.IClimateControlled;
import forestry.api.multiblock.IAlvearyComponent;
import forestry.apiculture.blocks.BlockAlveary;
import forestry.api.recipes.IHygroregulatorRecipe;
import forestry.apiculture.features.ApicultureRecipeTypes;
import forestry.apiculture.gui.ContainerAlvearyHygroregulator;
import forestry.apiculture.inventory.InventoryHygroregulator;
import forestry.core.config.Constants;
import forestry.core.fluids.FilteredTank;
import forestry.core.fluids.FluidHelper;
import forestry.core.fluids.FluidRecipeFilter;
import forestry.core.fluids.TankManager;
import forestry.api.core.IInventoryAdapter;
import forestry.core.tiles.ILiquidTankTile;
import forestry.core.utils.RecipeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

public class TileAlvearyHygroregulator extends TileAlveary implements Container, ILiquidTankTile, IAlvearyComponent.Climatiser<MultiblockLogicAlveary>, IAlvearyComponent.HasInventory {
	// Both of these lived in core (FluidRecipeFilter and RecipeUtils) and named the hygroregulator
	// recipe type, which is apiculture's. This tile is their only consumer
	private static final FluidRecipeFilter HYGROREGULATOR_INPUT = new FluidRecipeFilter(manager -> RecipeUtils.getTargetFluidsFromStacks(manager, ApicultureRecipeTypes.HYGROREGULATOR.type(), IHygroregulatorRecipe::getInputFluid));

	private final TankManager tankManager;
	private final FilteredTank liquidTank;
	private final IInventoryAdapter inventory;

	@Nullable
	private IHygroregulatorRecipe currentRecipe;
	// number of ticks the current temperature change lasts for
	private int heatTicks;

	public TileAlvearyHygroregulator(BlockPos pos, BlockState state) {
		super(BlockAlveary.Type.HYGRO, pos, state);

		this.inventory = new InventoryHygroregulator(this);
		this.liquidTank = new FilteredTank(Constants.PROCESSOR_TANK_CAPACITY).setFilter(HYGROREGULATOR_INPUT);
		this.tankManager = new TankManager(this, this.liquidTank);
	}

	@Override
	public IInventoryAdapter getInternalInventory() {
		return this.inventory;
	}

	@Override
	public boolean allowsAutomation() {
		return true;
	}

	/* UPDATING */
	@Override
	public void changeClimate(int tickCount, IClimateControlled climateControlled) {
		if (this.heatTicks <= 0) {
			FluidStack fluid = this.liquidTank.getFluid();

			if (!fluid.isEmpty()) {
				this.currentRecipe = getHygroRegulatorRecipe(this.level.getRecipeManager(), fluid);

				if (this.currentRecipe != null) {
					this.liquidTank.drainInternal(this.currentRecipe.getInputFluid().getAmount(), IFluidHandler.FluidAction.EXECUTE);
					this.heatTicks = 20;
				}
			}
		}

		if (this.heatTicks > 0) {
			this.heatTicks--;
			if (this.currentRecipe != null) {
				climateControlled.addHumidityChange(this.currentRecipe.getHumiditySteps());
				climateControlled.addTemperatureChange(this.currentRecipe.getTemperatureSteps());
			} else {
				this.heatTicks = 0;
			}
		}

		if (tickCount % 20 == 0) {
			// Check if we have suitable items waiting in the item slot
			FluidHelper.drainContainers(this.tankManager, this, 0);
		}
	}

	/* SAVING & LOADING */
	@Override
	public void loadAdditional(CompoundTag compoundNBT, HolderLookup.Provider registries) {
		super.loadAdditional(compoundNBT, registries);
        this.tankManager.read(compoundNBT, registries);

        this.heatTicks = compoundNBT.getInt("TransferTime");

		if (compoundNBT.contains("CurrentLiquid")) {
			FluidStack liquid = FluidStack.parseOptional(registries, compoundNBT.getCompound("CurrentLiquid"));
            this.currentRecipe = getHygroRegulatorRecipe(RecipeUtils.getRecipeManager(), liquid);
		}
	}


	@Override
	public void saveAdditional(CompoundTag compoundNBT, HolderLookup.Provider registries) {
		super.saveAdditional(compoundNBT, registries);
        this.tankManager.write(compoundNBT, registries);

		compoundNBT.putInt("TransferTime", this.heatTicks);
		if (this.currentRecipe != null) {
			compoundNBT.put("CurrentLiquid", this.currentRecipe.getInputFluid().save(registries));
		}
	}

	/* ILIQUIDTANKCONTAINER */
	@Override
	public TankManager getTankManager() {
		return this.tankManager;
	}

	@Nullable
	public IFluidHandler getFluidHandler(@Nullable Direction facing) {
		return this.tankManager;
	}

	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
		return new ContainerAlvearyHygroregulator(windowId, inv, this);
	}

	@Nullable
	private static IHygroregulatorRecipe getHygroRegulatorRecipe(RecipeManager manager, FluidStack input) {
		return RecipeUtils.getMatchingRecipe(manager, ApicultureRecipeTypes.HYGROREGULATOR, recipe -> FluidStack.isSameFluidSameComponents(input, recipe.getInputFluid()) && input.getAmount() >= recipe.getInputFluid().getAmount());
	}
}
