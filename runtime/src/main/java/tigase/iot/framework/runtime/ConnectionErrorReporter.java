/*
 * ConnectionErrorReporter.java
 *
 * Tigase IoT Framework
 * Copyright (C) 2011-2018 "Tigase, Inc." <office@tigase.com>
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
import tigase.bot.runtime.XmppService;
import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;

import java.util.logging.Level;
import java.util.logging.Logger;

@Autostart
public class ConnectionErrorReporter implements Initializable, UnregisterAware {

	private static final Logger log = Logger.getLogger(ConnectionErrorReporter.class.getCanonicalName());

	@Inject
	private EventBus eventBus;

	public ConnectionErrorReporter() {
	}

	@Override
	public void beforeUnregister() {
		if (eventBus != null) {
			eventBus.unregisterAll(this);
		}
	}

	@Override
	public void initialize() {
		if (eventBus != null) {
			eventBus.registerAll(this);
		}
	}

	@HandleEvent
	public void handleConnected(XmppService.JaxmppConnectedEvent event) {
		log.log(Level.INFO, "Connected to Tigase IoT Hub as " + event.jaxmpp.getSessionObject().getUserBareJid().getLocalpart());
	}

	@HandleEvent
	public void handleDisconnected(XmppService.JaxmppDisconnectedEvent event) {
		log.log(Level.INFO, "Disconnected from Tigase IoT Hub as " + event.jaxmpp.getSessionObject().getUserBareJid().getLocalpart());
	}

	@HandleEvent
	public void handleConnectionErrorEvent(XmppService.JaxmppConnectionErrorEvent event) {
		Throwable rootCause = event.cause;
		while (rootCause.getCause() != null) {
			rootCause = rootCause.getCause();
		}
		if (rootCause instanceof JaxmppException) {
			if ("Cannot create socket.".equals(rootCause.getMessage())) {
				log.log(Level.WARNING,
						"Tigase IoT Framework cannot connect to the local IoT hub. Please make sure IoT hub is running and it is available in the same network as Tigase IoT Framework.");
			}
		} else if (rootCause instanceof java.net.ConnectException) {
			if ("Connection timed out".equals(rootCause.getMessage()) || "Connection refused".equals(rootCause.getMessage())) {
				log.log(Level.WARNING, "Tigase IoT Framework cannot connect to the local IoT hub. Please make sure that IoT hub is running and that host of Tigase IoT Hub accepts connection at port 5222.");
			}
		}
	}

	@HandleEvent
	public void handleAccountStateChanged(AccountStatusMonitor.AccountStatusChangedHandler.AccountStatusChangedEvent event) {
		String accountId = event.getSessionObject().getUserBareJid().getLocalpart();
		switch (event.accountStatus) {
			case active:
				log.log(Level.INFO, "Account for this devices " + accountId + " is now active.");
				break;
			case disabled:
				log.log(Level.WARNING, "Account for this devices " + accountId + " is now disabled. If you want to enable it please use your IoT client and enabled it in 'Manage devices'.");
				break;
			case pending:
				log.log(Level.WARNING, "Account for this devices " + accountId + " is awaiting acceptance. If you want to enable it please use your IoT client and enabled it in 'Manage devices'. Until then it will not be possible to use this device.");
				break;
		}
	}

	@HandleEvent
	public void handlePolicyViolationErrorEvent(PolicyViolationErrorEvent event) {
		
	}

	public static class PolicyViolationErrorEvent {

		public static void fireIfNeeded(EventBus eventBus, XMPPException.ErrorCondition errorCondition, Stanza stanza)
				throws XMLException {
			if (eventBus == null) {
				throw new IllegalArgumentException("Instance of the EventBus is required!");
			}

			if (errorCondition != XMPPException.ErrorCondition.policy_violation) {
				return;
			}
			eventBus.fire(new PolicyViolationErrorEvent(stanza));
		}

		public final Stanza stanza;

		public final String deviceId;

		public PolicyViolationErrorEvent(Stanza stanza) throws XMLException {
			this.stanza = stanza;
			if (stanza.getFrom() != null) {
				deviceId = stanza.getFrom().getLocalpart();
			} else {
				deviceId = null;
			}
		}

	}
}
