/*
 * MovementFormatter.java
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
import tigase.iot.framework.values.Movement;

import java.time.LocalDateTime;

/**
 * Fomatter for {@link Movement}
 * Created by andrzej on 07.11.2016.
 */
@Bean(name = "movementFormatter", parent = Kernel.class, active = true, exportable = true)
public class MovementFormatter extends AbstractValueFormatter<Movement> {

	public MovementFormatter() {
		super("Movement", Movement.class);
	}

	@Override
	public Element toElement(Movement value) throws JaxmppException {
		Element timestampElem = createTimestampElement(value);

		Element valueElem = ElementFactory.create("boolean");
		valueElem.setAttribute("name", name);
		valueElem.setAttribute("automaticReadout", "true");
		valueElem.setAttribute("value", String.valueOf(value.getValue()));

		timestampElem.addChild(valueElem);

		return timestampElem;
	}

	@Override
	public Movement fromElement(Element elem) throws JaxmppException {
		Element valueElem = elem.getFirstChild("boolean");
		if (valueElem == null || !name.equals(valueElem.getAttribute("name"))) {
			return null;
		}

		LocalDateTime timestamp = parseTimestampElement(elem);
		Boolean value = Boolean.parseBoolean(valueElem.getAttribute("value"));

		return new Movement(value, timestamp);
	}
}
