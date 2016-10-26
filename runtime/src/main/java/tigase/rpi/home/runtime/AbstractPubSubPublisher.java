package tigase.rpi.home.runtime;

import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubModule;
import tigase.jaxmpp.j2se.Jaxmpp;
import tigase.kernel.beans.config.ConfigField;
import tigase.rpi.home.RequiredXmppModules;

import java.util.logging.Logger;

/**
 * Created by andrzej on 22.10.2016.
 */
@RequiredXmppModules({PubSubModule.class, DiscoveryModule.class})
public abstract class AbstractPubSubPublisher
		extends AbstractXmppBridge {

	private static final Logger log = Logger.getLogger(
			tigase.rpi.home.runtime.AbstractPubSubPublisher.class.getCanonicalName());

	@ConfigField(desc = "Name")
	protected String name;

	@ConfigField(desc = "Use PEP nodes")
	private boolean PEP = false;

	@ConfigField(desc = "Root node")
	protected String rootNode;


	protected JID getPubsubJid(Jaxmpp jaxmpp) {
		if (PEP) {
			return JID.jidInstance(jaxmpp.getSessionObject().getUserBareJid());
		}

		return JID.jidInstance("pubsub." + jaxmpp.getSessionObject().getUserBareJid().getDomain());
	}

}
