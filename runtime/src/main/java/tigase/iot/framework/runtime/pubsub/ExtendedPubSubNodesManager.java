/*
 * ExtendedPubSubNodesManager.java
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

package tigase.iot.framework.runtime.pubsub;

import tigase.eventbus.HandleEvent;
import tigase.iot.framework.devices.IDevice;
import tigase.iot.framework.devices.IExecutorDevice;
import tigase.iot.framework.devices.IValue;
import tigase.iot.framework.runtime.DeviceManager;
import tigase.iot.framework.runtime.DeviceNodesHelper;
import tigase.iot.framework.runtime.ValueFormatter;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.forms.JabberDataElement;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubErrorCondition;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.j2se.Jaxmpp;
import tigase.kernel.beans.Inject;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Extended version of {@link PubSubNodesManager} with added support for publication of values for devices
 * with support for converting values to elements using configured formatters and added support for
 * firing events when PubSub notification for one of observed node is received.
 *
 * Created by andrzej on 05.11.2016.
 */
public class ExtendedPubSubNodesManager
		extends PubSubNodesManager {

	private static final Logger log = Logger.getLogger(ExtendedPubSubNodesManager.class.getCanonicalName());

	@Inject(nullAllowed = true)
	private DeviceManager deviceManager;

	@Inject(nullAllowed = true)
	private List<IExecutorDevice<IValue>> executorDevices;

	@Inject
	private List<ValueFormatter> formatters;

	public ExtendedPubSubNodesManager() {
		this.rootNode = "devices";
	}

	@Override
	protected void jaxmppConnected(Jaxmpp jaxmpp) {
		super.jaxmppConnected(jaxmpp);
		Set<String> deviceIds = this.observedNodes.stream().map(DeviceNodesHelper::getDeviceIdFromNode).filter(deviceId -> deviceId != null).collect(
				Collectors.toSet());
		deviceIds.forEach(deviceId -> {
			try {
				updateNodeLabelFromNodeTitle(jaxmpp, deviceId);
			} catch (JaxmppException e) {
				log.log(Level.FINEST, "failed to update device label from node title", e);
			}
		});
	}

	/**
	 * Method converts value to PubSub item payload and publishes it to provided PubSub node
	 * with provided item id set.
	 * @param node
	 * @param itemId
	 * @param value
	 */
	public void publish(String node, String itemId, IValue value) {
		formatters.stream().filter(formatter -> formatter.isSupported(value)).forEach(formatter -> {
			try {
				Element result = formatter.toElement(value);
				if (result == null) {
					log.log(Level.FINE, "formatter {0} failed to return result for value {1}",
							new Object[]{formatter, value});
					return;
				}

				publishItem(node, itemId, result);
			} catch (JaxmppException ex) {
				log.log(Level.WARNING, "formatter " + formatter + " thrown exception while formatting value " + value,
						ex);
			}
		});
	}

	/**
	 * Method called when PubSub notification for one of observed node is received.
	 *
	 * If notification has payload and it can be convert to implementation of {@link IValue}
	 * interface then {@link ValueChangedEvent} is fired for this new value.
	 * @param event
	 */
	@HandleEvent
	public void receivedItem(PubSubModule.NotificationReceivedHandler.NotificationReceivedEvent event) {
		if (log.isLoggable(Level.FINEST)) {
			try {
				log.log(Level.FINEST, "received PubSub event from {0} with payload {1}",
						new Object[]{event.getNodeName(), event.getPayload().getAsString()});
			} catch (XMLException ex) {}
		}
		formatters.stream().map(formatter -> {
			try {
				return formatter.fromElement(event.getPayload());
			} catch (JaxmppException ex) {
				log.log(Level.WARNING,
						"formatter " + formatter + " thrown exception while parsing payload " + event.getPayload(), ex);
			}
			return null;
		}).filter(value -> value != null).forEach(value -> {
			eventBus.fire(new ValueChangedEvent(event.getNodeName(), value));
			if (executorDevices != null) {
				executorDevices.stream()
						.filter(executor -> event.getNodeName()
								.equals(DeviceNodesHelper.getDeviceStateNodeName(this.rootNode, executor)))
						.forEach(executor -> executor.setValue(value));
			}
		});
	}

	/**
	 * Returns stream of observed PubSub nodes
	 * @param o
	 * @return
	 */
	@Override
	protected Stream<String> getObservedNodes(NodesObserver o) {
		if (o instanceof IExecutorDevice) {
			return Stream.concat(Stream.of(DeviceNodesHelper.getDeviceStateNodeName(rootNode, (IDevice) o)),
								 super.getObservedNodes(o));
		} else {
			return super.getObservedNodes(o);
		}
	}

	@Override
	protected Stream<String> getObservedNodes() {
		if (executorDevices == null) {
			return super.getObservedNodes();
		}
		return Stream.concat(super.getObservedNodes(), executorDevices.stream()
				.map(device -> DeviceNodesHelper.getDeviceStateNodeName(rootNode, device)));
	}

	@HandleEvent
	public void handleNodeConfigurationChangeEvent(PubSubModule.NodeConfigurationChangeNotificationReceivedHandler.NodeConfigurationChangeNotificationReceivedEvent event) {
		try {
			String deviceId = DeviceNodesHelper.getDeviceIdFromNode(event.getNode());
			if (!event.getNode().endsWith("/" + deviceId)) {
				return;
			}
			
			if (event.getNodeConfig() == null) {
				Jaxmpp jaxmpp = this.xmppService.getConnection(event.getSessionObject());
				if (jaxmpp != null && jaxmpp.isConnected()) {
					updateNodeLabelFromNodeTitle(jaxmpp, deviceId);
				}
			} else {
				deviceManager.updateDeviceLabel(deviceId, (String) event.getNodeConfig().getField("pubsub#title").getFieldValue());
			}
		} catch (JaxmppException ex) {
			log.log(Level.WARNING, "failed to handle node configuration change notification", ex);
		}
	}

	protected void updateNodeLabelFromNodeTitle(Jaxmpp jaxmpp, String deviceId) throws JaxmppException {
		String node = rootNode + "/" + deviceId;
		JID pubsubJid = getPubsubJid(jaxmpp);
		jaxmpp.getModule(PubSubModule.class).getNodeConfiguration(pubsubJid.getBareJid(), node, new PubSubModule.NodeConfigurationAsyncCallback() {
			@Override
			protected void onReceiveConfiguration(IQ responseStanza, String node, JabberDataElement form) {
				try {
					deviceManager.updateDeviceLabel(deviceId, (String) form.getField("pubsub#title").getFieldValue());
				} catch (JaxmppException ex) {}
			}

			@Override
			protected void onEror(IQ response, XMPPException.ErrorCondition errorCondition,
								  PubSubErrorCondition pubSubErrorCondition) throws JaxmppException {
				log.log(Level.FINEST, "retrieval of pubsub node {0}/{1} configuration failed - {2}, {3}",
						new Object[]{pubsubJid, node, errorCondition, pubSubErrorCondition});
			}

			@Override
			public void onTimeout() throws JaxmppException {
				log.log(Level.FINEST, "retrieval of pubsub node {0}/{1} configuration failed - timeout",
						new Object[]{pubsubJid, node});
			}
		});
	}

	/**
	 * Event fired when PubSub notification about change on PubSub node is received.
	 * @param <T>
	 */
	public static class ValueChangedEvent<T> {

		public final String sourceId;
		public final T value;

		public ValueChangedEvent(String sourceId, T value) {
			this.sourceId = sourceId;
			this.value = value;
		}

		public boolean is(Class clazz) {
			return (value != null) && clazz.isAssignableFrom(value.getClass());
		}

		@Override
		public String toString() {
			return "ValueChangedEvent[source: " + sourceId + ", value: " + value + "]";
		}
	}

}
