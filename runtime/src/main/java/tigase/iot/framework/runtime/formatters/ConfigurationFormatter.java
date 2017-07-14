/*
 * ConfigurationFormatter.java
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
import tigase.jaxmpp.core.client.xmpp.forms.JabberDataElement;
import tigase.kernel.beans.Bean;
import tigase.kernel.core.Kernel;
import tigase.iot.framework.runtime.pubsub.AbstractConfigurationPubSubManager;

import java.time.LocalDateTime;

/**
 * Formatter which formats {@link tigase.iot.framework.runtime.pubsub.AbstractConfigurationPubSubManager.ConfigValue}
 * Created by andrzej on 05.11.2016.
 */
@Bean(name = "configurationFormatter", parent = Kernel.class, active = true, exportable = true)
public class ConfigurationFormatter
		extends AbstractValueFormatter<AbstractConfigurationPubSubManager.ConfigValue> {

	public ConfigurationFormatter() {
		super("configuration", AbstractConfigurationPubSubManager.ConfigValue.class);
	}

	@Override
	public Element toElement(AbstractConfigurationPubSubManager.ConfigValue value) throws JaxmppException {
		Element timestampElem = createTimestampElement(value);
		timestampElem.addChild(value.getValue());
		return timestampElem;
	}

	@Override
	public AbstractConfigurationPubSubManager.ConfigValue fromElement(Element elem) throws JaxmppException {
		try {
			LocalDateTime timestamp = parseTimestampElement(elem);
			Element x = elem.getFirstChild("x");
			if (x == null) {
				return null;
			}

			JabberDataElement value = new JabberDataElement(x);
			return new AbstractConfigurationPubSubManager.ConfigValue(value, timestamp);
		} catch (JaxmppException ex) {
			return null;
		}
	}
}
