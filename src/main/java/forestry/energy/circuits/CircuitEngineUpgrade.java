package forestry.energy.circuits;

import forestry.core.circuits.Circuit;
import forestry.core.circuits.IEngineUpgradeable;

public class CircuitEngineUpgrade extends Circuit {
	private final float outputBoost;
	private final float efficiencyMult;
	private final int heat;

	public CircuitEngineUpgrade(String id,float boost, float eff, int heat){
		super(id);
		outputBoost=boost;
		efficiencyMult=eff;
		this.heat=heat;
	}

	@Override
	public boolean isCircuitable(Object tile) {
		return tile instanceof IEngineUpgradeable;
	}

	@Override
	public void onInsertion(int slot, Object tile) {
		if(isCircuitable(tile) && tile instanceof IEngineUpgradeable engine){
			engine.applyEngineUpgrade(outputBoost,efficiencyMult,heat);
		}
	}

	@Override
	public void onLoad(int slot, Object tile) {
		onInsertion(slot,tile);
	}

	@Override
	public void onRemoval(int slot, Object tile) {
		if(isCircuitable(tile) && tile instanceof IEngineUpgradeable engine){
			engine.removeEngineUpgrade(outputBoost,efficiencyMult,heat);
		}
	}

	@Override
	public void onTick(int slot, Object tile) {

	}
}
