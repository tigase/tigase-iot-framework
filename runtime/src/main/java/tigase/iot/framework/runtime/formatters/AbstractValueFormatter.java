/*
 * AbstractValueFormatter.java
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

import tigase.iot.framework.devices.IValue;
import tigase.iot.framework.runtime.ValueFormatter;
import tigase.iot.framework.runtime.utils.XmppDateTimeFormatterFactory;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.jaxmpp.core.client.xml.XMLException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Abstract implementation of {@link ValueFormatter} interface providing implementation for commonly used features.
 * Created by andrzej on 05.11.2016.
 */
public abstract class AbstractValueFormatter<T extends IValue>
		implements ValueFormatter<T> {

	protected final String name;
	private final DateTimeFormatter formatter = XmppDateTimeFormatterFactory.newInstance();
	private final Class<T> supportedClass;

	public AbstractValueFormatter(String name, Class<T> supportedClass) {
		this.name = name;
		this.supportedClass = supportedClass;
	}

	@Override
	public Class<T> getSupportedClass() {
		return supportedClass;
	}

	protected Element createTimestampElement(T value) throws XMLException {
		Element timestampElem = ElementFactory.create("timestamp");
		timestampElem.setAttribute("value", value.getTimestamp().atZone(ZoneId.systemDefault()).format(formatter));
		return timestampElem;
	}

	protected LocalDateTime parseTimestampElement(Element elem) throws XMLException {
		if (!"timestamp".equals(elem.getName()) || elem.getAttribute("value") == null) {
			return null;
		}

		return formatter.parse(elem.getAttribute("value")).query(ZonedDateTime::from).toLocalDateTime();
	}
}
