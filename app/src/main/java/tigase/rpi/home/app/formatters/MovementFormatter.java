package tigase.rpi.home.app.formatters;

import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.kernel.beans.Bean;
import tigase.kernel.core.Kernel;
import tigase.rpi.home.values.Movement;

import java.time.LocalDateTime;

/**
 * Created by andrzej on 07.11.2016.
 */
@Bean(name = "movementFormatter", parent = Kernel.class, exportable = true)
public class MovementFormatter extends AbstractValueFormatter<Movement> {

	public MovementFormatter() {
		super("Movement", Movement.class);
	}

	@Override
	public Element toElement(Movement value) throws JaxmppException {
		Element timestampElem = createTimestampElement(value);

		Element valueElem = ElementFactory.create("boolean");
		valueElem.setAttribute("name", name);
		valueElem.setAttribute("automaticReadout", "true");
		valueElem.setAttribute("value", String.valueOf(value.getValue()));

		timestampElem.addChild(valueElem);

		return timestampElem;
	}

	@Override
	public Movement fromElement(Element elem) throws JaxmppException {
		Element valueElem = elem.getFirstChild("boolean");
		if (valueElem == null || !name.equals(valueElem.getAttribute("name"))) {
			return null;
		}

		LocalDateTime timestamp = parseTimestampElement(elem);
		Boolean value = Boolean.parseBoolean(valueElem.getAttribute("value"));

		return new Movement(value, timestamp);
	}
}
