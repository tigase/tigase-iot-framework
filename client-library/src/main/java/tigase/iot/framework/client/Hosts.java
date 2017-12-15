/*
 * Hosts.java
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
package tigase.iot.framework.client;

import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule;
import tigase.jaxmpp.core.client.xmpp.modules.extensions.Extension;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;

import java.util.HashMap;
import java.util.Map;

public class Hosts {

	private final JaxmppCore jaxmpp;

	private final Map<JID, DiscoveryModule.Identity> activeHosts = new HashMap<>();
	
	private Devices devices;

	public Hosts(final JaxmppCore jaxmpp, final Devices devices, final Devices.DevicesInfoRetrieved onActiveHostsChanged) {
		this.jaxmpp = jaxmpp;
		this.devices = devices;

		if (this.jaxmpp.getModule(MessageModule.class) == null) {
			this.jaxmpp.getModulesManager().register(new MessageModule());
		}

		jaxmpp.getEventBus().addHandler(JaxmppCore.LoggedOutHandler.LoggedOutEvent.class, new JaxmppCore.LoggedOutHandler() {
			@Override
			public void onLoggedOut(SessionObject sessionObject) {
				activeHosts.clear();
				onActiveHostsChanged.onDeviceInfoRetrieved(activeHosts);
			}
		});

		this.jaxmpp.getModule(PresenceModule.class).addContactChangedPresenceHandler(new PresenceModule.ContactChangedPresenceHandler() {
			@Override
			public void onContactChangedPresence(SessionObject sessionObject, Presence stanza, JID jid,
												 Presence.Show show, String status, Integer priority)
					throws JaxmppException {
				if (stanza.getType() != null) {
					if (sessionObject.getUserBareJid().equals(jid.getBareJid()) && "iot-hub".equals(jid.getResource())) {
						activeHosts.clear();
						onActiveHostsChanged.onDeviceInfoRetrieved(activeHosts);
					}
					else {
						activeHosts.remove(jid);
						onActiveHostsChanged.onDeviceInfoRetrieved(activeHosts);
					}
				} else {
					devices.checkIfJidIsHost(jid, new Devices.BiConsumer<JID, DiscoveryModule.Identity>() {
						@Override
						public void accept(JID jid, DiscoveryModule.Identity identity) {
							activeHosts.put(jid, identity);
							onActiveHostsChanged.onDeviceInfoRetrieved(activeHosts);
						}
					}, new Runnable() {
						@Override
						public void run() {
							// nothing to do..
						}
					});
				}
			}
		});

		final Extension forwardExtension = new Extension() {

			@Override
			public Element afterReceive(Element received) throws JaxmppException {
				Element forwarded = received.getChildrenNS("forwarded", "urn:xmpp:forward:0");
				if (forwarded == null) {
					return received;
				}
				Element presence = forwarded.getFirstChild("presence");
				if (presence == null) {
					return received;
				}

				PresenceModule presenceModule = jaxmpp.getModule(PresenceModule.class);
				presenceModule.process(presence);

				return null;
			}

			@Override
			public Element beforeSend(Element received) throws JaxmppException {
				return received;
			}

			@Override
			public String[] getFeatures() {
				return new String[0];
			}
		};
		
		this.jaxmpp.getModule(MessageModule.class).addExtension(forwardExtension);
	}

	public void getActiveDeviceHosts(Devices.DevicesInfoRetrieved callback) {
		callback.onDeviceInfoRetrieved(activeHosts);
	}

}
