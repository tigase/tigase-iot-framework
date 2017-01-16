package tigase.rpi.home.app.pubsub;

import tigase.bot.iot.AbstractSensor;
import tigase.bot.iot.IDevice;
import tigase.bot.iot.ISensor;
import tigase.bot.iot.IValue;
import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.rpi.home.app.DeviceNodesHelper;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by andrzej on 31.10.2016.
 */
public class DevicePubSubPublisher
		implements PubSubNodesManager.PubSubNodeAware, Initializable, UnregisterAware {

	private static final Logger log = Logger.getLogger(DevicePubSubPublisher.class.getCanonicalName());

	@ConfigField(desc = "Devices root node")
	private String devicesRootNode = "devices";

	@ConfigField(desc = "Devcies node name")
	private String devicesNodeName = "Devices";

	@Inject(nullAllowed = true)
	protected List<ISensor> devices;

	@Inject
	protected EventBus eventBus;

	private PubSubNodesManager.Node rootNode;
	private Set<String> stateNodes = Collections.synchronizedSet(new HashSet<>());

	@Inject
	private ExtendedPubSubNodesManager pubSubNodesManager;

	@Override
	public List<PubSubNodesManager.Node> getRequiredNodes() {
		return Collections.singletonList(rootNode);
	}

	public void initialize() {
		eventBus.registerAll(this);
		try {
			rootNode = DeviceNodesHelper.createDevicesRootNode(devicesRootNode, devicesNodeName);

			devices.forEach(this::addDevice);
		} catch (JaxmppException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
	}

	public void setDevices(List<ISensor> devices) {
		if (devices == null) {
			devices = new ArrayList<>();
		}
		List<ISensor> oldDevices = this.devices;
		this.devices = devices;

		if (oldDevices != null) {
			oldDevices.stream().filter(device -> !devices.contains(device)).forEach(this::removeDevice);
		}
		this.devices.stream()
				.filter(device -> oldDevices == null || oldDevices.contains(device))
				.forEach(this::addDevice);

		if (pubSubNodesManager != null) {
			pubSubNodesManager.updateRequiredNodes();
		}
	}

	public void addDevice(IDevice device) {
		if (rootNode == null) {
			return;
		}

		try {
			PubSubNodesManager.Node deviceNode = DeviceNodesHelper.createDeviceNode(devicesRootNode, device);
			rootNode.addChild(deviceNode);

			deviceNode.addChild(DeviceNodesHelper.createDeviceStateNode(devicesRootNode, device));
		} catch (JaxmppException ex) {
			log.log(Level.WARNING, "could not add child node for device " + device, ex);
		}
	}

	public void removeDevice(IDevice device) {
		if (rootNode == null) {
			return;
		}

		String deviceNodeName = devicesRootNode + "/" + device.getName();
		List<PubSubNodesManager.Node> toRemove = rootNode.getChildren()
				.stream()
				.filter(node -> node.getNodeName().equals(deviceNodeName))
				.collect(Collectors.toList());
		toRemove.forEach(rootNode::removeChild);
	}

	@HandleEvent
	public void nodeReady(PubSubNodesManager.NodeReady nodeReady) {
		if (stateNodes.contains(nodeReady.node)) {
			String deviceId = DeviceNodesHelper.getDeviceIdFromNode(nodeReady.node);
			devices.stream().filter(device -> device.getName().equals(deviceId)).forEach(this::publishDeviceValue);
		}
	}

	@HandleEvent
	public void valueChanged(AbstractSensor.ValueChangeEvent event) {
		if (!devices.contains(event.source)) {
			return;
		}

		publishValue(event.source, event.newValue);
	}

	protected void publishDeviceValue(ISensor source) {
		publishValue(source, source.getValue());
	}

	protected void publishValue(IDevice source, IValue value) {
		String node = DeviceNodesHelper.getDeviceStateNodeName(devicesRootNode, source);
		pubSubNodesManager.publish(node, null, value);
	}

}
