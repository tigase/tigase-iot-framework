/*
 * DiscoveryPublisher.java
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

import tigase.bot.runtime.AbstractXmppBridge;
import tigase.jaxmpp.core.client.xmpp.modules.SoftwareVersionModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule;
import tigase.jaxmpp.j2se.Jaxmpp;

import java.net.InetAddress;

/**
 * Bean provides Tigase IoT discovery information to <class>DiscoveryModule</class>.
 */
public class DiscoveryPublisher extends AbstractXmppBridge {

	private String hostname;

	public DiscoveryPublisher() {
		try {
			hostname = InetAddress.getLocalHost().getCanonicalHostName();
		} catch (Exception ex) {
			hostname = "Unknown";
		}
	}

	@Override
	protected void jaxmppConnected(Jaxmpp jaxmpp) {
		jaxmpp.getSessionObject().setUserProperty(DiscoveryModule.IDENTITY_CATEGORY_KEY, "device");
		jaxmpp.getSessionObject().setUserProperty(DiscoveryModule.IDENTITY_TYPE_KEY, "iot");
		jaxmpp.getSessionObject().setUserProperty(SoftwareVersionModule.NAME_KEY, hostname);
	}

	@Override
	protected void jaxmppDisconnected(Jaxmpp jaxmpp) {

	}
}
