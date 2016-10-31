package tigase.rpi.home.app;

import tigase.bot.AbstractDevice;
import tigase.bot.Autostart;
import tigase.bot.IValue;
import tigase.eventbus.HandleEvent;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.kernel.beans.Inject;
import tigase.rpi.home.sensors.w1.DS1820;
import tigase.rpi.home.sensors.w1.W1Master;
import tigase.rpi.home.values.Temperature;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 26.10.2016.
 */
@Autostart
public class TemperaturePubSubPublisher
		extends AbstractDevicePubSubPublisher {

	private static final Logger log = Logger.getLogger(TemperaturePubSubPublisher.class.getCanonicalName());

	@Inject
	private W1Master master;

	@Inject
	private DS1820 sensor;

	@Inject
	private List<ValueFormatter> pubsubFormatters;

	@Override
	protected String getDeviceId() {
		return sensor.getName();
	}

	@HandleEvent
	public void valueChanged(AbstractDevice.ValueChangeEvent event) {
		if (event.source == sensor) {
			tempChanged(event);
		}
	}

	@Override
	protected void nodesReady() {
		IValue<Temperature> value = sensor.getValue();
		if (value != null) {
			tempChanged(value);
		}
	}

	protected void tempChanged(AbstractDevice.ValueChangeEvent<Temperature> event) {
		IValue<Temperature> value = event.newValue;

		tempChanged(value);
	}

	public void tempChanged(IValue<Temperature> value) {
		pubsubFormatters.stream()
				.filter(valueFormatter -> valueFormatter.isSupported(value))
				.map(valueFormatter -> {
					try {
						return valueFormatter.toElement(value);
					} catch (JaxmppException ex) {
						log.log(Level.WARNING,
								"Formatter " + valueFormatter.getClass() + " throw exception while converting " +
										value + " to element", ex);
						return null;
					}
				})
				.filter(payload -> payload != null)
				.forEach(payload -> publishState(payload));
	}
}
