package forestry.energy.tiles;

import forestry.api.core.ForestryError;
import forestry.api.farming.HorizontalDirection;
import forestry.core.config.Constants;
import forestry.core.config.ForestryConfig;
import forestry.energy.blocks.SolarPanelBlock;
import forestry.energy.features.EnergyBlocks;
import forestry.energy.features.EnergyTiles;
import forestry.energy.menu.SolarEngineMenu;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

public class SolarEngineBlockEntity extends EngineBlockEntity {
	// level.getSkyDarken() must be less than 7 for efficiency to be nonzero
	public static final int MAX_SKY_DARKEN = 7;

	// smallest number of panels one tick of the sweep reads
	private static final int PANELS_PER_TICK = 5;

	// append-only until the array is cleared as a whole, so a sweep can index into it across ticks
	private final ArrayList<BlockPos> array;

	@VisibleForTesting
	public int activePanels;
	// fractional part of generated energy from previous ticks
	// ex. if we generate 2.5 FE, 2 is added to the energy buffer and 0.5 FE is kept here, rolling over to next tick
	private double energyRemainder;

	// used to track progress of panel light refresh checks
	private int refreshCursor;
	// number of lit panels found during current light refresh check
	private int refreshLit;
	// indices of the entries the current check found to be gone, pruned once the check wraps around
	private final IntArrayList refreshDead = new IntArrayList();

	// only sent to client for displaying in GUI
	private double outputRate;
	private int activeCount;
	private int totalCount;
	private int skyDarken;

	public SolarEngineBlockEntity(BlockPos pos, BlockState state) {
		super(EnergyTiles.SOLAR_ENGINE.tileType(), pos, state, "engine.tin", Constants.ENGINE_COPPER_HEAT_MAX, 10000);

		this.array = new ArrayList<>();
	}

	@Override
	public void serverTick(Level level, BlockPos pos, BlockState state) {
		super.serverTick(level, pos, state);

		getErrorLogic().setCondition(this.array.isEmpty(), ForestryError.NO_SOLAR_PANELS);
		getErrorLogic().setCondition(!this.array.isEmpty() && insolation(level) <= 0.0, ForestryError.NO_SUNLIGHT);

		if (updateOnInterval(20) && this.array.isEmpty()) {
			this.activePanels = 0;
			attachPanel(this.array, pos.above(), level);
			setChanged();
		}
		refreshPanelExposure(level);
	}

	// checks all panels over the course of several ticks instead of checking all at once every tick
	private void refreshPanelExposure(Level level) {
		int size = this.array.size();

		if (size == 0) {
			resetSweep();
			return;
		}
		if (this.refreshCursor >= size) {
			resetSweep();
		}

		for (int i = 0; i < PANELS_PER_TICK; i++) {
			switch (readPanel(level, this.array.get(this.refreshCursor))) {
				case LIT -> this.refreshLit++;
				// nothing else ever clears an entry whose panel is gone, so the sweep is what prunes it
				case GONE -> this.refreshDead.add(this.refreshCursor);
				// a dark panel is just not counted, and an unreadable one is left for a later sweep
				case DARK, UNREADABLE -> {
				}
			}
			this.refreshCursor++;

			if (this.refreshCursor >= size) {
				// the tally now covers every panel once, so it replaces the count instead of adjusting it
				if (this.refreshLit != this.activePanels) {
					this.activePanels = this.refreshLit;
					setChanged();
				}
				pruneDeadPanels();
				resetSweep();

				size = this.array.size();
				if (size == 0) {
					return;
				}
			}
		}
	}

	// drops the entries the sweep found to be gone, back to front so the earlier indices stay valid
	private void pruneDeadPanels() {
		if (this.refreshDead.isEmpty()) {
			return;
		}
		for (int i = this.refreshDead.size() - 1; i >= 0; i--) {
			this.array.remove(this.refreshDead.getInt(i));
		}
		setChanged();
	}

	private void resetSweep() {
		this.refreshCursor = 0;
		this.refreshLit = 0;
		this.refreshDead.clear();
	}

	private static PanelReading readPanel(Level level, BlockPos panelPos) {
		if (!level.hasChunkAt(panelPos)) {
			return PanelReading.UNREADABLE;
		}
		BlockState state = level.getBlockState(panelPos);
		if (!state.is(EnergyBlocks.SOLAR_PANEL.block())) {
			return PanelReading.GONE;
		}
		boolean exposed = level.canSeeSky(panelPos);
		if (state.getValue(SolarPanelBlock.IN_DAYLIGHT) != exposed) {
			// skip neighbor update but still sync to client (might want to make server-only instead)
			level.setBlock(panelPos, state.setValue(SolarPanelBlock.IN_DAYLIGHT, exposed), Block.UPDATE_CLIENTS);
		}
		return exposed ? PanelReading.LIT : PanelReading.DARK;
	}

	// what one sweep read of an array entry found
	private enum PanelReading {
		LIT,
		DARK,
		// the position is loaded and holds something other than a panel, so the entry is stale
		GONE,
		// the chunk is not loaded, so the entry cannot be judged either way
		UNREADABLE
	}

	public void attachPanel(List<BlockPos> array, BlockPos pos, Level level) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() == EnergyBlocks.SOLAR_PANEL.block() && !state.getValue(SolarPanelBlock.CONNECTED)) {
			if ((this.worldPosition.getX() - pos.getX()) * (this.worldPosition.getX() - pos.getX()) <= 256 && (this.worldPosition.getZ() - pos.getZ()) * (this.worldPosition.getZ() - pos.getZ()) <= 256) {
				array.add(pos);
				if (state.getValue(SolarPanelBlock.IN_DAYLIGHT))
					this.activePanels++;
				level.setBlock(pos, state.setValue(SolarPanelBlock.CONNECTED, true), 3);
				attachPanel(array, pos.north(), level);
				attachPanel(array, pos.east(), level);
				attachPanel(array, pos.south(), level);
				attachPanel(array, pos.west(), level);
			}
		}
	}

	public boolean clearPanels(BlockPos pos) {
		if (this.array.contains(pos)) {
			this.array.forEach(blockpos -> {
				BlockState state = this.level.getBlockState(blockpos);
				if (state.getBlock() == EnergyBlocks.SOLAR_PANEL.block())
					this.level.setBlock(blockpos, state.setValue(SolarPanelBlock.CONNECTED, false), Block.UPDATE_CLIENTS);
			});
			this.array.clear();
			this.activePanels = 0;
			resetSweep();
			setChanged();
			return true;
		}
		return false;
	}

	public boolean attachNewPanel(BlockPos pos, Level level, BlockState state) {
		for (Direction dir : HorizontalDirection.VALUES) {
			if (this.array.contains(pos.relative(dir))) {
				this.array.add(pos);
				if (state.getValue(SolarPanelBlock.IN_DAYLIGHT))
					this.activePanels++;
				level.setBlock(pos, state.setValue(SolarPanelBlock.CONNECTED, true), 3);
				attachPanel(this.array, pos.north(), level);
				attachPanel(this.array, pos.east(), level);
				attachPanel(this.array, pos.south(), level);
				attachPanel(this.array, pos.west(), level);
				setChanged();
				return true;
			}
		}
		return false;
	}

	@Override
	public void onDropContents(ServerLevel level) {
		this.array.forEach(pos -> {
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() == EnergyBlocks.SOLAR_PANEL.block())
				level.setBlock(pos, state.setValue(SolarPanelBlock.CONNECTED, false), 3);
		});
	}

	@Override
	protected void dissipateHeat() {
	}

	@Override
	protected void generateHeat() {
		if (this.activePanels >= 1024) {
			this.heat = 9000;
		} else if (this.activePanels >= 256) {
			this.heat = 8000;
		} else if (this.activePanels >= 64) {
			this.heat = 6000;
		} else if (this.activePanels >= 16) {
			this.heat = 4000;
		} else {
			this.heat = 0;
		}
	}

	@Override
	protected void burn() {
		if (!isRedstoneActivated()) {
			this.currentOutput = 0;
			this.outputRate = 0.0;
			return;
		}
		double currentOutput = calculateOutput(this.level, this.activePanels);
		if (currentOutput <= 0.0) {
			this.currentOutput = 0;
			this.outputRate = 0.0;
			return;
		} else {
			this.outputRate = currentOutput;
		}

		// add float output to remainder (which can have energy from previous ticks)
		this.energyRemainder += currentOutput;
		this.currentOutput = (int) this.energyRemainder;

		this.energyStorage.generateEnergy(this.currentOutput);

		// subtract the integer component of what was generated, leaving the fractional remainder
		this.energyRemainder -= this.currentOutput;
	}

	private static boolean isTwilightForest(Level level) {
		// todo use constant instead of calling toString each tick
		return level.dimension().location().toString().equals("twilightforest:twilight_forest");
	}

	public static double insolation(Level level) {
		return insolation(level, level.getSkyDarken());
	}

	public static double insolation(Level level, int skyDarken) {
		// todo replace with config map
		if (isTwilightForest(level)) {
			return 1.0;
		}
		return skyDarken > MAX_SKY_DARKEN ? 0.0 : Math.pow(2.0, -skyDarken);
	}

	public static double panelOutput(Level level) {
		// todo replace with config map
		return isTwilightForest(level) ? ForestryConfig.SERVER.twilightSolarFE.get() : ForestryConfig.SERVER.solarFE.get();
	}

	// uses bluffcon's formula to increase energy generated by large solar arrays
	public static double calculateArraySizeBonus(int panels) {
		return panels <= 0 ? 0.0 : ForestryConfig.SERVER.solarArrayBonusFactor.get() * Math.pow(panels - 1, 2);
	}

	public static double calculateMult(Level level, int panels) {
		if (panels <= 0) {
			return 0.0;
		}
		double base = panels * panelOutput(level);
		return base <= 0.0 ? 0.0 : (base + calculateArraySizeBonus(panels)) / base;
	}

	public static double calculateOutput(Level level, int panels) {
		if (panels <= 0) {
			return 0.0;
		}
		return (panels * panelOutput(level) + calculateArraySizeBonus(panels)) * insolation(level);
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
	public void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		nbt.putInt("active", this.activePanels);

		// save array as longs
		long[] positions = new long[this.array.size()];
		int i = 0;
		for (BlockPos pos : this.array) {
			positions[i++] = pos.asLong();
		}
		nbt.putLongArray("array", positions);
	}

	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		this.activePanels = nbt.getInt("active");
		this.array.clear();
		for (long packed : nbt.getLongArray("array")) {
			this.array.add(BlockPos.of(packed));
		}
		resetSweep();
	}

	@Override
	public void writeGuiData(FriendlyByteBuf data) {
		super.writeGuiData(data);
		data.writeInt(this.activePanels);
		data.writeInt(this.array.size());
		data.writeInt(this.level.getSkyDarken());
		data.writeFloat((float) this.outputRate);
	}

	@Override
	public void readGuiData(FriendlyByteBuf data) {
		super.readGuiData(data);
		this.activeCount = data.readInt();
		this.totalCount = data.readInt();
		this.skyDarken = data.readInt();
		this.outputRate = data.readFloat();
	}

	@Override
	public double getCurrentOutputRate() {
		return canOutput() ? this.outputRate : 0.0;
	}

	// WORKS CLIENTSIDE ONLY
	public int getActivePanelCount() {
		return this.activeCount;
	}

	// WORKS CLIENTSIDE ONLY
	public int getPanelCount() {
		return this.totalCount;
	}

	// WORKS CLIENTSIDE ONLY
	public int getSkyDarken() {
		return this.skyDarken;
	}
}
