/*
 * HC_SR501.java
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

package tigase.iot.framework.sensors.pir;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import tigase.bot.iot.AbstractSensor;
import tigase.iot.framework.runtime.IConfigurationAware;
import tigase.iot.framework.runtime.values.Movement;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Created by andrzej on 23.10.2016.
 */
public class HC_SR501
		extends AbstractSensor<Movement>
		implements Initializable, UnregisterAware, GpioPinListenerDigital, IConfigurationAware {

	private static final Logger log = Logger.getLogger(HC_SR501.class.getCanonicalName());

	@ConfigField(desc = "WiringPi Pin number")
	private int pin = 21;         // equals to broadcom 5

	@ConfigField(desc = "Time in miliseconds after which we should change state to inactive if there was no movement in this time")
	private long timeout = 5 * 60 * 1000;

	@Inject
	private ScheduledExecutorService scheduledExecutorService;

	private GpioController gpio;
	private GpioPinDigitalInput input;
	private ScheduledFuture future;

	public HC_SR501() {
		super("movement-sensor");
	}

	@Override
	public void beforeUnregister() {
		input.removeListener(this);
		input.unexport();
	}

	@Override
	public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
		if (event.getEdge() == PinEdge.RISING) {
			if (future != null) {
				future.cancel(false);
			}

			if (!isMovementDetected()) {
				updateValue(new Movement(true));
			}

			future = scheduledExecutorService.schedule(() -> updateValue(new Movement(false)), timeout,
													   TimeUnit.MILLISECONDS);
		}
	}

	public boolean isMovementDetected() {
		Movement val = getValue();

		if (val == null) {
			return false;
		}

		return val.getValue();
	}

	@Override
	public void initialize() {
		super.initialize();
		gpio = GpioFactory.getInstance();
		input = gpio.provisionDigitalInputPin(RaspiPin.getPinByAddress(pin), PinPullResistance.PULL_DOWN);
		input.setShutdownOptions(true);

		input.addListener(this);
	}
}