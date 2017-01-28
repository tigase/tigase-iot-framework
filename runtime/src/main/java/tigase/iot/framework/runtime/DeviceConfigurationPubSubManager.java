/*
 * DeviceConfigurationPubSubManager.java
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

package tigase.iot.framework.runtime;

import tigase.iot.framework.devices.IConfigurationAware;
import tigase.iot.framework.devices.IDevice;
import tigase.iot.framework.runtime.pubsub.AbstractConfigurationPubSubManager;
import tigase.iot.framework.runtime.pubsub.PubSubNodesManager;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.forms.JabberDataElement;
import tigase.jaxmpp.core.client.xmpp.forms.XDataType;
import tigase.kernel.beans.Inject;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by andrzej on 04.11.2016.
 */
public class DeviceConfigurationPubSubManager
		extends AbstractConfigurationPubSubManager<IConfigurationAware> {

	private static final Logger log = Logger.getLogger(DeviceConfigurationPubSubManager.class.getCanonicalName());

	@Inject
	private PubSubNodesManager pubSubNodesManager;

	public DeviceConfigurationPubSubManager() {
		super();
		rootNode = "devices";
	}

	@Override
	public void setConfigurationAware(List<IConfigurationAware> configurationAware) {
		super.setConfigurationAware(
				configurationAware.stream().filter(aware -> aware instanceof IDevice).collect(Collectors.toList()));
	}

	protected JabberDataElement prepareRootNodeConfig() throws JaxmppException {
		JabberDataElement config = new JabberDataElement(XDataType.submit);
		config.addTextSingleField("pubsub#title", "Devices");
		config.addTextSingleField("pubsub#node_type", "collection");
		config.addTextSingleField("pubsub#access_model", pubSubNodesManager.isPEP() ? "presence" : "open");
		config.addTextSingleField("pubsub#presence_based_delivery", "true");
		config.addTextSingleField("pubsub#persist_items", "1");

		return config;
	}

	protected JabberDataElement prepareNodeConfig(IConfigurationAware configurationAware) throws JaxmppException {
		JabberDataElement config = new JabberDataElement(XDataType.submit);
		config.addTextSingleField("pubsub#title", configurationAware.getName());
		config.addTextSingleField("pubsub#node_type", "collection");
		config.addTextSingleField("pubsub#access_model", pubSubNodesManager.isPEP() ? "presence" : "open");
		config.addTextSingleField("pubsub#presence_based_delivery", "true");
		config.addTextSingleField("pubsub#persist_items", "1");
		config.addTextSingleField("pubsub#collection", rootNode);

		return config;
	}

	protected JabberDataElement prepareConfigNodeConfig(String collection) throws JaxmppException {
		JabberDataElement config = new JabberDataElement(XDataType.submit);
		config.addTextSingleField("pubsub#title", "Configuration");
		config.addTextSingleField("pubsub#node_type", "leaf");
		config.addTextSingleField("pubsub#access_model", pubSubNodesManager.isPEP() ? "presence" : "open");
		config.addTextSingleField("pubsub#presence_based_delivery", "true");
		config.addTextSingleField("pubsub#persist_items", "1");
		config.addTextSingleField("pubsub#collection", collection);

		return config;
	}
}
