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

package tigase.iot.framework.client.values;

import tigase.iot.framework.client.Device;

import java.util.Date;

/**
 * Class represents pressure value and its unit.
 *
 * Created by andrzej on 26.11.2016.
 */
public class Pressure
		extends Device.Value<Double> {

	public static final String NAME = "Pressure";

	private final String unit;

	public Pressure(Double value, String unit, Date timestamp) {
		super(value, timestamp);
		this.unit = unit;
	}

	public String getUnit() {
		return unit;
	}

}
