package tigase.rpi.home.runtime;

import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.rpi.home.RequiredXmppModules;
import tigase.rpi.home.XmppBridge;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 23.10.2016.
 */
@RequiredXmppModules({PresenceModule.class})
public class PresencePublisherDemo
		implements XmppBridge, Initializable, UnregisterAware, PresenceModule.OwnPresenceStanzaFactory {

	private static final Logger log = Logger.getLogger(PresencePublisherDemo.class.getCanonicalName());

	@Inject
	private XmppService xmppService;

	@Override
	public void beforeUnregister() {
		xmppService.getAllConnections()
				.forEach(jaxmpp -> PresenceModule.setOwnPresenceStanzaFactory(jaxmpp.getSessionObject(), this));
	}

	@Override
	public Presence create(SessionObject sessionObject) {
		try {
			Presence presence = Presence.create();
			presence.setStatus("Hello again!");
			return presence;
		} catch (JaxmppException ex) {
			log.log(Level.WARNING, "Exception during preparation of presence packet", ex);
		}
		return null;
	}

	@Override
	public void initialize() {
		xmppService.getAllConnections()
				.forEach(jaxmpp -> PresenceModule.setOwnPresenceStanzaFactory(jaxmpp.getSessionObject(), this));
		xmppService.getAllConnections().stream().filter(jaxmpp -> jaxmpp.isConnected()).forEach(jaxmpp -> {
			try {
				jaxmpp.getModule(PresenceModule.class).sendInitialPresence();
			} catch (JaxmppException e) {
				e.printStackTrace();
			}
		});
	}

}
