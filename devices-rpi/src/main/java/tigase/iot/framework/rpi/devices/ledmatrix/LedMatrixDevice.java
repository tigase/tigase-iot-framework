package tigase.iot.framework.rpi.devices.ledmatrix;

import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;
import tigase.iot.framework.devices.AbstractSensor;
import tigase.iot.framework.devices.IConfigurationAware;
import tigase.iot.framework.devices.IExecutorDevice;
import tigase.iot.framework.values.LedMatrix;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LedMatrixDevice
		extends AbstractSensor<LedMatrix>
		implements IConfigurationAware, IExecutorDevice<LedMatrix> {

	private static final Logger log = Logger.getLogger(LedMatrixDevice.class.getCanonicalName());

	private SpiDevice spi;

	protected static byte[] getValues(byte position, byte[] buffer) {
		byte[] result = new byte[2 * 2];

		for (int deviceID = 0, j = 0; deviceID < 2; deviceID++) {
			result[j++] = (byte) (1 + position);
			result[j++] = buffer[(deviceID * 8) + position];
		}

		return result;
	}

	public LedMatrixDevice() {
		super("led-matrix", "LED Matrix", "LED Matrix");
	}

	@Override
	public void initialize() {
		super.initialize();
		try {
			log.config("Initializing SPI...");
			this.spi = SpiFactory.getInstance(SpiChannel.CS0, SpiDevice.DEFAULT_SPI_SPEED, SpiDevice.DEFAULT_SPI_MODE);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Cannot initialize", e);
			throw new RuntimeException(e);
		}

	}

	@Override
	public void setValue(LedMatrix value) {
		log.fine("Writing values to matrix");
		try {
			byte[] buffer = value.getValueAsArray();
			for (byte position = 0; position < 8; position++) {
				spi.write(getValues(position, buffer));
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Cannot update value", e);
			throw new RuntimeException(e);
		}
	}
}
