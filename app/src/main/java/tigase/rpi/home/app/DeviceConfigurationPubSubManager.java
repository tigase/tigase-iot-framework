package tigase.rpi.home.app;

import tigase.bot.IDevice;
import tigase.rpi.home.IConfigurationAware;
import tigase.rpi.home.app.pubsub.AbstractConfigurationPubSubManager;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by andrzej on 04.11.2016.
 */
public class DeviceConfigurationPubSubManager
		extends AbstractConfigurationPubSubManager<IConfigurationAware> {

	private static final Logger log = Logger.getLogger(DeviceConfigurationPubSubManager.class.getCanonicalName());

	public DeviceConfigurationPubSubManager() {
		super();
		rootNode = "devices";
	}

	@Override
	public void setConfigurationAware(List<IConfigurationAware> configurationAware) {
		super.setConfigurationAware(
				configurationAware.stream().filter(aware -> aware instanceof IDevice).collect(Collectors.toList()));
	}
}
