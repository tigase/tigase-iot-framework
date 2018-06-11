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
