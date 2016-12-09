/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.rpi.home.client.client;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;
import tigase.rpi.home.client.client.auth.AuthActivity;
import tigase.rpi.home.client.client.auth.AuthPlace;
import tigase.rpi.home.client.client.devices.DevicesListActivity;
import tigase.rpi.home.client.client.devices.DevicesListPlace;

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
		return null;
	}

}
