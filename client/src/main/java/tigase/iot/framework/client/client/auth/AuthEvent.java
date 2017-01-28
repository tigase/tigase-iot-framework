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
