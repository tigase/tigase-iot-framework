package tigase.rpi.home.values;

import tigase.bot.Value;

import java.time.LocalDateTime;

/**
 * Created by andrzej on 07.11.2016.
 */
public class Movement extends Value<Boolean> {

	public Movement(Boolean value) {
		super(value);
	}

	public Movement(Boolean value, LocalDateTime timestamp) {
		super(value, timestamp);
	}

	public boolean isMovementDetected() {
		return getValue();
	}

}
