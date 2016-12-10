package tigase.rpi.home.sensors.w1;

import tigase.bot.iot.ISensor;
import tigase.bot.iot.IValue;

/**
 * Created by andrzej on 24.10.2016.
 */
public interface W1Device<V extends IValue> extends ISensor<V> {

	com.pi4j.io.w1.W1Device getW1Device();

}
