/*
 * LedMatrixDevice.java
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

package tigase.iot.framework.client.devices;

import tigase.iot.framework.client.Device;
import tigase.iot.framework.client.Devices;
import tigase.iot.framework.client.values.LedMatrix;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.utils.DateTimeFormat;

import java.util.Date;

public class LedMatrixDevice
		extends Device<LedMatrix> {

	public LedMatrixDevice(Devices devices, JaxmppCore jaxmpp, JID pubsubJid, String node, String name) {
		super(devices, jaxmpp, pubsubJid, node, name);
	}

	@Override
	protected Element encodeToPayload(LedMatrix value) {
		try {
			Element timestampElem = ElementFactory.create("timestamp");
			timestampElem.setAttribute("value", new DateTimeFormat().format(value.getTimestamp()));

			Element numeric = ElementFactory.create("matrix");
			numeric.setAttribute("name", LedMatrix.NAME);
			numeric.setAttribute("automaticReadout", "false");
			numeric.setAttribute("value", value.getValue());

			timestampElem.addChild(numeric);

			return timestampElem;
		} catch (XMLException ex) {
		}
		return null;
	}

	@Override
	protected LedMatrix parsePayload(Element elem) {
		try {
			Element numeric = elem.getFirstChild("matrix");
			if (numeric == null || !LedMatrix.NAME.equals(numeric.getAttribute("name"))) {
				return null;
			}

			String value = numeric.getAttribute("value");
			Date timestamp = parseTimestamp(elem);

			return new LedMatrix(value, timestamp);
		} catch (XMLException ex) {
			return null;
		}
	}

	@Override
	public void setValue(LedMatrix newValue, Callback<LedMatrix> callback) throws JaxmppException {
		super.setValue(newValue, callback);
	}
}
