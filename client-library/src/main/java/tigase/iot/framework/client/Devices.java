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
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Message;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class handler device discovery and management based on PubSub nodes.
 * 
 * Created by andrzej on 26.11.2016.
 */
public class Devices {

	private static final Logger log = Logger.getLogger(Devices.class.getCanonicalName());

	private final String devicesNode;
	private final JaxmppCore jaxmpp;
	private final boolean pep;

	private List<Device> devices = new ArrayList<Device>();

	public Devices(JaxmppCore jaxmpp, boolean pep) {
		this(jaxmpp, "devices", pep);
	}

	/**
	 * Create instance of device manager for devices registered at device node passed as argument.
	 * @param jaxmpp
	 * @param devicesNode
	 * @param pep
	 */
	public Devices(JaxmppCore jaxmpp, String devicesNode, boolean pep) {
		this.jaxmpp = jaxmpp;
		this.pep = pep;
		this.devicesNode = devicesNode;

		jaxmpp.getModulesManager().register(new FeatureProviderModule(devicesNode));

		this.jaxmpp.getModule(PubSubModule.class).addNotificationReceivedHandler(new PubSubModule.NotificationReceivedHandler() {
			@Override
			public void onNotificationReceived(SessionObject sessionObject, Message message, JID pubSubJID,
											   String nodeName, String itemId, Element payload, Date delayTime,
											   String itemType) {
				processNotification(nodeName, payload);
			}
		});

		this.jaxmpp.getEventBus().addHandler(JaxmppCore.LoggedInHandler.LoggedInEvent.class, new JaxmppCore.LoggedInHandler() {
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

	private Device getDeviceByNode(String nodeName) {
		for (Device device : devices) {
			if (nodeName.startsWith(device.getNode()))
				return device;
		}

		return null;
	}

	private void processNotification(String nodeName, Element payload) {
		Device device = getDeviceByNode(nodeName);
		if (device == null) {
			return;
		}

		Device.IValue value = device.parsePayload(payload);
		if (value != null) {
			device.updateValue(value);
		}
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

	/**
	 * Mathod will clear list of all known devices and will execute discovery of devices to find all existing devices.
	 * @throws JaxmppException
	 */
	public void refreshDevices() throws JaxmppException {
		devices.clear();
		final JID pubsubJid = getPubSubJid();
		jaxmpp.getModule(DiscoveryModule.class).getItems(pubsubJid, devicesNode, new DiscoveryModule.DiscoItemsAsyncCallback() {

			private Integer counter;

			@Override
			public void onInfoReceived(String node, ArrayList<DiscoveryModule.Item> items) throws XMLException {
				counter = items.size();
				for (final DiscoveryModule.Item item : items) {
					try {
						Device.retrieveConfiguration(jaxmpp, pubsubJid, item.getNode(), new Device.Callback<Device.Configuration>() {

							@Override
							public void onError(XMPPException.ErrorCondition error) {
								checkAndNotify();
							}

							@Override
							public void onSuccess(Device.Configuration config) {
								devices.add(createDevice(item, config));
								checkAndNotify();
							}
						});
					} catch (JaxmppException ex) {
						log.log(Level.WARNING, "Failed to retrieve device configuration", ex);
					}
				}
				if (items.isEmpty()) {
					jaxmpp.getEventBus().fire(new ChangedHandler.ChangedEvent(devices));
				}
			}

			@Override
			public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
				// ignoring for now
				jaxmpp.getEventBus().fire(new ChangedHandler.ChangedEvent(devices));
			}

			@Override
			public void onTimeout() throws JaxmppException {
				// ignoring for now
				jaxmpp.getEventBus().fire(new ChangedHandler.ChangedEvent(devices));
			}

			private void checkAndNotify() {
				synchronized (this) {
					counter--;
					if (counter == 0) {
						jaxmpp.getEventBus().fire(new ChangedHandler.ChangedEvent(devices));
					}
				}
			}
		});

	}

	/**
	 * Discovers list of host devices connected to local IoT hub.
	 * 
	 * @param devicesInfoRetrieved
	 * @throws JaxmppException
	 */
	public void getActiveDeviceHosts(final DevicesInfoRetrieved devicesInfoRetrieved) throws JaxmppException {
		JabberDataElement form = new JabberDataElement(XDataType.submit);
		form.addTextSingleField("domainjid", jaxmpp.getSessionObject().getUserBareJid().getDomain());
		form.addTextSingleField("max_items", "100");

		AdHocCommansModule adHocCommansModule = jaxmpp.getModule(AdHocCommansModule.class);
		adHocCommansModule.execute(JID.jidInstance("sess-man", jaxmpp.getSessionObject().getUserBareJid().getDomain()), "http://jabber.org/protocol/admin#get-online-users-list",
								   Action.execute, form, new AdHocCommansModule.AdHocCommansAsyncCallback() {

					private Integer counter;
					private Map<JID, DiscoveryModule.Identity> discoveredDevices = new HashMap<>();

					@Override
					public void onError(Stanza responseStanza, XMPPException.ErrorCondition error)
							throws JaxmppException {
						devicesInfoRetrieved.onDeviceInfoRetrieved(new HashMap<JID, DiscoveryModule.Identity>());
					}

					@Override
					public void onTimeout() throws JaxmppException {
						devicesInfoRetrieved.onDeviceInfoRetrieved(new HashMap<JID, DiscoveryModule.Identity>());
					}

					@Override
					protected void onResponseReceived(String sessionid, String node, State status,
													  JabberDataElement data) throws JaxmppException {
						TextMultiField field = (TextMultiField) data.getFields().get(0);
						String[] jids = field.getFieldValue();
						counter = jids.length;
						for (String jidStr : jids) {
							final JID jid = JID.jidInstance(jidStr.split(" ")[0] + "/iot");
							DiscoveryModule discoveryModule = jaxmpp.getModule(DiscoveryModule.class);
							discoveryModule.getInfo(jid, new DiscoveryModule.DiscoInfoAsyncCallback(null)  {
								@Override
								protected void onInfoReceived(String node,
															  Collection<DiscoveryModule.Identity> identities,
															  Collection<String> features) throws XMLException {
									for (DiscoveryModule.Identity identity : identities) {
										if ("device".equals(identity.getCategory()) && "iot".equals(identity.getType())) {
											discoveredDevices.put(jid, identity);
										}
									}
									checkAndNotify();
								}

								@Override
								public void onError(Stanza responseStanza, XMPPException.ErrorCondition error)
										throws JaxmppException {
									checkAndNotify();
								}

								@Override
								public void onTimeout() throws JaxmppException {
									checkAndNotify();
								}
							});
						}
					}

					private void checkAndNotify() {
						synchronized (this) {
							counter--;
							if (counter == 0) {
								devicesInfoRetrieved.onDeviceInfoRetrieved(discoveredDevices);
							}
						}
					}

				});
	}

	/**
	 * Method creates device representation based on received information about device from its PubSub node.
	 * @param item
	 * @param config
	 * @return
	 */
	protected Device createDevice(DiscoveryModule.Item item, Device.Configuration config) {
		// TODO - use field from config to select device class!
		try {
			Field field = config.getValue().getField("type");
			if (field == null) {
				return null;
			}
			String type = (String) field.getFieldValue();
			if (type == null) {
				return null;
			}

			switch (type) {
				case "movement-sensor":
					return new MovementSensor(jaxmpp, item.getJid(), item.getNode(), item.getName());
				case "tv-sensor":
					return new TvSensor(jaxmpp, item.getJid(), item.getNode(), item.getName());
				case "light-dimmer":
					return new LightDimmer(jaxmpp, item.getJid(), item.getNode(), item.getName());
				case "light-sensor":
					return new LightSensor(jaxmpp, item.getJid(), item.getNode(), item.getName());
				case "temperature-sensor":
					return new TemperatureSensor(jaxmpp, item.getJid(), item.getNode(), item.getName());
				case "switch":
					return new Switch(jaxmpp, item.getJid(), item.getNode(), item.getName());
				default:
					return null;
			}
		} catch (XMLException ex) {
			return null;
		}
	}

	/**
	 * Interface required to be implemented by device list change observers.
	 */
	public interface ChangedHandler extends EventHandler {

		void devicesChanged(List<Device> devices);

		/**
		 * Event fired when list of known/discovered devices changes.
		 */
		class ChangedEvent extends Event<ChangedHandler> {

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
	
	public static class FeatureProviderModule
			implements XmppModule {

		private String[] features;

		public FeatureProviderModule(String node) {
			features = new String[] { node + "+notify" };
		}

		@Override
		public Criteria getCriteria() {
			return null;
		}

		@Override
		public String[] getFeatures() {
			return features;
		}

		@Override
		public void process(Element element) throws XMPPException, XMLException, JaxmppException {

		}
	}

	public interface DevicesInfoRetrieved {

		void onDeviceInfoRetrieved(Map<JID, DiscoveryModule.Identity> devicesInfo);

	}
}
