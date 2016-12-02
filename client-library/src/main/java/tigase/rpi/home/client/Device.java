package tigase.rpi.home.client;

import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.eventbus.Event;
import tigase.jaxmpp.core.client.eventbus.EventHandler;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.forms.JabberDataElement;
import tigase.jaxmpp.core.client.xmpp.forms.XDataType;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubErrorCondition;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.core.client.xmpp.utils.DateTimeFormat;

import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;

/**
 * Created by andrzej on 26.11.2016.
 */
public abstract class Device<S extends Device.IValue> {

	private final JaxmppCore jaxmpp;
	private final JID pubsubJid;
	private final String node;
	private final String name;

	private S value;

	public Device(JaxmppCore jaxmpp, JID pubsubJid, String node, String name) {
		this.jaxmpp = jaxmpp;
		this.pubsubJid = pubsubJid;
		this.node = node;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name, final Callback<String> callback) throws JaxmppException {
		if (this.name.equals(name)) {
			callback.onSuccess(name);
			return;
		}

		JabberDataElement config = new JabberDataElement(XDataType.submit);
		config.addTextSingleField("pubsub#title", name);
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
	}

	public String getNode() {
		return node;
	}

	public S getValue() {
		return value;
	}

	public void getValue(final Callback<S> callback) throws JaxmppException {
		jaxmpp.getModule(PubSubModule.class).retrieveItem(pubsubJid.getBareJid(), node, null, 1, new PubSubModule.RetrieveItemsAsyncCallback() {
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

	protected static Configuration parseConfig(Element payload) throws JaxmppException {
		Element valueEl = payload.getFirstChild();
		Date timestamp = parseTimestamp(payload);
		if (timestamp == null || valueEl == null) {
			throw  new JaxmppException("Invalid value to parse");
		}

		JabberDataElement data = new JabberDataElement(valueEl);
		return new Configuration(data, timestamp);
	}

	protected static Date parseTimestamp(Element payload) throws XMLException {
		if (payload == null || !"timestamp".equals(payload.getName())) {
			return null;
		}
		return new DateTimeFormat().parse(payload.getAttribute("value"));
	}

	protected void updateValue(S newValue) {
		if (value != null && value.getTimestamp().getTime() > newValue.getTimestamp().getTime()) {
			return;
		}

		value = newValue;

		jaxmpp.getEventBus().fire(new ValueChangedHandler.ValueChangedEvent<S>(newValue));
	}

	protected void setValue(final S newValue, final Callback<S> callback) throws JaxmppException {
		Element payload = encodeToPayload(newValue);
		jaxmpp.getModule(PubSubModule.class).publishItem(pubsubJid.getBareJid(), node, null, payload, new AsyncCallback() {
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

	protected abstract Element encodeToPayload(S value);
	protected abstract S parsePayload(Element item);

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

	public static class Configuration extends Value<JabberDataElement> {

		public Configuration(JabberDataElement value, Date timestamp) {
			super(value, timestamp);
		}
	}


	public interface IValue<D> {

		Date getTimestamp();

		D getValue();

	}

	public interface Callback<T> {

		void onError(XMPPException.ErrorCondition error);

		void onSuccess(T result);

	}

	public interface ValueChangedHandler<T extends IValue> extends EventHandler {

		void valueChanged(T value);

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
