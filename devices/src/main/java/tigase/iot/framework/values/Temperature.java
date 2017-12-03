/*
 * Temperature.java
 *
 * Tigase IoT Framework
 * Copyright (C) 2011-2017 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */

package tigase.iot.framework.values;

import tigase.iot.framework.devices.Value;

import java.time.LocalDateTime;

/**
 * Class holding value and unit for temperature measurement.
 *
 * Created by andrzej on 24.10.2016.
 */
public class Temperature
		extends Value<Double> {

	private final Scale scale;

	public Temperature(Scale scale, double value) {
		super(value);
		this.scale = scale;
	}

	public Temperature(Scale scale, double value, LocalDateTime timestamp) {
		super(value, timestamp);
		this.scale = scale;
	}

	public double getValue(Scale scale) {
		if (scale == this.scale) {
			return super.getValue();
		}

		return this.scale.convert(super.getValue(), scale);
	}

	@Override
	protected void toString(StringBuilder sb) {
		sb.append("scale: ").append(scale.getName()).append(", value: ").append(getValue());
	}

	/**
	 * Implementation of temperature scales and conversions.
	 */
	public enum Scale {

		CELSIUS("Celsius", "°C", -273.15),
		FARENHEIT("Farenheit", "°F", -459.67),
		KELVIN("Kelvin", "K", 0),
		RANKINE("Rankine", "°R", 0);

		private final String name;
		private final String units;
		private final double absZero;

		Scale(String name, String units, double absoluteZero) {
			this.name = name;
			this.units = units;
			this.absZero = absoluteZero;
		}

		public String getName() {
			return name;
		}

		public String getUnits() {
			return units;
		}

		public double convert(double value, Scale newScale) {
			switch (newScale) {
				case CELSIUS:
					switch (this) {
						case CELSIUS:
							return value;
						case FARENHEIT:
							return ((value - 32.0) * 5) / 9;
						case KELVIN:
							return value + CELSIUS.absZero;
						case RANKINE:
							return ((value + (CELSIUS.absZero - 32)) * 5) / 9;
					}
				case FARENHEIT:
					switch (this) {
						case CELSIUS:
							return ((value * 9) / 5) + 32;
						case FARENHEIT:
							return value;
						case KELVIN:
							return ((value * 9) / 5) + FARENHEIT.absZero;
						case RANKINE:
							return value + FARENHEIT.absZero;
					}
				case KELVIN:
					switch (this) {
						case CELSIUS:
							return value - CELSIUS.absZero;
						case FARENHEIT:
							return ((value - FARENHEIT.absZero) * 5) / 9;
						case KELVIN:
							return value;
						case RANKINE:
							return (value * 5) / 9;
					}
				case RANKINE:
					switch (this) {
						case CELSIUS:
							return ((value - CELSIUS.absZero) * 9) / 5;
						case FARENHEIT:
							return value - FARENHEIT.absZero;
						case KELVIN:
							return (value * 9) / 5;
						case RANKINE:
							return value;
					}
			}
			throw new RuntimeException("Should not happen");
		}

	}
}
