package tigase.rpi.home.runtime;

import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.rpi.home.AbstractDevice;
import tigase.rpi.home.Autostart;
import tigase.rpi.home.devices.LightDimmer;
import tigase.rpi.home.sensors.light.BH1750;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 24.10.2016.
 */
@Autostart
public class Test
		implements Initializable, UnregisterAware {

	private static final Logger log = Logger.getLogger(Test.class.getCanonicalName());

	@Inject
	private LightDimmer dimmer;

	@Inject
	private BH1750 sensor;

	@Inject
	private EventBus eventBus;

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
	}

	@Override
	public void initialize() {
		eventBus.registerAll(this);
	}

	@HandleEvent
	public void lightChanged(AbstractDevice.ValueChangeEvent event) {
		log.log(Level.INFO, "Received event: " + event);
		if (event.source == sensor) {
			log.log(Level.INFO, "Got light level: " + (event.newValue == null ? "null" : event.newValue.getValue()));
			int lux = (Integer) (event.newValue.getValue());
			if (lux < 23) {
				log.log(Level.INFO, "Setting light level to 60");
			}
		}
	}
}
