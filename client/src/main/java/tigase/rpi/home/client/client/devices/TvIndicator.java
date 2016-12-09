/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.rpi.home.client.client.devices;

import java.util.logging.Logger;
import tigase.rpi.home.client.values.Movement;

/**
 *
 * @author andrzej
 */
public class TvIndicator extends DeviceRemoteConfigAware<Boolean, Movement, tigase.rpi.home.client.devices.TvSensor> {
	
	private static final Logger log = Logger.getLogger(TvIndicator.class.getCanonicalName());
	
	public TvIndicator(tigase.rpi.home.client.devices.TvSensor sensor) {
		super("tv-sensor", "\uD83D\uDCFA", sensor);
		
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
