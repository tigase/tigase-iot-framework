package tigase.rpi.home.app.devices;

import tigase.bot.iot.IValue;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;
import tigase.rpi.home.app.pubsub.ExtendedPubSubNodesManager;
import tigase.rpi.home.app.pubsub.PubSubNodesManager;
import tigase.rpi.home.devices.LightDimmer;
import tigase.rpi.home.values.Light;
import tigase.rpi.home.values.Movement;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 07.11.2016.
 */
public class ReactiveLightDimmer
		extends LightDimmer
		implements PubSubNodesManager.NodesObserver, ConfigurationChangedAware, Initializable {

	private static final Logger log = Logger.getLogger(ReactiveLightDimmer.class.getCanonicalName());

	private Map<String, Light> currentLightLevel = new ConcurrentHashMap<>();
	private Map<String, Movement> currentPirState = new ConcurrentHashMap<>();
	@ConfigField(desc = "Dim light level")
	private Integer dimLevel = 60;
	@ConfigField(desc = "Light sensor PubSub nodes devices")
	private ArrayList<String> lightSensorPubSubNodes = new ArrayList<>();
	private List<String> observedNodes = new ArrayList<>();
	@ConfigField(desc = "PIR sensor PubSub nodes devices")
	private ArrayList<String> pirSensorPubSubNodes = new ArrayList<>();
	@ConfigField(desc = "Require PIR movement to turn ON light")
	private Boolean requirePir = false;
	@ConfigField(desc = "Turn OFF if light higher than")
	private Integer turnOffIfHigherThan;
	@ConfigField(desc = "Turn ON if light lower than")
	private Integer turnOnIfLowerThan;

	@Inject
	private PubSubNodesManager pubSubNodesManager;

	@Override
	public List<String> getObservedNodes() {
		return Collections.unmodifiableList(observedNodes);
	}

	public void setLightSensorPubSubNodes(ArrayList<String> lightSensorPubSubNodes) {
		this.lightSensorPubSubNodes = lightSensorPubSubNodes;
		updateObservedNodes();
		currentLightLevel.keySet().removeIf(node -> !lightSensorPubSubNodes.contains(node));
	}

	public void setPirSensorPubSubNodes(ArrayList<String> pirSensorPubSubNodes) {
		this.pirSensorPubSubNodes = pirSensorPubSubNodes;
		updateObservedNodes();
		currentPirState.keySet().removeIf(node -> !pirSensorPubSubNodes.contains(node));
	}

	@Override
	public void beanConfigurationChanged(Collection<String> changes) {
		updateObservedNodes();
		if (pubSubNodesManager != null) {
			pubSubNodesManager.updateObservedNodes();
			updateState();
		}
	}

	@Override
	public void initialize() {
		updateState();
		super.initialize();
	}

	@HandleEvent
	public void onValueChanged(ExtendedPubSubNodesManager.ValueChangedEvent event) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "{0}, received value changed event from {1}",
					new Object[]{getName(), event.sourceId});
		}
		IValue value = (IValue) event.value;
		if (lightSensorPubSubNodes.contains(event.sourceId) && (value instanceof Light)) {
			synchronized (currentLightLevel) {
				Light oldValue = currentLightLevel.get(event.sourceId);
				if (oldValue == null || oldValue.getTimestamp().isBefore(value.getTimestamp())) {
					currentLightLevel.put(event.sourceId, (Light) value);
					updateState();
				}
			}
		}
		if (pirSensorPubSubNodes.contains(event.sourceId) && (value instanceof Movement)) {
			synchronized (currentPirState) {
				Movement oldValue = currentPirState.get(event.sourceId);
				if (oldValue == null || oldValue.getTimestamp().isBefore(value.getTimestamp())) {
					currentPirState.put(event.sourceId, (Movement) value);
					updateState();
				}
			}
		}
//		// TODO - fix this static name of PubSub node
//		// TODO - move to ExtendedPubSubNodesManager??
//		String deviceNode = "devices/" + getName() + "/state";
//		if (deviceNode.contains(event.sourceId) && (value instanceof Light)) {
//			Light newValue = (Light) value;
//			Light oldValue = getValue();
//			if (oldValue == null || (oldValue.getValue() != newValue.getValue()) &&
//					newValue.getTimestamp().isAfter(oldValue.getTimestamp())) {
//				setValue(newValue);
//			}
//		}
	}

	protected void updateState() {
		log.log(Level.FINEST, getName() + ", updating state");
		if (requirePir && currentPirState.values()
				.stream()
				.filter(movement -> movement.getTimestamp().isAfter(LocalDateTime.now().minusMinutes(5)))
				.map(movement -> movement.isMovementDetected())
				.noneMatch(v -> v)) {
			log.log(Level.FINEST, getName() + ", no movement - turning off");
			setValue(0);
			return;
		}

		int current = currentLightLevel.values()
				.stream()
				.filter(light -> light.getTimestamp().isAfter(LocalDateTime.now().minusMinutes(5)))
				.map(light -> light.getValue())
				.min(Integer::min)
				.orElse(Integer.MAX_VALUE);
		if (current < turnOnIfLowerThan) {
			log.log(Level.FINEST, getName() + ", too dark - turning on");
			setValue(dimLevel);
		} else if (current > turnOffIfHigherThan) {
			log.log(Level.FINEST, getName() + ", there is enough light - turning off");
			setValue(0);
		}
	}

	protected void updateObservedNodes() {
		List<String> tmp = new ArrayList<>();
		tmp.addAll(lightSensorPubSubNodes);
		tmp.addAll(pirSensorPubSubNodes);

		observedNodes = tmp;

		if (pubSubNodesManager != null) {
			pubSubNodesManager.updateObservedNodes();
		}
	}
}
