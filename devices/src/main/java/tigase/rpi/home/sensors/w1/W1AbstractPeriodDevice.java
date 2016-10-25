package tigase.rpi.home.sensors.w1;

import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.UnregisterAware;
import tigase.rpi.home.AbstractPeriodDevice;

/**
 * Created by andrzej on 23.10.2016.
 */
public abstract class W1AbstractPeriodDevice<T> extends AbstractPeriodDevice<T> implements W1Device<T>, Initializable, UnregisterAware {

	protected final com.pi4j.io.w1.W1Device w1Device;

	protected W1AbstractPeriodDevice(long period, com.pi4j.io.w1.W1Device w1Device) {
		super(period);
		this.w1Device = w1Device;
	}

	@Override
	public com.pi4j.io.w1.W1Device getW1Device() {
		return w1Device;
	}
}
