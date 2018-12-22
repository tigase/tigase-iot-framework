/*
 * LedMatrixDevice.java
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

package tigase.iot.framework.rpi.devices.ledmatrix;

import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;
import tigase.iot.framework.devices.AbstractSensor;
import tigase.iot.framework.devices.IConfigurationAware;
import tigase.iot.framework.devices.IExecutorDevice;
import tigase.iot.framework.values.LedMatrix;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LedMatrixDevice
		extends AbstractSensor<LedMatrix>
		implements IConfigurationAware, IExecutorDevice<LedMatrix> {

	// "decode mode" register
	public static final byte REG_DECODE = (byte) 0x09;
	// "intensity" register
	public static final byte REG_INTENSITY = (byte) 0x0a;
	// "scan limit" register
	public static final byte REG_SCAN_LIMIT = (byte) 0x0b;
	// "shutdown" register
	public static final byte REG_SHUTDOWN = (byte) 0x0c;
	// "display test" register
	public static final byte REG_DISPLAY_TEST = (byte) 0x0f;
	// minimum display intensity
	public static final byte INTENSITY_MIN = (byte) 0x00;
	// maximum display intensity
	public static final byte INTENSITY_MAX = (byte) 0x0f;

	private static final Logger log = Logger.getLogger(LedMatrixDevice.class.getCanonicalName());

	private SpiDevice spi;

	protected static byte[] getValues(byte position, byte[] buffer) {
		byte[] result = new byte[2 * 2];

		for (int deviceID = 0, j = 0; deviceID < 2; deviceID++) {
			result[j++] = (byte) (1 + position);
			result[j++] = buffer[(deviceID * 8) + position];
		}

		return result;
	}

	public LedMatrixDevice() {
		super("led-matrix", "LED Matrix", "LED Matrix");
	}

	private void driverProperty(byte property, byte value) throws IOException {
		spi.write(property, value, property, value);
	}

	@Override
	public void initialize() {
		super.initialize();
		try {
			log.config("Initializing SPI...");
			this.spi = SpiFactory.getInstance(SpiChannel.CS0, SpiDevice.DEFAULT_SPI_SPEED, SpiDevice.DEFAULT_SPI_MODE);

			LedMatrix lm = new LedMatrix();
			setValue(lm);

			driverProperty(REG_DECODE, (byte) 0x00);
			driverProperty(REG_INTENSITY, (byte) 0x08);
			driverProperty(REG_SCAN_LIMIT, (byte) 0x07);
			driverProperty(REG_SHUTDOWN, (byte) 0x01);
			driverProperty(REG_DISPLAY_TEST, (byte) 0x00);

		} catch (Exception e) {
			log.log(Level.SEVERE, "Cannot initialize", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setValue(LedMatrix value) {
		log.fine("Writing values to matrix");
		try {
			byte[] buffer = value.getValueAsArray();
			for (byte position = 0; position < 8; position++) {
				spi.write(getValues(position, buffer));
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Cannot update value", e);
			throw new RuntimeException(e);
		}
	}
}
