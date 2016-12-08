package tigase.rpi.home.client.devices;

import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;

/**
 * Created by andrzej on 05.12.2016.
 */
public class TvSensor extends MovementSensor {

	public TvSensor(JaxmppCore jaxmpp, JID pubsubJid, String node, String name) {
		super(jaxmpp, pubsubJid, node, name);
	}
}
