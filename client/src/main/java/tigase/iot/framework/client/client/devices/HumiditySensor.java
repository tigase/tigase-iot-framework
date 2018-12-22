/*
 * HumiditySensor.java
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.devices;

import tigase.iot.framework.client.client.ClientFactory;
import tigase.iot.framework.client.values.Humidity;

/**
 *
 * @author andrzej
 */
public class HumiditySensor extends DeviceRemoteConfigAware<Double, Humidity, tigase.iot.framework.client.devices.HumiditySensor> {
	
	public HumiditySensor(ClientFactory factory, tigase.iot.framework.client.devices.HumiditySensor sensor) {
		super(factory, "humidity-sensor", Icons.INSTANCE.humidity(), sensor);
	}

	@Override
	public void setValue(Double value) {
		if (value == null) {
			super.setValue("-- %");
		} else {
			value = ((double) (Math.round(value * 10))) / 10;
			super.setValue("" + value + " %");
		}
	}
	
}