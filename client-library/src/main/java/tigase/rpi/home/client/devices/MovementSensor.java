package tigase.rpi.home.client.devices;

import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.rpi.home.client.Device;
import tigase.rpi.home.client.values.Movement;

import java.util.Date;

/**
 * Created by andrzej on 27.11.2016.
 */
public class MovementSensor extends Device<Movement> {

	public MovementSensor(JaxmppCore jaxmpp, JID pubsubJid, String node, String name) {
		super(jaxmpp, pubsubJid, node, name);
	}

	@Override
	protected Element encodeToPayload(Movement value) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected Movement parsePayload(Element elem) {
		try {
			Element numeric = elem.getFirstChild("boolean");
			if (numeric == null || !Movement.NAME.equals(numeric.getAttribute("name"))) {
				return null;
			}

			Boolean value = Boolean.parseBoolean(numeric.getAttribute("value"));
			Date timestamp = parseTimestamp(elem);

			return new Movement(value, timestamp);
		} catch (XMLException ex) {
			return null;
		}
	}
}
