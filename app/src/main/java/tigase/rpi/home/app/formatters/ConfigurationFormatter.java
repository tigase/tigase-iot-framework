package tigase.rpi.home.app.formatters;

import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xmpp.forms.JabberDataElement;
import tigase.kernel.beans.Bean;
import tigase.kernel.core.Kernel;
import tigase.rpi.home.app.pubsub.AbstractConfigurationPubSubManager;

import java.time.LocalDateTime;

/**
 * Created by andrzej on 05.11.2016.
 */
@Bean(name = "configurationFormatter", parent = Kernel.class, exportable = true)
public class ConfigurationFormatter
		extends AbstractValueFormatter<AbstractConfigurationPubSubManager.ConfigValue> {

	public ConfigurationFormatter() {
		super("configuration", AbstractConfigurationPubSubManager.ConfigValue.class);
	}

	@Override
	public Element toElement(AbstractConfigurationPubSubManager.ConfigValue value) throws JaxmppException {
		Element timestampElem = createTimestampElement(value);
		timestampElem.addChild(value.getValue());
		return timestampElem;
	}

	@Override
	public AbstractConfigurationPubSubManager.ConfigValue fromElement(Element elem) throws JaxmppException {
		try {
			LocalDateTime timestamp = parseTimestampElement(elem);
			Element x = elem.getFirstChild("x");
			if (x == null) {
				return null;
			}

			JabberDataElement value = new JabberDataElement(x);
			return new AbstractConfigurationPubSubManager.ConfigValue(value, timestamp);
		} catch (JaxmppException ex) {
			return null;
		}
	}
}
