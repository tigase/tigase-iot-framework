package tigase.rpi.home.sensors.w1;

import tigase.bot.AbstractPeriodDevice;
import tigase.bot.IValue;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.UnregisterAware;

/**
 * Created by andrzej on 23.10.2016.
 */
public abstract class W1AbstractPeriodDevice<T extends IValue> extends AbstractPeriodDevice<T>
		implements W1Device<T>, Initializable, UnregisterAware {

	protected com.pi4j.io.w1.W1Device w1Device;

	protected W1AbstractPeriodDevice(String type, long period) {
		super(type, period);
	}

	@Override
	public com.pi4j.io.w1.W1Device getW1Device() {
		return w1Device;
	}

	@Override
	public void initialize() {
		if (w1Device == null) {
			w1Device = W1Master.KNOWN_DEVICES.get(getName());
		}
		super.initialize();
	}
}
