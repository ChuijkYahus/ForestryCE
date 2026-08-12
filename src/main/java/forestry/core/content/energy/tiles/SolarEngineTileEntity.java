package forestry.core.content.energy.tiles;

import forestry.api.IForestryApi;
import forestry.api.core.circuits.ForestryCircuitSocketTypes;
import forestry.api.core.circuits.ICircuitBoard;
import forestry.api.core.ForestryError;
import forestry.core.engine.circuits.ISocketable;
import forestry.core.engine.circuits.ISolarEngineUpgradeable;
import forestry.core.platform.config.Constants;
import forestry.core.platform.config.ForestryConfig;
import forestry.core.platform.inventory.InventoryAdapter;
import forestry.core.content.energy.blocks.SolarPanelBlock;
import forestry.core.content.energy.features.EnergyBlocks;
import forestry.core.content.energy.features.EnergyTiles;
import forestry.core.content.energy.menu.SolarEngineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;

public class SolarEngineTileEntity extends EngineBlockEntity implements WorldlyContainer, ISocketable, ISolarEngineUpgradeable {
	/**
	 * Sky darkening level past which panels stop generating entirely. Insolation halves for every
	 * level below this, so a panel at this level is already down to 1/128th of its rated output.
	 */
	public static final int MAX_SKY_DARKEN = 7;
	/**
	 * Scales the array size bonus. Total bonus grows with the square of the array, so a single
	 * large array is worth considerably more than the same panels split into several small ones.
	 */
	private static final double ARRAY_BONUS_FACTOR = 0.03;

	public static final Direction[] HORIZONTAL_DIRECTOINS = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

	public int activePanels;
	private final HashSet<BlockPos> array;
	private final InventoryAdapter sockets = new InventoryAdapter(1, "sockets");
	/**
	 * Fractional FE left over from the previous tick. Output is a decimal amount of FE/t but energy
	 * can only be generated in whole units, so the remainder is carried instead of being discarded.
	 */
	private double energyBuffer;
	/** Output before it is truncated to whole units, kept so the GUI can show the real rate. */
	private double outputRate;

	// Client-side mirrors of the server state, synced through the GUI stream.
	private int activeCount;
	private int totalCount;
	private int skyDarken;

	public SolarEngineTileEntity(BlockPos pos, BlockState state) {
		super(EnergyTiles.SOLAR_ENGINE.tileType(), pos, state, "engine.tin", Constants.ENGINE_COPPER_HEAT_MAX, 10000);

		this.array = new HashSet<>();
	}

	@Override
	public void serverTick(Level level, BlockPos pos, BlockState state) {
		super.serverTick(level, pos, state);

		this.getErrorLogic().setCondition(this.array.isEmpty(), ForestryError.NO_SOLAR_PANELS);
		this.getErrorLogic().setCondition(!this.array.isEmpty() && insolation(level) <= 0.0, ForestryError.NO_SUNLIGHT);

		if (!updateOnInterval(20)) {
			return;
		}
		if (this.array.isEmpty()) {
			activePanels = 0;
			attachPanel(this.array, pos.above(), level);
			setChanged();
		}
		refreshPanelExposure(level);
	}

	/**
	 * Re-reads the sky exposure of every attached panel and recounts how many of them are lit.
	 * <p>
	 * Panels used to refresh themselves from their own random tick, which for any one block averages
	 * {@code 4096 / randomTickSpeed} ticks — over a minute at the default speed, with no upper bound — so roofing an
	 * array kept paying out long after it should have stopped. Doing it from the engine bounds the delay to one
	 * interval. Recounting from scratch rather than incrementing and decrementing also means a single missed update
	 * can no longer leave the count permanently wrong.
	 *
	 * @param level The level the engine is in
	 */
	private void refreshPanelExposure(Level level) {
		int lit = 0;

		for (BlockPos panelPos : this.array) {
			// A panel in an unloaded chunk can't be measured. Treating it as dark is the conservative choice, and
			// the next sweep after the chunk loads puts it back.
			if (!level.hasChunkAt(panelPos)) {
				continue;
			}
			BlockState state = level.getBlockState(panelPos);
			if (!state.is(EnergyBlocks.SOLAR_PANEL.block())) {
				continue;
			}
			boolean exposed = level.canSeeSky(panelPos);
			if (state.getValue(SolarPanelBlock.IN_DAYLIGHT) != exposed) {
				// No neighbour updates: IN_DAYLIGHT only feeds this count, nothing reacts to it.
				level.setBlock(panelPos, state.setValue(SolarPanelBlock.IN_DAYLIGHT, exposed), Block.UPDATE_CLIENTS);
			}
			if (exposed) {
				lit++;
			}
		}

		if (lit != this.activePanels) {
			this.activePanels = lit;
			setChanged();
		}
	}

	public void attachPanel(HashSet<BlockPos> array, BlockPos pos, Level level) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() == EnergyBlocks.SOLAR_PANEL.block() && !state.getValue(SolarPanelBlock.CONNECTED)) {
			if ((worldPosition.getX() - pos.getX()) * (worldPosition.getX() - pos.getX()) <= 256 && (worldPosition.getZ() - pos.getZ()) * (worldPosition.getZ() - pos.getZ()) <= 256) {
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

	public boolean clearPanels(BlockPos pos) {
		if (array.contains(pos)) {
			array.forEach(blockpos -> {
				BlockState state = level.getBlockState(blockpos);
				if (state.getBlock() == EnergyBlocks.SOLAR_PANEL.block())
					level.setBlock(blockpos, state.setValue(SolarPanelBlock.CONNECTED, false), 3);
			});
			array.clear();
			activePanels = 0;
			setChanged();
			return true;
		}
		return false;
	}

	public boolean attachNewPanel(BlockPos pos, Level level, BlockState state) {
		for (Direction dir : HORIZONTAL_DIRECTOINS) {
			if (array.contains(pos.relative(dir))) {
				array.add(pos);
				if (state.getValue(SolarPanelBlock.IN_DAYLIGHT))
					activePanels++;
				level.setBlock(pos, state.setValue(SolarPanelBlock.CONNECTED, true), 3);
				attachPanel(array, pos.north(), level);
				attachPanel(array, pos.east(), level);
				attachPanel(array, pos.south(), level);
				attachPanel(array, pos.west(), level);
				setChanged();
				return true;
			}
		}
		return false;
	}

	@Override
	public void onDropContents(ServerLevel level) {
		array.forEach(pos -> {
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
		heat = 0;
		if (activePanels >= 16)
			heat = 4000;
		if (activePanels >= 64)
			heat = 6000;
		if (activePanels >= 256)
			heat = 8000;
		if (activePanels >= 1024)
			heat = 9000;
	}

	@Override
	protected void burn() {
		if (!isRedstoneActivated()) {
			currentOutput = 0;
			this.outputRate = 0.0;
			return;
		}
		double output = calculateOutput(this.level, this.activePanels);
		if (output <= 0.0) {
			currentOutput = 0;
			this.outputRate = 0.0;
			return;
		}
		this.outputRate = output;
		this.energyBuffer += output;
		currentOutput = (int) this.energyBuffer;
		this.energyBuffer -= currentOutput;
		energyStorage.generateEnergy(currentOutput);
	}

	private static boolean isTwilightForest(Level level) {
		return level.dimension().location().toString().equals("twilightforest:twilight_forest");
	}

	/**
	 * The fraction of a panel's rated output that reaches it under the current sky conditions.
	 * Output halves for every level of sky darkening, and the Twilight Forest is treated as a
	 * constant dim sky rather than a darkening one.
	 *
	 * @param level The level the engine is in
	 * @return Insolation as a fraction between 0 and 1
	 */
	public static double insolation(Level level) {
		return insolation(level, level.getSkyDarken());
	}

	/**
	 * Same as {@link #insolation(Level)}, but for a sky darkening level supplied by the caller so
	 * the GUI can show the value the server actually generated with.
	 *
	 * @param level     The level the engine is in
	 * @param skyDarken The sky darkening level to evaluate
	 * @return Insolation as a fraction between 0 and 1
	 */
	public static double insolation(Level level, int skyDarken) {
		if (isTwilightForest(level)) {
			return 1.0;
		}
		return skyDarken > MAX_SKY_DARKEN ? 0.0 : Math.pow(2.0, -skyDarken);
	}

	/**
	 * The rated output of a single panel before any array size bonus or insolation is applied.
	 *
	 * @param level The level the engine is in
	 * @return FE/t produced by one panel
	 */
	public static double panelOutput(Level level) {
		return isTwilightForest(level) ? ForestryConfig.SERVER.twilightSolarFE.get() : ForestryConfig.SERVER.solarFE.get();
	}

	/**
	 * Calculates the total bonus FE that should be generated by this engine based on the number of
	 * active panels, before insolation is applied.
	 *
	 * @param panels The number of active and connected solar panels
	 * @return Total bonus FE produced per tick
	 */
	public static double calcBonusOutput(int panels) {
		return panels <= 0 ? 0.0 : ARRAY_BONUS_FACTOR * Math.pow(panels - 1, 2);
	}

	/**
	 * The multiplier the array size bonus applies on top of the flat per-panel output. Insolation
	 * scales the flat output and the bonus equally, so it cancels out and does not appear here.
	 *
	 * @param level  The level the engine is in
	 * @param panels The number of active and connected solar panels
	 * @return Multiplier of 1 or more, or 0 when there are no active panels
	 */
	public static double calculateMult(Level level, int panels) {
		if (panels <= 0) {
			return 0.0;
		}
		double base = panels * panelOutput(level);
		return base <= 0.0 ? 0.0 : (base + calcBonusOutput(panels)) / base;
	}

	/**
	 * The total energy this engine generates per tick, including the array size bonus and the
	 * current insolation.
	 *
	 * @param level  The level the engine is in
	 * @param panels The number of active and connected solar panels
	 * @return FE produced per tick, as a decimal amount
	 */
	public static double calculateOutput(Level level, int panels) {
		if (panels <= 0) {
			return 0.0;
		}
		return (panels * panelOutput(level) + calcBonusOutput(panels)) * insolation(level);
	}

	@Override
	protected boolean isBurning() {
		return mayBurn();
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new SolarEngineMenu(containerId, playerInventory, this);
	}

	// Deviation from 1.20.1: 1.21.1 block entities save through saveAdditional(CompoundTag, HolderLookup.Provider),
	// and InventoryAdapter's NBT methods take the registry lookup too.
	@Override
	public void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.saveAdditional(nbt, registries);
		this.sockets.write(nbt, registries);
		nbt.putInt("active", activePanels);
		// it has to be this way, long array stalls the game
		nbt.putInt("array_size", array.size());
		int i = 0;
		for (BlockPos pos : array) {
			nbt.putLong("array_" + i, pos.asLong());
			i++;
		}
	}

	// Deviation from 1.20.1: load(CompoundTag) became loadAdditional(CompoundTag, HolderLookup.Provider).
	@Override
	public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.loadAdditional(nbt, registries);
		this.sockets.read(nbt, registries);
		activePanels = nbt.getInt("active");
		int i = nbt.getInt("array_size");
		i--;
		while (i >= 0) {
			array.add(BlockPos.of(nbt.getLong("array_" + i)));
			i--;

		}

		// Deviation from 1.20.1: the 1.20.1 solar engine never re-applied a socketed circuit on load,
		// unlike every other socketed machine. Mirrors TileCentrifuge#loadAdditional.
		ItemStack chip = this.sockets.getItem(0);
		if (!chip.isEmpty()) {
			ICircuitBoard chipset = IForestryApi.INSTANCE.getCircuitManager().getCircuitBoard(chip);
			if (chipset != null) {
				chipset.onLoad(this);
			}
		}
	}

	@Override
	public void writeGuiData(FriendlyByteBuf data) {
		super.writeGuiData(data);
		this.sockets.writeData(data);
		data.writeInt(this.activePanels);
		data.writeInt(this.array.size());
		data.writeInt(this.level.getSkyDarken());
		data.writeFloat((float) this.outputRate);
	}

	@Override
	public void readGuiData(FriendlyByteBuf data) {
		super.readGuiData(data);
		this.sockets.readData(data);
		this.activeCount = data.readInt();
		this.totalCount = data.readInt();
		this.skyDarken = data.readInt();
		this.outputRate = data.readFloat();
	}

	@Override
	public double getCurrentOutputRate() {
		return canOutput() ? this.outputRate : 0.0;
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
		return ForestryCircuitSocketTypes.SOLAR_ENGINE;
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
