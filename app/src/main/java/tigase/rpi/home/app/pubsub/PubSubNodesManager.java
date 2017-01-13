package tigase.rpi.home.app.pubsub;

import tigase.bot.RequiredXmppModules;
import tigase.bot.runtime.AbstractPubSubPublisher;
import tigase.bot.runtime.XmppService;
import tigase.eventbus.HandleEvent;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.XmppModule;
import tigase.jaxmpp.core.client.criteria.Criteria;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.forms.JabberDataElement;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubAsyncCallback;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubErrorCondition;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.j2se.Jaxmpp;
import tigase.kernel.beans.Inject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by andrzej on 04.11.2016.
 */
@RequiredXmppModules({PubSubModule.class, PresenceModule.class, DiscoveryModule.class, CapabilitiesModule.class,
					  PubSubNodesManager.FeatureProviderModule.class})
public class PubSubNodesManager
		extends AbstractPubSubPublisher {

	private static final Logger log = Logger.getLogger(PubSubNodesManager.class.getCanonicalName());

	private static final String PUBSUB_PUBLISH_QUEUE = "pubsubPublishQueue";

	protected Set<String> features = new CopyOnWriteArraySet<>();
	protected Set<String> observedNodes;
	@Inject(nullAllowed = true)
	protected List<NodesObserver> observers;

	@Inject
	private List<PubSubNodeAware> nodesAware;

	private List<Node> requiredNodes = new ArrayList<>();

	public void setNodesAware(List<PubSubNodeAware> nodesAware) {
		if (nodesAware == null) {
			nodesAware = new ArrayList<PubSubNodeAware>();
		}
		this.nodesAware = nodesAware;

		updateRequiredNodes();
	}

	public void setObservers(List<NodesObserver> observers) {
		if (observers == null) {
			observers = new ArrayList<>();
		}

		this.observers = observers;

		updateObservedNodes();;
	}

	@Override
	public void initialize() {
		super.initialize();
		if (requiredNodes.isEmpty()) {
			updateRequiredNodes();
		}
	}

	public synchronized void updateObservedNodes() {
		this.features.clear();
		this.observedNodes = observers.stream().flatMap(o -> o.getObservedNodes().stream()).collect(Collectors.toSet());
		this.observedNodes.stream().map(node -> node + "+notify").forEach(feature -> this.features.add(feature));

		if (xmppService != null) {
			xmppService.getAllConnections().stream().filter(jaxmpp -> jaxmpp.isConnected()).forEach(jaxmpp -> {
				PresenceModule presenceModule = jaxmpp.getModule(PresenceModule.class);
				try {
					presenceModule.sendInitialPresence();
				} catch (JaxmppException ex) {
					log.log(Level.WARNING, "failed to send update presence", ex);
				}
			});
		}
	}

	public synchronized void updateRequiredNodes() {
		if (nodesAware == null) {
			return;
		}

		List<Node> oldRequiredNodes = requiredNodes;

		Map<String, List<Node>> groupedNodes = nodesAware.stream()
				.flatMap(nodeAware -> nodeAware.getRequiredNodes().stream())
				.collect(Collectors.groupingBy(Node::getNodeName));

		List<Node> tmp = new ArrayList<>();
		groupedNodes.forEach((node, nodes) -> {
			if (nodes.size() == 1) {
				tmp.addAll(nodes);
				return;
			}

			tmp.add(Node.merge(nodes));
		});
		requiredNodes = tmp;

		if (xmppService != null) {
			xmppService.getAllConnections()
					.stream()
					.filter(jaxmpp -> jaxmpp.isConnected())
					.forEach(this::ensureNodeExists);
		}
	}

	public void ensureNodeExists(Jaxmpp jaxmpp) {
		List<Node> requiredNode = this.requiredNodes;
		List<String> existingNodes = jaxmpp.getSessionObject().getUserProperty("EXISTING_PUBSUB_NODES");
		if (existingNodes == null) {
			existingNodes = new CopyOnWriteArrayList<>();
			jaxmpp.getSessionObject().setUserProperty("EXISTING_PUBSUB_NODES", existingNodes);
			retrieveNodes(jaxmpp, null, () -> ensureNodeExists(jaxmpp));
		} else {
			requiredNodes.forEach(node -> ensureNodeExists(jaxmpp, node));
		}
	}

	public void ensureNodeExists(Jaxmpp jaxmpp, Node node) {
		List<String> existingNodes = jaxmpp.getSessionObject().getUserProperty("EXISTING_PUBSUB_NODES");
		if (!existingNodes.contains(node.getNodeName())) {
			createNodeAndSubnodes(jaxmpp, node);
		} else {
			eventBus.fire(new NodeReady(jaxmpp, getPubsubJid(jaxmpp), node.getNodeName()));
			if (!node.getChildren().isEmpty()) {
				retrieveNodes(jaxmpp, node.getNodeName(), () -> {
					node.getChildren().forEach(child -> ensureNodeExists(jaxmpp, child));
				});
			}
		}
	}

	public void createNodeAndSubnodes(Jaxmpp jaxmpp, Node node) {
		try {
			JID pubsubJid = getPubsubJid(jaxmpp);
			createNode(jaxmpp, pubsubJid, node.getNodeName(), node.getConfig(), () -> {
				node.getChildren().forEach(child -> createNodeAndSubnodes(jaxmpp, child));
			});
		} catch (JaxmppException ex) {
			log.log(Level.WARNING, "failed to ensure that node exists");
		}
	}

	public void publishItem(String node, String itemId, Element payload) {
		Item item = new Item(node, itemId, payload);
		xmppService.getAllConnections().forEach(jaxmpp -> {
			Queue<Item> queue;
			synchronized (jaxmpp) {
				queue = jaxmpp.getSessionObject().getUserProperty(PUBSUB_PUBLISH_QUEUE);
				if (queue == null) {
					queue = new ArrayDeque<>();
					jaxmpp.getSessionObject().setUserProperty(PUBSUB_PUBLISH_QUEUE, queue);
				}
			}
			synchronized (queue) {
				queue.offer(item);
			}
			publishWaitingItems(jaxmpp);
		});
	}

	private void publishWaitingItems(Jaxmpp jaxmpp) {
		Queue<Item> queue = jaxmpp.getSessionObject().getUserProperty(PUBSUB_PUBLISH_QUEUE);
		if (queue == null) {
			return;
		}

		synchronized (queue) {
			Collection<String> existingNodes = jaxmpp.getSessionObject().getUserProperty("EXISTING_PUBSUB_NODES");
			if (existingNodes == null) {
				return;
			}

			final JID pubsubJid = getPubsubJid(jaxmpp);
			PubSubModule pubSubModule = jaxmpp.getModule(PubSubModule.class);

			for (Iterator<Item> it = queue.iterator(); it.hasNext(); ) {
				final Item item = it.next();
				if (!existingNodes.contains(item.node)) {
					continue;
				}

				if (!jaxmpp.isConnected()) {
					return;
				}

				try {
					pubSubModule.publishItem(pubsubJid.getBareJid(), item.node, item.itemId, item.payload,
											 new PubSubModule.PublishAsyncCallback() {

												 @Override
												 public void onTimeout() throws JaxmppException {

												 }

												 @Override
												 protected void onEror(IQ response,
																	   XMPPException.ErrorCondition errorCondition,
																	   PubSubErrorCondition pubSubErrorCondition)
														 throws JaxmppException {

												 }

												 @Override
												 public void onPublish(String itemId) {
													 log.log(Level.FINEST, "{0}, item published at {1}/{2} as {3}",
															 new Object[]{PubSubNodesManager.this.name, pubsubJid,
																		  item.node, itemId});
												 }
											 });
					it.remove();
				} catch (JaxmppException ex) {
					log.log(Level.WARNING, this.name + ", item publication failed", ex);
				}
			}
		}
	}

	public void retrieveNodes(Jaxmpp jaxmpp, String node, Runnable callback) {
		try {
			DiscoveryModule discoveryModule = jaxmpp.getModule(DiscoveryModule.class);
			JID pubsubJid = getPubsubJid(jaxmpp);
			discoveryModule.getItems(pubsubJid, node, new DiscoveryModule.DiscoItemsAsyncCallback() {
				@Override
				public void onInfoReceived(String node, ArrayList<DiscoveryModule.Item> items) throws XMLException {
					List<String> existingNodes = jaxmpp.getSessionObject().getUserProperty("EXISTING_PUBSUB_NODES");
					items.forEach(item -> existingNodes.add(item.getNode()));
					callback.run();
				}

				@Override
				public void onError(Stanza stanza, XMPPException.ErrorCondition errorCondition) throws JaxmppException {
					callback.run();
				}

				@Override
				public void onTimeout() throws JaxmppException {
					log.log(Level.WARNING, "{0}, node discovery timed out", name);
				}
			});
		} catch (JaxmppException ex) {
			log.log(Level.WARNING, this.name + ", failed to retieve subnodes of " + node, ex);
		}
	}

	public void createNode(Jaxmpp jaxmpp, JID pubsubJid, String node, JabberDataElement config, Runnable callback)
			throws JaxmppException {
		PubSubModule pubSubModule = jaxmpp.getModule(PubSubModule.class);
		pubSubModule.createNode(pubsubJid.getBareJid(), node, config, new PubSubAsyncCallback() {
			@Override
			protected void onEror(IQ response, XMPPException.ErrorCondition errorCondition,
								  PubSubErrorCondition pubSubErrorCondition) throws JaxmppException {
				log.log(Level.WARNING, "{0}, failed to create device node - {1} {2}",
						new Object[]{PubSubNodesManager.this.name, errorCondition, pubSubErrorCondition});
			}

			@Override
			public void onSuccess(Stanza stanza) throws JaxmppException {
				log.log(Level.FINEST, "{0}, node {1}/{2} created",
						new Object[]{PubSubNodesManager.this.name, pubsubJid, node});

				List<String> existingNodes = jaxmpp.getSessionObject().getUserProperty("EXISTING_PUBSUB_NODES");
				existingNodes.add(node);

				eventBus.fire(new NodeReady(jaxmpp, pubsubJid, node));
				if (callback != null) {
					callback.run();
				}
			}

			@Override
			public void onTimeout() throws JaxmppException {
				log.log(Level.WARNING, "{0}, failed to create device node - {1}",
						new Object[]{PubSubNodesManager.this.name, "request timeout!"});
			}
		});
	}

	@HandleEvent
	private void nodeReady(NodeReady nodeReady) {
		publishWaitingItems(nodeReady.jaxmpp);
	}

	@HandleEvent
	public void onJaxmppAdded(XmppService.JaxmppAddedEvent event) {
		event.jaxmpp.getModule(FeatureProviderModule.class).setFeatures(this, features);
	}

	@Override
	protected void jaxmppConnected(Jaxmpp jaxmpp) {
		FeatureProviderModule module = jaxmpp.getModule(FeatureProviderModule.class);
		module.setFeatures(this, features);
		ensureNodeExists(jaxmpp);
	}

	@Override
	protected void jaxmppDisconnected(Jaxmpp jaxmpp) {

	}

	public interface PubSubNodeAware {

		List<Node> getRequiredNodes();

	}

	public interface NodesObserver {

		List<String> getObservedNodes();

	}

	public static class Item {

		private final String node;
		private final String itemId;
		private final Element payload;

		public Item(String node, String itemId, Element payload) {
			this.node = node;
			this.itemId = itemId;
			this.payload = payload;
		}

	}

	public static class Node {

		private String nodeName;
		private JabberDataElement config;
		private Collection<Node> children;

		public Node(String nodeName, JabberDataElement config, Collection<Node> children) {
			this.nodeName = nodeName;
			this.config = config;
			this.children = children;
		}

		public Node(String nodeName, JabberDataElement config) {
			this(nodeName, config, new ArrayList<>());
		}

		public String getNodeName() {
			return nodeName;
		}

		public JabberDataElement getConfig() {
			return config;
		}

		public void addChild(Node child) {
			children.add(child);
		}

		public void removeChild(Node child) {
			children.remove(child);
		}

		public Collection<Node> getChildren() {
			return Collections.unmodifiableCollection(children);
		}

		public static Node merge(List<Node> nodes) {
			String nodeName = nodes.get(0).nodeName;
			JabberDataElement config = nodes.get(0).config;

			Map<String, List<Node>> groupedNodes = nodes.stream()
					.flatMap(node -> node.getChildren().stream())
					.collect(Collectors.groupingBy(Node::getNodeName));

			List<Node> tmp = new ArrayList<>();
			groupedNodes.forEach((node, children) -> {
				if (children.size() == 1) {
					tmp.addAll(children);
					return;
				}

				tmp.add(Node.merge(children));
			});

//			List<Node> children = nodes.stream()
//					.flatMap(node -> node.getChildren().stream())
//					.collect(Collectors.toList());
			return new Node(nodeName, config, tmp);
		}

	}

	public interface NodeCreated {

		void nodeCreated();

	}

	public static class NodeReady {

		public final Jaxmpp jaxmpp;
		public final JID pubSubJid;
		public final String node;

		public NodeReady(Jaxmpp jaxmpp, JID pubSubJid, String node) {
			this.jaxmpp = jaxmpp;
			this.pubSubJid = pubSubJid;
			this.node = node;
		}

	}

	public static class FeatureProviderModule
			implements XmppModule {

		private Map<Object, Set<String>> featuresByObject = new ConcurrentHashMap<>();

		public FeatureProviderModule() {

		}

		@Override
		public Criteria getCriteria() {
			return null;
		}

		@Override
		public String[] getFeatures() {
			return featuresByObject.values()
					.stream()
					.flatMap(features -> features.stream())
					.distinct()
					.toArray(String[]::new);
		}

		@Override
		public void process(Element element) throws XMPPException, XMLException, JaxmppException {

		}

		public void setFeatures(Object object, Set<String> features) {
			if (features == null) {
				featuresByObject.remove(object);
			} else {
				featuresByObject.put(object, features);
			}
		}
	}

}
