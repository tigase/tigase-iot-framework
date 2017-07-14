/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.devices;

import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.iot.framework.client.Device.IValue;
import tigase.iot.framework.client.values.Movement;

/**
 * Extended implementation of {@link Device} with support for representation of remote device and its state.
 * @author andrzej
 */
public abstract class DeviceRemoteAware<S, T extends IValue<S>> extends Device implements tigase.iot.framework.client.Device.ValueChangedHandler<T> {
	
	private static final Logger log = Logger.getLogger(DeviceRemoteAware.class.getCanonicalName());
	
	public DeviceRemoteAware(String deviceClass, String iconStr, tigase.iot.framework.client.Device<T> sensor) {
		super(deviceClass, iconStr);
		
		setDescription(sensor.getName());
		
		sensor.setObserver(this);
		try {
			sensor.getValue(new tigase.iot.framework.client.Device.Callback<T>() {
				@Override
				public void onError(XMPPException.ErrorCondition error) {
				}

				@Override
				public void onSuccess(T result) {
					valueChanged(result);
				}
			});	
		} catch (JaxmppException ex) {
			log.log(Level.FINE, "exception retrieving current value");
		}
	}

	protected abstract void setValue(S value);
	
	@Override
	public void valueChanged(T value) {
		if (value != null) {
			setValue(value.getValue());
		}
	}
	
}
