package forestry.apiculture.alveary.multiblock;

import forestry.api.core.climate.IClimateControlled;
import forestry.api.core.multiblock.IAlvearyComponent;
import forestry.apiculture.alveary.AlvearyBlock;
import forestry.core.platform.tile.IActivatable;
import forestry.core.content.energy.EnergyHelper;
import forestry.core.content.energy.EnergyTransferMode;
import forestry.core.content.energy.ForestryEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

// Used by Heater and Fan, which increase and decrease temperature respectively
public abstract class AlvearyClimatizerBlockEntity extends AbstractAlvearyBlockEntity implements IActivatable, IAlvearyComponent.Climatiser<AlvearyMultiblockLogic> {
	private static final int TICKS_PER_CYCLE = 1;
	private static final int FE_PER_OPERATION = 50;

	private final ForestryEnergyStorage energyStorage;
	private final byte temperatureSteps;

	private int workingTime = 0;

	protected AlvearyClimatizerBlockEntity(AlvearyBlock.Type alvearyType, BlockPos pos, BlockState state, byte temperatureSteps) {
		super(alvearyType, pos, state);
		this.temperatureSteps = temperatureSteps;

		this.energyStorage = new ForestryEnergyStorage(1000, 2000, EnergyTransferMode.RECEIVE);
	}

	/* UPDATING */
	@Override
	public void changeClimate(int tick, IClimateControlled climateControlled) {
		if (this.workingTime < 20 && EnergyHelper.consumeEnergyToDoWork(this.energyStorage, TICKS_PER_CYCLE, FE_PER_OPERATION)) {
			// one tick of work for every 10 FE
            this.workingTime += FE_PER_OPERATION / 10;
		}

		if (this.workingTime > 0) {
            this.workingTime--;
			climateControlled.addTemperatureChange(this.temperatureSteps);
		}

		setActive(this.workingTime > 0);
	}

	/* LOADING & SAVING */
	@Override
	public void loadAdditional(CompoundTag compoundNBT, HolderLookup.Provider registries) {
		super.loadAdditional(compoundNBT, registries);
        this.energyStorage.read(compoundNBT, registries);
        this.workingTime = compoundNBT.getInt("Heating");
	}

	@Override
	public void saveAdditional(CompoundTag compoundNBT, HolderLookup.Provider registries) {
		super.saveAdditional(compoundNBT, registries);
        this.energyStorage.write(compoundNBT, registries);
		compoundNBT.putInt("Heating", this.workingTime);
	}

	/* Network */
	@Override
	protected void encodeDescriptionPacket(CompoundTag packetData) {
		super.encodeDescriptionPacket(packetData);
	}

	@Override
	protected void decodeDescriptionPacket(CompoundTag packetData) {
		super.decodeDescriptionPacket(packetData);
	}

	/* IActivatable */
	@Override
	public boolean isActive() {
		return getBlockState().getValue(AlvearyBlock.STATE) == AlvearyBlock.State.ON;
	}

	@Override
	public void setActive(boolean active) {
		if (isActive() != active) {
			this.level.setBlockAndUpdate(this.worldPosition, getBlockState().setValue(AlvearyBlock.STATE, active ? AlvearyBlock.State.ON : AlvearyBlock.State.OFF));
		}
	}

	@Nullable
	public IEnergyStorage getEnergyHandler(@Nullable Direction facing) {
		return this.remove ? null : this.energyStorage;
	}
}
