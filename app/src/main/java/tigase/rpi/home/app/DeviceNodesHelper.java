package tigase.rpi.home.app;

import tigase.bot.IDevice;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.forms.JabberDataElement;
import tigase.jaxmpp.core.client.xmpp.forms.XDataType;
import tigase.rpi.home.app.pubsub.PubSubNodesManager;

/**
 * Created by andrzej on 04.11.2016.
 */
public class DeviceNodesHelper {

	public static PubSubNodesManager.Node createDevicesRootNode(String node, String name) throws JaxmppException {
		JabberDataElement config = new JabberDataElement(XDataType.submit);
		config.addTextSingleField("pubsub#title", name);
		config.addTextSingleField("pubsub#node_type", "collection");
		config.addTextSingleField("pubsub#access_model", "presence");
		config.addTextSingleField("pubsub#persist_items", "1");

		return new PubSubNodesManager.Node(node, config);
	}

	public static PubSubNodesManager.Node createDeviceNode(String rootNode, IDevice device) throws JaxmppException {
		JabberDataElement config = new JabberDataElement(XDataType.submit);
		config.addTextSingleField("pubsub#title", device.getName());
		config.addTextSingleField("pubsub#node_type", "collection");
		config.addTextSingleField("pubsub#access_model", "presence");
		config.addTextSingleField("pubsub#persist_items", "1");
		config.addTextSingleField("pubsub#collection", rootNode);
		String node = getDeviceNodeName(rootNode, device);
		return new PubSubNodesManager.Node(node, config);
	}

	public static PubSubNodesManager.Node createDeviceStateNode(String rootNode, IDevice device) throws JaxmppException {
		String deviceNode = getDeviceNodeName(rootNode, device);
		JabberDataElement config = new JabberDataElement(XDataType.submit);
		config.addTextSingleField("pubsub#title", "State");
		config.addTextSingleField("pubsub#node_type", "leaf");
		config.addTextSingleField("pubsub#access_model", "presence");
		config.addTextSingleField("pubsub#persist_items", "1");
		config.addTextSingleField("pubsub#collection", deviceNode);
		String node = getDeviceStateNodeName(rootNode, device);
		return new PubSubNodesManager.Node(node, config);
	}

	public static String getDeviceNodeName(String rootNode, IDevice device) {
		return rootNode + "/" + device.getName();
	}

	public static String getDeviceConfigNodeName(String rootNode, IDevice device) {
		return getDeviceNodeName(rootNode, device) + "/config";
	}

	public static String getDeviceStateNodeName(String rootNode, IDevice device) {
		return getDeviceNodeName(rootNode, device) + "/state";
	}

	public static String getDeviceIdFromNode(String node) {
		String[] parts = node.split("/");
		if (parts.length > 1) {
			return parts[1];
		}
		return null;
	}
}
