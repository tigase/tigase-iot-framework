/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.devices;

import tigase.iot.framework.client.client.ClientFactory;
import tigase.iot.framework.client.values.Humidity;

/**
 *
 * @author andrzej
 */
public class HumiditySensor extends DeviceRemoteConfigAware<Double, Humidity, tigase.iot.framework.client.devices.HumiditySensor> {
	
	public HumiditySensor(ClientFactory factory, tigase.iot.framework.client.devices.HumiditySensor sensor) {
		super(factory, "humidity-sensor", Icons.INSTANCE.humidity(), sensor);
	}

	@Override
	public void setValue(Double value) {
		if (value == null) {
			super.setValue("-- %");
		} else {
			value = ((double) (Math.round(value * 10))) / 10;
			super.setValue("" + value + " %");
		}
	}
	
}