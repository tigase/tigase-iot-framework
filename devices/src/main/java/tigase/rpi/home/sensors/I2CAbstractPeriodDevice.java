package tigase.rpi.home.sensors;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import tigase.bot.AbstractPeriodDevice;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.rpi.home.IConfigurationAware;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 23.10.2016.
 */
public abstract class I2CAbstractPeriodDevice<T> extends AbstractPeriodDevice<T>
		implements Initializable, UnregisterAware, IConfigurationAware {

	private static final Logger log = Logger.getLogger(I2CAbstractPeriodDevice.class.getCanonicalName());

	@ConfigField(desc = "Device address on I2C bus")
	private int address;

	@ConfigField(desc = "I2C Bus number")
	private int bus = I2CBus.BUS_1;

	private I2CBus i2cBus;

	public I2CAbstractPeriodDevice(long period) {
		super(period);
	}

	protected abstract T readValue(I2CDevice device) throws IOException;

	@Override
	protected T readValue() {
		try {
			I2CDevice device = i2cBus.getDevice(address);
			return readValue(device);
		} catch (IOException ex) {
			log.log(Level.WARNING, "Could not read data from I2C device at " + address, ex);
			return null;
		}
	}

	@Override
	public void initialize() {
		try {
			i2cBus = I2CFactory.getInstance(bus);
		} catch (I2CFactory.UnsupportedBusNumberException|IOException ex) {
			log.log(Level.WARNING, "Failed to retrieve I2C bus with number " + bus, ex);
			throw new RuntimeException("Failed to retrieve I2C bus with number " + bus, ex);
		}

		super.initialize();
	}

	@Override
	public void beforeUnregister() {
		super.beforeUnregister();
	}
}
