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

//	private static final Map<Integer, String> levelToCode = new ConcurrentHashMap<>();

	private static final List<Integer> knownLightLevels = Arrays.asList(20, 60);

//	private static final String OFF = "1101011101111101011011111111101110110111011";
//	private static final String ON = "1101011101111101011011111111110110110111101";
//
//	static {
//		levelToCode.put(20, "110101110111110101101111111011011101101011011");
//		levelToCode.put(60, "1101011101111101011011111110111110110101111");
//	}
//
//	@ConfigField(desc = "Pin used for transmission")
//	private int transmitterPin;

//	@ConfigField(desc = "Code repetitions")
//	private int codeRetransmissions = 5;

	@ConfigField(desc = "Path to TransmitRF.py file")
	private String pathToTransmitRF;

//	private GpioController gpio;
//	private GpioPinDigitalOutput output;
//
//	private static final long preambleTimeMS = 100;
//	private static final long preambleDelayMS = 3;
//	private static final int stepNS = 500000;
//	private static final long finishTimeMS = 100;

	public void setValue(Integer lightLevel) {
		if (lightLevel == 0) {
			sendCode("off");
			updateValue(new Value<>(lightLevel));
			return;
		}

//		List<Integer> levels = new ArrayList<>(levelToCode.keySet());
//		Collections.sort(levels);

		sendCode("on");
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {

		}
		for (int i = 0; i < knownLightLevels.size(); i++) {
			int level = knownLightLevels.get(i);

			if (lightLevel < level || (i + 1 == knownLightLevels.size())) {
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
//		System.runFinalization();
//		System.gc();
//		try {
//			char[] code = codeStr.toCharArray();
//			for (int i = 0; i < codeRetransmissions; i++) {
//				output.high();
//				Thread.sleep(preambleTimeMS);
//				output.low();
//				Thread.sleep(preambleDelayMS);
//				for (int j = 0; j < code.length; j++) {
//					switch (code[j]) {
//						case '0':
//							Thread.sleep(1);
//						case '1':
//							output.high();
//							long time1 = System.nanoTime();
//							LockSupport.parkNanos(stepNS);
//							long time2 = System.nanoTime();
//							System.out.println("waited for " + stepNS + " = " + (time2 - time1));
//							//Thread.sleep(0, stepNS);
//							output.low();
//							LockSupport.parkNanos(stepNS);
//							//Thread.sleep(0, stepNS);
//						default:
//							break;
//					}
//				}
//				Thread.sleep(finishTimeMS);
//			}
//		} catch (InterruptedException ex) {
//			log.log(Level.WARNING, "Got interrupted during sending code: " + codeStr);
//		}
	}

}
