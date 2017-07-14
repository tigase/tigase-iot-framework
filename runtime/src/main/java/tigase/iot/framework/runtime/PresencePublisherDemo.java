/*
 * PresencePublisherDemo.java
 *
 * Tigase IoT Framework
 * Copyright (C) 2011-2017 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */

package tigase.iot.framework.runtime;

import tigase.bot.Autostart;
import tigase.bot.RequiredXmppModules;
import tigase.bot.XmppService;
import tigase.bot.runtime.AbstractXmppBridge;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.j2se.Jaxmpp;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An example class which creates Presence which should be set for XMPP connection.
 *
 * Created by andrzej on 23.10.2016.
 */
@Autostart
@RequiredXmppModules({PresenceModule.class})
public class PresencePublisherDemo extends AbstractXmppBridge
		implements Initializable, UnregisterAware, PresenceModule.OwnPresenceStanzaFactory {

	private static final Logger log = Logger.getLogger(PresencePublisherDemo.class.getCanonicalName());

	@Inject
	private XmppService xmppService;

	@Override
	public void beforeUnregister() {
		xmppService.getAllConnections()
				.forEach(jaxmpp -> PresenceModule.setOwnPresenceStanzaFactory(jaxmpp.getSessionObject(), this));
	}

	/**
	 * Creates instance of {@link tigase.jaxmpp.core.client.xmpp.stanzas.Presence}
	 * and sets its properties as required.
	 * @param sessionObject
	 * @return
	 */
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
	}

	@Override
	protected void jaxmppConnected(Jaxmpp jaxmpp) {
		try {
			jaxmpp.getModule(PresenceModule.class).sendInitialPresence();
		} catch (JaxmppException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void jaxmppDisconnected(Jaxmpp jaxmpp) {

	}

}
