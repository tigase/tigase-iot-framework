package tigase.rpi.home;

import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.j2se.Jaxmpp;

import java.util.Collection;
import java.util.Map;

/**
 * Created by andrzej on 22.10.2016.
 */
public interface XmppService {

	Jaxmpp getConnection(String name);

	Jaxmpp getConnection(SessionObject sessionObject);

	Map<String, Jaxmpp> getNamedConnections();

	Collection<Jaxmpp> getAnonymousConnections();

	Collection<Jaxmpp> getAllConnections();

}
