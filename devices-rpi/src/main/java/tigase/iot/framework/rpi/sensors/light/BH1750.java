/*
 * BH1750.java
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

package tigase.iot.framework.rpi.sensors.light;

import com.pi4j.io.i2c.I2CDevice;
import tigase.iot.framework.rpi.sensors.I2CAbstractPeriodDevice;
import tigase.iot.framework.values.Light;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.UnregisterAware;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of a sensor which reads data from the connection BH1750 sensor.
 *
 * Created by andrzej on 23.10.2016.
 */
public class BH1750
		extends I2CAbstractPeriodDevice<Light>
		implements Initializable, UnregisterAware {

	private static final Logger log = Logger.getLogger(BH1750.class.getCanonicalName());

	public BH1750() {
		super("light-sensor", "Light sensor", "BH1750", 60 * 1000);
	}

	@Override
	protected Light readValue(I2CDevice device) throws IOException {
		device.write((byte) 0x20);

		byte[] data = new byte[2];

		int r = device.read(data, 0, 2);
		if (r != 2) {
			throw new RuntimeException("Read error: read only " + r + " bytes");
		}

		int v1 = ((data[0] & 0xff) << 8) | (data[1] & 0xff);
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST,
					"read data: " + Integer.toHexString(data[0] & 0xff) + Integer.toHexString(data[1] & 0xff) +
							", value = " + v1);
		}

		return new Light(v1, Light.Unit.lm);
	}

}
