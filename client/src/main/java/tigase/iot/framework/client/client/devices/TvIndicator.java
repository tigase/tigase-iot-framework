/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.devices;

import tigase.iot.framework.client.client.ClientFactory;
import tigase.iot.framework.client.values.Movement;

import java.util.logging.Logger;

/**
 * Representation of a remote TvSensor presenting TV state.
 * @author andrzej
 */
public class TvIndicator extends DeviceRemoteConfigAware<Boolean, Movement, tigase.iot.framework.client.devices.TvSensor> {
	
	private static final Logger log = Logger.getLogger(TvIndicator.class.getCanonicalName());
	
	public TvIndicator(ClientFactory factory, tigase.iot.framework.client.devices.TvSensor sensor) {
		super(factory, "tv-sensor", Icons.INSTANCE.tv(), sensor);
		
	}
		
	@Override
	public void setValue(Boolean value) {
		if (value == null) {
			setValue("--");
		} else {
			setValue(value ? "ON" : "OFF");
		}
	}
	
}
