/*
 * DHT22.java
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
package tigase.iot.framework.rpi.sensors.humidity;

import tigase.iot.framework.devices.AbstractPeriodSensor;
import tigase.iot.framework.devices.IConfigurationAware;
import tigase.iot.framework.devices.annotations.Fixed;
import tigase.iot.framework.values.Humidity;
import tigase.kernel.beans.config.ConfigField;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class DHT22 extends AbstractPeriodSensor<Humidity>
		implements IConfigurationAware {

	@Fixed
	@ConfigField(desc = "Information")
	private String info = "This device requires DHTXXD from http://abyz.me.uk/rpi/pigpio/code/DHTXXD.zip to be installed and working.";

	@ConfigField(desc = "Path to DHTXXD file")
	private String pathToDht22File = "/usr/local/bin/dht22";

	@ConfigField(desc = "BCM Pin number")
	private Integer pin = 19;

	public DHT22() {
		super("humidity-sensor", "Humidity sensor", "DHT22", 60 * 1000);
	}

	@Override
	protected Humidity readValue() {
		try {
			Process p = Runtime.getRuntime().exec(pathToDht22File + " -g " + pin);
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				String line = null;
				while ((line = reader.readLine()) != null) {
					String[] parts = line.split(" ");
					if (!"0".equals(parts[0])) {
						throw new RuntimeException("Read failed");
					}
					if (parts.length == 3) {
						return new Humidity(Double.parseDouble(parts[2]));
					} else if (parts.length == 5) {
						return new Humidity(Double.parseDouble(parts[3]));
					} else {
						throw new RuntimeException("Read failed");
					}
				}
				throw new RuntimeException("Read failed");
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}
