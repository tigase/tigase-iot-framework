package tigase.rpi.home.app;

import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.rpi.home.app.pubsub.DevicePubSubListener;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by andrzej on 02.11.2016.
 */
public class LightSensorListener implements DevicePubSubListener.DeviceListener, Initializable, UnregisterAware {

	private static final Logger log = Logger.getLogger(LightSensorListener.class.getCanonicalName());

	@ConfigField(desc = "Observed device nodes")
	private ArrayList<String> observes;

	@Inject
	private EventBus eventBus;

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
	}

	@Override
	public List<String> getObservedDevicesNodes() {
		return observes.stream().map(device -> device + "/state").collect(Collectors.toList());
	}

	@Override
	public void initialize() {
		eventBus.registerAll(this);
	}

	@HandleEvent
	public void onValueChanged(DevicePubSubListener.ValueChangedEvent event) {
		if (observes.contains(event.sourceId)) {
			log.log(Level.INFO, "received new event from " + event.sourceId + " with value " + event);
		}
	}
}
