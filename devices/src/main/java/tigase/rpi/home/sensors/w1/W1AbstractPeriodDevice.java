package tigase.rpi.home.sensors.w1;

import tigase.bot.AbstractPeriodDevice;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.UnregisterAware;

/**
 * Created by andrzej on 23.10.2016.
 */
public abstract class W1AbstractPeriodDevice<T> extends AbstractPeriodDevice<T>
		implements W1Device<T>, Initializable, UnregisterAware {

	protected com.pi4j.io.w1.W1Device w1Device;

	protected W1AbstractPeriodDevice(long period) {
		super(period);
	}

	@Override
	public com.pi4j.io.w1.W1Device getW1Device() {
		return w1Device;
	}

	public void setW1Device(com.pi4j.io.w1.W1Device w1Device) {
		this.w1Device = w1Device;
		initialize();
	}

	@Override
	public void initialize() {
		if (w1Device == null) {
			return;
		}
		super.initialize();
	}
}
