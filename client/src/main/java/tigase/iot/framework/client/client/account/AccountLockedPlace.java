package tigase.iot.framework.client.client.account;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import tigase.iot.framework.client.client.auth.AuthPlace;

public class AccountLockedPlace extends Place {

	public AccountLockedPlace() {

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
