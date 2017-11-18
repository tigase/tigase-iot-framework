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
import tigase.iot.framework.runtime.pubsub.PubSubNodesManager;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.forms.*;
import tigase.kernel.BeanUtils;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.AbstractBeanConfigurator;
import tigase.kernel.beans.config.ConfigField;
import tigase.osgi.util.ClassUtilBean;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bean responsible for management of a device "driver" instances.
 * All actions related to node device creation or removal are handler here and proper changes to local configuration
 * file are made using <code>ConfigManager</code>.
 */
public class DeviceManager {

	private static final Logger log = Logger.getLogger(DeviceManager.class.getCanonicalName());

	@Inject(nullAllowed = true)
	private ConfigManager configManager;               

	@Inject
	private PubSubNodesManager pubSubNodesManager;

	@Inject(nullAllowed = true)
	private List<IDevice> devices;

	private List<DeviceTypeInfo> knownDeviceTypes;

	public DeviceManager() {
		knownDeviceTypes = collectKnownDeviceTypesInfo();
	}

	public void createDevice(String type, JabberDataElement form) throws XMLException {
		// actually create device in config file and reload config
		DeviceTypeInfo info = findKnownDeviceType(type);

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
	}

	public void removeDevice(String deviceId) {
		configManager.removeBeanDefinition(deviceId);
		pubSubNodesManager.cleanupNodes();
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

	public JabberDataElement getDeviceForm(String type) throws XMLException {
		DeviceTypeInfo info = findKnownDeviceType(type);
		if (info != null) {
			try {
				return getDeviceForm(info.getImplementation());
			} catch (Exception ex) {
				throw new RuntimeException("failed to retrieve device configuration form", ex);
			}
		}
		return null;
	}

	public List<DeviceTypeInfo> getKnownDeviceTypes() {
		return knownDeviceTypes;
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

	protected DeviceTypeInfo findKnownDeviceType(String type) {
		for (DeviceTypeInfo info : knownDeviceTypes) {
			if (type.equals(info.getId())) {
				return info;
			}
		}
		return null;
	}

	protected List<DeviceTypeInfo> collectKnownDeviceTypesInfo() {
		List<DeviceTypeInfo> knownDeviceTypes = new ArrayList<>();
		for (Class clazz : ClassUtilBean.getInstance().getAllClasses()) {
			if (IDevice.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers()) && !Modifier.isInterface(clazz.getModifiers())) {
				try {
					DeviceTypeInfo deviceTypeInfo = collectDeviceTypeInfo(clazz);
					if (deviceTypeInfo != null) {
						knownDeviceTypes.add(deviceTypeInfo);
					}
				} catch (Exception ex) {
					log.log(Level.SEVERE, "could not read device type information from " + clazz.getCanonicalName(),
							ex);
				}
			}
		}
		return knownDeviceTypes;
	}

	protected DeviceTypeInfo collectDeviceTypeInfo(Class<IDevice> clazz)
			throws IllegalAccessException, InstantiationException {
		IDevice device = clazz.newInstance();
		String type = device.getType();
		String typeLabel = device.getLabel();

		return new DeviceTypeInfo() {
			@Override
			public Class<IDevice> getImplementation() {
				return clazz;
			}

			@Override
			public String getName() {
				return typeLabel;
			}

			@Override
			public String getId() {
				return type;
			}
		};
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

					if (Collection.class.isAssignableFrom(field.getType())) {
						Collection collectionOfValues = (Collection) field.get(device);
						TextMultiField f = data.addTextMultiField(field.getName());
						f.setLabel(cf.desc());
						if (collectionOfValues != null) {
							String[] values = (String[]) collectionOfValues.stream()
									.map(value -> value.toString())
									.toArray(String[]::new);
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
				} catch (IllegalAccessException ex) {
					log.log(Level.WARNING, "could not retrieve data from field " + field, ex);
				}
			}
		}

	}

	public interface DeviceTypeInfo {

		Class<IDevice> getImplementation();

		String getName();

		String getId();

	}

}
