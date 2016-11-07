package tigase.rpi.home.values;

import tigase.bot.Value;

import java.time.LocalDateTime;

/**
 * Created by andrzej on 05.11.2016.
 */
public class Light extends Value<Integer> {

	public Light(int value) {
		super(value);
	}

	public Light(int value, LocalDateTime timestamp) {
		super(value, timestamp);
	}

}
