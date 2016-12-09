/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.rpi.home.client.client.devices;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

/**
 *
 * @author andrzej
 */
public class DevicesListPlace extends Place {

	public DevicesListPlace() {

	}

	public static class Tokenizer implements PlaceTokenizer<DevicesListPlace> {

		public DevicesListPlace getPlace(String token) {
			return new DevicesListPlace();
		}

		public String getToken(DevicesListPlace place) {
			return null;
		}

	}

}
