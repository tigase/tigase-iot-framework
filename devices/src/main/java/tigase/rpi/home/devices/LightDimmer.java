package tigase.rpi.home.devices;

import tigase.bot.iot.AbstractSensor;
import tigase.bot.iot.IExecutorDevice;
import tigase.kernel.beans.config.ConfigField;
import tigase.rpi.home.IConfigurationAware;
import tigase.rpi.home.values.Light;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 24.10.2016.
 */
public class LightDimmer
		extends AbstractSensor<Light>
		implements IExecutorDevice<Light>, IConfigurationAware {

	private static final Logger log = Logger.getLogger(LightDimmer.class.getCanonicalName());

	private static final List<Integer> knownLightLevels = Arrays.asList(10, 20, 40, 60, 80,100);

	@ConfigField(desc = "Path to TransmitRF.py file")
	private String pathToTransmitRF;

	public LightDimmer() {
		super("light-dimmer");
	}

	public synchronized void setValue(Integer lightLevel) {
		setValue(new Light(lightLevel, Light.Unit.percent));
	}

	@Override
	public synchronized void setValue(Light light) {
		Light currentValue = getValue();
		if (currentValue != null && light.getValue() == currentValue.getValue()
				&& light.getTimestamp().isAfter(currentValue.getTimestamp())) {
			return;
		}
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, getName() + "{0}, setting value to {1} from {2}",
					new Object[]{getName(), light.getValue(), (currentValue == null ? 0 : currentValue.getValue())});
		}
		if (light.getValue() == 0) {
			sendCode("off");
			updateValue(light);
			return;
		}

		if (currentValue == null || currentValue.getValue() == 0) {
			sendCode("on");
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {

			}
		}
		//sendCode("on");
		for (int i = 0; i < knownLightLevels.size(); i++) {
			int level = knownLightLevels.get(i);

			if (light.getValue() <= level || (i + 1 == knownLightLevels.size())) {
				//sendCode(String.valueOf(level));
				sendCode(String.valueOf(level));
				updateValue(light);
				break;
			}
		}
	}

	protected void sendCode(String code) {
		try {
			Process process = new ProcessBuilder("python", pathToTransmitRF, code).start();
			int result = process.waitFor();
			log.log(Level.FINEST, "{0}, got result from RF transmitter = {1}", new Object[]{getName(), result});
			Thread.sleep(200);
		} catch (Exception e) {
			log.log(Level.WARNING, getName() + ", failed to send code " + code, e);
		}
	}

}
