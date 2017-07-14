/*
 * I2CAbstractPeriodDevice.java
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

package tigase.iot.framework.rpi.sensors;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import tigase.iot.framework.devices.AbstractPeriodSensor;
import tigase.iot.framework.devices.IConfigurationAware;
import tigase.iot.framework.devices.IValue;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract class implementing support for reading data periodically from a sensor connected using I2C.
 *
 * Created by andrzej on 23.10.2016.
 */
public abstract class I2CAbstractPeriodDevice<T extends IValue> extends AbstractPeriodSensor<T>
		implements Initializable, UnregisterAware, IConfigurationAware {

	private static final Logger log = Logger.getLogger(I2CAbstractPeriodDevice.class.getCanonicalName());

	@ConfigField(desc = "Device address on I2C bus")
	private int address;

	@ConfigField(desc = "I2C Bus number")
	private int bus = I2CBus.BUS_1;

	private I2CBus i2cBus;

	public I2CAbstractPeriodDevice(String type, long period) {
		super(type, period);
	}

	/**
	 * Method responsible for actual reading data from I2C device.
	 *
	 * @param device
	 * @return
	 * @throws IOException
	 */
	protected abstract T readValue(I2CDevice device) throws IOException;

	@Override
	protected T readValue() {
		try {
			I2CDevice device = i2cBus.getDevice(address);
			return readValue(device);
		} catch (IOException ex) {
			log.log(Level.WARNING, "Could not read data from I2C device at " + address, ex);
			return null;
		}
	}

	@Override
	public void initialize() {
		try {
			i2cBus = I2CFactory.getInstance(bus);
		} catch (I2CFactory.UnsupportedBusNumberException|IOException ex) {
			log.log(Level.WARNING, "Failed to retrieve I2C bus with number " + bus, ex);
			throw new RuntimeException("Failed to retrieve I2C bus with number " + bus, ex);
		}

		super.initialize();
	}

}
