/*
 * Icons.java
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface Icons
		extends ClientBundle {

	static ImageResource getByCategory(String category, ImageResource defaultValue) {
		if (category == null) {
			return defaultValue;
		}
		switch (category) {
			case "lights-ceiling":
				return Icons.INSTANCE.lightsCeiling();
			case "lights-external":
				return Icons.INSTANCE.lightsExternal();
			case "lights-led":
				return Icons.INSTANCE.lightsLed();
			case "lights-spotlight":
				return Icons.INSTANCE.lightsSpotlight();
			case "lights-table":
				return Icons.INSTANCE.lightsTable();
			case "motor":
				return Icons.INSTANCE.engine();
			case "socket":
				return Icons.INSTANCE.socket();
			default:
				return defaultValue;
		}
	}
	
	Icons INSTANCE = GWT.create(Icons.class);

	@Source("icons/engine-64.png")
	ImageResource engine();

	@Source("icons/humidity-64.png")
	ImageResource humidity();

	@Source("icons/dimmer-64.png")
	ImageResource dimmer();

	@Source("icons/lightsensor-64.png")
	ImageResource lightSensor();

	@Source("icons/lightsceiling-64.png")
	ImageResource lightsCeiling();

	@Source("icons/lightsexternal-64.png")
	ImageResource lightsExternal();

	@Source("icons/led-64.png")
	ImageResource lightsLed();

	@Source("icons/lightsspotlight-64.png")
	ImageResource lightsSpotlight();

	@Source("icons/lightstable-64.png")
	ImageResource lightsTable();

	@Source("icons/pressuresensor-64.png")
	ImageResource pressureSensor();

	@Source("icons/proximity-64.png")
	ImageResource proximitySensor();

	@Source("icons/socket-64.png")
	ImageResource socket();

	@Source("icons/shutdown-64.png")
	ImageResource shutdown();

	@Source("icons/thermometer-64.png")
	ImageResource thermometer();

	@Source("icons/tv-64.png")
	ImageResource tv();

	@Source("icons/unknown-64.png")
	ImageResource unknown();

}
