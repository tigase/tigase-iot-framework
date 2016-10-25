package tigase.rpi.home.sensors.light;

import com.pi4j.io.i2c.I2CDevice;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.UnregisterAware;
import tigase.rpi.home.sensors.I2CAbstractPeriodDevice;

import java.io.IOException;

/**
 * Created by andrzej on 23.10.2016.
 */
public class BH1750
		extends I2CAbstractPeriodDevice<Integer>
		implements Initializable, UnregisterAware {

	public BH1750() {
		super(60 * 1000);
	}

	@Override
	protected Integer readValue(I2CDevice device) throws IOException {
		device.write((byte) 0x10);

		byte[] data = new byte[2];

		int r = device.read(data, 0, 2);
		if (r != 2) {
			throw new RuntimeException("Read error: read only " + r + " bytes");
		}

		return (data[0] << 8) | data[1];
	}

}
