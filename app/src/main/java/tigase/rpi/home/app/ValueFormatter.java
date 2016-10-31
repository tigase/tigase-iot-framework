package tigase.rpi.home.app;

import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;

/**
 * Created by andrzej on 30.10.2016.
 */
public interface ValueFormatter<T> {

	Class<T> getSupportedClass();

	default boolean isSupported(Object o) {
		return getSupportedClass().isAssignableFrom(o.getClass());
	}

	Element toElement(T value) throws JaxmppException;

	T fromElement(Element elem) throws JaxmppException;

}
