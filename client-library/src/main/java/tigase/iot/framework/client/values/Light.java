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

package tigase.iot.framework.client.values;

import tigase.iot.framework.client.Device;

import java.util.Date;

/**
 * Class represents light measurements value and unit.
 * 
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
