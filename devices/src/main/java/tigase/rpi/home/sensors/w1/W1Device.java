package tigase.rpi.home.sensors.w1;

import tigase.rpi.home.IDevice;

/**
 * Created by andrzej on 24.10.2016.
 */
public interface W1Device<V> extends IDevice<V> {

	com.pi4j.io.w1.W1Device getW1Device();

	void setW1Device(com.pi4j.io.w1.W1Device device);

}
