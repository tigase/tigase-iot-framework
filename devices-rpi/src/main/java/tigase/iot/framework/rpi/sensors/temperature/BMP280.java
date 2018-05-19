/*
 * BMP280.java
 *
 * Tigase IoT Framework
 * Copyright (C) 2011-2018 "Tigase, Inc." <office@tigase.com>
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
package tigase.iot.framework.rpi.sensors.temperature;

import com.pi4j.io.i2c.I2CDevice;
import tigase.iot.framework.rpi.sensors.I2CAbstractPeriodDevice;
import tigase.iot.framework.values.Temperature;

import java.io.IOException;

/**
 * Implementation of a sensor which reads temperature data from the connected BMP280 sensor.
 */
public class BMP280
		extends I2CAbstractPeriodDevice<Temperature> {

	public BMP280() {
		super("temperature-sensor", "Temperature sensor", "BMP280", 60 * 1000, "76");
	}

	@Override
	protected Temperature readValue(I2CDevice device) throws IOException {
		byte[] b1 = new byte[24];
		device.read(0x88, b1, 0, 24);

		double t1 = readTemperatureCoefficiency(b1, 0);
		double t2 = readTemperatureCoefficiency(b1, 1);
		double t3 = readTemperatureCoefficiency(b1, 2);

		device.write(0xF4, (byte) 0x27);
		device.write(0xF5, (byte) 0xA0);

		byte[] data = new byte[8];
		device.read(0xF7, data, 0, 8);

		long adc_t = (((long)(data[3] & 0xFF) * 65536) + ((long)(data[4] & 0xFF) * 256) + (long)(data[5] & 0xF0)) / 16;
		
		double var1 = (((double)adc_t) / 16384.0 - t1 / 1024.0) * t2;
		double var2 = ((((double)adc_t) / 131072.0 - t1 / 8192.0) *
				(((double)adc_t)/131072.0 - t1/8192.0)) * t3;
		double cTemp = (var1 + var2) / 5120.0;
		
		return new Temperature(Temperature.Scale.CELSIUS, cTemp);
	}

	private int readTemperatureCoefficiency(byte[] b1, int pos) {
		int x = (pos * 2);
		int c = (b1[x] & 0xFF) + ((b1[x+1] & 0xFF) * 256);
		if (pos > 0 && c > 32767) {
			c -= 65536;
		}
		return c;
	}
	
}
