/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.rpi.home.client.client.devices;

import tigase.rpi.home.client.values.Movement;

/**
 *
 * @author andrzej
 */
public class MovementSensor extends DeviceRemoteConfigAware<Boolean, Movement, tigase.rpi.home.client.devices.MovementSensor> {
	
	public MovementSensor(tigase.rpi.home.client.devices.MovementSensor sensor) {
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
