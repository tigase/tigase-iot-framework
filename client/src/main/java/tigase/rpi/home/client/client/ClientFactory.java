/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.rpi.home.client.client;

import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import tigase.jaxmpp.gwt.client.Jaxmpp;
import tigase.rpi.home.client.Devices;
import tigase.rpi.home.client.client.auth.AuthView;
import tigase.rpi.home.client.client.devices.DevicesListView;

/**
 *
 * @author andrzej
 */
public interface ClientFactory {
	
	Jaxmpp jaxmpp();
	
	EventBus eventBus();
	
	Devices devices();
	
	AuthView authView();		

	DevicesListView devicesListView();
	
	PlaceController placeController();
}
