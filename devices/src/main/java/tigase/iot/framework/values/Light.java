/*
 * Light.java
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
 * Class holding value and unit for light measurements.
 *
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

	@Override
	protected void toString(StringBuilder sb) {
		sb.append("unit: ").append(unit);
		super.toString(sb);
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
