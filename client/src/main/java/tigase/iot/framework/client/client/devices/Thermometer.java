/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.devices;

import tigase.iot.framework.client.devices.TemperatureSensor;
import tigase.iot.framework.client.values.Temperature;

/**
 *
 * @author andrzej
 */
public class Thermometer extends DeviceRemoteConfigAware<Double, Temperature, tigase.iot.framework.client.devices.TemperatureSensor> {
	
	public Thermometer(TemperatureSensor sensor) {
		super("thermometer", "\uD83C\uDF21", sensor);
	}

	@Override
	public void setValue(Double value) {
		if (value == null) {
			super.setValue("-- \u2103");
		} else {
			value = ((double) (Math.round(value * 10))) / 10;
			super.setValue("" + value + "\u2103");
		}
	}
	
}
