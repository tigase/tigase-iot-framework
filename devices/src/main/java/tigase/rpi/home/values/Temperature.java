package tigase.rpi.home.values;

import com.pi4j.temperature.TemperatureConversion;
import com.pi4j.temperature.TemperatureScale;
import tigase.bot.Value;

import java.time.LocalDateTime;

/**
 * Created by andrzej on 24.10.2016.
 */
public class Temperature extends Value<Double> {

	private final TemperatureScale scale;

	public Temperature(TemperatureScale scale, double value) {
		super(value);
		this.scale = scale;
	}

	public Temperature(TemperatureScale scale, double value, LocalDateTime timestamp) {
		super(value, timestamp);
		this.scale = scale;
	}

	public double getValue(TemperatureScale scale) {
		if (scale == this.scale)
			return super.getValue();

		return TemperatureConversion.convert(this.scale, scale, super.getValue());
	}

}
