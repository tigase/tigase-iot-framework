/*
 * LightSensorListener.java
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

import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.iot.framework.devices.IConfigurationAware;
import tigase.iot.framework.runtime.pubsub.ExtendedPubSubNodesManager;
import tigase.iot.framework.runtime.pubsub.PubSubNodesManager;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class implements a listener/observer for light sensor.
 *
 * Created by andrzej on 02.11.2016.
 */
public class LightSensorListener implements PubSubNodesManager.NodesObserver, Initializable, UnregisterAware,
											IConfigurationAware {

	private static final Logger log = Logger.getLogger(LightSensorListener.class.getCanonicalName());

	@ConfigField(desc = "Observed device nodes")
	private ArrayList<String> observes;

	@ConfigField(desc = "Bean name")
	private String name;

	@Inject
	private EventBus eventBus;

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * Return list of PubSub nodes which should be observed.
	 * @return
	 */
	@Override
	public List<String> getObservedNodes() {
		return observes;
	}

	@Override
	public void initialize() {
		eventBus.registerAll(this);
	}

	/**
	 * Method called when state of observed device changes
	 * @param event
	 */
	@HandleEvent
	public void onValueChanged(ExtendedPubSubNodesManager.ValueChangedEvent event) {
		if (observes.contains(event.sourceId)) {
			log.log(Level.INFO, "received new event from " + event.sourceId + " with value " + event);
		}
	}
}
