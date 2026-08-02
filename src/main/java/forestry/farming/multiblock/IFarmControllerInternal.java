package forestry.farming.multiblock;

import forestry.api.core.climate.IClimateProvider;
import forestry.api.core.multiblock.IFarmController;
import forestry.core.circuits.ISocketable;
import forestry.core.fluids.ITankManager;
import forestry.api.core.IInventoryAdapter;
import forestry.core.network.IStreamableGui;
import forestry.core.owner.IOwnedTile;
import forestry.cultivation.IFarmHousingInternal;
import forestry.farming.gui.IFarmLedgerDelegate;

/**
 * Machine-specific surface of the farm controller.
 *
 * After the engine rewrite (plan Task 2.3) this extends the <b>public</b> {@link IFarmController},
 * which extends the public {@code forestry.api.core.multiblock.IMultiblockController}, rather than the
 * deleted engine-internal {@code IMultiblockControllerInternal}. Only the farm-specific accessors
 * remain (sockets, tank, farm-logic/inventory, GUI streaming).
 */
public interface IFarmControllerInternal extends IFarmController, ISocketable, IClimateProvider, IOwnedTile, IStreamableGui, IFarmHousingInternal {
	IFarmLedgerDelegate getFarmLedgerDelegate();

	IInventoryAdapter getInternalInventory();

	@Override
	ITankManager getTankManager();
}
