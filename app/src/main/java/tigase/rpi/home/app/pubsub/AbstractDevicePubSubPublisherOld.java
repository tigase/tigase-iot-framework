package tigase.rpi.home.app.pubsub;

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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by andrzej on 25.10.2016.
 */
public abstract class AbstractDevicePubSubPublisherOld
		extends AbstractPubSubPublisher {

	private static final Logger log = Logger.getLogger(AbstractDevicePubSubPublisherOld.class.getCanonicalName());

	private Set<String> nodesReady = new HashSet<>();

	public AbstractDevicePubSubPublisherOld() {
		rootNode = "devices";
	}

	protected abstract String getDeviceId();

	protected abstract void nodesReady();

	protected boolean areNodesReady() {
		if (!nodesReady.contains(rootNode))
			return false;
		if (!nodesReady.contains(getDeviceNode()))
			return false;
		if (!nodesReady.contains(getDeviceNode()+"/state"))
			return false;
		if (!nodesReady.contains(getDeviceNode()+"/config"))
			return false;
		return true;
	}

	protected void ifReady(Runnable run) {
		if (areNodesReady()) {
			run.run();
		}
	}

	@Override
	protected void jaxmppConnected(Jaxmpp jaxmpp) {
		if (!areNodesReady()) {
			ensureDeviceNodeExists(jaxmpp);
			return;
		}

		nodesReady();
	}

	@Override
	protected void jaxmppDisconnected(Jaxmpp jaxmpp) {

	}

	protected String getDeviceNode() {
		return rootNode + "/" + getDeviceId();
	}

	public void publishState(Element state) throws IllegalStateException {
		if (!areNodesReady()) {
			throw new IllegalStateException("Nodes are not ready yet!");
		}

		xmppService.getAllConnections().forEach(jaxmpp -> {
			try {
				JID pubsubJid = getPubsubJid(jaxmpp);
				String stateNode = getDeviceNode() + "/state";
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
													 new Object[]{AbstractDevicePubSubPublisherOld.this.name, pubsubJid,
																  stateNode, itemId});
										 }
									 });
			} catch (JaxmppException ex) {
				log.log(Level.WARNING, this.name + ", item publication failed", ex);
			}
		});
	}

	protected void ensureDeviceNodeExists(Jaxmpp jaxmpp) {
		try {
			DiscoveryModule discoveryModule = jaxmpp.getModule(DiscoveryModule.class);
			JID pubsubJid = getPubsubJid(jaxmpp);
			discoveryModule.getItems(pubsubJid, rootNode, new DiscoveryModule.DiscoItemsAsyncCallback() {
				@Override
				public void onInfoReceived(String node, ArrayList<DiscoveryModule.Item> items) throws XMLException {
					if (items.isEmpty()) {
						createRootNode(jaxmpp, pubsubJid);
					} else if (!items.stream().anyMatch(item -> getDeviceNode().equals(item.getNode()))) {
						nodesReady.add(rootNode);
						createDeviceNode(jaxmpp, pubsubJid, getDeviceNode());
					} else {
						nodesReady.add(rootNode);
						nodesReady.add(getDeviceNode());
						ensureDeviceSubnodesExist(jaxmpp, pubsubJid, getDeviceNode());
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

			createNode(jaxmpp, pubsubJid, rootNode, config, () -> createDeviceNode(jaxmpp, pubsubJid, getDeviceNode()));
		} catch (JaxmppException ex) {
			log.log(Level.WARNING, this.name + ", failed to create device node", ex);
		}
	}

	protected void createDeviceNode(Jaxmpp jaxmpp, JID pubsubJid, String node) {
		try {
			JabberDataElement config = new JabberDataElement(XDataType.submit);
			config.addTextSingleField("pubsub#title", getDeviceId());
			config.addTextSingleField("pubsub#node_type", "collection");
			config.addTextSingleField("pubsub#access_model", "presence");
			config.addTextSingleField("pubsub#persist_items", "1");
			config.addTextSingleField("pubsub#collection", rootNode);

			createNode(jaxmpp, pubsubJid, node, config, () -> {
				createDeviceStateNode(jaxmpp, pubsubJid, node + "/state");
				createDeviceConfigurationNode(jaxmpp, pubsubJid, node + "/config");
			});
		} catch (JaxmppException ex) {
			log.log(Level.WARNING, this.name + ", failed to create device node", ex);
		}
	}

	protected void ensureDeviceSubnodesExist(Jaxmpp jaxmpp, JID pubsubJid, String deviceNode) {
		try {
			DiscoveryModule discoveryModule = jaxmpp.getModule(DiscoveryModule.class);
			discoveryModule.getItems(pubsubJid, deviceNode, new DiscoveryModule.DiscoItemsAsyncCallback() {
				@Override
				public void onInfoReceived(String node, ArrayList<DiscoveryModule.Item> items) throws XMLException {
					nodesReady.addAll(items.stream().map(item -> item.getNode()).collect(Collectors.toSet()));

					String stateNode = getDeviceNode() + "/state";
					if (!nodesReady.contains(stateNode)) {
						createDeviceStateNode(jaxmpp, pubsubJid, stateNode);
					}

					String configNode = getDeviceNode() + "/config";
					if (!nodesReady.contains(configNode)) {
						createDeviceConfigurationNode(jaxmpp, pubsubJid, configNode);
					}

					ifReady(() -> nodesReady());
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

	protected void createDeviceStateNode(Jaxmpp jaxmpp, JID pubsubJid, String stateNode) {
		try {
			JabberDataElement config = new JabberDataElement(XDataType.submit);
			config.addTextSingleField("pubsub#title", "State");
			config.addTextSingleField("pubsub#node_type", "leaf");
			config.addTextSingleField("pubsub#access_model", "presence");
			config.addTextSingleField("pubsub#persist_items", "1");
			config.addTextSingleField("pubsub#collection", getDeviceNode());

			createNode(jaxmpp, pubsubJid, stateNode, config, () -> ifReady(() -> nodesReady()));
		} catch (JaxmppException ex) {
			log.log(Level.WARNING, this.name + ", failed to create device node", ex);
		}
	}

	protected void createDeviceConfigurationNode(Jaxmpp jaxmpp, JID pubsubJid, String configNode) {
		try {
			JabberDataElement config = new JabberDataElement(XDataType.submit);
			config.addTextSingleField("pubsub#title", "Configuration");
			config.addTextSingleField("pubsub#node_type", "leaf");
			config.addTextSingleField("pubsub#access_model", "presence");
			config.addTextSingleField("pubsub#persist_items", "1");
			config.addTextSingleField("pubsub#collection", getDeviceNode());

			createNode(jaxmpp, pubsubJid, configNode, config, () -> ifReady(() -> nodesReady()));
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
								new Object[]{AbstractDevicePubSubPublisherOld.this.name, errorCondition,
											 pubSubErrorCondition});
					}

					@Override
					public void onSuccess(Stanza stanza) throws JaxmppException {
						log.log(Level.FINEST, "{0}, node {1}/{2} created",
								new Object[]{AbstractDevicePubSubPublisherOld.this.name, pubsubJid, node});
						nodesReady.add(node);
						if (nodeCreated != null) {
							nodeCreated.nodeCreated();
						}
					}

					@Override
					public void onTimeout() throws JaxmppException {
						log.log(Level.WARNING, "{0}, failed to create device node - {1}",
								new Object[]{AbstractDevicePubSubPublisherOld.this.name, "request timeout!"});
					}
				});
	}

	public static interface NodeCreated {

		void nodeCreated();

	}
}
