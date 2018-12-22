/*
 * ClientFactory.java
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
package tigase.iot.framework.client.client;

import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import tigase.iot.framework.client.client.account.AccountLockedView;
import tigase.jaxmpp.gwt.client.Jaxmpp;
import tigase.iot.framework.client.Devices;
import tigase.iot.framework.client.Hosts;
import tigase.iot.framework.client.Hub;
import tigase.iot.framework.client.client.auth.AuthView;
import tigase.iot.framework.client.client.devices.DevicesListView;

/**
 *
 * @author andrzej
 */
public interface ClientFactory {
	
	Jaxmpp jaxmpp();
	
	EventBus eventBus();
	
	Devices devices();

	AccountLockedView accountLockedView();

	AuthView authView();

	DevicesListView devicesListView();
	
	PlaceController placeController();
	
	Hosts hosts();
	
	Hub hub();
}
