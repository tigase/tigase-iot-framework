/*
 * PubSubNodesManager.java
 *
 * Tigase IoT Framework
 * Copyright (C) 2011-2017 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */

package tigase.iot.framework.runtime.pubsub;

import tigase.bot.RequiredXmppModules;
import tigase.bot.runtime.AbstractPubSubPublisher;
import tigase.bot.runtime.XmppService;
import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.iot.framework.runtime.ConnectionErrorReporter;
import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.XmppModule;
import tigase.jaxmpp.core.client.criteria.Criteria;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.forms.AbstractField;
import tigase.jaxmpp.core.client.xmpp.forms.JabberDataElement;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubAsyncCallback;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubErrorCondition;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.j2se.Jaxmpp;
import tigase.kernel.beans.Inject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class is implementation of a PubSub nodes manager.
 * It manages PubSub nodes for classes implementing <code>PubSubNodeAware</code> interface and manages
 * subscriptions of PubSub nodes for classes implementing <code>NodesObserver</code> interface.
 *
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
	protected List<NodesObserver> observers = Collections.emptyList();

	@Inject(nullAllowed = true)
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

		updateObservedNodes();
	}

	@Override
	public void initialize() {
		super.initialize();
		if (requiredNodes.isEmpty()) {
			updateRequiredNodes();
		}
	}

	/**
	 * Update subscriptions for observed PubSub nodes.
	 */
	public synchronized void updateObservedNodes() {
		synchronized (this) {
			this.features.clear();
			this.observedNodes = getObservedNodes().collect(Collectors.toSet());
			this.observedNodes.stream().map(node -> node + "+notify").forEach(this.features::add);

			if (xmppService != null) {
				xmppService.getAllConnections().stream().filter(jaxmpp -> jaxmpp.isConnected()).forEach(jaxmpp -> {
					sendPresenceToPubSubIfNeeded(jaxmpp);
				});
			}
		}
	}

	/**
	 * Implementation of a task queue to deal with race conditions during discovery/creation/removal of PubSub nodes
	 * from PubSub service. This task queue queues all tasks related to this changes so that they will be executed in
	 * the proper order.
	 */
	private static class TaskQueue {

		private static final String TASK_QUEUE_KEY = "TASK_QUEUE";

		public static TaskQueue get(Jaxmpp jaxmpp) {
			return jaxmpp.getSessionObject().getUserProperty(TASK_QUEUE_KEY);
		}

		public static void initializeTaskQueue(Jaxmpp jaxmpp) {
			TaskQueue queue = get(jaxmpp);
			if (queue != null) {
				queue.shutdown();
			}
			queue = new TaskQueue();
			jaxmpp.getSessionObject().setUserProperty(TASK_QUEUE_KEY, queue);
		}

		private final Queue<Consumer<Runnable>> queue = new LinkedBlockingQueue<>();
		private final Lock progressLock = new Lock();
		private volatile boolean shutdown = false;

		public TaskQueue() {

		}

		public void offer(Consumer<Runnable> consumer) {
			if (shutdown) {
				return;
			}
			queue.offer(consumer);
			tryProcess();
		}

		private void tryProcess() {
			if (shutdown) {
				return;
			}
			if (progressLock.tryLock()) {
				Consumer<Runnable> task = queue.poll();
				if (task != null) {
					task.accept(this::taskFinished);
				} else {
					progressLock.unlock();
				}
			}
		}

		private void taskFinished() {
			progressLock.unlock();
			tryProcess();
		}

		private void shutdown() {
			this.shutdown = true;
		}

		private static class Lock {

			private AtomicBoolean lock = new AtomicBoolean(false);

			public synchronized boolean tryLock() {
				return lock.compareAndSet(false, true);
			}

			public synchronized void unlock() {
				lock.set(false);
			}

		}
	}

	/**
	 * Update PubSub nodes on PubSub service for PubSubNodesAware classes.
	 */
	public synchronized void updateRequiredNodes() {
		synchronized (this) {
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
				Set<String> oldNodeNames = oldRequiredNodes == null
										   ? Collections.emptySet()
										   : oldRequiredNodes.stream()
												   .flatMap(Node::flattened)
												   .map(Node::getNodeName)
												   .collect(Collectors.toSet());
				xmppService.getAllConnections()
						.stream()
						.filter(jaxmpp -> jaxmpp.isConnected())
						.forEach(jaxmpp -> scheduleNodeVerification(jaxmpp, node -> !oldNodeNames.contains(node.getNodeName())));

				Set<String> nodeNames = requiredNodes.stream()
						.flatMap(Node::flattened)
						.map(Node::getNodeName)
						.collect(Collectors.toSet());
				if (oldRequiredNodes != null) {
					oldRequiredNodes.stream()
							.flatMap(Node::flattened)
							.filter(node -> !nodeNames.contains(node.getNodeName()))
							.sorted((n1, n2) -> n1.getNodeName().compareTo(n2.getNodeName()) * -1)
							.forEach(node -> {
								xmppService.getAllConnections().stream().filter(Jaxmpp::isConnected).forEach(jaxmpp -> this.scheduleNodeRemoval(jaxmpp, node));
							});
				}
			}
		}
	}

	private EventBus getEventBus() {
		return eventBus;
	}

	private static final String EXISTING_PUBSUB_NODES = "EXISTING_PUBSUB_NODES";

	/**
	 * Task discovers set of PubSub nodes created by this devices host and stores it
	 * for future usage.
	 */
	private class DiscoverNodesTask implements Consumer<Runnable> {

		private final Jaxmpp jaxmpp;
		private final Set<String> localNodes = new HashSet<>();
		private Runnable callback;

		public DiscoverNodesTask(Jaxmpp jaxmpp) {
			this.jaxmpp = jaxmpp;
		}

		public void accept(Runnable callback) {
			this.callback = callback;
			JID pubsubJid = getPubsubJid(jaxmpp);
			if (log.isLoggable(Level.FINE)) {
				log.fine("staring discovery of PubSub nodes owned by us at " + pubsubJid);
			}
			discoverNodes(pubsubJid, null, this::taskFinished);
		}

		private void taskFinished() {
			JID pubsubJid = getPubsubJid(jaxmpp);
			jaxmpp.getSessionObject().setUserProperty(EXISTING_PUBSUB_NODES, localNodes);
			if (log.isLoggable(Level.FINE)) {
				log.fine("finished PubSub nodes discovery at " + pubsubJid + " and found " + localNodes.size() +
								 " owned nodes: " + localNodes);
			}
			localNodes.forEach(node -> eventBus.fire(new NodeReady(jaxmpp, pubsubJid, node)));
			callback.run();
		}

		private void discoverNodes(JID pubsubJid, String node, Runnable callback) {
			try {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("discovering at " + pubsubJid + " subnodes of node "  + node);
				}
				jaxmpp.getModule(DiscoveryModule.class).getItems(pubsubJid, node, new DiscoveryModule.DiscoItemsAsyncCallback() {

					private int counter = 0;
					
					@Override
					public void onInfoReceived(String attribute, ArrayList<DiscoveryModule.Item> items) throws XMLException {
						if (log.isLoggable(Level.FINER)) {
							log.finer("at " + pubsubJid + " for node " + node + " found subnodes = " + items.stream()
									.filter(item -> item.getNode() != null)
									.collect(Collectors.toList()));
						}
						items.forEach(item -> {
							if (item.getNode() != null) {
								synchronized (this) {
									counter++;
									checkCreatorJid(pubsubJid, item.getNode(), this::taskFinished);
									counter++;
									discoverNodes(pubsubJid, item.getNode(), this::taskFinished);
								}
							}
						});
						if (!items.stream().anyMatch(item -> item.getNode() != null)) {
							callback.run();
						}
					}

					@Override
					public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
						log.log(Level.FINE, "at {0} failed to get node {1} subnodes: {2}",
								new Object[]{pubsubJid, node, error});
						ConnectionErrorReporter.PolicyViolationErrorEvent.fireIfNeeded(eventBus, error, responseStanza);
						callback.run();
					}

					@Override
					public void onTimeout() throws JaxmppException {
						log.log(Level.FINE, "at {0} failed to get node {1} subnodes - timeout",
								new Object[]{pubsubJid, node});
						callback.run();
					}

					private void taskFinished() {
						synchronized (this) {
							counter--;
							if (counter <= 0) {
								if (log.isLoggable(Level.FINEST)) {
									log.finest("finished discovery at " + pubsubJid + " subnodes of node " + node);
								}
								callback.run();
							}
						}
					}
				});
			} catch (JaxmppException ex) {
				throw new RuntimeException(ex);
			}
		}

		private void checkCreatorJid(JID pubsubJid, String node, Runnable callback) {
			try {
				jaxmpp.getModule(DiscoveryModule.class).getInfo(pubsubJid, node, new AsyncCallback() {
					@Override
					public void onError(Stanza responseStanza, XMPPException.ErrorCondition error)
							throws JaxmppException {
						log.log(Level.FINE, "{0}, failed to get node {1} info: {2}",
								new Object[]{PubSubNodesManager.this.name, node, error});
						ConnectionErrorReporter.PolicyViolationErrorEvent.fireIfNeeded(eventBus, error, responseStanza);
						callback.run();
					}

					@Override
					public void onSuccess(Stanza responseStanza) throws JaxmppException {
						Element x = responseStanza.findChild(new String[] { "iq", "query", "x"});
						if (x != null) {
							JabberDataElement data = new JabberDataElement(x);
							if (data != null) {
								AbstractField<JID> creator = data.getField("pubsub#creator");
								if (creator != null && JID.jidInstance(jaxmpp.getSessionObject().getUserBareJid()).equals(creator.getFieldValue())) {
									localNodes.add(node);
								}
							}
						}
						callback.run();
					}

					@Override
					public void onTimeout() throws JaxmppException {
						log.log(Level.WARNING, "{0}, failed to get node {1} info - timeout",
								new Object[]{PubSubNodesManager.this.name, node});
						callback.run();
					}
				});
			} catch (JaxmppException ex) {
				ex.printStackTrace();
				throw new RuntimeException(ex);
			}
		}

	}

	/**
	 * Task checks if PubSub node is available at PubSub service and creates it if node is missing
	 */
	private class EnsureNodeExistsTask implements Consumer<Runnable> {
		private final Jaxmpp jaxmpp;
		private final Node node;
		private Runnable completionHandler;
		public EnsureNodeExistsTask(Jaxmpp jaxmpp, Node node) {
			this.jaxmpp = jaxmpp;
			this.node = node;
		}

		@Override
		public void accept(Runnable runnable) {
			this.completionHandler = runnable;
			startProcess(jaxmpp);
		}

		private void startProcess(Jaxmpp jaxmpp) {
			Set<String> existingNodes = jaxmpp.getSessionObject().getUserProperty("EXISTING_PUBSUB_NODES");
			if (!existingNodes.contains(node.getNodeName())) {
				try {
					JID pubsubJid = getPubsubJid(jaxmpp);
					createNode(jaxmpp, pubsubJid, node.getNodeName(), node.getConfig(), this::finished);
				} catch (JaxmppException ex) {
					log.log(Level.FINE, "failed to ensure that node exists", ex);
					finished();
				}
			} else {
				//eventBus.fire(new NodeReady(jaxmpp, getPubsubJid(jaxmpp), node.getNodeName()));
				finished();
			}
		}

		private void finished() {
			completionHandler.run();
		}
	}

	/**
	 * Tasks checks if PubSub node exists and if so it removes it from PubSub service.
	 */
	private class EnsureNodeRemovedTask implements Consumer<Runnable> {
		private final Jaxmpp jaxmpp;
		private final String node;
		private Runnable completionHandler;

		public EnsureNodeRemovedTask(Jaxmpp jaxmpp, Node node) {
			this(jaxmpp, node.getNodeName());
		}

		public EnsureNodeRemovedTask(Jaxmpp jaxmpp, String node) {
			this.jaxmpp = jaxmpp;
			this.node = node;
		}

		@Override
		public void accept(Runnable runnable) {
			this.completionHandler = runnable;
			startProcess(jaxmpp);
		}

		private void startProcess(Jaxmpp jaxmpp) {
			Set<String> existingNodes = jaxmpp.getSessionObject().getUserProperty("EXISTING_PUBSUB_NODES");
			if (existingNodes != null && existingNodes.contains(node)) {
				try {
					JID pubsubJid = getPubsubJid(jaxmpp);
					deleteNode(jaxmpp, pubsubJid, node, this::finished);
				} catch (JaxmppException ex) {
					log.log(Level.FINE, "failed to delete node", ex);
					finished();
				}
			} else {
				finished();
			}
		}

		private void finished() {
			completionHandler.run();
		}
	}
	
	/**
	 * Task retrieves a list of all nodes created by this devices hosts and removes all nodes
	 * which are not available in <code>requiredNodes</code> structure.
	 */
	private class NodeCleanupTask implements Consumer<Runnable> {

		private final Jaxmpp jaxmpp;

		public NodeCleanupTask(Jaxmpp jaxmpp) {
			this.jaxmpp = jaxmpp;
		}

		@Override
		public void accept(Runnable runnable) {
			Set<String> existingNodes = jaxmpp.getSessionObject().getProperty(EXISTING_PUBSUB_NODES);
			if (existingNodes != null) {
				Set<String> requiredNodes;
				synchronized (PubSubNodesManager.this) {
					requiredNodes = PubSubNodesManager.this.requiredNodes.stream()
							.flatMap(Node::flattened)
							.sorted((n1, n2) -> n1.getNodeName().compareTo(n2.getNodeName()) * -1)
							.map(Node::getNodeName)
							.collect(Collectors.toSet());
				}
				existingNodes.stream().filter(node -> !requiredNodes.contains(node)).map(node -> new EnsureNodeRemovedTask(jaxmpp, node)).forEach(task -> {
					TaskQueue.get(jaxmpp).offer(task);
				});
			}
			runnable.run();
		}
	}

	private void scheduleNodeDiscovery(Jaxmpp jaxmpp) {
		TaskQueue.get(jaxmpp).offer(new DiscoverNodesTask(jaxmpp));
	}

	private void scheduleNodeVerification(Jaxmpp jaxmpp) {
	    scheduleNodeVerification(jaxmpp, (n)-> true);
	}
	
	private void scheduleNodeVerification(Jaxmpp jaxmpp, Predicate<Node> predicate) {
		TaskQueue taskQueue = TaskQueue.get(jaxmpp);
		synchronized (this) {
			requiredNodes.stream()
					.flatMap(Node::flattened)
					.filter(predicate)
					.sorted((n1, n2) -> n1.getNodeName().compareTo(n2.getNodeName()))
					.map(node -> new EnsureNodeExistsTask(jaxmpp, node))
					.forEach(taskQueue::offer);
		}
	}

	/**
	 * Method schedules removal of PubSub node
	 * @param jaxmpp
	 * @param node
	 */
	private void scheduleNodeRemoval(Jaxmpp jaxmpp, Node node) {
		TaskQueue taskQueue = TaskQueue.get(jaxmpp);
		taskQueue.offer(new EnsureNodeRemovedTask(jaxmpp, node));
	}

	/**
	 * Method schedules node cleanup of PubSub nodes for particular Jaxmpp instance
	 * @param jaxmpp
	 */
	private void scheduleNodeCleanup(Jaxmpp jaxmpp) {
		TaskQueue taskQueue = TaskQueue.get(jaxmpp);
		taskQueue.offer(new NodeCleanupTask(jaxmpp));
	}

	/**
	 * Publish an item to PubSub node
	 * @param node
	 * @param itemId
	 * @param payload
	 */
	public void publishItem(String node, String itemId, Element payload) {
		Item item = new Item(node, itemId, payload);
		xmppService.getAllConnections().forEach(jaxmpp -> {
			ItemsPublisherQueue queue;
			synchronized (jaxmpp) {
				queue = jaxmpp.getSessionObject().getUserProperty(PUBSUB_PUBLISH_QUEUE);
				if (queue == null) {
					queue = new ItemsPublisherQueue(name, jaxmpp, this::getEventBus, this::getPubsubJid);

					jaxmpp.getSessionObject().setUserProperty(PUBSUB_PUBLISH_QUEUE, queue);
				}
			}

			queue.publish(item);
		});
	}
	
	/**
	 * Create PubSub node with provided configuration.
	 * @param jaxmpp
	 * @param pubsubJid
	 * @param node
	 * @param config
	 * @param callback
	 * @throws JaxmppException
	 */
	public void createNode(Jaxmpp jaxmpp, JID pubsubJid, String node, JabberDataElement config, Runnable callback)
			throws JaxmppException {
		if (log.isLoggable(Level.FINE)) {
			log.finest("at " + pubsubJid + " creating node " + node);
		}
		PubSubModule pubSubModule = jaxmpp.getModule(PubSubModule.class);
		pubSubModule.createNode(pubsubJid.getBareJid(), node, config, new PubSubAsyncCallback() {
			@Override
			protected void onEror(IQ response, XMPPException.ErrorCondition errorCondition,
								  PubSubErrorCondition pubSubErrorCondition) throws JaxmppException {
				log.log(Level.FINE, "at {0} failed to create device node - {1} {2}",
						new Object[]{pubsubJid, errorCondition, pubSubErrorCondition});
				if (errorCondition == XMPPException.ErrorCondition.conflict) {
					Set<String> existingNodes = jaxmpp.getSessionObject().getUserProperty("EXISTING_PUBSUB_NODES");
					if (!existingNodes.contains(node)) {
						existingNodes.add(node);
					}

					eventBus.fire(new NodeReady(jaxmpp, pubsubJid, node));
					if (callback != null) {
						callback.run();
					}
				}
				ConnectionErrorReporter.PolicyViolationErrorEvent.fireIfNeeded(eventBus, errorCondition, response);
			}

			@Override
			public void onSuccess(Stanza stanza) throws JaxmppException {
				log.log(Level.FINE, "at {0} node {1} created successfully",
						new Object[]{pubsubJid, node});

				Set<String> existingNodes = jaxmpp.getSessionObject().getUserProperty("EXISTING_PUBSUB_NODES");
				if (!existingNodes.contains(node)) {
					existingNodes.add(node);
				}

				eventBus.fire(new NodeReady(jaxmpp, pubsubJid, node));
				if (callback != null) {
					callback.run();
				}
			}

			@Override
			public void onTimeout() throws JaxmppException {
				log.log(Level.WARNING, "at {0} failed to create device node {1} - {2}",
						new Object[]{pubsubJid, node, "request timeout!"});
				callback.run();
			}
		});
	}

	/**
	 * Method called to schedule PubSub nodes cleanup for all connected Jaxmpp instances.
	 * Retrieves a list of all nodes created by this devices hosts and removes all nodes
	 * which are not available in <code>requiredNodes</code> structure.
	 */
	public void cleanupNodes() {
		xmppService.getAllConnections().forEach(this::scheduleNodeCleanup);
	}

	/**
	 * Helper method used to remove particular PubSub node
	 * @param jaxmpp
	 * @param pubsubJid
	 * @param node
	 * @param callback
	 * @throws JaxmppException
	 */
	public void deleteNode(Jaxmpp jaxmpp, JID pubsubJid, String node, Runnable callback) throws JaxmppException {
		PubSubModule pubSubModule = jaxmpp.getModule(PubSubModule.class);
		pubSubModule.deleteNode(pubsubJid.getBareJid(), node, new PubSubAsyncCallback() {
			@Override
			protected void onEror(IQ response, XMPPException.ErrorCondition errorCondition,
								  PubSubErrorCondition pubSubErrorCondition) throws JaxmppException {
				log.log(Level.FINE, "at {0} failed to delete device node - {1} {2}",
						new Object[]{pubsubJid, errorCondition, pubSubErrorCondition});
				ConnectionErrorReporter.PolicyViolationErrorEvent.fireIfNeeded(eventBus, errorCondition, response);
				if (callback != null) {
					callback.run();
				}
			}

			@Override
			public void onSuccess(Stanza responseStanza) throws JaxmppException {
				log.log(Level.FINE, "from {0} node {1} was successfully deleted",
						new Object[]{pubsubJid, node});

				Set<String> existingNodes = jaxmpp.getSessionObject().getUserProperty("EXISTING_PUBSUB_NODES");
				existingNodes.remove(node);
				if (callback != null) {
					callback.run();
				}
			}

			@Override
			public void onTimeout() throws JaxmppException {
				log.log(Level.WARNING, "at {0} failed to delete device node {1} - {2}",
						new Object[]{pubsubJid, node, "request timeout!"});
				if (callback != null) {
					callback.run();
				}
			}
		});
	}

	/**
	 * Method is called when required PubSub node is ready.
	 * @param nodeReady
	 */
	@HandleEvent
	private void nodeReady(NodeReady nodeReady) {
		Optional.ofNullable(
				(ItemsPublisherQueue) nodeReady.jaxmpp.getSessionObject().getUserProperty(PUBSUB_PUBLISH_QUEUE))
				.ifPresent(ItemsPublisherQueue::publish);
	}

	@HandleEvent
	public void onJaxmppAdded(XmppService.JaxmppAddedEvent event) {
		event.jaxmpp.getModule(FeatureProviderModule.class).setFeatures(this, () -> {
			synchronized (this) {
				return new HashSet<>(features).stream();
			}
		});
	}

	/**
	 * Returns a stream of observed PubSub node names
	 * @return
	 */
	protected Stream<String> getObservedNodes() {
		return observers.stream().flatMap(o -> getObservedNodes(o));
	}

	/**
	 * Returns stream of observed PubSub nodes from particular instance of NodesObserver
	 * @param o
	 * @return
	 */
	protected Stream<String> getObservedNodes(NodesObserver o) {
		return o.getObservedNodes().stream();
	}

	@Override
	protected void jaxmppConnected(Jaxmpp jaxmpp) {
//		FeatureProviderModule module = jaxmpp.getModule(FeatureProviderModule.class);
//		module.setFeatures(this, features);
		TaskQueue.initializeTaskQueue(jaxmpp);
		scheduleNodeDiscovery(jaxmpp);
		scheduleNodeVerification(jaxmpp);
		sendPresenceToPubSubIfNeeded(jaxmpp);
		scheduleNodeCleanup(jaxmpp);
	}

	@Override
	protected void jaxmppDisconnected(Jaxmpp jaxmpp) {

	}

	protected void sendPresenceToPubSubIfNeeded(Jaxmpp jaxmpp) {
		jaxmpp.getSessionObject().setProperty(CapabilitiesModule.VERIFICATION_STRING_KEY, null);
		PresenceModule presenceModule = jaxmpp.getModule(PresenceModule.class);

		try {
			presenceModule.sendInitialPresence();

			// If we are not using PEP then we need to send presence with CAPS to PubSub service
			// so it would know we are connected and we want to get notifications for particular
			// pubsub nodes (custom filtering using +notify)
			if (!isPEP()) {
				Presence presence = Stanza.createPresence();
				presence.setTo(getPubsubJid(jaxmpp));
				jaxmpp.getEventBus().fire(new PresenceModule.BeforePresenceSendHandler.BeforePresenceSendEvent(jaxmpp.getSessionObject(), presence, event -> {
					try {
						jaxmpp.send(event.getPresence());
					} catch (JaxmppException ex) {
						ex.printStackTrace();
					}
				}));
			}
		} catch (JaxmppException ex) {
			log.log(Level.WARNING, "failed to send update presence", ex);
		}
	}

	/**
	 * Interface which needs to be implemented by all classes which instances require PubSub nodes
	 * to be managed by <code>PubSubNodesManager</code>
	 */
	public interface PubSubNodeAware {

		/**
		 * List of nodes which are required.
		 * @return
		 */
		List<Node> getRequiredNodes();

	}

	/**
	 * Interface which needs to be implemented by all classes which want to observer PubSub nodes.
	 */
	public interface NodesObserver {

		/**
		 * Returns list of nodes which should be observed.
		 * @return
		 */
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

	/**
	 * Class represents a PubSub node name, configuration and structure.
	 */
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

			return new Node(nodeName, config, tmp);
		}

		/**
		 * Returns stream of flattened node instance
		 * @return
		 */
		public Stream<Node> flattened() {
			return Stream.concat(Stream.of(this), getChildren().stream().flatMap(Node::flattened));
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

		private Map<Object, Supplier<Stream<String>>> featuresByObject = new ConcurrentHashMap<>();

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
					.flatMap(features -> features.get())
					.distinct()
					.toArray(String[]::new);
		}

		@Override
		public void process(Element element) throws XMPPException, XMLException, JaxmppException {

		}

		public void setFeatures(Object object, Supplier<Stream<String>> features) {
			if (features == null) {
				featuresByObject.remove(object);
			} else {
				featuresByObject.put(object, features);
			}
		}
	}

	private static class ItemsPublisherQueue {

		private static final Logger log = Logger.getLogger(ItemsPublisherQueue.class.getCanonicalName());

		private final LinkedBlockingQueue<Item> items = new LinkedBlockingQueue<>();
		private final String name;
		private final Jaxmpp jaxmpp;
		private final Function<Jaxmpp, JID> pubsubJidSupplier;
		private final Supplier<EventBus> eventBusSupplier;

		ItemsPublisherQueue(String name, Jaxmpp jaxmpp, Supplier<EventBus> eventBusSupplier, Function<Jaxmpp,JID> pubsubJidSupplier) {
			this.name = name;
			this.jaxmpp = jaxmpp;
			this.eventBusSupplier = eventBusSupplier;
			this.pubsubJidSupplier = pubsubJidSupplier;
		}

		public synchronized void publish() {
			processItems();
		}

		public synchronized void publish(Item item) {
			invalidateItems(item.node);
			items.offer(item);
			processItems();
		}

		public synchronized void clear() {
			items.clear();
		}

		public synchronized void invalidateItems(String node) {
			boolean result = items.removeIf(item -> node.equals(item.node));
			log.log(Level.WARNING, "invalidating items for node " + node + ", result = " + result);
		}

		private synchronized void processItems() {
			if (!jaxmpp.isConnected()) {
				return;
			}

			Collection<String> existingNodes = jaxmpp.getSessionObject().getUserProperty("EXISTING_PUBSUB_NODES");
			if (existingNodes == null) {
				return;
			}

			Iterator<Item> it = items.iterator();
			while (it.hasNext()) {
				Item item = it.next();

				if (!existingNodes.contains(item.node)) {
					continue;
				}

				if (!publishItem(item)) {
					return;
				}
				it.remove();
			}
		}

		private boolean publishItem(Item item) {
			final JID pubsubJid = pubsubJidSupplier.apply(jaxmpp);
			PubSubModule pubSubModule = jaxmpp.getModule(PubSubModule.class);

			try {
				if (log.isLoggable(Level.FINE)) {
					log.fine("at " + pubsubJid.getBareJid() + " node " + item.node + " publishing item with id = " +
									 item.itemId + " and payload = " + item.payload.getAsString());
				}
				log.log(Level.WARNING, "publishing item at node " + item.node);
				pubSubModule.publishItem(pubsubJid.getBareJid(), item.node, item.itemId, item.payload,
										 new PubSubModule.PublishAsyncCallback() {

											 @Override
											 public void onTimeout() throws JaxmppException {
												 log.log(Level.WARNING,
														 "at {0} node {1} failed to publish item {2} with timeout",
														 new Object[]{pubsubJid, item.node, item.itemId});
												 onFailure(item);
											 }

											 @Override
											 protected void onEror(IQ response,
																   XMPPException.ErrorCondition errorCondition,
																   PubSubErrorCondition pubSubErrorCondition)
													 throws JaxmppException {
												 log.log(Level.FINE,
														 "at {0} node {1} failed to publish item {2} with errors {3} / {4}",
														 new Object[]{pubsubJid, item.node, item.itemId,
																	  errorCondition, pubSubErrorCondition});
												 ConnectionErrorReporter.PolicyViolationErrorEvent.fireIfNeeded(eventBusSupplier.get(), errorCondition, response);
												 if (errorCondition == XMPPException.ErrorCondition.item_not_found ||
														 errorCondition ==
																 XMPPException.ErrorCondition.feature_not_implemented ||
														 errorCondition == XMPPException.ErrorCondition.forbidden) {
													 return;
												 } else {
													 onFailure(item);
												 }
											 }

											 @Override
											 public void onPublish(String itemId) {
												 log.log(Level.FINE, "at {0} node {1} published item as {2}",
														 new Object[]{pubsubJid,
																	  item.node, itemId});
											 }
										 });
				return true;
			} catch (JaxmppException ex) {
				log.log(Level.WARNING, this.name + ", item publication failed", ex);
				return false;
			}
		}

		private void onFailure(Item item) {
			synchronized (this) {
				if (!items.stream().filter(it ->  item.node.equals(it.node)).findFirst().isPresent()) {
					items.offer(item);
				}
			}
		}
	}

}
