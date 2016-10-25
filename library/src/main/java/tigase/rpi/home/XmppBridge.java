package tigase.rpi.home;

import tigase.jaxmpp.core.client.XmppModule;
import tigase.kernel.beans.Bean;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by andrzej on 23.10.2016.
 */
@Bean(name = "xmppBridge", exportable = true)
public interface XmppBridge {

	default Set<Class<? extends XmppModule>> getRequiredXmppModules() {
		RequiredXmppModules annotation = this.getClass().getAnnotation(RequiredXmppModules.class);
		if (annotation == null) {
			return Collections.emptySet();
		}

		return new HashSet<>(Arrays.asList(annotation.value()));
	}

}
