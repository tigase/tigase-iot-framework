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
package tigase.iot.framework.rpi.sensors.pressure;

import com.pi4j.io.i2c.I2CDevice;
import tigase.iot.framework.rpi.sensors.I2CAbstractPeriodDevice;
import tigase.iot.framework.values.Pressure;

import java.io.IOException;

/**
 * Implementation of a sensor which reads pressure data from the connected BMP280 sensor.
 */
public class BMP280 extends I2CAbstractPeriodDevice<Pressure> {

	public BMP280() {
		super("pressure-sensor", "Pressure sensor", "BMP280", 60 * 1000, "76");
	}

	@Override
	protected Pressure readValue(I2CDevice device) throws IOException {
		byte[] b1 = new byte[24];
		device.read(0x88, b1, 0, 24);

		double t1 = readTemperatureCoefficiency(b1, 0);
		double t2 = readTemperatureCoefficiency(b1, 1);
		double t3 = readTemperatureCoefficiency(b1, 2);

		double p1 = readPressureCoefficiency(b1, 0);
		double p2 = readPressureCoefficiency(b1, 1);
		double p3 = readPressureCoefficiency(b1, 2);
		double p4 = readPressureCoefficiency(b1, 3);
		double p5 = readPressureCoefficiency(b1, 4);
		double p6 = readPressureCoefficiency(b1, 5);
		double p7 = readPressureCoefficiency(b1, 6);
		double p8 = readPressureCoefficiency(b1, 7);
		double p9 = readPressureCoefficiency(b1, 8);

		device.write(0xF4, (byte) 0x27);
		device.write(0xF5, (byte) 0xA0);

		byte[] data = new byte[8];
		device.read(0xF7, data, 0, 8);

		long adc_p = (((long)(data[0] & 0xFF) * 65536) + ((long)(data[1] & 0xFF) * 256) + (long)(data[2] & 0xF0)) / 16;
		long adc_t = (((long)(data[3] & 0xFF) * 65536) + ((long)(data[4] & 0xFF) * 256) + (long)(data[5] & 0xF0)) / 16;
		
		double var1 = (((double)adc_t) / 16384.0 - t1 / 1024.0) * t2;
		double var2 = ((((double)adc_t) / 131072.0 - t1 / 8192.0) *
				(((double)adc_t)/131072.0 - t1/8192.0)) * t3;
		double t_fine = (long)(var1 + var2);

		var1 = (t_fine / 2.0) - 64000.0;
		var2 = var1 * var1 * p6 / 32768.0;
		var2 = var2 + var1 * p5 * 2.0;
		var2 = (var2 / 4.0) + (p4 * 65536.0);
		var1 = (p3 * var1 * var1 / 524288.0 + p2 * var1) / 524288.0;
		var1 = (1.0 + var1 / 32768.0) * p1;
		double p = 1048576.0 - (double)adc_p;
		p = (p - (var2 / 4096.0)) * 6250.0 / var1;
		var1 = p9 * p * p / 2147483648.0;
		var2 = p * p8 / 32768.0;
		double pressure = (p + (var1 + var2 + p7) / 16.0) / 100;

		return new Pressure(Pressure.Scale.hPa, pressure);
	}

	private int readTemperatureCoefficiency(byte[] b1, int pos) {
		int x = (pos * 2);
		int c = (b1[x] & 0xFF) + ((b1[x+1] & 0xFF) * 256);
		if (pos > 0 && c > 32767) {
			c -= 65536;
		}
		return c;
	}

	private int readPressureCoefficiency(byte[] b1, int pos) {
		int x = 6 + (pos * 2);
		int c = (b1[x] & 0xFF) + ((b1[x+1] & 0xFF) * 256);
		if (pos > 0 && c > 32767) {
			c -= 65536;
		}
		return c;
	}
}
