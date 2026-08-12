package forestry.core.circuits;

public interface ISolarEngineUpgradeable {
	void applyEngineUpgrade(float outputBoost, float efficiencyMult, int heat);

	void removeEngineUpgrade(float outputBoost, float efficiencyMult, int heat);
}
