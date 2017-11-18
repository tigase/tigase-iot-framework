package tigase.iot.framework.devices.demo;

import tigase.iot.framework.devices.AbstractSensor;
import tigase.iot.framework.devices.IConfigurationAware;
import tigase.iot.framework.devices.IExecutorDevice;
import tigase.iot.framework.values.OnOffState;
import tigase.kernel.beans.config.ConfigField;

import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Example implementation of a switch which may be used as an example for implementation of drivers for switches.
 */
public class DemoSwitch extends AbstractSensor<OnOffState> implements IConfigurationAware, IExecutorDevice<OnOffState> {

	private static final Logger log = Logger.getLogger(DemoSwitch.class.getCanonicalName());

	@ConfigField(desc = "Pin no.")
	private Integer pin = 2;

	@ConfigField(desc = "Enable with high state")
	private boolean enableWithHigh = true;

	@ConfigField(desc = "Initial state")
	private boolean initialValue = false;

	public DemoSwitch() {
		super("switch", "Switch", "Demo Switch");
	}

	@Override
	public void initialize() {
		this.setValue(new OnOffState(initialValue, LocalDateTime.MIN));
	}

	@Override
	public void setValue(OnOffState value) {
		OnOffState currentValue = getValue();
		if (currentValue != null && value.getValue() == currentValue.getValue()
				&& !value.getTimestamp().isAfter(currentValue.getTimestamp())) {
			return;
		}

		log.log(Level.INFO, "{0} - {1}, setting switch to {2}",
				new Object[]{this.getName(), getLabel(), value.getValue()});
		updateValue(value);

	}
}
