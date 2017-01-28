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

package tigase.iot.framework.sensors.w1;

import tigase.bot.Autostart;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 24.10.2016.
 */
@Autostart
public class W1Master
		implements RegistrarBean, Initializable, UnregisterAware {

	public static final Map<String, com.pi4j.io.w1.W1Device> KNOWN_DEVICES = new ConcurrentHashMap<>();

	private static final Logger log = Logger.getLogger(W1Master.class.getCanonicalName());

	private final com.pi4j.io.w1.W1Master w1Master;

	@ConfigField(desc = "Miliseconds between reads")
	private long period;

	@Inject
	private ScheduledExecutorService scheduledExecutorService;

	private Kernel parentKernel;
	private List<String> registeredDevices = new ArrayList<>();
	private Map<Class<? extends com.pi4j.io.w1.W1Device>, Class<? extends W1Device>> w1DeviceToBeanClass = new ConcurrentHashMap<>();
	private ScheduledFuture future;

	public W1Master() {
		w1DeviceToBeanClass.put(DS1820.W1Device.class, DS1820.class);
		w1Master = new com.pi4j.io.w1.W1Master();
	}

	@Override
	public void beforeUnregister() {
		future.cancel(true);
	}

	@Override
	public void initialize() {
		updateDevices();
		future = scheduledExecutorService.scheduleAtFixedRate(() -> updateDevices(), period, period, TimeUnit.MILLISECONDS);
	}

	protected void updateDevices() {
		w1Master.checkDeviceChanges();
		List<com.pi4j.io.w1.W1Device> deviceList = w1Master.getDevices();
		List<String> found = new ArrayList<>();
		deviceList.forEach(w1Device -> {
			String id = "w1-" + w1Device.getId().trim();
			found.add(id);
			if (!registeredDevices.contains(id)) {
				KNOWN_DEVICES.put(id, w1Device);
				registerDeviceBean(w1Device);
			}
		});

		Iterator<String> iter = registeredDevices.iterator();
		while (iter.hasNext()) {
			String id = iter.next();
			if (!found.contains(id)) {
				KNOWN_DEVICES.remove(id);
				parentKernel.unregister(id);
				iter.remove();
			}
		}
	}

	@Override
	public void register(Kernel kernel) {
		parentKernel = kernel.getParent();
	}

	@Override
	public void unregister(Kernel kernel) {
		registeredDevices.forEach(beanName -> {
			W1Device bean = parentKernel.getInstance(beanName);
			parentKernel.unregister(beanName);
			KNOWN_DEVICES.remove(beanName);
			if (bean instanceof UnregisterAware) {
				((UnregisterAware) bean).beforeUnregister();
			}
		});
		parentKernel = null;
	}

	public void registerDeviceBean(com.pi4j.io.w1.W1Device device) {
		Class<? extends W1Device> beanClass = w1DeviceToBeanClass.get(device.getClass());
		if (beanClass == null) {
			return;
		}

		try {
			String beanName = "w1-" + device.getId().trim();
			parentKernel.registerBean(beanName).asClass(beanClass).exec();
			registeredDevices.add(beanName);
		} catch (SecurityException ex) {
			log.log(Level.WARNING, "Could not instantiate class " + beanClass.getCanonicalName(), ex);
		}
	}

}
