/*
 * TemperatureSensor.java
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
import tigase.iot.framework.client.values.Temperature;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;

import java.util.Date;

/**
 * Class implements representation of remote thermometer.
 * 
 * Created by andrzej on 26.11.2016.
 */
public class TemperatureSensor extends Device<Temperature> {

	public TemperatureSensor(JaxmppCore jaxmpp, JID pubsubJid, String node, String name) {
		super(jaxmpp, pubsubJid, node, name);
	}

	@Override
	protected Element encodeToPayload(Temperature value) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected Temperature parsePayload(Element elem) {
		try {
			Element numeric = elem.getFirstChild("numeric");
			if (numeric == null || !Temperature.NAME.equals(numeric.getAttribute("name"))) {
				return null;
			}

			Double value = Double.parseDouble(numeric.getAttribute("value"));
			String unit = numeric.getAttribute("unit");
			Date timestamp = parseTimestamp(elem);

			return new Temperature(value, unit, timestamp);
		} catch (XMLException ex) {
			return null;
		}
	}
}
