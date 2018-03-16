package tigase.iot.framework.client.devices;

import tigase.iot.framework.client.Device;
import tigase.iot.framework.client.Devices;
import tigase.iot.framework.client.values.LedMatrix;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.utils.DateTimeFormat;

import java.util.Date;

public class LedMatrixDevice
		extends Device<LedMatrix> {

	public LedMatrixDevice(Devices devices, JaxmppCore jaxmpp, JID pubsubJid, String node, String name) {
		super(devices, jaxmpp, pubsubJid, node, name);
	}

	@Override
	protected Element encodeToPayload(LedMatrix value) {
		try {
			Element timestampElem = ElementFactory.create("timestamp");
			timestampElem.setAttribute("value", new DateTimeFormat().format(value.getTimestamp()));

			Element numeric = ElementFactory.create("matrix");
			numeric.setAttribute("name", LedMatrix.NAME);
			numeric.setAttribute("automaticReadout", "false");
			numeric.setAttribute("value", value.getValue());

			timestampElem.addChild(numeric);

			return timestampElem;
		} catch (XMLException ex) {
		}
		return null;
	}

	@Override
	protected LedMatrix parsePayload(Element elem) {
		try {
			Element numeric = elem.getFirstChild("matrix");
			if (numeric == null || !LedMatrix.NAME.equals(numeric.getAttribute("name"))) {
				return null;
			}

			String value = numeric.getAttribute("value");
			Date timestamp = parseTimestamp(elem);

			return new LedMatrix(value, timestamp);
		} catch (XMLException ex) {
			return null;
		}
	}

	@Override
	public void setValue(LedMatrix newValue, Callback<LedMatrix> callback) throws JaxmppException {
		super.setValue(newValue, callback);
	}
}
