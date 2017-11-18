package tigase.iot.framework.devices.demo;

import tigase.iot.framework.devices.AbstractSensor;
import tigase.iot.framework.devices.IConfigurationAware;
import tigase.iot.framework.devices.IExecutorDevice;
import tigase.iot.framework.values.Light;
import tigase.kernel.beans.config.ConfigField;

import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Example implementation of a dimmer which can be used as an example for implementation of drivers for dimmers.
 */
public class DemoDimmer extends AbstractSensor<Light> implements IConfigurationAware, IExecutorDevice<Light> {

	private static final Logger log = Logger.getLogger(DemoDimmer.class.getCanonicalName());

	@ConfigField(desc = "Pin no.")
	private Integer pin = 5;

	@ConfigField(desc = "Initial value in percent")
	private Integer initialValue = 0;

	public DemoDimmer() {
		super("light-dimmer", "Dimmer", "Demo Dimmer");
	}

	@Override
	public void initialize() {
		this.setValue(new Light(initialValue, Light.Unit.percent, LocalDateTime.MIN));
	}

	@Override
	public void setValue(Light light) {
		Light currentValue = getValue();
		if (currentValue != null && light.getValue() == currentValue.getValue()
				&& !light.getTimestamp().isAfter(currentValue.getTimestamp())) {
			return;
		}

		log.log(Level.INFO, "{0} - {1}, setting light level to {2}{3}",
				new Object[]{this.getName(), getLabel(), light.getValue(), light.getUnit()});
		updateValue(light);
	}
}
