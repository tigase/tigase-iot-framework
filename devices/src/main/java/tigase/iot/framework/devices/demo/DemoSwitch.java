/*
 * DemoSwitch.java
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
import tigase.iot.framework.devices.annotations.Advanced;
import tigase.iot.framework.values.OnOffState;
import tigase.kernel.beans.config.ConfigField;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Example implementation of a switch which may be used as an example for implementation of drivers for switches.
 */
public class DemoSwitch extends AbstractSensor<OnOffState> implements IConfigurationAware, IExecutorDevice<OnOffState> {

	private static final Logger log = Logger.getLogger(DemoSwitch.class.getCanonicalName());

	private static final Collection<Category> SUPPORTED_CATEGORIES = Collections.unmodifiableCollection(Stream.of(
			new Category("switch", "Switch"),
			new Category("socket", "Socket"),
			new Category("lights-external", "External lights"),
			new Category("lights-table", "Table light"),
			new Category("lights-ceiling", "Ceiling lights"),
			new Category("lights-spotlight", "Spotlight"),
			new Category("lights-led", "LED"),
			new Category("motor", "Motor")
	).collect(Collectors.toList()));
	
	@ConfigField(desc = "Pin no.")
	private Integer pin = 2;

	@Advanced
	@ConfigField(desc = "Enable with high state")
	private boolean enableWithHigh = true;

	@ConfigField(desc = "Initial state")
	private boolean initialValue = false;

	public DemoSwitch() {
		super("switch", "Switch", "Demo Switch");
	}

	@Override
	public void initialize() {
		this.setValue(new OnOffState(initialValue, LocalDateTime.MIN));
	}

	@Override
	public void setValue(OnOffState value) {
		OnOffState currentValue = getValue();
		if (currentValue != null && value.getValue() == currentValue.getValue()
				&& !value.getTimestamp().isAfter(currentValue.getTimestamp())) {
			return;
		}

		log.log(Level.INFO, "{0} - {1}, setting switch to {2}",
				new Object[]{this.getName(), getLabel(), value.getValue()});
		
		super.setValue(value);
	}

	@Override
	public Collection<Category> getCategories() {
		return SUPPORTED_CATEGORIES;
	}
}
