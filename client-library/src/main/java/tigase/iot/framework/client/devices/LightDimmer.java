/*
 * LightDimmer.java
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

import tigase.iot.framework.client.Devices;
import tigase.iot.framework.client.values.Light;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.utils.DateTimeFormat;

/**
 * Class implements representation of remote light dimmer device.
 *
 * Created by andrzej on 26.11.2016.
 */
public class LightDimmer extends LightSensor {

	public LightDimmer(Devices devices, JaxmppCore jaxmpp, JID pubsubJid, String node, String name) {
		super(devices, jaxmpp, pubsubJid, node, name);
	}

	@Override
	public void setValue(Light newValue, Callback<Light> callback) throws JaxmppException {
		super.setValue(newValue, callback);
	}

	@Override
	protected Element encodeToPayload(Light value) {
		try {
			Element timestampElem = ElementFactory.create("timestamp");
			timestampElem.setAttribute("value", new DateTimeFormat().format(value.getTimestamp()));

			Element numeric = ElementFactory.create("numeric");
			numeric.setAttribute("name", Light.NAME);
			numeric.setAttribute("automaticReadout", "false");
			numeric.setAttribute("value", String.valueOf(value.getValue()));
			numeric.setAttribute("unit", value.getUnit().toString());

			timestampElem.addChild(numeric);

			return timestampElem;
		} catch (XMLException ex) {
		}
		return null;
	}
}
