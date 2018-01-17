package tigase.iot.framework.client.client.devices;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface Icons
		extends ClientBundle {

	Icons INSTANCE = GWT.create(Icons.class);

	@Source("icons/humidity-64.png")
	ImageResource humidity();

	@Source("icons/lightbulb-64.png")
	ImageResource lightBulb();

	@Source("icons/lightsensor-64.png")
	ImageResource lightSensor();

	@Source("icons/pressuresensor-64.png")
	ImageResource pressureSensor();

	@Source("icons/proximity-64.png")
	ImageResource proximitySensor();

	@Source("icons/shutdown-64.png")
	ImageResource shutdown();

	@Source("icons/thermometer-64.png")
	ImageResource thermometer();

	@Source("icons/tv-64.png")
	ImageResource tv();

}
