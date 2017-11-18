/*
 * AbstractSensor.java
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

package tigase.iot.framework.devices;

import tigase.eventbus.EventBus;
import tigase.iot.framework.devices.annotations.Hidden;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract implementation of {@link tigase.iot.framework.devices.ISensor} interfaces.
 *
 * Created by andrzej on 22.10.2016.
 */
public abstract class AbstractSensor<T extends IValue>
		implements ISensor<T>, Initializable, UnregisterAware {

	private static final Logger log = Logger.getLogger(AbstractSensor.class.getCanonicalName());
	@Inject
	private EventBus eventBus;
	@Hidden
	@ConfigField(desc = "Name")
	private String name;
	@ConfigField(desc = "Label")
	private String label;
	@Hidden
	@ConfigField(desc = "Device type")
	private String type;
	private T value;

	public AbstractSensor(String type, String name, String label) {
		this.type = type;
		this.name = name;
		this.label = label;
	}

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
	}

	protected void fireEvent(Object event) {
		log.log(Level.FINEST, "{0}, firing event {1}", new Object[]{getName(), event});
		eventBus.fire(event);
	}

	protected void fireValueChanged(T oldValue, T newValue) {
		if (oldValue == null || !oldValue.getValue().equals(newValue.getValue())) {
			fireEvent(new ValueChangeEvent<T>(this, oldValue, newValue));
		}
	}

	public String getLabel() {
		return label;
	}

	public String getName() {
		return name;
	}

	@Override
	public String getType() {
		return type;
	}

	public synchronized T getValue() {
		return value;
	}

	@Override
	public void initialize() {
		eventBus.registerAll(this);
	}

	/**
	 * Method which should be called when sensor value is changed.
	 * 
	 * @param newValue - value to set
	 */
	protected synchronized void updateValue(T newValue) {
		T oldValue = value;
		this.value = newValue;
		fireValueChanged(oldValue, newValue);
	}

	/**
	 * Class of an event which is fired when sensor value is updated/changed.
	 *
	 * @param <T> - type of value
	 */
	public static class ValueChangeEvent<T extends IValue> {

		public final T newValue;
		public final T oldValue;
		public final ISensor<T> source;

		public ValueChangeEvent(ISensor<T> device, T oldValue, T newValue) {
			this.source = device;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}

		@Override
		public String toString() {
			return "ValueChangeEvent[source: " + source + ", oldValue: " + oldValue + ", newValue: " + newValue + "]";
		}
	}
}
