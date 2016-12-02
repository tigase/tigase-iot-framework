package tigase.rpi.home.client.devices;

import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.utils.DateTimeFormat;
import tigase.rpi.home.client.values.Light;

/**
 * Created by andrzej on 26.11.2016.
 */
public class LightDimmer extends LightSensor {

	public LightDimmer(JaxmppCore jaxmpp, JID pubsubJid, String node, String name) {
		super(jaxmpp, pubsubJid, node, name);
	}

	@Override
	public void setValue(Light newValue, Callback<Light> callback) throws JaxmppException {
		super.setValue(newValue, callback);
	}

	@Override
	protected Element encodeToPayload(Light value) {
		try {
			Element timestampElem = ElementFactory.create("timestamp");
			timestampElem.setAttribute("value", new DateTimeFormat().format(value.getTimestamp()));

			Element numeric = ElementFactory.create("numeric");
			numeric.setAttribute("name", Light.NAME);
			numeric.setAttribute("automaticReadout", "false");
			numeric.setAttribute("value", String.valueOf(value.getValue()));
			numeric.setAttribute("unit", value.getUnit().toString());

			timestampElem.addChild(numeric);

			return timestampElem;
		} catch (XMLException ex) {
		}
		return null;
	}
}
