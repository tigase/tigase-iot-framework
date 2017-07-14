/*
 * DevicePubSubPublisher.java
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

import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.iot.framework.devices.AbstractSensor;
import tigase.iot.framework.devices.IDevice;
import tigase.iot.framework.devices.ISensor;
import tigase.iot.framework.devices.IValue;
import tigase.iot.framework.runtime.DeviceNodesHelper;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Class implements PubSub nodes publisher for registered sensors.
 *
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
			rootNode = DeviceNodesHelper.createDevicesRootNode(devicesRootNode, devicesNodeName, pubSubNodesManager.isPEP());

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
			oldDevices.stream().filter(device -> !this.devices.contains(device)).forEach(this::removeDevice);
		}
		this.devices.stream()
				.filter(device -> oldDevices == null || oldDevices.contains(device))
				.forEach(this::addDevice);

		if (pubSubNodesManager != null) {
			pubSubNodesManager.updateRequiredNodes();
		}
	}

	/**
	 * Add device to list of managed devices for publication of items.
	 * @param device
	 */
	public void addDevice(IDevice device) {
		if (rootNode == null) {
			return;
		}

		try {
			PubSubNodesManager.Node deviceNode = DeviceNodesHelper.createDeviceNode(devicesRootNode, device, pubSubNodesManager.isPEP());
			rootNode.addChild(deviceNode);

			deviceNode.addChild(DeviceNodesHelper.createDeviceStateNode(devicesRootNode, device, pubSubNodesManager.isPEP()));
		} catch (JaxmppException ex) {
			log.log(Level.WARNING, "could not add child node for device " + device, ex);
		}
	}

	/**
	 * Remove device from list of managed devices for publication of items.
	 * @param device
	 */
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

	/**
	 * Method called when event {@link tigase.iot.framework.devices.AbstractSensor.ValueChangeEvent} is fired
	 * @param event
	 */
	@HandleEvent
	public void valueChanged(AbstractSensor.ValueChangeEvent event) {
		if (!devices.contains(event.source)) {
			return;
		}

		publishValue(event.source, event.newValue);
	}

	/**
	 * Method called to publish current sensor value.
	 * @param source
	 */
	protected void publishDeviceValue(ISensor source) {
		publishValue(source, source.getValue());
	}

	/**
	 * Method called to publish passed value for particular device.
	 * @param source
	 * @param value
	 */
	protected void publishValue(IDevice source, IValue value) {
		String node = DeviceNodesHelper.getDeviceStateNodeName(devicesRootNode, source);
		pubSubNodesManager.publish(node, null, value);
	}

}
