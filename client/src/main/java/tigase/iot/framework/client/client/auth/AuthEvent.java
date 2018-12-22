/*
 * AuthEvent.java
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.auth;

import com.google.web.bindery.event.shared.Event;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.xmpp.modules.auth.SaslModule.SaslError;
import tigase.iot.framework.client.client.auth.AuthEvent.Handler;

/**
 *
 * @author andrzej
 */
public class AuthEvent extends Event<Handler> {

	public static final Type<Handler> TYPE = new Type<Handler>();

	private final JID jid;
	private final String message;
	private final SaslError saslError;

	public AuthEvent(JID jid) {
		this(jid, null, null);
	}

	public AuthEvent(JID jid, String message, SaslError saslError) {
		this.jid = jid;
		this.message = message;
		this.saslError = saslError;
	}

	@Override
	public Type<Handler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(Handler handler) {
		if (jid == null) {
			if (saslError != null) {
				handler.deauthenticated(message, saslError);
			} else {
				handler.deauthenticated(null, null);
			}
		} else {
			handler.authenticated(jid);
		}
	}

	public interface Handler {

		void authenticated(JID jid);

		void deauthenticated(String msg, SaslError saslError);

	}
}
