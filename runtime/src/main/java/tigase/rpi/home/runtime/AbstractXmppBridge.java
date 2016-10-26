package tigase.rpi.home.runtime;

import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.jaxmpp.j2se.Jaxmpp;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.rpi.home.XmppBridge;

/**
 * Created by andrzej on 25.10.2016.
 */
public abstract class AbstractXmppBridge
		implements XmppBridge, Initializable, UnregisterAware {

	@Inject
	protected EventBus eventBus;

	@Inject
	protected tigase.rpi.home.XmppService xmppService;

	protected abstract void jaxmppConnected(Jaxmpp jaxmpp);

	protected abstract void jaxmppDisconnected(Jaxmpp jaxmpp);

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
		xmppService.getAllConnections()
				.stream()
				.filter(jaxmpp -> jaxmpp.isConnected())
				.forEach(jaxmpp -> jaxmppDisconnected(jaxmpp));
	}

	@Override
	public void initialize() {
		eventBus.registerAll(this);
		xmppService.getAllConnections()
				.stream()
				.filter(jaxmpp -> jaxmpp.isConnected())
				.forEach(jaxmpp -> jaxmppConnected(jaxmpp));
	}

	@HandleEvent
	public void jaxmppConnectedHandler(XmppService.JaxmppConnectedEvent event) {
		jaxmppConnected(event.jaxmpp);
	}

	@HandleEvent
	public void jaxmppDisconnectedHandler(XmppService.JaxmppDisconnectedEvent event) {
		jaxmppDisconnected(event.jaxmpp);
	}

}
