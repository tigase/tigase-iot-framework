package tigase.iot.framework.client.values;

import tigase.iot.framework.client.Device;

import java.util.Date;

/**
 * Class represents On/Off state value.
 */
public class OnOffState extends Device.Value<Boolean> {

	public static final String NAME = "OnOffState";

	public OnOffState(Boolean value) {
		this(value, new Date());
	}

	public OnOffState(Boolean value, Date timestamp) {
		super(value, timestamp);
	}

}
