package forestry.energy.circuits;

import forestry.core.circuits.Circuit;
import forestry.core.circuits.IEngineUpgradeable;
import forestry.core.circuits.ISolarEngineUpgradeable;

public class CircuitSolarEngineUpgrade extends Circuit {

	//TODO: Literally add anything here
	public CircuitSolarEngineUpgrade(String id) {
		super(id);
	}

	@Override
	public boolean isCircuitable(Object tile) {
		return tile instanceof ISolarEngineUpgradeable;
	}

	@Override
	public void onInsertion(int slot, Object tile) {

	}

	@Override
	public void onLoad(int slot, Object tile) {

	}

	@Override
	public void onRemoval(int slot, Object tile) {

	}

	@Override
	public void onTick(int slot, Object tile) {

	}
}
