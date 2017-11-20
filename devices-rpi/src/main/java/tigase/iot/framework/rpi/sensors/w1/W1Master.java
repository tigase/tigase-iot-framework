/*
 * W1Master.java
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

package tigase.iot.framework.rpi.sensors.w1;

import tigase.bot.Autostart;
import tigase.iot.framework.ValuesProvider;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Implementation of a W1 connectivity support. This class is a manager for any
 * W1 connected device.
 *
 * Created by andrzej on 24.10.2016.
 */
@Autostart
public class W1Master
		implements Initializable, UnregisterAware, ValuesProvider {

	private static final Logger log = Logger.getLogger(W1Master.class.getCanonicalName());

	private final com.pi4j.io.w1.W1Master w1Master;

	@ConfigField(desc = "Miliseconds between reads")
	private long period;

	@Inject
	private ScheduledExecutorService scheduledExecutorService;

	private ScheduledFuture future;

	private Map<String, com.pi4j.io.w1.W1Device> knownDevices = new ConcurrentHashMap<>();

	public W1Master() {
		//w1DeviceToBeanClass.put(DS1820.W1Device.class, DS1820.class);
		w1Master = new com.pi4j.io.w1.W1Master();
	}

	@Override
	public void beforeUnregister() {
		future.cancel(true);
		acquiredDevices.values().stream().flatMap(map -> map.values().stream()).forEach(consumer -> consumer.accept(null));
	}

	@Override
	public void initialize() {
		updateDevices();
		future = scheduledExecutorService.scheduleAtFixedRate(() -> updateDevices(), period, period, TimeUnit.MILLISECONDS);
	}

	@Override
	public List<ValuePair> getValuesFor(Object bean, Field field, Kernel kernel) {
		if (bean instanceof W1Device) {
			List<com.pi4j.io.w1.W1Device> devices = w1Master.getDevices(((W1Device) bean).getDeviceType().getDeviceFamilyCode());
			if (devices != null) {
				devices.stream().forEach(device -> System.out.println("Device: " + device.getId().trim() + ", " + device.getName() + ", " + device.getFamilyId()));
				return devices.stream().map(device -> new ValuePair(device.getId().trim(), device.getName())).collect(Collectors.toList());
			}
		}
		return Collections.emptyList();
	}

	/**
	 * Check state of all connected W1 devices and update its list.
	 */
	protected void updateDevices() {
		w1Master.checkDeviceChanges();
		List<com.pi4j.io.w1.W1Device> deviceList = w1Master.getDevices();
		Set<String> currentDeviceIds = deviceList.stream().map(com.pi4j.io.w1.W1Device::getId).map(String::trim).collect(Collectors.toSet());

		knownDevices.keySet().stream().filter(id -> !currentDeviceIds.contains(id)).forEach(id -> {
			knownDevices.remove(id);
			notifyConsumersOfDevice(id, null);
		});

		deviceList.forEach(w1Device -> {
			if (knownDevices.putIfAbsent(w1Device.getId().trim(), w1Device) == null) {
				notifyConsumersOfDevice(w1Device.getId().trim(), w1Device);
			}
		});
	}

	private final Map<String, Map<W1Device, Consumer<com.pi4j.io.w1.W1Device>>> acquiredDevices = new ConcurrentHashMap<>();

	public void acquireDevice(String deviceId, W1Device w1Device, Consumer<com.pi4j.io.w1.W1Device> consumer) {
		Map<W1Device, Consumer<com.pi4j.io.w1.W1Device>> entries = acquiredDevices.computeIfAbsent(deviceId, id -> new ConcurrentHashMap<>());
		entries.put(w1Device, consumer);
		w1Master.getDevices().stream().filter(device -> deviceId.equals(device.getId().trim())).findAny().ifPresent(consumer);
	}

	public void releaseDevice(String deviceId, W1Device w1Device) {
		Map<W1Device, Consumer<com.pi4j.io.w1.W1Device>> entries = acquiredDevices.get(deviceId);
		if (entries == null) {
			return;
		}
		Consumer<com.pi4j.io.w1.W1Device> consumer = entries.remove(w1Device);
		if (consumer != null) {
			consumer.accept(null);
		}
	}

	private void notifyConsumersOfDevice(String deviceId, com.pi4j.io.w1.W1Device w1Device) {
		Map<W1Device, Consumer<com.pi4j.io.w1.W1Device>> entries = acquiredDevices.get(deviceId);
		if (entries != null) {
			entries.values().forEach(consumer -> consumer.accept(w1Device));
		}
	}
	
}
