/*
 * AppActivityMapper.java
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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;
import tigase.iot.framework.client.client.account.AccountLockedActivity;
import tigase.iot.framework.client.client.account.AccountLockedPlace;
import tigase.iot.framework.client.client.auth.AuthActivity;
import tigase.iot.framework.client.client.auth.AuthPlace;
import tigase.iot.framework.client.client.devices.DevicesListActivity;
import tigase.iot.framework.client.client.devices.DevicesListPlace;

/**
 *
 * @author andrzej
 */
public class AppActivityMapper implements ActivityMapper {

	private final ClientFactory clientFactory;

	public AppActivityMapper(ClientFactory clientFactory) {
		super();
		this.clientFactory = clientFactory;
	}

	@Override
	public Activity getActivity(Place place) {
		if (place instanceof AuthPlace) {
			return new AuthActivity((AuthPlace) place, clientFactory);
		}
		if (place instanceof DevicesListPlace) {
			return new DevicesListActivity((DevicesListPlace) place, clientFactory);
		}
		if (place instanceof AccountLockedPlace) {
			return new AccountLockedActivity((AccountLockedPlace) place, clientFactory);
		}
		return null;
	}

}
