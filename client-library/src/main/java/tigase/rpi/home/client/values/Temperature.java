package tigase.rpi.home.client.values;

import tigase.rpi.home.client.Device;

import java.util.Date;

/**
 * Created by andrzej on 26.11.2016.
 */
public class Temperature extends Device.Value<Double> {

	public static final String NAME = "Temperature";

	private final String unit;

	public Temperature(Double value, String unit, Date timestamp) {
		super(value, timestamp);
		this.unit = unit;
	}

	public String getUnit() {
		return unit;
	}

}
