/*
 * AbstractPeriodSensor.java
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

import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;

import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 22.10.2016.
 */
public abstract class AbstractPeriodSensor<T extends IValue>
		extends AbstractSensor<T>
		implements Initializable, UnregisterAware, ConfigurationChangedAware {

	private static final Logger log = Logger.getLogger(AbstractPeriodSensor.class.getCanonicalName());
	@Inject
	protected ScheduledExecutorService scheduledExecutorService;
	private ScheduledFuture future;
	@ConfigField(desc = "Miliseconds between reads")
	private long period;

	protected AbstractPeriodSensor(String type, long period) {
		super(type);
		this.period = period;
	}

	@Override
	public void beanConfigurationChanged(Collection<String> collection) {
		if (future == null)
			return;

		if (collection.contains("period")) {
			future.cancel(false);
			future = scheduledExecutorService.scheduleAtFixedRate(() -> refresh(), period / 2, period, TimeUnit.MILLISECONDS);
		}
	}

	@Override
	public void beforeUnregister() {
		super.beforeUnregister();
		if (future != null) {
			future.cancel(false);
		}
	}

	@Override
	public void initialize() {
		future = scheduledExecutorService.scheduleAtFixedRate(() -> refresh(), period / 2, period, TimeUnit.MILLISECONDS);
	}

	protected abstract T readValue();

	protected void refresh() {
		log.log(Level.FINEST, "{0}, refreshing..", getName());
		T val = readValue();
		log.log(Level.FINEST, "{0}, got data: {1}", new Object[]{getName(), val});
		updateValue(val);
	}
}
