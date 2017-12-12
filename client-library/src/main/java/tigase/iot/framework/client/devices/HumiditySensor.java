/*
 * HumiditySensor.java
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
package tigase.iot.framework.client.devices;

import tigase.iot.framework.client.Device;
import tigase.iot.framework.client.Devices;
import tigase.iot.framework.client.values.Humidity;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;

import java.util.Date;

public class HumiditySensor extends Device<Humidity> {

	public HumiditySensor(Devices devices, JaxmppCore jaxmpp, JID pubsubJid, String node, String name) {
		super(devices, jaxmpp, pubsubJid, node, name);
	}

	@Override
	protected Element encodeToPayload(Humidity value) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected Humidity parsePayload(Element elem) {
		try {
			Element numeric = elem.getFirstChild("numeric");
			if (numeric == null || !Humidity.NAME.equals(numeric.getAttribute("name"))) {
				return null;
			}

			Double value = Double.parseDouble(numeric.getAttribute("value"));
			Date timestamp = parseTimestamp(elem);

			return new Humidity(value, timestamp);
		} catch (XMLException ex) {
			return null;
		}
	}
}
