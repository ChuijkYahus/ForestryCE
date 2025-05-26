package forestry.api.climate;

/**
 * Used to
 */
public interface IClimateControlled {
	void addTemperatureChange(byte steps);

	void addHumidityChange(byte steps);
}
