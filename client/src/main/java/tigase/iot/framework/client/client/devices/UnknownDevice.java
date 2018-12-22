/*
 * UnknownDevice.java
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

package tigase.iot.framework.client.client.devices;

import tigase.iot.framework.client.Device;
import tigase.iot.framework.client.client.ClientFactory;

public class UnknownDevice extends DeviceRemoteConfigAware<Object, tigase.iot.framework.client.Device.IValue<Object>, tigase.iot.framework.client.Device<Device.IValue<Object>>> {

	public UnknownDevice(ClientFactory factory, Device<Device.IValue<Object>> sensor) {
		super(factory, "unknown", Icons.INSTANCE.unknown(), sensor);
	}

	@Override
	protected void setValue(Object value) {

	}
}
