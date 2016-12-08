package tigase.rpi.home.sensors.w1;

import com.pi4j.temperature.TemperatureScale;
import tigase.rpi.home.values.Temperature;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 24.10.2016.
 */
public class DS1820 extends W1AbstractPeriodDevice<Temperature> {

	private static final Logger log = Logger.getLogger(DS1820.class.getCanonicalName());

	public DS1820() {
		super("temperature-sensor", 60 * 1000);
	}

	@Override
	protected Temperature readValue() {
		try {
			String input = getW1Device().getValue();
			String[] data = input.split(" t=");

			if (data.length < 2) {
				return null;
			}
			double value = Double.parseDouble(data[1]) / 1000;

			return new Temperature(TemperatureScale.CELSIUS, value);
		} catch (IOException ex) {
			log.log(Level.WARNING, "Could not read data from DS1820, id= " + getW1Device().getId(), ex);
		}

		return null;
	}

	public static class W1Device extends com.pi4j.io.w1.W1BaseDevice {

		public W1Device(File deviceDir) {
			super(deviceDir);
		}

		@Override
		public int getFamilyId() {
			return DS1820DeviceType.FAMILY_CODE;
		}
	}
}
