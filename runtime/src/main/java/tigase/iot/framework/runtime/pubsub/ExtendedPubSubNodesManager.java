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

import tigase.bot.iot.IDevice;
import tigase.bot.iot.IExecutorDevice;
import tigase.bot.iot.IValue;
import tigase.eventbus.HandleEvent;
import tigase.iot.framework.runtime.ValueFormatter;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubModule;
import tigase.kernel.beans.Inject;
import tigase.iot.framework.runtime.DeviceNodesHelper;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Created by andrzej on 05.11.2016.
 */
public class ExtendedPubSubNodesManager
		extends PubSubNodesManager {

	private static final Logger log = Logger.getLogger(ExtendedPubSubNodesManager.class.getCanonicalName());

	@Inject(nullAllowed = true)
	private List<IExecutorDevice<IValue>> executorDevices;

	@Inject
	private List<ValueFormatter> formatters;

	public ExtendedPubSubNodesManager() {
		this.rootNode = "devices";
	}

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

	@Override
	protected Stream<String> getObservedNodes(NodesObserver o) {
		if (o instanceof IExecutorDevice) {
			return Stream.concat(Stream.of(DeviceNodesHelper.getDeviceStateNodeName(rootNode, (IDevice) o)),
								 super.getObservedNodes(o));
		} else {
			return super.getObservedNodes(o);
		}
	}

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