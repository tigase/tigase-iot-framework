/*
 * DS1820.java
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

package tigase.iot.framework.rpi.sensors.w1;

import tigase.iot.framework.values.Temperature;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of a sensor reading temperature from DS1820 sensor.
 *
 * It should not be used directly as it requires W1 support. Due to that
 * you should use {@link tigase.iot.framework.rpi.sensors.w1.W1Master}
 * which will instantiate and register instance of this class.
 *
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

			return new Temperature(Temperature.Scale.CELSIUS, value);
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
