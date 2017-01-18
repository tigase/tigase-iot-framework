package tigase.rpi.home.app;

import tigase.bot.iot.IDevice;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.forms.JabberDataElement;
import tigase.jaxmpp.core.client.xmpp.forms.XDataType;
import tigase.kernel.beans.Inject;
import tigase.rpi.home.IConfigurationAware;
import tigase.rpi.home.app.pubsub.AbstractConfigurationPubSubManager;
import tigase.rpi.home.app.pubsub.PubSubNodesManager;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by andrzej on 04.11.2016.
 */
public class DeviceConfigurationPubSubManager
		extends AbstractConfigurationPubSubManager<IConfigurationAware> {

	private static final Logger log = Logger.getLogger(DeviceConfigurationPubSubManager.class.getCanonicalName());

	@Inject
	private PubSubNodesManager pubSubNodesManager;

	public DeviceConfigurationPubSubManager() {
		super();
		rootNode = "devices";
	}

	@Override
	public void setConfigurationAware(List<IConfigurationAware> configurationAware) {
		super.setConfigurationAware(
				configurationAware.stream().filter(aware -> aware instanceof IDevice).collect(Collectors.toList()));
	}

	protected JabberDataElement prepareRootNodeConfig() throws JaxmppException {
		JabberDataElement config = new JabberDataElement(XDataType.submit);
		config.addTextSingleField("pubsub#title", "Devices");
		config.addTextSingleField("pubsub#node_type", "collection");
		config.addTextSingleField("pubsub#access_model", pubSubNodesManager.isPEP() ? "presence" : "open");
		config.addTextSingleField("pubsub#presence_based_delivery", "true");
		config.addTextSingleField("pubsub#persist_items", "1");

		return config;
	}

	protected JabberDataElement prepareNodeConfig(IConfigurationAware configurationAware) throws JaxmppException {
		JabberDataElement config = new JabberDataElement(XDataType.submit);
		config.addTextSingleField("pubsub#title", configurationAware.getName());
		config.addTextSingleField("pubsub#node_type", "collection");
		config.addTextSingleField("pubsub#access_model", pubSubNodesManager.isPEP() ? "presence" : "open");
		config.addTextSingleField("pubsub#presence_based_delivery", "true");
		config.addTextSingleField("pubsub#persist_items", "1");
		config.addTextSingleField("pubsub#collection", rootNode);

		return config;
	}

	protected JabberDataElement prepareConfigNodeConfig(String collection) throws JaxmppException {
		JabberDataElement config = new JabberDataElement(XDataType.submit);
		config.addTextSingleField("pubsub#title", "Configuration");
		config.addTextSingleField("pubsub#node_type", "leaf");
		config.addTextSingleField("pubsub#access_model", pubSubNodesManager.isPEP() ? "presence" : "open");
		config.addTextSingleField("pubsub#presence_based_delivery", "true");
		config.addTextSingleField("pubsub#persist_items", "1");
		config.addTextSingleField("pubsub#collection", collection);

		return config;
	}
}
