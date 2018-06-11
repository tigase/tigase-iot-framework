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
