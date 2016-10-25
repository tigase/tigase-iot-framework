package tigase.rpi.home;

import java.time.LocalDateTime;

/**
 * Created by andrzej on 22.10.2016.
 */
public class Value<T> implements IValue<T> {

	private final LocalDateTime timestamp = LocalDateTime.now();
	private final T value;

	public Value(T value) {
		this.value = value;
	}

	@Override
	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	@Override
	public T getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "Value(" + value + ")";
	}
}
