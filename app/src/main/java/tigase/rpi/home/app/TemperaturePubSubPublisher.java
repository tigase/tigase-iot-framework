package tigase.rpi.home.app;

import com.pi4j.temperature.TemperatureScale;
import tigase.bot.AbstractDevice;
import tigase.bot.Autostart;
import tigase.bot.IValue;
import tigase.eventbus.HandleEvent;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.kernel.beans.Inject;
import tigase.rpi.home.sensors.w1.DS1820;
import tigase.rpi.home.sensors.w1.W1Master;
import tigase.rpi.home.values.Temperature;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.time.temporal.ChronoField.*;

/**
 * Created by andrzej on 26.10.2016.
 */
@Autostart
public class TemperaturePubSubPublisher
		extends AbstractDevicePubSubPublisher {

	private static final Logger log = Logger.getLogger(TemperaturePubSubPublisher.class.getCanonicalName());

	@Inject
	private W1Master master;

	@Inject(nullAllowed = true)
	private DS1820 sensor;

	private DateTimeFormatter formatter = new DateTimeFormatterBuilder().parseCaseInsensitive()
			.appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
			.appendLiteral('-')
			.appendValue(MONTH_OF_YEAR, 2)
			.appendLiteral('-')
			.appendValue(DAY_OF_MONTH, 2)
			.appendLiteral('T')
			.appendValue(HOUR_OF_DAY, 2)
			.appendLiteral(':')
			.appendValue(MINUTE_OF_HOUR, 2)
			.appendLiteral(':')
			.appendValue(SECOND_OF_MINUTE, 2)
			.optionalStart()
			.appendLiteral('.')
			.appendFraction(MILLI_OF_SECOND, 3, 3, false)
			.optionalEnd()
			.appendOffsetId().toFormatter();

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
