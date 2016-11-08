package tigase.rpi.home.devices;

import tigase.bot.AbstractDevice;
import tigase.bot.IExecutorDevice;
import tigase.bot.Value;
import tigase.kernel.beans.config.ConfigField;
import tigase.rpi.home.IConfigurationAware;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 24.10.2016.
 */
public class LightDimmer
		extends AbstractDevice<Integer>
		implements IExecutorDevice<Integer>, IConfigurationAware {

	private static final Logger log = Logger.getLogger(LightDimmer.class.getCanonicalName());

	private static final List<Integer> knownLightLevels = Arrays.asList(20, 60);

	@ConfigField(desc = "Path to TransmitRF.py file")
	private String pathToTransmitRF;

	public synchronized void setValue(Integer lightLevel) {
		if (lightLevel == 0) {
			sendCode("off");
			updateValue(new Value<>(lightLevel));
			return;
		}

		sendCode("on");
		sendCode("on");
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {

		}
		for (int i = 0; i < knownLightLevels.size(); i++) {
			int level = knownLightLevels.get(i);

			if (lightLevel < level || (i + 1 == knownLightLevels.size())) {
				sendCode(String.valueOf(level));
				sendCode(String.valueOf(level));
				updateValue(new Value<>(level));
				break;
			}
		}
	}

	protected void sendCode(String code) {
		try {
			Process process = new ProcessBuilder("python", pathToTransmitRF, code).start();
			int result = process.waitFor();
			log.log(Level.FINEST, "{0}, got result from RF transmitter = {1}", new Object[]{getName(), result});
		} catch (Exception e) {
			log.log(Level.WARNING, getName() + ", failed to send code " + code, e);
		}
	}

}
