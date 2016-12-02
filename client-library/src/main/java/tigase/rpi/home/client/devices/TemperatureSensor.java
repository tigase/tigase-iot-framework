package tigase.rpi.home.client.devices;

import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.rpi.home.client.Device;
import tigase.rpi.home.client.values.Temperature;

import java.util.Date;

/**
 * Created by andrzej on 26.11.2016.
 */
public class TemperatureSensor extends Device<Temperature> {

	public TemperatureSensor(JaxmppCore jaxmpp, JID pubsubJid, String node, String name) {
		super(jaxmpp, pubsubJid, node, name);
	}

	@Override
	protected Element encodeToPayload(Temperature value) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected Temperature parsePayload(Element elem) {
		try {
			Element numeric = elem.getFirstChild("numeric");
			if (numeric == null || !Temperature.NAME.equals(numeric.getAttribute("name"))) {
				return null;
			}

			Double value = Double.parseDouble(numeric.getAttribute("value"));
			String unit = numeric.getAttribute("unit");
			Date timestamp = parseTimestamp(elem);

			return new Temperature(value, unit, timestamp);
		} catch (XMLException ex) {
			return null;
		}
	}
}
