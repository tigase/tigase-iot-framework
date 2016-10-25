package tigase.rpi.home.values;

import com.pi4j.temperature.TemperatureConversion;
import com.pi4j.temperature.TemperatureScale;

/**
 * Created by andrzej on 24.10.2016.
 */
public class Temperature {

	private final TemperatureScale scale;
	private final double value;

	public Temperature(TemperatureScale scale, double value) {
		this.scale = scale;
		this.value = value;
	}

	public double getValue(TemperatureScale scale) {
		if (scale == this.scale)
			return value;

		return TemperatureConversion.convert(this.scale, scale, value);
	}

}
