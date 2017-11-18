package tigase.iot.framework.client.devices;

import tigase.iot.framework.client.Device;
import tigase.iot.framework.client.values.OnOffState;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.utils.DateTimeFormat;

import java.util.Date;

/**
 * Class represents an implementation of a switch device.
 */
public class Switch extends Device<OnOffState> {

	public Switch(JaxmppCore jaxmpp, JID pubsubJid, String node, String name) {
		super(jaxmpp, pubsubJid, node, name);
	}

	@Override
	public void setValue(OnOffState newValue, Callback<OnOffState> callback) throws JaxmppException {
		super.setValue(newValue, callback);
	}

	@Override
	protected Element encodeToPayload(OnOffState value) {
		try {
			Element timestampElem = ElementFactory.create("timestamp");
			timestampElem.setAttribute("value", new DateTimeFormat().format(value.getTimestamp()));

			Element numeric = ElementFactory.create("boolean");
			numeric.setAttribute("name", OnOffState.NAME);
			numeric.setAttribute("automaticReadout", "false");
			numeric.setAttribute("value", String.valueOf(value.getValue()));
			
			timestampElem.addChild(numeric);

			return timestampElem;
		} catch (XMLException ex) {
		}
		return null;
	}

	@Override
	protected OnOffState parsePayload(Element elem) {
		try {
			Element numeric = elem.getFirstChild("boolean");
			if (numeric == null || !OnOffState.NAME.equals(numeric.getAttribute("name"))) {
				return null;
			}

			Boolean value = Boolean.parseBoolean(numeric.getAttribute("value"));
			Date timestamp = parseTimestamp(elem);

			return new OnOffState(value, timestamp);
		} catch (XMLException ex) {
			return null;
		}
	}

}
