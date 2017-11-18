/*
 * AndroidTv.java
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

package tigase.iot.framework.sensors.pir;

import tigase.iot.framework.devices.AbstractPeriodSensor;
import tigase.iot.framework.devices.IConfigurationAware;
import tigase.iot.framework.values.Movement;
import tigase.kernel.beans.config.ConfigField;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.NoRouteToHostException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sensor for Android-based TV devices.
 *
 * Created by andrzej on 02.12.2016.
 */
public class AndroidTv
		extends AbstractPeriodSensor<Movement>
		implements IConfigurationAware {

	private static final Logger log = Logger.getLogger(AndroidTv.class.getCanonicalName());

	@ConfigField(desc = "HTTP URL for TV Web API")
	protected String address;

	public AndroidTv() {
		super("tv-sensor", "Android TV Sensor", "Android TV Sensor", 60 * 1000);
	}

	@Override
	protected Movement readValue() {
		try {
			URL url = new URL(address);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json");

			con.setDoOutput(true);
			DataOutputStream writer = new DataOutputStream(con.getOutputStream());
			writer.writeBytes("{\"id\":2,\"method\":\"getPowerStatus\",\"version\":\"1.0\",\"params\":[]}");
			writer.flush();
			writer.close();

			int responseCode = con.getResponseCode();
			if (responseCode != 200) {
				return null;
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
			reader.close();

			boolean enabled = sb.toString().contains("{\"status\":\"active\"}");
			return new Movement(enabled);
		} catch (NoRouteToHostException ex) {
			log.log(Level.WARNING, getName() + ", could connect to " + address + " - assuming TV is OFF", ex);
			return new Movement(false);
		} catch (Exception ex) {
			log.log(Level.WARNING, getName() + ", could not read state from " + address, ex);
			return null;
		}
	}
}
