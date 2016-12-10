package tigase.rpi.home.app;

import com.pi4j.temperature.TemperatureScale;
import tigase.bot.Autostart;
import tigase.bot.iot.AbstractSensor;
import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.rpi.home.devices.LightDimmer;
import tigase.rpi.home.sensors.light.BH1750;
import tigase.rpi.home.sensors.w1.DS1820;
import tigase.rpi.home.sensors.w1.W1Master;
import tigase.rpi.home.values.Temperature;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 24.10.2016.
 */
@Autostart
public class Test
		implements Initializable, UnregisterAware {

	private static final Logger log = Logger.getLogger(tigase.rpi.home.app.Test.class.getCanonicalName());

	@ConfigField(desc = "Bean name")
	private String name;

	@Inject
	private LightDimmer dimmer;

	@Inject
	private BH1750 sensor;

	@Inject(nullAllowed = true)
	private DS1820 tempSensor;

	@Inject
	private W1Master w1Master;

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
	public void lightChanged(AbstractSensor.ValueChangeEvent event) {
		log.log(Level.FINEST, "{0}, received event: {1}", new Object[]{this.name, event});
		if (event.source == sensor) {
			log.log(Level.FINEST, "{0}, got light level: {1}",
					new Object[]{this.name, (event.newValue == null ? "null" : event.newValue.getValue())});
			int lux = (Integer) (event.newValue.getValue());
			if (lux < 30) {
				log.log(Level.FINER, "{0}, setting light level to 60", this.name);
				dimmer.setValue(60);
			}
		}
		if (event.source == tempSensor) {
			log.log(Level.FINER, "{0}, got temparature: {1}\u2103", new Object[]{this.name, event.newValue == null
																							? "null"
																							: ((Temperature) event.newValue
																									.getValue()).getValue(
																									TemperatureScale.CELSIUS)});
		}
	}
}
