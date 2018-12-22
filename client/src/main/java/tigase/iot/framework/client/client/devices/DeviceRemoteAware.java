/*
 * DeviceRemoteAware.java
 *
 * Tigase IoT Framework
 * Copyright (C) 2011-2018 "Tigase, Inc." <office@tigase.com>
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.devices;

import com.google.gwt.resources.client.ImageResource;
import tigase.iot.framework.client.Device.IValue;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Extended implementation of {@link Device} with support for representation of remote device and its state.
 *
 * @author andrzej
 */
public abstract class DeviceRemoteAware<S, T extends IValue<S>>
		extends Device
		implements tigase.iot.framework.client.Device.ValueChangedHandler<T> {

	private static final Logger log = Logger.getLogger(DeviceRemoteAware.class.getCanonicalName());

	public DeviceRemoteAware(String deviceClass, ImageResource icon, tigase.iot.framework.client.Device<T> sensor) {
		super(deviceClass, icon);

		setDescription(sensor.getName());

		sensor.setObserver(this);
		try {
			sensor.getValue(new tigase.iot.framework.client.Device.Callback<T>() {
				@Override
				public void onError(Stanza response, XMPPException.ErrorCondition errorCondition) {
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
