/*
 * W1AbstractPeriodDevice.java
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

import tigase.iot.framework.devices.AbstractPeriodSensor;
import tigase.iot.framework.devices.IConfigurationAware;
import tigase.iot.framework.devices.IValue;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.UnregisterAware;

/**
 * Created by andrzej on 23.10.2016.
 */
public abstract class W1AbstractPeriodDevice<T extends IValue> extends AbstractPeriodSensor<T>
		implements W1Device<T>, Initializable, UnregisterAware, IConfigurationAware {

	protected com.pi4j.io.w1.W1Device w1Device;

	protected W1AbstractPeriodDevice(String type, long period) {
		super(type, period);
	}

	@Override
	public com.pi4j.io.w1.W1Device getW1Device() {
		return w1Device;
	}

	@Override
	public void initialize() {
		if (w1Device == null) {
			w1Device = W1Master.KNOWN_DEVICES.get(getName());
		}
		super.initialize();
	}
}
