package tigase.rpi.home.client.devices;

import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.rpi.home.client.Device;
import tigase.rpi.home.client.values.Light;

import java.util.Date;

/**
 * Created by andrzej on 26.11.2016.
 */
public class LightSensor extends Device<Light> {

	public LightSensor(JaxmppCore jaxmpp, JID pubsubJid, String node, String name) {
		super(jaxmpp, pubsubJid, node, name);
	}

	@Override
	protected Element encodeToPayload(Light value) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected Light parsePayload(Element elem) {
		try {
			Element numeric = elem.getFirstChild("numeric");
			if (numeric == null || !Light.NAME.equals(numeric.getAttribute("name"))) {
				return null;
			}

			Integer value = Integer.parseInt(numeric.getAttribute("value"));
			Date timestamp = parseTimestamp(elem);
			String unitStr = numeric.getAttribute("unit");

			for (Light.Unit unit : Light.Unit.values()) {
				if (unit.toString().equals(unitStr)) {
					return new Light(value, unit, timestamp);
				}
			}
		} catch (XMLException ex) {
		}
		return null;
	}
}
