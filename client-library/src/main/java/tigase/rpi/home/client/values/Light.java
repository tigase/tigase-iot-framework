package tigase.rpi.home.client.values;

import tigase.rpi.home.client.Device;

import java.util.Date;

/**
 * Created by andrzej on 26.11.2016.
 */
public class Light extends Device.Value<Integer> {

	public static final String NAME = "Light";

	private final Unit unit;

	public Light(Integer value, Unit unit) {
		this(value, unit, new Date());
	}

	public Light(Integer value, Unit unit, Date timestamp) {
		super(value, timestamp);
		this.unit = unit;
	}

	public Unit getUnit() {
		return unit;
	}

	public enum Unit {
		lm("lm"),
		procent("%");

		private final String value;

		Unit(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}
}
