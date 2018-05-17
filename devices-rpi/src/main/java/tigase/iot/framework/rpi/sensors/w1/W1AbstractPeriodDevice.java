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
import tigase.iot.framework.devices.annotations.ValuesProvider;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;

/**
 * Created by andrzej on 23.10.2016.
 */
public abstract class W1AbstractPeriodDevice<T extends IValue> extends AbstractPeriodSensor<T>
		implements W1Device<T>, Initializable, UnregisterAware, IConfigurationAware {

	@Inject
	private W1Master w1Master;
	
	protected com.pi4j.io.w1.W1Device w1Device;

	@ConfigField(desc = "1-Wire device id")
	@ValuesProvider(beanName = "w1Master")
	private String deviceId;

	protected W1AbstractPeriodDevice(String type, String name, String label, long period) {
		super(type, name, label, period);
	}

	@Override
	public com.pi4j.io.w1.W1Device getW1Device() {
		return w1Device;
	}

	public void setDeviceId(String deviceId) {
		if (this.deviceId == deviceId) {
			return;
		}
		if (this.deviceId != null && w1Master != null) {
			w1Master.releaseDevice(this.deviceId, this);
		}
		this.deviceId = deviceId;
		if (this.deviceId != null && w1Master != null) {
			w1Master.acquireDevice(this.deviceId, this, this::setW1Device);
		}
	}

	@Override
	public void initialize() {
		if (w1Device == null && deviceId != null) {
			w1Master.acquireDevice(this.deviceId, this, this::setW1Device);
		}
		super.initialize();
	}

	protected void setW1Device(com.pi4j.io.w1.W1Device device) {
		this.w1Device = device;
	}

	@Override
	public void beforeUnregister() {
		super.beforeUnregister();
		if (deviceId != null) {
			w1Master.releaseDevice(deviceId, this);
		}
	}
}
