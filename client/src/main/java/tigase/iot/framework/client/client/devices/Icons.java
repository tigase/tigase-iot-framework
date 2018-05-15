package tigase.iot.framework.client.client.devices;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface Icons
		extends ClientBundle {

	static ImageResource getByCategory(String category, ImageResource defaultValue) {
		if (category == null) {
			return defaultValue;
		}
		switch (category) {
			case "lights-ceiling":
				return Icons.INSTANCE.lightsCeiling();
			case "lights-external":
				return Icons.INSTANCE.lightsExternal();
			case "lights-spotlight":
				return Icons.INSTANCE.lightsSpotlight();
			case "lights-table":
				return Icons.INSTANCE.lightsTable();
			case "socket":
				return Icons.INSTANCE.socket();
			default:
				return defaultValue;
		}
	}
	
	Icons INSTANCE = GWT.create(Icons.class);

	@Source("icons/humidity-64.png")
	ImageResource humidity();

	@Source("icons/lightbulb-64.png")
	ImageResource lightBulb();

	@Source("icons/lightsensor-64.png")
	ImageResource lightSensor();

	@Source("icons/lightsceiling-64.png")
	ImageResource lightsCeiling();

	@Source("icons/lightsexternal-64.png")
	ImageResource lightsExternal();

	@Source("icons/lightsspotlight-64.png")
	ImageResource lightsSpotlight();

	@Source("icons/lightstable-64.png")
	ImageResource lightsTable();

	@Source("icons/pressuresensor-64.png")
	ImageResource pressureSensor();

	@Source("icons/proximity-64.png")
	ImageResource proximitySensor();

	@Source("icons/socket-64.png")
	ImageResource socket();

	@Source("icons/shutdown-64.png")
	ImageResource shutdown();

	@Source("icons/thermometer-64.png")
	ImageResource thermometer();

	@Source("icons/tv-64.png")
	ImageResource tv();

	@Source("icons/unknown-64.png")
	ImageResource unknown();

}
