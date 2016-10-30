package tigase.rpi.home.app;

import com.pi4j.temperature.TemperatureScale;
import tigase.eventbus.HandleEvent;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.kernel.beans.Inject;
import tigase.rpi.home.AbstractDevice;
import tigase.rpi.home.Autostart;
import tigase.rpi.home.IValue;
import tigase.rpi.home.sensors.w1.DS1820;
import tigase.rpi.home.values.Temperature;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 26.10.2016.
 */
@Autostart
public class TemparaturePubSubPublisher
		extends AbstractDevicePubSubPublisher {

	private static final Logger log = Logger.getLogger(TemparaturePubSubPublisher.class.getCanonicalName());

	@Inject
	private DS1820 sensor;

	private DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("UTC"));

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

		try {
			Element timestampElem = ElementFactory.create("timestamp");
			timestampElem.setAttribute("value", value.getTimestamp().atZone(ZoneId.systemDefault()).format(formatter));

			Element numeric = ElementFactory.create("numeric");
			numeric.setAttribute("name", "Temperature");
			numeric.setAttribute("momentary", "true");
			numeric.setAttribute("automaticReadout", "true");
			numeric.setAttribute("value", String.valueOf(value.getValue().getValue(TemperatureScale.CELSIUS)));
			numeric.setAttribute("unit", "°C");

			timestampElem.addChild(numeric);

			publishState(timestampElem);
		} catch (JaxmppException ex) {
			log.log(Level.WARNING, this.name + ", publication of value " + value + " failed", ex);
		}
	}
}
