package tigase.rpi.home.app.pubsub;

import tigase.bot.IValue;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.kernel.beans.Inject;
import tigase.rpi.home.app.ValueFormatter;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 02.11.2016.
 */
public class DevicePubSubListener
		extends AbstractDevicePubSubListener {

	private static Logger log = Logger.getLogger(DevicePubSubListener.class.getCanonicalName());

	@Inject
	private List<ValueFormatter> formatters;

	@Override
	public void onNotificationReceived(String node, Element payload) {
		String deviceId = parseDeviceId(node);
		if (deviceId == null) {
			return;
		}

		formatters.stream().map(formatter -> {
			try {
				return formatter.fromElement(payload);
			} catch (JaxmppException ex) {
				log.log(Level.WARNING, "formatter " + formatter + " thrown exception while parsing payload " + payload,
						ex);
			}
			return null;
		}).filter(value -> value != null).forEach(value -> eventBus.fire(new ValueChangedEvent(deviceId, value)));
	}

	public static class ValueChangedEvent<T> {

		public final String sourceId;
		public final IValue<T> value;

		public ValueChangedEvent(String sourceId, IValue<T> value) {
			this.sourceId = sourceId;
			this.value = value;
		}

		@Override
		public String toString() {
			return "ValueChangedEvent[source: " + sourceId + ", value: " + value + "]";
		}
	}
}

