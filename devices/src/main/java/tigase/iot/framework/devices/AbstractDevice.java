/*
 * AbstractDevice.java
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
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by bmalkow on 08.12.2016.
 */
public abstract class AbstractDevice
		implements IDevice, Initializable, UnregisterAware {

	private final Logger log = Logger.getLogger(this.getClass().getCanonicalName());
	@Inject
	private EventBus eventBus;
	@ConfigField(desc = "Device name")
	private String name;

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
	}

	protected void fireEvent(Object event) {
		log.log(Level.FINEST, "{0}, firing event {1}", new Object[]{getName(), event});
		eventBus.fire(event);
	}

	public String getName() {
		return name;
	}

	@Override
	public void initialize() {
		eventBus.registerAll(this);
	}

}
