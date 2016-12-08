package tigase.rpi.home.sensors.pir;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import tigase.bot.AbstractDevice;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.rpi.home.IConfigurationAware;
import tigase.rpi.home.values.Movement;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Created by andrzej on 23.10.2016.
 */
public class HC_SR501
		extends AbstractDevice<Movement>
		implements Initializable, UnregisterAware, GpioPinListenerDigital, IConfigurationAware {

	private static final Logger log = Logger.getLogger(HC_SR501.class.getCanonicalName());

	@ConfigField(desc = "WiringPi Pin number")
	private int pin = 21;         // equals to broadcom 5

	@ConfigField(desc = "Time in miliseconds after which we should change state to inactive if there was no movement in this time")
	private long timeout = 5 * 60 * 1000;

	@Inject
	private ScheduledExecutorService scheduledExecutorService;

	private GpioController gpio;
	private GpioPinDigitalInput input;
	private ScheduledFuture future;

	public HC_SR501() {
		super("movement-sensor");
	}

	@Override
	public void beforeUnregister() {
		input.removeListener(this);
		input.unexport();
	}

	@Override
	public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
		if (event.getEdge() == PinEdge.RISING) {
			if (future != null) {
				future.cancel(false);
			}

			if (!isMovementDetected()) {
				updateValue(new Movement(true));
			}

			future = scheduledExecutorService.schedule(() -> updateValue(new Movement(false)), timeout,
													   TimeUnit.MILLISECONDS);
		}
	}

	public boolean isMovementDetected() {
		Movement val = getValue();

		if (val == null) {
			return false;
		}

		return val.getValue();
	}

	@Override
	public void initialize() {
		super.initialize();
		gpio = GpioFactory.getInstance();
		input = gpio.provisionDigitalInputPin(RaspiPin.getPinByAddress(pin), PinPullResistance.PULL_DOWN);
		input.setShutdownOptions(true);

		input.addListener(this);
	}
}
