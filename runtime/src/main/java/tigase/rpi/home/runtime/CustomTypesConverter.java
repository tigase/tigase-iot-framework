package tigase.rpi.home.runtime;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.kernel.DefaultTypesConverter;

import java.lang.reflect.Type;

/**
 * Created by andrzej on 22.10.2016.
 */
public class CustomTypesConverter extends DefaultTypesConverter {

	@Override
	protected <T> T customConversion(Object value, Class<T> expectedType, Type genericType) {
		if (expectedType.equals(JID.class)) {
			return (T) JID.jidInstance(value.toString());
		} else if (expectedType.equals(BareJID.class)) {
			return (T) BareJID.bareJIDInstance(value.toString());
		}
		return super.customConversion(value, expectedType, genericType);
	}
}
