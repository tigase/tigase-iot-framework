package tigase.iot.framework.values;

import tigase.iot.framework.devices.Value;

import java.time.LocalDateTime;

/**
 * Value class holds information about on/off state.
 */
public class OnOffState extends Value<Boolean> {

	public OnOffState(Boolean value) {
		super(value);
	}

	public OnOffState(Boolean value, LocalDateTime timestamp) {
		super(value, timestamp);
	}
}
