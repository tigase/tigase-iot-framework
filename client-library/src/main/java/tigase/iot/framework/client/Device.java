/*
 * Device.java
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

import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.eventbus.Event;
import tigase.jaxmpp.core.client.eventbus.EventHandler;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.forms.AbstractField;
import tigase.jaxmpp.core.client.xmpp.forms.JabberDataElement;
import tigase.jaxmpp.core.client.xmpp.forms.TextSingleField;
import tigase.jaxmpp.core.client.xmpp.forms.XDataType;
import tigase.jaxmpp.core.client.xmpp.modules.adhoc.Action;
import tigase.jaxmpp.core.client.xmpp.modules.adhoc.AdHocCommansModule;
import tigase.jaxmpp.core.client.xmpp.modules.adhoc.State;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubErrorCondition;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.core.client.xmpp.utils.DateTimeFormat;

import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract class providing base implementation for client side device representation.
 *
 * Created by andrzej on 26.11.2016.
 */
public abstract class Device<S extends Device.IValue> {

	private static final Logger log = Logger.getLogger(Device.class.getCanonicalName());

	private final Devices devices;
	private final JaxmppCore jaxmpp;
	private final JID pubsubJid;
	private final String id;
	private final String node;
	private final String name;

	private S value;
	private ValueChangedHandler<S> observer;

	public Device(Devices devices, JaxmppCore jaxmpp, JID pubsubJid, String node, String name) {
		this.devices = devices;
		this.jaxmpp = jaxmpp;
		this.pubsubJid = pubsubJid;
		this.id = node.split("/")[1];
		this.node = node;
		this.name = name;
	}

	/**
	 * Retrieve name of device
	 * @return device name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set device name
	 *
	 * @param name of device
	 * @param callback fired when name is set
	 * @throws JaxmppException
	 */
	public void setName(final String name, final Callback<String> callback) throws JaxmppException {
		if (this.name.equals(name)) {
			callback.onSuccess(name);
			return;
		}

		jaxmpp.getModule(PubSubModule.class).getNodeConfiguration(pubsubJid.getBareJid(), node, new PubSubModule.NodeConfigurationAsyncCallback() {
			@Override
			protected void onReceiveConfiguration(IQ responseStanza, String node, JabberDataElement config) {
				try {
					TextSingleField field = config.getField("pubsub#title");
					if (field == null) {
						config.addTextSingleField("pubsub#title", name);
					} else {
						field.setFieldValue(name);
					}
					jaxmpp.getModule(PubSubModule.class).configureNode(pubsubJid.getBareJid(), node, config, new AsyncCallback() {
						@Override
						public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
							callback.onError(error);
						}

						@Override
						public void onSuccess(Stanza responseStanza) throws JaxmppException {
							callback.onSuccess(name);
						}

						@Override
						public void onTimeout() throws JaxmppException {
							callback.onError(null);
						}
					});

				} catch (JaxmppException ex) {
					callback.onError(XMPPException.ErrorCondition.internal_server_error);
				}
			}

			@Override
			protected void onEror(IQ response, XMPPException.ErrorCondition errorCondition,
								  PubSubErrorCondition pubSubErrorCondition) throws JaxmppException {
				callback.onError(errorCondition);
			}

			@Override
			public void onTimeout() throws JaxmppException {
				callback.onError(null);
			}
		});
	}

	/**
	 * Retrieve name of pubsub node of a device
	 * @return
	 */
	public String getNode() {
		return node;
	}

	public String getId() {
		return id;
	}

	/**
	 * Retrieve cached device state/value
	 * @return
	 */
	public S getValue() {
		return value;
	}

	/**
	 * Retrieve device state/value from PubSub node
	 * @param callback - called with retrieval result
	 * @throws JaxmppException
	 */
	public void getValue(final Callback<S> callback) throws JaxmppException {
		jaxmpp.getModule(PubSubModule.class).retrieveItem(pubsubJid.getBareJid(), node + "/state", null, 1, new PubSubModule.RetrieveItemsAsyncCallback() {
			@Override
			protected void onRetrieve(IQ responseStanza, String nodeName, Collection<Item> items) {
				S value = null;
				if (items != null && !items.isEmpty()) {
					Item item = items.iterator().next();
					value = parsePayload(item.getPayload());
				}
				updateValue(value);
				callback.onSuccess(value);
			}

			@Override
			protected void onEror(IQ response, XMPPException.ErrorCondition errorCondition,
								  PubSubErrorCondition pubSubErrorCondition) throws JaxmppException {
				callback.onError(errorCondition);
			}

			@Override
			public void onTimeout() throws JaxmppException {
				callback.onError(null);
			}
		});
	}

	/**
	 * Method called to retrieve device configuration from PubSub node
	 * @param jaxmpp
	 * @param pubsubJid
	 * @param deviceNode
	 * @param callback
	 * @throws JaxmppException
	 */
	public static void retrieveConfiguration(JaxmppCore jaxmpp, JID pubsubJid, String deviceNode,
											 final Callback<Configuration> callback) throws JaxmppException {
		jaxmpp.getModule(PubSubModule.class).retrieveItem(pubsubJid.getBareJid(), deviceNode + "/config", null, 1, new PubSubModule.RetrieveItemsAsyncCallback() {
			@Override
			protected void onRetrieve(IQ responseStanza, String nodeName, Collection<Item> items) {
				Configuration config = null;
				if (!items.isEmpty()) {
					try {
						config = parseConfig(items.iterator().next().getPayload());
					} catch (JaxmppException ex) {
						log.log(Level.WARNING, "Failed to retrieve device configuration", ex);
					}
				}
				if (config == null) {
					callback.onError(null);
				} else {
					callback.onSuccess(config);
				}
			}

			@Override
			protected void onEror(IQ response, XMPPException.ErrorCondition errorCondition,
								  PubSubErrorCondition pubSubErrorCondition) throws JaxmppException {
				callback.onError(errorCondition);
			}

			@Override
			public void onTimeout() throws JaxmppException {
				callback.onError(null);
			}
		});
	}

	/**
	 * Metod retrieves device configuration from PubSub node
	 * @param callback
	 * @throws JaxmppException
	 */
	public void retrieveConfiguration(final Callback<Configuration> callback) throws JaxmppException {
		Device.retrieveConfiguration(jaxmpp, pubsubJid, node, callback);
	}

	/**
	 * Set configuration of the device.
	 * It will result in configuration being published to PubSub node and delivered to the actual device.
	 *
	 * @param config
	 * @param callback
	 * @throws JaxmppException
	 */
	public void setConfiguration(final JabberDataElement config, final Callback<Configuration> callback) throws JaxmppException {
		Element timestamp = ElementFactory.create("timestamp");
		final Date date = new Date();
		timestamp.setAttribute("value", new DateTimeFormat().format(date));

		timestamp.addChild(config);

		jaxmpp.getModule(PubSubModule.class).publishItem(pubsubJid.getBareJid(), node + "/config", null, timestamp, new PubSubModule.PublishAsyncCallback() {
			@Override
			public void onTimeout() throws JaxmppException {
				callback.onError(null);
			}

			@Override
			protected void onEror(IQ response, XMPPException.ErrorCondition errorCondition,
								  PubSubErrorCondition pubSubErrorCondition) throws JaxmppException {
				callback.onError(errorCondition);
			}

			@Override
			public void onPublish(String itemId) {
				callback.onSuccess(new Configuration(config, date));
			}
		});
	}

	public void remove(final Callback<Object> callback) throws JaxmppException {
		String node = devices.nodeForwardEncoder(JID.jidInstance("pubsub.tigase-iot-hub.local"), this.node);
		jaxmpp.getModule(DiscoveryModule.class).getInfo(devices.isRemoteMode() ? devices.getRemoteHubJid() : pubsubJid, node, new AsyncCallback() {
			@Override
			public void onError(Stanza responseStanza, XMPPException.ErrorCondition error)
					throws JaxmppException {
				callback.onError(error);
			}

			@Override
			public void onSuccess(Stanza responseStanza) throws JaxmppException {
				Element x = responseStanza.findChild(new String[] { "iq", "query", "x"});
				if (x != null) {
					JabberDataElement data = new JabberDataElement(x);
					if (data != null) {
						AbstractField<JID> creator = data.getField("pubsub#creator");
						if (creator != null) {
							JabberDataElement form = new JabberDataElement(XDataType.submit);
							form.addFixedField("device", id);

							JID creatorJid = creator.getFieldValue();
							if (creatorJid.getResource() == null) {
								creatorJid = JID.jidInstance(creatorJid.getBareJid(), "iot");
							}
							devices.executeDeviceHostAdHocCommand(creatorJid, "remove-device", Action.execute,
																			   form, new AdHocCommansModule.AdHocCommansAsyncCallback() {
										@Override
										protected void onResponseReceived(String sessionid, String node, State status,
																		  JabberDataElement data)
												throws JaxmppException {
											callback.onSuccess(null);
										}

										@Override
										public void onError(Stanza responseStanza, XMPPException.ErrorCondition error)
												throws JaxmppException {
											callback.onError(error);
										}

										@Override
										public void onTimeout() throws JaxmppException {
											callback.onError(XMPPException.ErrorCondition.remote_server_timeout);
										}
									});
						} else {
							callback.onError(XMPPException.ErrorCondition.internal_server_error);
						}
					}
				} else {
					callback.onError(XMPPException.ErrorCondition.internal_server_error);
				}
			}

			@Override
			public void onTimeout() throws JaxmppException {
				callback.onError(XMPPException.ErrorCondition.remote_server_timeout);
			}
		});
	}

	/**
	 * Set or replace device value observer. It will be called whenever device value changes.
	 * @param observer
	 */
	public void setObserver(ValueChangedHandler<S> observer) {
		this.observer = observer;
	}

	/**
	 * Method user to parse element and create device configuration change event from it.
	 * 
	 * @param payload
	 * @return
	 * @throws JaxmppException
	 */
	protected static Configuration parseConfig(Element payload) throws JaxmppException {
		Element valueEl = payload.getFirstChild();
		Date timestamp = parseTimestamp(payload);
		if (timestamp == null || valueEl == null) {
			throw  new JaxmppException("Invalid value to parse");
		}

		JabberDataElement data = new JabberDataElement(valueEl);
		return new Configuration(data, timestamp);
	}

	/**
	 * Method to parse element and retrieve timestamp from it.
	 * @param payload
	 * @return
	 * @throws XMLException
	 */
	protected static Date parseTimestamp(Element payload) throws XMLException {
		if (payload == null || !"timestamp".equals(payload.getName())) {
			return null;
		}
		return new DateTimeFormat().parse(payload.getAttribute("value"));
	}

	/**
	 * Method called when local device representation is informed that remote
	 * device state/value has changed.
	 * 
	 * @param newValue
	 */
	protected void updateValue(S newValue) {
		if (value != null && value.getTimestamp().getTime() > newValue.getTimestamp().getTime()) {
			return;
		}

		value = newValue;
		if (observer != null) {
			observer.valueChanged(value);
		}

		jaxmpp.getEventBus().fire(new ValueChangedHandler.ValueChangedEvent<S>(newValue));
	}

	/**
	 * Method informs remote device that its value is being changed. It should result on
	 * value change on the remote device.
	 * 
	 * @param newValue
	 * @param callback
	 * @throws JaxmppException
	 */
	protected void setValue(final S newValue, final Callback<S> callback) throws JaxmppException {
		Element payload = encodeToPayload(newValue);
		jaxmpp.getModule(PubSubModule.class).publishItem(pubsubJid.getBareJid(), node + "/state", null, payload, new AsyncCallback() {
			@Override
			public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
				callback.onError(error);
			}

			@Override
			public void onSuccess(Stanza responseStanza) throws JaxmppException {
				Device.this.value = newValue;
				callback.onSuccess(newValue);
			}

			@Override
			public void onTimeout() throws JaxmppException {
				callback.onError(null);
			}
		});
	}

	/**
	 * Method serializes value to element representation.
	 * @param value
	 * @return
	 */
	protected abstract Element encodeToPayload(S value);

	/**
	 * Method converts element into value object.
	 * @param item
	 * @return
	 */
	protected abstract S parsePayload(Element item);

	/**
	 * Base class for objects representing value/state of remote device.
	 * @param <D>
	 */
	public static class Value<D> implements IValue<D> {

		private final Date timestamp;
		private final D value;

		public Value(D value, Date timestamp) {
			this.value = value;
			this.timestamp = timestamp;
		}

		@Override
		public Date getTimestamp() {
			return timestamp;
		}

		@Override
		public D getValue() {
			return value;
		}
	}

	/**
	 * Class represents configuration of a device.
	 */
	public static class Configuration extends Value<JabberDataElement> {

		public Configuration(JabberDataElement value, Date timestamp) {
			super(value, timestamp);
		}
	}

	/**
	 * Interface which needs to be implemented by all classes representing device state/value.
	 * @param <D>
	 */
	public interface IValue<D> {

		/**
		 * Get timestamp of value/state change
		 * @return
		 */
		Date getTimestamp();

		/**
		 * Get new value/state of a device
		 * @return
		 */
		D getValue();

	}

	/**
	 * Interface implemented by callbacks which should be called as a result of execution of asynchronous methods.
	 * @param <T>
	 */
	public interface Callback<T> {

		/**
		 * Called when call returned error.
		 * @param error
		 */
		void onError(XMPPException.ErrorCondition error);

		/**
		 * Called when call returned success.
		 * @param result
		 */
		void onSuccess(T result);

	}

	/**
	 * Interface which needs to be implemented by any class which wants to observer device state/value changes.
	 * @param <T>
	 */
	public interface ValueChangedHandler<T extends IValue> extends EventHandler {

		void valueChanged(T value);

		/**
		 * Event fired when value/state of device changes.
		 * @param <T>
		 */
		class ValueChangedEvent<T extends IValue> extends Event<ValueChangedHandler<T>> {

			private final T value;

			public ValueChangedEvent(T value) {
				this.value = value;
			}

			@Override
			public void dispatch(ValueChangedHandler<T> handler) {
				handler.valueChanged(value);
			}

		}
	}
}
