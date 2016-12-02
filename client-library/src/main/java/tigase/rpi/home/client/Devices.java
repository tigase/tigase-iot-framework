package tigase.rpi.home.client;

import tigase.jaxmpp.core.client.*;
import tigase.jaxmpp.core.client.criteria.Criteria;
import tigase.jaxmpp.core.client.eventbus.Event;
import tigase.jaxmpp.core.client.eventbus.EventHandler;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Message;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.rpi.home.client.devices.LightDimmer;
import tigase.rpi.home.client.devices.LightSensor;
import tigase.rpi.home.client.devices.MovementSensor;
import tigase.rpi.home.client.devices.TemperatureSensor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 26.11.2016.
 */
public class Devices {

	private static final Logger log = Logger.getLogger(Devices.class.getCanonicalName());

	private final String devicesNode;
	private final JaxmppCore jaxmpp;
	private final Boolean pep;

	private List<Device> devices = new ArrayList<Device>();

	public Devices(JaxmppCore jaxmpp, boolean pep) {
		this(jaxmpp, "devices", pep);
	}

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
	}

	private Device getDeviceByNode(String nodeName) {
		for (Device device : devices) {
			if (device.getNode().equals(nodeName))
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

	public List<Device> getDevices() {
		return devices;
	}

	protected JID getPubSubJid() {
		if (pep != null) {
			return JID.jidInstance(jaxmpp.getSessionObject().getUserBareJid());
		} else {
			return JID.jidInstance("pubsub." + jaxmpp.getSessionObject().getUserBareJid().getDomain());
		}
	}

	protected void refreshDevices() throws JaxmppException {
		devices.clear();
		final JID pubsubJid = getPubSubJid();
		jaxmpp.getModule(DiscoveryModule.class).getItems(pubsubJid, devicesNode, new DiscoveryModule.DiscoItemsAsyncCallback() {
			@Override
			public void onInfoReceived(String node, ArrayList<DiscoveryModule.Item> items) throws XMLException {
				for (final DiscoveryModule.Item item : items) {
					try {
						Device.retrieveConfiguration(jaxmpp, pubsubJid, node, new Device.Callback<Device.Configuration>() {

							@Override
							public void onError(XMPPException.ErrorCondition error) {

							}

							@Override
							public void onSuccess(Device.Configuration config) {
								devices.add(createDevice(item, config));
							}
						});
					} catch (JaxmppException ex) {
						log.log(Level.WARNING, "Failed to retrieve device configuration", ex);
					}
				}
			}

			@Override
			public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
				// ignoring for now
			}

			@Override
			public void onTimeout() throws JaxmppException {
				// ignoring for now
			}
		});
	}

	protected Device createDevice(DiscoveryModule.Item item, Device.Configuration config) {
		// TODO - use field from config to select device class!
		try {
			String type = (String) config.getValue().getField("type").getFieldValue();
			if (type == null) {
				return null;
			}

			switch (type) {
				case "movement-sensor":
					return new MovementSensor(jaxmpp, item.getJid(), item.getNode(), item.getName());
				case "light-dimmer":
					return new LightDimmer(jaxmpp, item.getJid(), item.getNode(), item.getName());
				case "light-sensor":
					return new LightSensor(jaxmpp, item.getJid(), item.getNode(), item.getName());
				case "temperature-sensor":
					return new TemperatureSensor(jaxmpp, item.getJid(), item.getNode(), item.getName());
				default:
					return null;
			}
		} catch (XMLException ex) {
			return null;
		}
	}

	public interface ChangedHandler extends EventHandler {

		void devicesChanged(List<Device> devices);

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
}
