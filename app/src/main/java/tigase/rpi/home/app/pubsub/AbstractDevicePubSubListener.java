package tigase.rpi.home.app.pubsub;

import tigase.bot.RequiredXmppModules;
import tigase.bot.runtime.AbstractXmppBridge;
import tigase.bot.runtime.XmppService;
import tigase.eventbus.HandleEvent;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.XmppModule;
import tigase.jaxmpp.core.client.criteria.Criteria;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubModule;
import tigase.jaxmpp.j2se.Jaxmpp;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by andrzej on 02.11.2016.
 */
@RequiredXmppModules({PubSubModule.class, PresenceModule.class, DiscoveryModule.class, CapabilitiesModule.class,
					  AbstractDevicePubSubListener.FeatureProviderModule.class})
public abstract class AbstractDevicePubSubListener
		extends AbstractXmppBridge {

	private static final Logger log = Logger.getLogger(PresenceModule.class.getCanonicalName());
	protected Set<String> features = new CopyOnWriteArraySet<>();
	protected Set<String> observedNodes;
	@Inject(nullAllowed = true)
	protected List<DeviceListener> observers;
	@ConfigField(desc = "Root node")
	private String rootNode = "devices";
	@Inject
	private XmppService xmppService;

	@Override
	public void beforeUnregister() {
		super.beforeUnregister();
		xmppService.getAllConnections().stream().forEach(jaxmpp -> {
			jaxmpp.getModule(FeatureProviderModule.class).setFeatures(AbstractDevicePubSubListener.this, null);
			if (jaxmpp.isConnected()) {
				try {
					jaxmpp.getModule(PresenceModule.class).sendInitialPresence();
				} catch (JaxmppException ex) {
					log.log(Level.WARNING, "failed to send update presence", ex);
				}
			}
		});
	}

	protected String getDeviceNode(String deviceId) {
		return rootNode + "/" + deviceId;
	}

	protected Stream<String> getObservedNodes() {
		return observedNodes.stream();
	}

	@Override
	protected void jaxmppConnected(Jaxmpp jaxmpp) {
		jaxmpp.getModule(FeatureProviderModule.class).setFeatures(this, features);
	}

	@Override
	protected void jaxmppDisconnected(Jaxmpp jaxmpp) {
	}

	@HandleEvent
	public void onJaxmppAdded(XmppService.JaxmppAddedEvent event) {
		event.jaxmpp.getModule(FeatureProviderModule.class).setFeatures(this, features);
	}

	@HandleEvent
	public void onNotificationReceived(PubSubModule.NotificationReceivedHandler.NotificationReceivedEvent event) {
		if (observedNodes.contains(event.getNodeName())) {
			onNotificationReceived(event.getNodeName(), event.getPayload());
		}
	}

	public abstract void onNotificationReceived(String node, Element event);

	protected String parseDeviceId(String node) {
		String[] parts = node.split("/");
		if (parts.length > 1 && rootNode.equals(parts[0])) {
			return parts[1];
		}
		return null;
	}

	public void setObservers(List<DeviceListener> observers) {
		if (observers == null) {
			observers = new ArrayList<>();
		}

		this.observers = observers;
		this.features.clear();
		this.observedNodes = observers.stream()
				.flatMap(o -> o.getObservedDevicesNodes().stream().map(deviceId -> getDeviceNode(deviceId)))
				.collect(Collectors.toSet());
		getObservedNodes().map(node -> node + "+notify").forEach(feature -> this.features.add(feature));

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

	public interface DeviceListener {

		List<String> getObservedDevicesNodes();

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
