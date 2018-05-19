/*
 * Pressure.java
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
 * Class holding value and unit for pressure measurement.
 */
public class Pressure extends Value<Double> {

	private final Scale scale;

	public Pressure(Scale scale, Double value) {
		super(value);
		this.scale = scale;
	}

	public Pressure(Scale scale, Double value, LocalDateTime timestamp) {
		super(value, timestamp);
		this.scale = scale;
	}

	public double getValue(Scale scale) {
		if (scale == this.scale) {
			return super.getValue();
		}
		return this.scale.convert(getValue(), scale);
	}

	@Override
	protected void toString(StringBuilder sb) {
		sb.append("scale: ").append(scale.getName()).append(", value: ").append(getValue());
	}

	public enum Scale {
		hPa("hPa", "hPa"),
		inHg("inHg", "inHg");

		private final String name;
		private final String units;

		Scale(String name, String units) {
			this.name = name;
			this.units = units;
		}

		public String getName() {
			return name;
		}

		public String getUnits() {
			return units;
		}

		public double convert(double value, Scale newScale) {
			switch (newScale) {
				case hPa:
					switch (this) {
						case hPa:
							return value;
						case inHg:
							return (value * 3386.389) / 100;
					}
					break;
				case inHg:
					switch (this) {
						case hPa:
							return (value * 100) / 3386.389;
						case inHg:
							return value;
					}
					break;
			}
			throw new RuntimeException("Should not happen");
		}
	}
}
