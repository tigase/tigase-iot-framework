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

package tigase.iot.framework.rpi.sensors.pir;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import tigase.iot.framework.devices.AbstractSensor;
import tigase.iot.framework.devices.IConfigurationAware;
import tigase.iot.framework.values.Movement;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Implementation of a sensor responsible for reading data from HC-SR501 sensor.
 *
 * Created by andrzej on 23.10.2016.
 */
public class HC_SR501
		extends AbstractSensor<Movement>
		implements Initializable, UnregisterAware, GpioPinListenerDigital, IConfigurationAware {

	private static final Logger log = Logger.getLogger(HC_SR501.class.getCanonicalName());

	@ConfigField(desc = "WiringPi Pin number")
	private int pin = 21;         // equals to broadcom 5

	@ConfigField(desc = "Delay time in ms")
	private long timeout = 5 * 60 * 1000;

	@Inject
	private ScheduledExecutorService scheduledExecutorService;

	private GpioController gpio;
	private GpioPinDigitalInput input;
	private ScheduledFuture future;

	public HC_SR501() {
		super("movement-sensor", "Motion sensor", "HC-SR501");
	}

	@Override
	public void beforeUnregister() {
		if (input != null) {
			input.removeListener(this);
			gpio.unprovisionPin(input);
		}
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

	public void setPin(Integer pin) {
		this.pin = pin;
		if (gpio != null) {
			if (input != null) {
				input.removeListener(this);
				gpio.unprovisionPin(input);
				input = null;
			}
			initializeInput();
		}
	}
	
	@Override
	public void initialize() {
		super.initialize();
		try {
			gpio = GpioFactory.getInstance();
		} catch (Throwable ex) {
			throw new RuntimeException("Failed to retrieve instance of GpioFactory!", ex);
		}
		if (input == null) {
			initializeInput();
		}
	}

	private void initializeInput() {
		input = gpio.provisionDigitalInputPin(RaspiPin.getPinByAddress(pin), PinPullResistance.PULL_DOWN);
		input.setShutdownOptions(true);

		input.addListener(this);
	}
}
