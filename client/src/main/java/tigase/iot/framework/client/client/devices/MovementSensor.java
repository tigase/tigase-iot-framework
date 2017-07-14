/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.devices;

import tigase.iot.framework.client.values.Movement;

/**
 * Implementation of a representation (UI) of remote movement detection sensor.
 * @author andrzej
 */
public class MovementSensor extends DeviceRemoteConfigAware<Boolean, Movement, tigase.iot.framework.client.devices.MovementSensor> {
	
	public MovementSensor(tigase.iot.framework.client.devices.MovementSensor sensor) {
		super("movement-sensor", "\uD83D\uDEB6", sensor);
	}
		
	@Override
	public void setValue(Boolean value) {
		if (value == null) {
			setValue("--");
		} else {
			setValue(value ? "\uD83D\uDEA8" : "");
		}
	}
	
}
