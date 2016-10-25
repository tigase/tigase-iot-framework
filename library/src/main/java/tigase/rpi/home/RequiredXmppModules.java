package tigase.rpi.home;

import tigase.jaxmpp.core.client.XmppModule;

import java.lang.annotation.*;

/**
 * Created by andrzej on 22.10.2016.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface RequiredXmppModules {

	Class<? extends XmppModule>[] value();

}
