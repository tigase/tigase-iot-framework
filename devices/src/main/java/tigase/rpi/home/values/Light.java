package tigase.rpi.home.values;

import tigase.bot.Value;

import java.time.LocalDateTime;

/**
 * Created by andrzej on 05.11.2016.
 */
public class Light extends Value<Integer> {

	private final Unit unit;

	public Light(int value, Unit unit) {
		super(value);
		this.unit = unit;
	}

	public Light(int value, Unit unit, LocalDateTime timestamp) {
		super(value, timestamp);
		this.unit = unit;
	}

	public Unit getUnit() {
		return unit;
	}

	public enum Unit {
		lm("lm"),
		percent("%");

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
