package tigase.rpi.home.app;

import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.rpi.home.IConfigurationAware;
import tigase.rpi.home.app.pubsub.ExtendedPubSubNodesManager;
import tigase.rpi.home.app.pubsub.PubSubNodesManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 02.11.2016.
 */
public class LightSensorListener implements PubSubNodesManager.NodesObserver, Initializable, UnregisterAware,
											IConfigurationAware {

	private static final Logger log = Logger.getLogger(LightSensorListener.class.getCanonicalName());

	@ConfigField(desc = "Observed device nodes")
	private ArrayList<String> observes;

	@ConfigField(desc = "Bean name")
	private String name;

	@Inject
	private EventBus eventBus;

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<String> getObservedNodes() {
		return observes;
	}

	@Override
	public void initialize() {
		eventBus.registerAll(this);
	}

	@HandleEvent
	public void onValueChanged(ExtendedPubSubNodesManager.ValueChangedEvent event) {
		if (observes.contains(event.sourceId)) {
			log.log(Level.INFO, "received new event from " + event.sourceId + " with value " + event);
		}
	}
}
