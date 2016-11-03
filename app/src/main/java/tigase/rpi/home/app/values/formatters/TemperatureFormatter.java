package tigase.rpi.home.app.values.formatters;

import com.pi4j.temperature.TemperatureScale;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.kernel.beans.Bean;
import tigase.kernel.core.Kernel;
import tigase.rpi.home.app.ValueFormatter;
import tigase.rpi.home.app.utils.XmppDateTimeFormatterFactory;
import tigase.rpi.home.values.Temperature;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Created by andrzej on 30.10.2016.
 */
@Bean(name = "temperatureFormatter", parent = Kernel.class, exportable = true)
public class TemperatureFormatter
		implements ValueFormatter<Temperature> {

	private DateTimeFormatter formatter = XmppDateTimeFormatterFactory.newInstance();

	private String name;

	public TemperatureFormatter() {
		name = "Temperature";
	}

	@Override
	public Class<Temperature> getSupportedClass() {
		return Temperature.class;
	}

	@Override
	public Element toElement(Temperature value) throws JaxmppException {
		Element timestampElem = ElementFactory.create("timestamp");
		timestampElem.setAttribute("value", value.getTimestamp().atZone(ZoneId.systemDefault()).format(formatter));

		Element numeric = ElementFactory.create("numeric");
		numeric.setAttribute("name", name);
		numeric.setAttribute("momentary", "true");
		numeric.setAttribute("automaticReadout", "true");
		numeric.setAttribute("value", String.valueOf(value.getValue(TemperatureScale.CELSIUS)));
		numeric.setAttribute("unit", "°C");

		timestampElem.addChild(numeric);

		return timestampElem;
	}

	@Override
	public Temperature fromElement(Element elem) throws JaxmppException, IllegalArgumentException {
		if (!"timestamp".equals(elem.getName()) || elem.getAttribute("value") == null) {
			return null;
		}

		Element numeric = elem.getFirstChild("numeric");
		if (numeric == null || !name.equals(numeric.getAttribute("name"))) {
			return null;
		}

		LocalDateTime timestamp = formatter.parse(elem.getAttribute("value")).query(LocalDateTime::from);

		Double value  = Double.parseDouble(numeric.getAttribute("value"));
		for (TemperatureScale scale : TemperatureScale.values()) {
			if (scale.getUnits().equals(numeric.getAttribute("unit"))) {
				return new Temperature(scale, value, timestamp);
			}
		}

		return null;
	}

}
