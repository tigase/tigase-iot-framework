package tigase.rpi.home.app.formatters;

import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.kernel.beans.Bean;
import tigase.kernel.core.Kernel;
import tigase.rpi.home.values.Light;

import java.time.LocalDateTime;

/**
 * Created by andrzej on 05.11.2016.
 */
@Bean(name = "lightFormatter", parent = Kernel.class, exportable = true)
public class LightFormatter extends AbstractValueFormatter<Light> {

	public LightFormatter() {
		super("Light", Light.class);
	}

	@Override
	public Element toElement(Light value) throws JaxmppException {
		Element timestampElem = createTimestampElement(value);

		Element numeric = ElementFactory.create("numeric");
		numeric.setAttribute("name", name);
		numeric.setAttribute("momentary", "true");
		numeric.setAttribute("automaticReadout", "true");
		numeric.setAttribute("value", String.valueOf(value.getValue()));
		numeric.setAttribute("unit", value.getUnit().toString());

		timestampElem.addChild(numeric);

		return timestampElem;

	}

	@Override
	public Light fromElement(Element elem) throws JaxmppException {
		Element numeric = elem.getFirstChild("numeric");
		if (numeric == null || !name.equals(numeric.getAttribute("name"))) {
			return null;
		}

		LocalDateTime timestamp = parseTimestampElement(elem);
		Integer value  = Integer.parseInt(numeric.getAttribute("value"));
		String unitStr = numeric.getAttribute("unit");

		for (Light.Unit unit : Light.Unit.values()) {
			if (unit.toString().equals(unitStr)) {
				return new Light(value, unit, timestamp);
			}
		}

		return null;
	}
}
