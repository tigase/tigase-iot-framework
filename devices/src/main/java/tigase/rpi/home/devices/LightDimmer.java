package tigase.rpi.home.devices;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.RaspiPin;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.rpi.home.AbstractDevice;
import tigase.rpi.home.IExecutorDevice;
import tigase.rpi.home.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 24.10.2016.
 */
public class LightDimmer
		extends AbstractDevice<Integer>
		implements IExecutorDevice<Integer>, Initializable, UnregisterAware {

	private static final Logger log = Logger.getLogger(LightDimmer.class.getCanonicalName());

	private static final Map<Integer, String> levelToCode = new ConcurrentHashMap<>();

	private static final String OFF = "1101011101111101011011111111101110110111011";
	private static final String ON = "1101011101111101011011111111110110110111101";

	static {
		levelToCode.put(20, "110101110111110101101111111011011101101011011");
		levelToCode.put(60, "1101011101111101011011111110111110110101111");
	}

	@ConfigField(desc = "Pin used for transmission")
	private int transmitterPin;

	@ConfigField(desc = "Code repetitions")
	private int codeRetransmissions = 3;

	private GpioController gpio;
	private GpioPinDigitalOutput output;

	private static final long preambleTimeMS = 100;
	private static final long preambleDelayMS = 3;
	private static final int stepNS = 500000;
	private static final long finishTimeMS = 100;

	public void setValue(Integer lightLevel) {
		if (lightLevel == 0) {
			sendCode(OFF);
			updateValue(new Value<>(lightLevel));
			return;
		}

		List<Integer> levels = new ArrayList<>(levelToCode.keySet());
		Collections.sort(levels);

		sendCode(ON);
		for (int i = 0; i < levels.size(); i++) {
			int level = levels.get(i);

			if (lightLevel < level || (i + 1 == levels.size())) {
				sendCode(levelToCode.get(level));
				updateValue(new Value<>(level));
				break;
			}
		}
	}

	protected void sendCode(String codeStr) {
		System.runFinalization();
		System.gc();
		try {
			char[] code = codeStr.toCharArray();
			for (int i = 0; i < codeRetransmissions; i++) {
				output.high();
				Thread.sleep(preambleTimeMS);
				output.low();
				Thread.sleep(preambleDelayMS);
				switch (code[i]) {
					case '0':
						Thread.sleep(1);
					case '1':
						output.high();
						Thread.sleep(0, stepNS);
						output.low();
						Thread.sleep(0, stepNS);
					default:
						break;
				}
				Thread.sleep(finishTimeMS);
			}
		} catch (InterruptedException ex) {
			log.log(Level.WARNING, "Got interrupted during sending code: " + codeStr);
		}
	}

	@Override
	public void beforeUnregister() {
		output.unexport();
	}

	@Override
	public void initialize() {
		gpio = GpioFactory.getInstance();
		output = gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(transmitterPin));
		output.setShutdownOptions(true);
	}

}
