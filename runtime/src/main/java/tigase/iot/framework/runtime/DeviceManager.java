/*
 * DeviceManager.java
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
package tigase.iot.framework.runtime;

import tigase.iot.framework.devices.IDevice;
import tigase.iot.framework.devices.annotations.Hidden;
import tigase.iot.framework.devices.annotations.ValuesProvider;
import tigase.iot.framework.runtime.pubsub.PubSubNodesManager;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.forms.*;
import tigase.kernel.BeanUtils;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.config.AbstractBeanConfigurator;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.osgi.util.ClassUtilBean;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Bean responsible for management of a device "driver" instances.
 * All actions related to node device creation or removal are handler here and proper changes to local configuration
 * file are made using <code>ConfigManager</code>.
 */
public class DeviceManager implements RegistrarBean {

	private static final Logger log = Logger.getLogger(DeviceManager.class.getCanonicalName());

	@Inject(nullAllowed = true)
	private ConfigManager configManager;
	private Kernel kernel;

	@Inject
	private PubSubNodesManager pubSubNodesManager;

	@Inject(nullAllowed = true)
	private List<IDevice> devices;

	private List<DeviceDriverInfo> knownDeviceDrivers;

	public DeviceManager() {
		knownDeviceDrivers = collectKnownDeviceTypesInfo();
	}

	public void createDevice(String deviceClass, JabberDataElement form) throws XMLException {
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "creating device of class " + deviceClass + " with configuration " + form.getAsString());
		}
		// actually create device in config file and reload config
		DeviceDriverInfo info = findKnownDeviceDriverInfo(deviceClass);

		AbstractBeanConfigurator.BeanDefinition.Builder builder = new AbstractBeanConfigurator.BeanDefinition.Builder();
		builder.name(UUID.randomUUID().toString()).clazz(info.getImplementation()).active(true);
		for (AbstractField f : form.getFields()) {
			if (f instanceof HiddenField || f instanceof FixedField) {
				continue;
			}
			else {
				builder.with(f.getVar(), f.getFieldValue());
			}
		}
		AbstractBeanConfigurator.BeanDefinition definition = builder.build();
		definition.setExportable(true);
		configManager.setBeanDefinition(definition);

		try {
			kernel.getInstance(definition.getBeanName());
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "created devices with id = " + definition.getBeanName());
			}
		} catch (Exception ex) {
			configManager.removeBeanDefinition(definition.getBeanName());
			log.log(Level.WARNING, "failed creation of device " + deviceClass + " with configuration " + form.getAsString());
			throw new RuntimeException(ex);
		}
	}

	public void removeDevice(String deviceId) {
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "removing device with id = " + deviceId);
		}
		boolean removed = configManager.removeBeanDefinition(deviceId);
		pubSubNodesManager.cleanupNodes();
		if (removed) {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "removed device with id = " + deviceId);
			}
		} else {
			log.log(Level.WARNING, "failed to remove device with id = " + deviceId + ", device does not exist");
		}
	}
	
	public Map<String, String> getDevices() {
		Map<String, String> devices = new HashMap<>();
		if (this.devices != null) {
			this.devices.stream().forEach(device -> {
				devices.put(device.getName(), device.getLabel());
			});
		}
		return devices;
	}

	public JabberDataElement getDeviceForm(String deviceId) throws XMLException {
		DeviceDriverInfo info = findKnownDeviceDriverInfo(deviceId);
		if (info != null) {
			try {
				return getDeviceForm(info.getImplementation());
			} catch (Exception ex) {
				throw new RuntimeException("failed to retrieve device configuration form", ex);
			}
		}
		return null;
	}

	public List<DeviceDriverInfo> getDeviceDriversInfo(String type) {
		return knownDeviceDrivers.stream()
				.filter(info -> type.equals(info.getDeviceType().getId()))
				.collect(Collectors.toList());
	}

	public List<DeviceType> getKnownDeviceTypes() {
		return knownDeviceDrivers.stream().map(DeviceDriverInfo::getDeviceType).distinct().collect(Collectors.toList());
	}

	public void updateDeviceLabel(String deviceId, String label) {
		configManager.updateBeanDefinition(deviceId, beanDefinition -> {
			if (label == null) {
				beanDefinition.remove("label");
			} else {
				beanDefinition.put("label", label);
			}
		});
	}

	@Override
	public void register(Kernel kernel) {
		this.kernel = kernel;
	}

	@Override
	public void unregister(Kernel kernel) {

	}

	protected DeviceDriverInfo findKnownDeviceDriverInfo(String deviceId) {
		return knownDeviceDrivers.stream()
				.filter(info -> deviceId.equals(info.getId()))
				.findAny()
				.get();
	}

	protected List<DeviceDriverInfo> collectKnownDeviceTypesInfo() {
		return ClassUtilBean.getInstance()
				.getAllClasses()
				.stream()
				.filter(clazz -> IDevice.class.isAssignableFrom(clazz))
				.filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()) &&
						!Modifier.isInterface(clazz.getModifiers()))
				.map(clazz -> (Class<IDevice>) clazz)
				.map(clazz -> {
					try {
						return collectDeviceTypeInfo(clazz);
					} catch (Exception ex) {
						log.log(Level.SEVERE, "could not read device type information from " + clazz.getCanonicalName(),
								ex);
						return null;
					}
				})
				.filter(info -> info != null)
				.collect(Collectors.toList());
	}

	protected DeviceDriverInfo collectDeviceTypeInfo(Class<IDevice> clazz)
			throws IllegalAccessException, InstantiationException {
		IDevice device = clazz.newInstance();

		return new DeviceDriverInfo(clazz, device.getLabel(), new DeviceType(device.getType(), device.getName()));
	}

	protected JabberDataElement getDeviceForm(Class<IDevice> clazz)
			throws XMLException, IllegalAccessException, InstantiationException {
		JabberDataElement form = new JabberDataElement(XDataType.form);
		prepareDeviceForm(form, clazz.newInstance());
		return form;
	}

	protected void prepareDeviceForm(JabberDataElement data, IDevice device) throws XMLException {
		for (Field field : BeanUtils.getAllFields(device.getClass())) {
			if ("name".equals(field.getName())) {
				continue;
			}
			ConfigField cf = field.getAnnotation(ConfigField.class);
			if (cf != null) {
				field.setAccessible(true);
				try {
					Hidden hidden = field.getAnnotation(Hidden.class);
					if (hidden != null) {
						Object value = field.get(device);
						data.addHiddenField(field.getName(), String.valueOf(value));
						continue;
					}

					ValuesProvider providerAnnotation = field.getAnnotation(ValuesProvider.class);
					if (providerAnnotation != null) {
						tigase.iot.framework.ValuesProvider provider = kernel.getParent()
								.getInstance(providerAnnotation.beanName());
						List<tigase.iot.framework.ValuesProvider.ValuePair> pairs = provider.getValuesFor(device, field,
																										  kernel);
						if (Collection.class.isAssignableFrom(field.getType())) {
							Collection collectionOfValues = (Collection) field.get(device);
							ListMultiField f = data.addListMultiField(field.getName());
							f.setLabel(cf.desc());
							for (tigase.iot.framework.ValuesProvider.ValuePair pair : pairs) {
								f.addOption(pair.getLabel(), pair.getValue());
							}
							if (collectionOfValues != null) {
								String[] values = (String[]) collectionOfValues.stream().map(value -> value.toString()).toArray(String[]::new);
								f.setFieldValue(values);
							}
						} else {
							Object value = field.get(device);
							ListSingleField f = data.addListSingleField(field.getName(), String.valueOf(value));
							f.setLabel(cf.desc());
							for (tigase.iot.framework.ValuesProvider.ValuePair pair : pairs) {
								f.addOption(pair.getLabel(), pair.getValue());
							}
						}
					} else {
						if (Collection.class.isAssignableFrom(field.getType())) {
							Collection collectionOfValues = (Collection) field.get(device);
							TextMultiField f = data.addTextMultiField(field.getName());
							f.setLabel(cf.desc());
							if (collectionOfValues != null) {
								String[] values = (String[]) collectionOfValues.stream().map(value -> value.toString()).toArray(String[]::new);
								f.setFieldValue(values);
							}
						} else {
							Object value = field.get(device);
							tigase.jaxmpp.core.client.xmpp.forms.Field f;
							if (value instanceof Boolean) {
								f = data.addBooleanField(field.getName(), (Boolean) value);
							} else {
								f = data.addTextSingleField(field.getName(), String.valueOf(value));
							}
							f.setLabel(cf.desc());
//						f.setDesc(cf.desc());
						}
					}
				} catch (IllegalAccessException ex) {
					log.log(Level.WARNING, "could not retrieve data from field " + field, ex);
				}
			}
		}

	}

	public class DeviceType {

		private final String name;
		private final String id;

		public DeviceType(String id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public String getId() {
			return id;
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof DeviceType) {
				return id.equals(((DeviceType) obj).id);
			}
			return false;
		}
	}

	public class DeviceDriverInfo {

		private final DeviceType deviceType;

		private final String name;
		private final Class<IDevice> implementation;

		public DeviceDriverInfo(Class<IDevice> implementation, String name, DeviceType deviceType) {
			this.implementation = implementation;
			this.name = name;
			this.deviceType = deviceType;
		}

		public Class<IDevice> getImplementation() {
			return implementation;
		}

		public String getId() {
			return implementation.getCanonicalName();
		}

		public String getName() {
			return name;                                       
		}

		public DeviceType getDeviceType() {
			return deviceType;
		}
	}
	
}
