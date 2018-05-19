/*
 * PressureFormatter.java
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

package tigase.iot.framework.runtime.formatters;

import tigase.iot.framework.values.Pressure;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.kernel.beans.Bean;
import tigase.kernel.core.Kernel;

import java.time.LocalDateTime;

/**
 * Formatter for {@link Pressure}
 * Created by andrzej on 30.10.2016.
 */
@Bean(name = "pressureFormatter", parent = Kernel.class, active = true, exportable = true)
public class PressureFormatter
		extends AbstractValueFormatter<Pressure> {


	public PressureFormatter() {
		super("Pressure", Pressure.class);
	}

	@Override
	public Element toElement(Pressure value) throws JaxmppException {
		Element timestampElem = createTimestampElement(value);

		Element numeric = ElementFactory.create("numeric");
		numeric.setAttribute("name", name);
		numeric.setAttribute("momentary", "true");
		numeric.setAttribute("automaticReadout", "true");
		numeric.setAttribute("value", String.valueOf(value.getValue(Pressure.Scale.hPa)));
		numeric.setAttribute("unit", "hPa");

		timestampElem.addChild(numeric);

		return timestampElem;
	}

	@Override
	public Pressure fromElement(Element elem) throws JaxmppException, IllegalArgumentException {
		Element numeric = elem.getFirstChild("numeric");
		if (numeric == null || !name.equals(numeric.getAttribute("name"))) {
			return null;
		}

		LocalDateTime timestamp = parseTimestampElement(elem);

		Double value  = Double.parseDouble(numeric.getAttribute("value"));
		for (Pressure.Scale scale : Pressure.Scale.values()) {
			if (scale.getUnits().equals(numeric.getAttribute("unit"))) {
				return new Pressure(scale, value, timestamp);
			}
		}

		return null;
	}

}
