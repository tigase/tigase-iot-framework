/*
 * MovementSensor.java
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
import tigase.iot.framework.client.values.Movement;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;

import java.util.Date;

/**
 * Class implements representation of remote movement detector.
 *
 * Created by andrzej on 27.11.2016.
 */
public class MovementSensor extends Device<Movement> {

	public MovementSensor(JaxmppCore jaxmpp, JID pubsubJid, String node, String name) {
		super(jaxmpp, pubsubJid, node, name);
	}

	@Override
	protected Element encodeToPayload(Movement value) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected Movement parsePayload(Element elem) {
		try {
			Element numeric = elem.getFirstChild("boolean");
			if (numeric == null || !Movement.NAME.equals(numeric.getAttribute("name"))) {
				return null;
			}

			Boolean value = Boolean.parseBoolean(numeric.getAttribute("value"));
			Date timestamp = parseTimestamp(elem);

			return new Movement(value, timestamp);
		} catch (XMLException ex) {
			return null;
		}
	}
}
