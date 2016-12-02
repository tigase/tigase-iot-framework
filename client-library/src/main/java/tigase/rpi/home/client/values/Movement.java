package tigase.rpi.home.client.values;

import tigase.rpi.home.client.Device;

import java.util.Date;

/**
 * Created by andrzej on 27.11.2016.
 */
public class Movement extends Device.Value<Boolean> {

	public static final String NAME = "Movement";

	public Movement(Boolean value, Date timestamp) {
		super(value, timestamp);
	}
}
