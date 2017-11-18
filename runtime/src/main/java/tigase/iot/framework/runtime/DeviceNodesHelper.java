/*
 * DeviceNodesHelper.java
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

import tigase.iot.framework.devices.IDevice;
import tigase.iot.framework.runtime.pubsub.PubSubNodesManager;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.forms.JabberDataElement;
import tigase.jaxmpp.core.client.xmpp.forms.XDataType;

/**
 * Helper class for PubSub nodes creation for devices.
 * Created by andrzej on 04.11.2016.
 */
public class DeviceNodesHelper {

	public static PubSubNodesManager.Node createDevicesRootNode(String node, String name, boolean pep) throws JaxmppException {
		JabberDataElement config = new JabberDataElement(XDataType.submit);
		config.addTextSingleField("pubsub#title", name);
		config.addTextSingleField("pubsub#node_type", "collection");
		config.addTextSingleField("pubsub#access_model", pep ? "presence" : "open");
		config.addTextSingleField("pubsub#presence_based_delivery", "true");
		config.addTextSingleField("pubsub#persist_items", "1");

		return new PubSubNodesManager.Node(node, config);
	}

	public static PubSubNodesManager.Node createDeviceNode(String rootNode, IDevice device, boolean pep) throws JaxmppException {
		JabberDataElement config = new JabberDataElement(XDataType.submit);
		config.addTextSingleField("pubsub#title", device.getLabel());
		config.addTextSingleField("pubsub#node_type", "collection");
		config.addTextSingleField("pubsub#access_model", pep ? "presence" : "open");
		config.addTextSingleField("pubsub#presence_based_delivery", "true");
		config.addTextSingleField("pubsub#persist_items", "1");
		config.addTextSingleField("pubsub#notify_config", "1");
		config.addTextSingleField("pubsub#collection", rootNode);
		String node = getDeviceNodeName(rootNode, device);
		return new PubSubNodesManager.Node(node, config);
	}

	public static PubSubNodesManager.Node createDeviceStateNode(String rootNode, IDevice device, boolean pep) throws JaxmppException {
		String deviceNode = getDeviceNodeName(rootNode, device);
		JabberDataElement config = new JabberDataElement(XDataType.submit);
		config.addTextSingleField("pubsub#title", "State");
		config.addTextSingleField("pubsub#node_type", "leaf");
		config.addTextSingleField("pubsub#access_model", pep ? "presence" : "open");
		config.addTextSingleField("pubsub#presence_based_delivery", "true");
		config.addTextSingleField("pubsub#persist_items", "1");
		config.addTextSingleField("pubsub#collection", deviceNode);
		config.addTextSingleField("pubsub#send_last_published_item", "on_sub_and_presence");
		String node = getDeviceStateNodeName(rootNode, device);
		return new PubSubNodesManager.Node(node, config);
	}

	/**
	 * Get PubSub node name for device
	 * @param rootNode
	 * @param device
	 * @return
	 */
	public static String getDeviceNodeName(String rootNode, IDevice device) {
		return rootNode + "/" + device.getName();
	}

	/**
	 * Get PubSub node name for device configuration
	 * @param rootNode
	 * @param device
	 * @return
	 */
	public static String getDeviceConfigNodeName(String rootNode, IDevice device) {
		return getDeviceNodeName(rootNode, device) + "/config";
	}

	/**
	 * Get PubSub node name for device state
	 * @param rootNode
	 * @param device
	 * @return
	 */
	public static String getDeviceStateNodeName(String rootNode, IDevice device) {
		return getDeviceNodeName(rootNode, device) + "/state";
	}

	/**
	 * Parse PubSub node name and retrieve device name from it
	 * @param node
	 * @return
	 */
	public static String getDeviceIdFromNode(String node) {
		String[] parts = node.split("/");
		if (parts.length > 1) {
			return parts[1];
		}
		return null;
	}
}
