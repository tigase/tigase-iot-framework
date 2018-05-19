/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.devices;

import tigase.iot.framework.client.client.ClientFactory;
import tigase.iot.framework.client.values.Pressure;

/**
 * Representation of a remote pressure sensor.
 * @author andrzej
 */
public class PressureSensor
		extends DeviceRemoteConfigAware<Double, Pressure, tigase.iot.framework.client.devices.PressureSensor> {

	private String unit;

	public PressureSensor(ClientFactory factory, tigase.iot.framework.client.devices.PressureSensor sensor) {
		super(factory, "pressure-sensor", Icons.INSTANCE.pressureSensor(), sensor);
	}

	@Override
	public void setValue(Double value) {
		if (value == null) {
			super.setValue("--");
		} else {
			value = ((double) (Math.round(value * 10))) / 10;
			super.setValue("" + value + " " + unit);
		}
	}

	public void valueChanged(Pressure value) {
		if (value != null) {
			unit = value.getUnit();
		}
		super.valueChanged(value);
	}

}
