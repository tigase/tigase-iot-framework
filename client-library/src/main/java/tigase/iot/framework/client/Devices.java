/*
 * Devices.java
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

package tigase.iot.framework.client;

import tigase.iot.framework.client.devices.*;
import tigase.jaxmpp.core.client.*;
import tigase.jaxmpp.core.client.criteria.Criteria;
import tigase.jaxmpp.core.client.eventbus.Event;
import tigase.jaxmpp.core.client.eventbus.EventHandler;
import tigase.jaxmpp.core.client.eventbus.JaxmppEventWithCallback;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.forms.Field;
import tigase.jaxmpp.core.client.xmpp.forms.JabberDataElement;
import tigase.jaxmpp.core.client.xmpp.forms.TextMultiField;
import tigase.jaxmpp.core.client.xmpp.forms.XDataType;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.modules.adhoc.Action;
import tigase.jaxmpp.core.client.xmpp.modules.adhoc.AdHocCommansModule;
import tigase.jaxmpp.core.client.xmpp.modules.adhoc.State;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Message;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class handler device discovery and management based on PubSub nodes.
 * <p>
 * Created by andrzej on 26.11.2016.
 */
public class Devices {

	private static final Logger log = Logger.getLogger(Devices.class.getCanonicalName());

	private final String devicesNode;
	private final JaxmppCore jaxmpp;
	private List<Device> devices = new ArrayList<Device>();
	private boolean pep;

	public Devices(JaxmppCore jaxmpp, boolean pep) {
		this(jaxmpp, "devices", pep);
	}

	/**
	 * Create instance of device manager for devices registered at device node passed as argument.
	 *
	 * @param jaxmpp
	 * @param devicesNode
	 * @param pep
	 */
	public Devices(final JaxmppCore jaxmpp, String devicesNode, boolean pep) {
		this.jaxmpp = jaxmpp;
		this.pep = pep;
		this.devicesNode = devicesNode;

		jaxmpp.getModulesManager().register(new FeatureProviderModule(devicesNode));

		this.jaxmpp.getModule(PubSubModule.class)
				.addNotificationReceivedHandler(new PubSubModule.NotificationReceivedHandler() {
					@Override
					public void onNotificationReceived(SessionObject sessionObject, Message message, JID pubSubJID,
													   String nodeName, String itemId, Element payload, Date delayTime,
													   String itemType) {
						processNotification(pubSubJID, nodeName, payload);
					}
				});

		this.jaxmpp.getEventBus()
				.addHandler(
						PubSubModule.NotificationCollectionChildrenChangedHandler.NotificationCollectionChildrenChangedEvent.class,
						new PubSubModule.NotificationCollectionChildrenChangedHandler() {
							@Override
							public void onNotificationCollectionChildrenChangedReceived(SessionObject sessionObject,
																						Message message, JID pubSubJID,
																						String nodeName,
																						String childNode, Action action,
																						Date delayTime) {
								if ("devices".equals(nodeName)) {
									FeatureProviderModule featureProviderModule = jaxmpp.getModule(
											FeatureProviderModule.class);
									featureProviderModule.addNewDevice(childNode);
									devicesNodesDiscoveryFinished();
									try {
										checkForNewDevice(pubSubJID, childNode);
									} catch (JaxmppException e) {
										log.log(Level.WARNING, "Failed to check new device node", e);
									}
								}
							}
						});

		this.jaxmpp.getEventBus()
				.addHandler(PubSubModule.NotificationNodeDeletedHander.NotificationNodeDeletedEvent.class,
							new PubSubModule.NotificationNodeDeletedHander() {
								@Override
								public void onNodeDeleted(SessionObject sessionObject, Message message, JID pubSubJID,
														  String nodeName) {
									Iterator<Device> it = devices.iterator();
									Device device = null;
									while (it.hasNext()) {
										device = it.next();
										if (device.getNode().equals(nodeName)) {
											it.remove();
											break;
										}
									}
									if (device != null) {
										devicesNodesDiscoveryFinished();
									}
								}
							});

		this.jaxmpp.getEventBus()
				.addHandler(JaxmppCore.LoggedInHandler.LoggedInEvent.class, new JaxmppCore.LoggedInHandler() {
					@Override
					public void onLoggedIn(SessionObject sessionObject) {
						try {
							refreshDevices();
						} catch (JaxmppException ex) {
							log.log(Level.WARNING, "Failed to refresh devices list", ex);
						}
					}
				});
	}

	private void checkForNewDevice(final JID jid, final String node) throws JaxmppException {
		DiscoveryModule discoModule = jaxmpp.getModule(DiscoveryModule.class);
		discoModule.getItems(jid, this.devicesNode, new DiscoveryModule.DiscoItemsAsyncCallback() {

			@Override
			public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
				devicesNodesDiscoveryFinished();
			}

			@Override
			public void onInfoReceived(String attribute, ArrayList<DiscoveryModule.Item> items) throws XMLException {
				List<DiscoveryModule.Item> filtered = new ArrayList<>();
				for (DiscoveryModule.Item item : items) {
					if (jid.equals(item.getJid()) && node.equals(item.getNode())) {
						filtered.add(item);
					}
				}
				devicesNodesFound(node, filtered);
			}

			@Override
			public void onTimeout() throws JaxmppException {
				devicesNodesDiscoveryFinished();
			}
		});
	}

	public void checkIfJidIsHost(final JID jid, final BiConsumer<JID, DiscoveryModule.Identity> foundHost,
								 final Runnable finished) throws JaxmppException {
		final JID hubJid = isRemoteMode()
						   ? getRemoteHubJid()
						   : JID.jidInstance(jaxmpp.getSessionObject().getUserBareJid().getDomain());

		DiscoveryModule discoveryModule = jaxmpp.getModule(DiscoveryModule.class);
		String queryNode = nodeForwardEncoder(jid, null);
		discoveryModule.getInfo(isRemoteMode() ? hubJid : jid, queryNode,
								new DiscoveryModule.DiscoInfoAsyncCallback(queryNode) {
									@Override
									public void onError(Stanza responseStanza, XMPPException.ErrorCondition error)
											throws JaxmppException {
										finished.run();
									}

									@Override
									protected void onInfoReceived(String node,
																  Collection<DiscoveryModule.Identity> identities,
																  Collection<String> features) throws XMLException {
										for (DiscoveryModule.Identity identity : identities) {
											if ("device".equals(identity.getCategory()) &&
													"iot".equals(identity.getType())) {
												foundHost.accept(jid, identity);
											}
										}
										finished.run();
									}

									@Override
									public void onTimeout() throws JaxmppException {
										finished.run();
									}
								});
	}

	/**
	 * Method creates device representation based on received information about device from its PubSub node.
	 *
	 * @param item
	 * @param config
	 *
	 * @return
	 */
	protected Device createDevice(DiscoveryModule.Item item, Device.Configuration config) {
		try {
			Field field = config.getValue().getField("type");
			if (field == null) {
				return null;
			}
			String type = (String) field.getFieldValue();
			if (type == null) {
				return null;
			}
			field = config.getValue().getField("category");
			String category = field == null ? null : (String) field.getFieldValue();

			switch (type) {
				case "movement-sensor":
					return new MovementSensor(this, jaxmpp, item.getJid(), item.getNode(), item.getName());
				case "tv-sensor":
					return new TvSensor(this, jaxmpp, item.getJid(), item.getNode(), item.getName());
				case "light-dimmer":
					return new LightDimmer(this, jaxmpp, item.getJid(), item.getNode(), item.getName(), category);
				case "light-sensor":
					return new LightSensor(this, jaxmpp, item.getJid(), item.getNode(), item.getName());
				case "temperature-sensor":
					return new TemperatureSensor(this, jaxmpp, item.getJid(), item.getNode(), item.getName());
				case "switch":
					return new Switch(this, jaxmpp, item.getJid(), item.getNode(), item.getName(), category);
				case "humidity-sensor":
					return new HumiditySensor(this, jaxmpp, item.getJid(), item.getNode(), item.getName());
				case "led-matrix":
					return new LedMatrixDevice(this, jaxmpp, item.getJid(), item.getNode(), item.getName());
				case "pressure-sensor":
					return new PressureSensor(this, jaxmpp, item.getJid(), item.getNode(), item.getName());
				default:
					return new UnknownDevice(this, jaxmpp, item.getJid(), item.getNode(), item.getName());
			}
		} catch (XMLException ex) {
			return null;
		}
	}

	protected void deviceNodeFound(final DiscoveryModule.Item item, final Runnable finisher) {
		try {
			Device.retrieveConfiguration(jaxmpp, item.getJid(), item.getNode(),
										 new Device.Callback<Device.Configuration>() {

											 @Override
											 public void onError(Stanza response,
																 XMPPException.ErrorCondition errorCondition) {
												 finisher.run();
											 }

											 @Override
											 public void onSuccess(Device.Configuration config) {
												 Device device = createDevice(item, config);
												 synchronized (devices) {
													 boolean found = false;
													 for (Device d : devices) {
														 if (d.getNode().equals(device.getNode())) {
															 found = true;
														 }
													 }
													 if (!found) {
														 devices.add(device);
													 }
												 }
												 finisher.run();
											 }
										 });
		} catch (JaxmppException ex) {
			log.log(Level.WARNING, "Failed to retrieve device configuration", ex);
		}
	}

	protected void devicesNodesDiscoveryFinished() {
		updateCapsOnDevicesChange();
		jaxmpp.getEventBus().fire(new ChangedHandler.ChangedEvent(devices));
	}

	protected void devicesNodesFound(String node, List<DiscoveryModule.Item> items) {
		final Counter counter = new Counter(items.size());

		Runnable finisher = new Runnable() {
			@Override
			public void run() {
				synchronized (counter) {
					counter.decrement();
					if (counter.value() <= 0) {
						devicesNodesDiscoveryFinished();
					}
				}
			}
		};

		for (final DiscoveryModule.Item item : items) {
			deviceNodeFound(item, finisher);
		}
		if (items.isEmpty()) {
			devicesNodesDiscoveryFinished();
		}
	}

	public void executeDeviceHostAdHocCommand(JID deviceHostJid, String node, Action action, JabberDataElement form,
											  AsyncCallback callback) throws JaxmppException {
		JID hubJid = isRemoteMode() ? getRemoteHubJid() : deviceHostJid;

		jaxmpp.getModule(AdHocCommansModule.class)
				.execute(hubJid, nodeForwardEncoder(deviceHostJid, node), action, form, callback);
	}

	/**
	 * Discovers list of host devices connected to local IoT hub.
	 *
	 * @param devicesInfoRetrieved
	 *
	 * @throws JaxmppException
	 */
	public void getActiveDeviceHosts(final DevicesInfoRetrieved devicesInfoRetrieved) throws JaxmppException {
		JabberDataElement form = new JabberDataElement(XDataType.submit);
		form.addTextSingleField("domainjid", "tigase-iot-hub.local");
		form.addTextSingleField("max_items", "100");

		executeDeviceHostAdHocCommand(JID.jidInstance("tigase-iot-hub.local"), "get-users-connections-list",
									  Action.execute, form, new AdHocCommansModule.AdHocCommansAsyncCallback() {

					private Integer counter;
					private Map<JID, DiscoveryModule.Identity> discoveredDevices = new HashMap<>();

					private void checkAndNotify() {
						synchronized (this) {
							counter--;
							if (counter == 0) {
								devicesInfoRetrieved.onDeviceInfoRetrieved(discoveredDevices);
							}
						}
					}

					@Override
					public void onError(Stanza responseStanza, XMPPException.ErrorCondition error)
							throws JaxmppException {
						devicesInfoRetrieved.onDeviceInfoRetrieved(new HashMap<JID, DiscoveryModule.Identity>());
					}

					@Override
					protected void onResponseReceived(String sessionid, String node, State status,
													  JabberDataElement data) throws JaxmppException {
						TextMultiField field = (TextMultiField) data.getFields().get(0);
						String[] jids = field.getFieldValue();
						counter = jids.length;
						for (String jidStr : jids) {
							final JID jid = JID.jidInstance(jidStr);
							if (jid.equals(ResourceBinderModule.getBindedJID(jaxmpp.getSessionObject()))) {
								checkAndNotify();
								continue;
							}

							checkIfJidIsHost(jid, new BiConsumer<JID, DiscoveryModule.Identity>() {
								@Override
								public void accept(JID jid, DiscoveryModule.Identity identity) {
									discoveredDevices.put(jid, identity);
								}
							}, new Runnable() {
								@Override
								public void run() {
									checkAndNotify();
								}
							});
						}
					}

					@Override
					public void onTimeout() throws JaxmppException {
						devicesInfoRetrieved.onDeviceInfoRetrieved(new HashMap<JID, DiscoveryModule.Identity>());
					}

				});
	}

	private Device getDeviceByNode(String nodeName) {
		for (Device device : devices) {
			if (nodeName.startsWith(device.getNode())) {
				return device;
			}
		}

		return null;
	}

	/**
	 * Method returns all known/discovered remote devices.
	 *
	 * @return
	 */
	public List<Device> getDevices() {
		return devices;
	}

	/**
	 * Method returns JID of PubSub service which is being used as middleware for event delivery.
	 *
	 * @return
	 */
	protected JID getPubSubJid() {
		BareJID userJid = jaxmpp.getSessionObject().getUserBareJid();
		if (userJid == null) {
			userJid = ResourceBinderModule.getBindedJID(jaxmpp.getSessionObject()).getBareJid();
		}
		if (pep) {
			return JID.jidInstance(userJid);
		} else {
			return JID.jidInstance("pubsub." + userJid.getDomain());
		}
	}

	public JID getRemoteHubJid() {
		if (!isRemoteMode()) {
			throw new RuntimeException("Invalid mode! You cannot get remote hub JID in local mode!");
		}
		return JID.jidInstance(jaxmpp.getSessionObject().getUserBareJid(), "iot-hub");
	}

	public boolean isRemoteMode() {
		return pep;
	}

	public void setRemoteMode(boolean remote) {
		this.pep = remote;
	}

	public String nodeForwardEncoder(JID jid, String node) {
		if (isRemoteMode()) {
			if (node == null) {
				node = "";
			}
			String resource = jid.getResource();
			if (resource == null) {
				resource = "";
			}
			return "forward:" + jid.getBareJid() + "/" + resource + "/" + node;
		}
		return node;
	}

	private void processNotification(JID jid, String nodeName, Element payload) {
		Device device = getDeviceByNode(nodeName);
		if (device == null) {
			if (nodeName.endsWith("/config")) {
				String tmp = nodeName.substring(0, nodeName.length() - "/config".length());
				try {
					checkForNewDevice(jid, tmp);
				} catch (JaxmppException ex) {
					log.log(Level.WARNING, "failed to check for new device at " + jid + " node " + tmp);
				}
			}

			return;
		}

		Device.IValue value = device.parsePayload(payload);
		if (value != null) {
			device.updateValue(value);
		}
	}

	/**
	 * Mathod will clear list of all known devices and will execute discovery of devices to find all existing devices.
	 *
	 * @throws JaxmppException
	 */
	public void refreshDevices() throws JaxmppException {
		devices.clear();
		jaxmpp.getModule(FeatureProviderModule.class).resetNewDevicesNodes();

		final JID pubsubJid = getPubSubJid();
		jaxmpp.getModule(DiscoveryModule.class)
				.getItems(pubsubJid, devicesNode, new DiscoveryModule.DiscoItemsAsyncCallback() {

					@Override
					public void onError(Stanza responseStanza, XMPPException.ErrorCondition error)
							throws JaxmppException {
						// ignoring for now
						devicesNodesDiscoveryFinished();
					}

					@Override
					public void onInfoReceived(String node, ArrayList<DiscoveryModule.Item> items) throws XMLException {
						devicesNodesFound(node, items);
					}

					@Override
					public void onTimeout() throws JaxmppException {
						// ignoring for now
						devicesNodesDiscoveryFinished();
					}
				});

	}

	protected void updateCapsOnDevicesChange() {
		try {
			List<String> features = new ArrayList<>();
			for (Device device : devices) {
				//features.add(device.getNode() + "/state");
				features.add(device.getNode());
			}
			jaxmpp.getModule(FeatureProviderModule.class).setDevicesNodes(features);
			jaxmpp.getSessionObject().setProperty("XEP115VerificationString", null);
			jaxmpp.getModule(PresenceModule.class).sendInitialPresence();
			if (!pep) {
				Presence presence = Stanza.createPresence();
				presence.setTo(getPubSubJid());
				jaxmpp.getEventBus()
						.fire(new PresenceModule.BeforePresenceSendHandler.BeforePresenceSendEvent(
								jaxmpp.getSessionObject(), presence,
								new JaxmppEventWithCallback.RunAfter<PresenceModule.BeforePresenceSendHandler.BeforePresenceSendEvent>() {

									@Override
									public void after(
											PresenceModule.BeforePresenceSendHandler.BeforePresenceSendEvent event) {
										try {
											jaxmpp.send(event.getPresence());
										} catch (JaxmppException ex) {
											ex.printStackTrace();
										}
									}
								}));
			}
		} catch (JaxmppException ex) {
			ex.printStackTrace();
		}
	}

	public interface BiConsumer<A, B> {

		void accept(A a, B b);

	}

	/**
	 * Interface required to be implemented by device list change observers.
	 */
	public interface ChangedHandler
			extends EventHandler {

		void devicesChanged(List<Device> devices);

		/**
		 * Event fired when list of known/discovered devices changes.
		 */
		class ChangedEvent
				extends Event<ChangedHandler> {

			private final List<Device> devices;

			public ChangedEvent(List<Device> devices) {
				this.devices = devices;
			}

			@Override
			public void dispatch(ChangedHandler handler) {
				handler.devicesChanged(devices);
			}

		}
	}

	public interface DevicesInfoRetrieved {

		void onDeviceInfoRetrieved(Map<JID, DiscoveryModule.Identity> devicesInfo);

	}

	public static class FeatureProviderModule
			implements XmppModule {

		private final String rootFeature;

		private List<String> devicesNodes = new ArrayList<>();
		private List<String> features = new ArrayList<>();
		private List<String> newDevicesNodes = new ArrayList<>();

		public FeatureProviderModule(String node) {
			rootFeature = node + "+notify";
		}

		public void addNewDevice(String node) {
			newDevicesNodes.add(node);
			updateFeatures();
		}

		@Override
		public Criteria getCriteria() {
			return null;
		}

		@Override
		public String[] getFeatures() {
			return features.toArray(new String[features.size()]);
		}

		@Override
		public void process(Element element) throws XMPPException, XMLException, JaxmppException {

		}

		public void resetNewDevicesNodes() {
			newDevicesNodes.clear();
			updateFeatures();
		}

		public void setDevicesNodes(List<String> devicesNodes) {
			this.devicesNodes = devicesNodes;
			this.updateFeatures();
		}

		public void updateFeatures() {
			List<String> nodes = new ArrayList<>();
			nodes.add(rootFeature);
			for (String node : devicesNodes) {
				nodes.add(node + "+notify");
			}
			for (String node : newDevicesNodes) {
				nodes.add(node + "+notify");
			}
			this.features = nodes;
		}
	}

	private class Counter {

		private int counter;

		Counter(int counter) {
			this.counter = counter;
		}

		void decrement() {
			counter--;
		}

		int value() {
			return counter;
		}
	}
}
