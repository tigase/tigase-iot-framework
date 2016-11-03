package tigase.rpi.home.app.pubsub;

import tigase.bot.AbstractDevice;
import tigase.bot.IDevice;
import tigase.bot.IValue;
import tigase.eventbus.HandleEvent;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.kernel.beans.Inject;
import tigase.rpi.home.app.ValueFormatter;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 31.10.2016.
 */
public class DevicePubSubPublisher
		extends AbstractDevicePubSubPublisher {

	private static final Logger log = Logger.getLogger(DevicePubSubPublisher.class.getCanonicalName());

	@Inject
	private List<ValueFormatter> formatters;

	@HandleEvent
	public void valueChanged(AbstractDevice.ValueChangeEvent event) {
		if (!devices.contains(event.source)) {
			return;
		}

		publishValue(event.source, event.newValue);
	}

	protected void publishValue(IDevice source, IValue value) {
		formatters.stream().filter(formatter -> formatter.isSupported(value)).forEach(formatter -> {
			try {
				Element result = formatter.toElement(value);
				if (result == null) {
					log.log(Level.FINE, "formatter {0} failed to return result for value {1}",
							new Object[]{formatter, value});
					return;
				}

				publishState(source, result);
			} catch (JaxmppException ex) {
				log.log(Level.WARNING,
						"formatter " + formatter + " thrown exception while formatting value " + value, ex);
			}
		});
	}

	@Override
	protected void nodesReady(IDevice device) {
		IValue value = device.getValue();

		publishValue(device, value);
	}
}
