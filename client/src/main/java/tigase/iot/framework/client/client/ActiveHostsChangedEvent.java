/*
 * ActiveHostsChangedEvent.java
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
package tigase.iot.framework.client.client;

import com.google.gwt.event.shared.EventHandler;
import com.google.web.bindery.event.shared.Event;
import java.util.Map;
import tigase.iot.framework.client.client.ActiveHostsChangedEvent.Handler;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule;

/**
 *
 * @author andrzej
 */
public class ActiveHostsChangedEvent extends Event<Handler> {

	public static final Type<Handler> TYPE = new Type<Handler>();

	private final Map<JID, DiscoveryModule.Identity> activeHosts;

	public ActiveHostsChangedEvent(Map<JID, DiscoveryModule.Identity> devicesInfo) {
		this.activeHosts = devicesInfo;
	}

	@Override
	public Type<Handler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(Handler handler) {
		handler.onActiveHostsChange(activeHosts);
	}

	public interface Handler extends EventHandler {

		void onActiveHostsChange(Map<JID, DiscoveryModule.Identity> activeHosts);

	}

}
