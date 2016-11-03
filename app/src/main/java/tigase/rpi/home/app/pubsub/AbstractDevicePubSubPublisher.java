package tigase.rpi.home.app.pubsub;

import tigase.bot.IDevice;
import tigase.bot.runtime.AbstractPubSubPublisher;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.forms.JabberDataElement;
import tigase.jaxmpp.core.client.xmpp.forms.XDataType;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubAsyncCallback;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubErrorCondition;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.j2se.Jaxmpp;
import tigase.kernel.beans.Inject;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by andrzej on 25.10.2016.
 */
public abstract class AbstractDevicePubSubPublisher
		extends AbstractPubSubPublisher {

	private static final Logger log = Logger.getLogger(AbstractDevicePubSubPublisher.class.getCanonicalName());

	@Inject
	protected List<IDevice> devices;

	private Set<String> nodesReady = new HashSet<>();

	public AbstractDevicePubSubPublisher() {
		rootNode = "devices";
	}

	protected abstract void nodesReady(IDevice device);

	protected boolean areNodesReady() {
		if (!nodesReady.contains(rootNode)) {
			return false;
		}

		return devices.stream().allMatch(device -> areNodesReady(device));
	}

	protected boolean areNodesReady(IDevice device) {
		return getNodesForDevice(device).stream().allMatch(node -> nodesReady.contains(node));
	}

	protected List<String> getNodesForDevice(IDevice device) {
		List<String> nodes = new ArrayList<>();

		nodes.add(getDeviceNode(device));
		nodes.add(getDeviceStateNode(device));
		nodes.add(getDeviceConfigNode(device));

		return nodes;
	}

	public void setDevices(List<IDevice> devices) {
		List<IDevice> oldDevices = this.devices;
		this.devices = devices;
		if (xmppService != null) {
			devices.stream().filter(device -> !oldDevices.contains(device)).forEach(device -> {
				xmppService.getAllConnections()
						.stream()
						.filter(jaxmpp -> jaxmpp.isConnected())
						.forEach(jaxmpp -> ensureDevicesNodeExists(jaxmpp, device));
			});
		}
	}

	protected void ifReady(Runnable run) {
		if (areNodesReady()) {
			run.run();
		}
	}

	protected void ifReadyForDevice(IDevice device, Runnable run) {
		if (areNodesReady(device)) {
			run.run();
		}
	}

	@Override
	protected void jaxmppConnected(Jaxmpp jaxmpp) {
		if (!areNodesReady()) {
			ensureDevicesNodeExists(jaxmpp, null);
			return;
		}

		devices.forEach(device -> nodesReady(device));
	}

	@Override
	protected void jaxmppDisconnected(Jaxmpp jaxmpp) {

	}

	protected String getDeviceNode(IDevice device) {
		return rootNode + "/" + device.getName();
	}

	protected String getDeviceStateNode(IDevice device) {
		return getDeviceNode(device) + "/state";
	}

	protected String getDeviceConfigNode(IDevice device) {
		return getDeviceNode(device) + "/config";
	}

	public void publishState(IDevice device, Element state) throws IllegalStateException {
		if (!areNodesReady(device)) {
			throw new IllegalStateException("Nodes are not ready yet for device " + device + " !");
		}

		xmppService.getAllConnections().forEach(jaxmpp -> {
			try {
				JID pubsubJid = getPubsubJid(jaxmpp);
				String stateNode = getDeviceStateNode(device);
				jaxmpp.getModule(PubSubModule.class)
						.publishItem(pubsubJid.getBareJid(), stateNode, null, state,
									 new PubSubModule.PublishAsyncCallback() {

										 @Override
										 public void onTimeout() throws JaxmppException {

										 }

										 @Override
										 protected void onEror(IQ response, XMPPException.ErrorCondition errorCondition,
															   PubSubErrorCondition pubSubErrorCondition)
												 throws JaxmppException {

										 }

										 @Override
										 public void onPublish(String itemId) {
											 log.log(Level.FINEST, "{0}, item published at {1}/{2} as {3}",
													 new Object[]{AbstractDevicePubSubPublisher.this.name, pubsubJid,
																  stateNode, itemId});
										 }
									 });
			} catch (JaxmppException ex) {
				log.log(Level.WARNING, this.name + ", item publication failed", ex);
			}
		});
	}

	protected void ensureDevicesNodeExists(Jaxmpp jaxmpp, IDevice device) {
		try {
			DiscoveryModule discoveryModule = jaxmpp.getModule(DiscoveryModule.class);
			JID pubsubJid = getPubsubJid(jaxmpp);
			discoveryModule.getItems(pubsubJid, rootNode, new DiscoveryModule.DiscoItemsAsyncCallback() {
				@Override
				public void onInfoReceived(String node, ArrayList<DiscoveryModule.Item> items) throws XMLException {
					if (items.isEmpty()) {
						createRootNode(jaxmpp, pubsubJid);
					} else {
						if (device == null) {
							nodesReady.clear();
						}
						nodesReady.addAll(items.stream().map(item -> item.getNode()).collect(Collectors.toSet()));

						if (device == null) {
							devices.stream().forEach(device -> {
								if (nodesReady.contains(getDeviceNode(device))) {
									ensureDeviceSubnodesExist(jaxmpp, pubsubJid, device);
								} else {
									createDeviceNode(jaxmpp, pubsubJid, device);
								}
							});
						} else {
							if (nodesReady.contains(getDeviceNode(device))) {
								ensureDeviceSubnodesExist(jaxmpp, pubsubJid, device);
							} else {
								createDeviceNode(jaxmpp, pubsubJid, device);
							}
						}
					}
				}

				@Override
				public void onError(Stanza stanza, XMPPException.ErrorCondition errorCondition) throws JaxmppException {
					if (errorCondition == XMPPException.ErrorCondition.item_not_found) {
						createRootNode(jaxmpp, pubsubJid);
					}
				}

				@Override
				public void onTimeout() throws JaxmppException {
					log.log(Level.WARNING, "{0}, node discovery timed out", name);
				}
			});
		} catch (JaxmppException ex) {
			log.log(Level.WARNING, this.name + ", failed to initialize publisher", ex);
		}
	}

	protected void createRootNode(Jaxmpp jaxmpp, JID pubsubJid) {
		try {
			JabberDataElement config = new JabberDataElement(XDataType.submit);
			config.addTextSingleField("pubsub#title", "Devices");
			config.addTextSingleField("pubsub#node_type", "collection");
			config.addTextSingleField("pubsub#access_model", "presence");
			config.addTextSingleField("pubsub#persist_items", "1");

			createNode(jaxmpp, pubsubJid, rootNode, config, () -> {
				devices.stream().forEach(device -> createDeviceNode(jaxmpp, pubsubJid, device));
			});
		} catch (JaxmppException ex) {
			log.log(Level.WARNING, this.name + ", failed to create device node", ex);
		}
	}

	protected void createDeviceNode(Jaxmpp jaxmpp, JID pubsubJid, IDevice device) {
		try {
			String node = getDeviceNode(device);

			JabberDataElement config = new JabberDataElement(XDataType.submit);
			config.addTextSingleField("pubsub#title", device.getName());
			config.addTextSingleField("pubsub#node_type", "collection");
			config.addTextSingleField("pubsub#access_model", "presence");
			config.addTextSingleField("pubsub#persist_items", "1");
			config.addTextSingleField("pubsub#collection", rootNode);

			createNode(jaxmpp, pubsubJid, node, config, () -> {
				createDeviceStateNode(jaxmpp, pubsubJid, device);
				createDeviceConfigurationNode(jaxmpp, pubsubJid, device);
			});
		} catch (JaxmppException ex) {
			log.log(Level.WARNING, this.name + ", failed to create device node", ex);
		}
	}

	protected void ensureDeviceSubnodesExist(Jaxmpp jaxmpp, JID pubsubJid, IDevice device) {
		try {
			DiscoveryModule discoveryModule = jaxmpp.getModule(DiscoveryModule.class);
			discoveryModule.getItems(pubsubJid, getDeviceNode(device), new DiscoveryModule.DiscoItemsAsyncCallback() {
				@Override
				public void onInfoReceived(String node, ArrayList<DiscoveryModule.Item> items) throws XMLException {
					nodesReady.addAll(items.stream().map(item -> item.getNode()).collect(Collectors.toSet()));

					String stateNode = getDeviceStateNode(device);
					if (!nodesReady.contains(stateNode)) {
						createDeviceStateNode(jaxmpp, pubsubJid, device);
					}

					String configNode = getDeviceConfigNode(device);
					if (!nodesReady.contains(configNode)) {
						createDeviceConfigurationNode(jaxmpp, pubsubJid, device);
					}

					ifReadyForDevice(device, () -> nodesReady(device));
				}

				@Override
				public void onError(Stanza stanza, XMPPException.ErrorCondition errorCondition) throws JaxmppException {
					log.log(Level.WARNING, "{0}, node discovery failed {1}", new Object[]{name, errorCondition});
				}

				@Override
				public void onTimeout() throws JaxmppException {
					log.log(Level.WARNING, "{0}, node discovery timed out", name);
				}
			});
		} catch (JaxmppException ex) {
			log.log(Level.WARNING, this.name + ", failed to initialize publisher", ex);
		}
	}

	protected void createDeviceStateNode(Jaxmpp jaxmpp, JID pubsubJid, IDevice device) {
		try {
			JabberDataElement config = new JabberDataElement(XDataType.submit);
			config.addTextSingleField("pubsub#title", "State");
			config.addTextSingleField("pubsub#node_type", "leaf");
			config.addTextSingleField("pubsub#access_model", "presence");
			config.addTextSingleField("pubsub#persist_items", "1");
			config.addTextSingleField("pubsub#collection", getDeviceNode(device));

			createNode(jaxmpp, pubsubJid, getDeviceStateNode(device), config,
					   () -> ifReadyForDevice(device, () -> nodesReady(device)));
		} catch (JaxmppException ex) {
			log.log(Level.WARNING, this.name + ", failed to create device node", ex);
		}
	}

	protected void createDeviceConfigurationNode(Jaxmpp jaxmpp, JID pubsubJid, IDevice device) {
		try {
			JabberDataElement config = new JabberDataElement(XDataType.submit);
			config.addTextSingleField("pubsub#title", "Configuration");
			config.addTextSingleField("pubsub#node_type", "leaf");
			config.addTextSingleField("pubsub#access_model", "presence");
			config.addTextSingleField("pubsub#persist_items", "1");
			config.addTextSingleField("pubsub#collection", getDeviceNode(device));

			createNode(jaxmpp, pubsubJid, getDeviceConfigNode(device), config,
					   () -> ifReadyForDevice(device, () -> nodesReady(device)));
		} catch (JaxmppException ex) {
			log.log(Level.WARNING, this.name + ", failed to create device node", ex);
		}
	}

	public void createNode(Jaxmpp jaxmpp, JID pubsubJid, String node, JabberDataElement config, NodeCreated nodeCreated)
			throws JaxmppException {
		jaxmpp.getModule(PubSubModule.class)
				.createNode(pubsubJid.getBareJid(), node, config, new PubSubAsyncCallback() {
					@Override
					protected void onEror(IQ response, XMPPException.ErrorCondition errorCondition,
										  PubSubErrorCondition pubSubErrorCondition) throws JaxmppException {
						log.log(Level.WARNING, "{0}, failed to create device node - {1} {2}",
								new Object[]{AbstractDevicePubSubPublisher.this.name, errorCondition,
											 pubSubErrorCondition});
					}

					@Override
					public void onSuccess(Stanza stanza) throws JaxmppException {
						log.log(Level.FINEST, "{0}, node {1}/{2} created",
								new Object[]{AbstractDevicePubSubPublisher.this.name, pubsubJid, node});
						nodesReady.add(node);
						if (nodeCreated != null) {
							nodeCreated.nodeCreated();
						}
					}

					@Override
					public void onTimeout() throws JaxmppException {
						log.log(Level.WARNING, "{0}, failed to create device node - {1}",
								new Object[]{AbstractDevicePubSubPublisher.this.name, "request timeout!"});
					}
				});
	}

	public static interface NodeCreated {

		void nodeCreated();

	}
}
