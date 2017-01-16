package tigase.rpi.home.app.pubsub;

import tigase.bot.iot.IDevice;
import tigase.bot.iot.IExecutorDevice;
import tigase.bot.iot.IValue;
import tigase.eventbus.HandleEvent;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubModule;
import tigase.kernel.beans.Inject;
import tigase.rpi.home.app.DeviceNodesHelper;
import tigase.rpi.home.app.ValueFormatter;

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

	@Inject
	private List<IExecutorDevice<IValue>> executorDevices;

	@Inject
	private List<ValueFormatter> formatters;

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
