/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.rpi.home.client.client.auth;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

/**
 *
 * @author andrzej
 */
public class AuthPlace extends Place {

	public AuthPlace() {

	}

	public static class Tokenizer implements PlaceTokenizer<AuthPlace> {

		public AuthPlace getPlace(String token) {
			return new AuthPlace();
		}

		public String getToken(AuthPlace place) {
			return null;
		}

	}

}
