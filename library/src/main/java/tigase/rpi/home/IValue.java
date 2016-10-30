package tigase.rpi.home;

import java.time.LocalDateTime;

/**
 * Created by andrzej on 22.10.2016.
 */
public interface IValue<T> {

	LocalDateTime getTimestamp();

	T getValue();

}
