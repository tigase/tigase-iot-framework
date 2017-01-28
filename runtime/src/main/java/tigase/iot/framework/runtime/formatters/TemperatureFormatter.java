/*
 * TemperatureFormatter.java
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

package tigase.iot.framework.runtime.formatters;

import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.kernel.beans.Bean;
import tigase.kernel.core.Kernel;
import tigase.iot.framework.runtime.values.Temperature;

import java.time.LocalDateTime;

/**
 * Created by andrzej on 30.10.2016.
 */
@Bean(name = "temperatureFormatter", parent = Kernel.class, exportable = true)
public class TemperatureFormatter
		extends AbstractValueFormatter<Temperature> {


	public TemperatureFormatter() {
		super("Temperature", Temperature.class);
	}

	@Override
	public Element toElement(Temperature value) throws JaxmppException {
		Element timestampElem = createTimestampElement(value);

		Element numeric = ElementFactory.create("numeric");
		numeric.setAttribute("name", name);
		numeric.setAttribute("momentary", "true");
		numeric.setAttribute("automaticReadout", "true");
		numeric.setAttribute("value", String.valueOf(value.getValue(Temperature.Scale.CELSIUS)));
		numeric.setAttribute("unit", "°C");

		timestampElem.addChild(numeric);

		return timestampElem;
	}

	@Override
	public Temperature fromElement(Element elem) throws JaxmppException, IllegalArgumentException {
		Element numeric = elem.getFirstChild("numeric");
		if (numeric == null || !name.equals(numeric.getAttribute("name"))) {
			return null;
		}

		LocalDateTime timestamp = parseTimestampElement(elem);

		Double value  = Double.parseDouble(numeric.getAttribute("value"));
		for (Temperature.Scale scale : Temperature.Scale.values()) {
			if (scale.getUnits().equals(numeric.getAttribute("unit"))) {
				return new Temperature(scale, value, timestamp);
			}
		}

		return null;
	}

}
