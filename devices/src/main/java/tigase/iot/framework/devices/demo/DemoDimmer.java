/*
 * DemoDimmer.java
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

package tigase.iot.framework.devices.demo;

import tigase.iot.framework.devices.AbstractSensor;
import tigase.iot.framework.devices.IConfigurationAware;
import tigase.iot.framework.devices.IExecutorDevice;
import tigase.iot.framework.values.Light;
import tigase.kernel.beans.config.ConfigField;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Example implementation of a dimmer which can be used as an example for implementation of drivers for dimmers.
 */
public class DemoDimmer extends AbstractSensor<Light> implements IConfigurationAware, IExecutorDevice<Light> {

	private static final Logger log = Logger.getLogger(DemoDimmer.class.getCanonicalName());

	private static final Collection<Category> SUPPORTED_CATEGORIES = Collections.unmodifiableCollection(Stream.of(
			new Category("light-dimmer", "Dimmer"),
			new Category("lights-external", "External lights"),
			new Category("lights-table", "Table light"),
			new Category("lights-ceiling", "Ceiling lights"),
			new Category("lights-spotlight", "Spotlight")
	).collect(Collectors.toList()));

	@ConfigField(desc = "Pin no.")
	private Integer pin = 5;

	@ConfigField(desc = "Initial value in percent")
	private Integer initialValue = 0;

	public DemoDimmer() {
		super("light-dimmer", "Dimmer", "Demo Dimmer");
	}

	@Override
	public void initialize() {
		this.setValue(new Light(initialValue, Light.Unit.percent, LocalDateTime.MIN));
	}

	@Override
	public void setValue(Light light) {
		Light currentValue = getValue();
		if (currentValue != null && light.getValue() == currentValue.getValue()
				&& !light.getTimestamp().isAfter(currentValue.getTimestamp())) {
			return;
		}

		log.log(Level.INFO, "{0} - {1}, setting light level to {2}{3}",
				new Object[]{this.getName(), getLabel(), light.getValue(), light.getUnit()});

		super.setValue(light);
	}

	@Override
	public Collection<Category> getCategories() {
		return SUPPORTED_CATEGORIES;
	}
}
