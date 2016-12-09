/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.rpi.home.client.client.auth;

import com.google.gwt.event.shared.EventHandler;
import com.google.web.bindery.event.shared.Event;

import tigase.jaxmpp.core.client.JID;
import tigase.rpi.home.client.client.auth.AuthRequestEvent.Handler;

/**
 *
 * @author andrzej
 */
public class AuthRequestEvent extends Event<Handler> {

        public static final Type<Handler> TYPE = new Type<Handler>();
        
        private final JID jid;
        private final String password;
        private final String url;
        
        public AuthRequestEvent(JID j, String p) {
                this.jid = j;
                this.password = p;                
                this.url = null;
        }

        public AuthRequestEvent(JID j, String p, String url) {
                this.jid = j;
                this.password = p;
                this.url = url;
        }
        
        @Override
        public Type<Handler> getAssociatedType() {
                return TYPE;
        }


        @Override
        protected void dispatch(Handler handler) {
                handler.authenticate(jid, password, url);
        }
 
	public interface Handler extends EventHandler {
		
		void authenticate(JID jid, String password, String boshUrl);

	}
}
