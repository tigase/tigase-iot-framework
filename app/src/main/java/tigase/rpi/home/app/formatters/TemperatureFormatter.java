package tigase.rpi.home.app.formatters;

import com.pi4j.temperature.TemperatureScale;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.kernel.beans.Bean;
import tigase.kernel.core.Kernel;
import tigase.rpi.home.values.Temperature;

import java.time.LocalDateTime;

/**
 * Created by andrzej on 30.10.2016.
 */
@Bean(name = "temperatureFormatter", parent = Kernel.class, exportable = true)
public class TemperatureFormatter
		extends AbstractValueFormatter<Temperature> {


	public TemperatureFormatter() {
		super("Temperature", Temperature.class);
	}

	@Override
	public Element toElement(Temperature value) throws JaxmppException {
		Element timestampElem = createTimestampElement(value);

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
		Element numeric = elem.getFirstChild("numeric");
		if (numeric == null || !name.equals(numeric.getAttribute("name"))) {
			return null;
		}

		LocalDateTime timestamp = parseTimestampElement(elem);

		Double value  = Double.parseDouble(numeric.getAttribute("value"));
		for (TemperatureScale scale : TemperatureScale.values()) {
			if (scale.getUnits().equals(numeric.getAttribute("unit"))) {
				return new Temperature(scale, value, timestamp);
			}
		}

		return null;
	}

}
