/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.devices;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import java.time.Clock;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.iot.framework.client.Devices;
import tigase.iot.framework.client.Device;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.iot.framework.client.client.ClientFactory;
import tigase.iot.framework.client.client.FlexGrid;
import tigase.iot.framework.client.client.ui.TopBar;
import tigase.iot.framework.client.devices.TemperatureSensor;

/**
 *
 * @author andrzej
 */
public class DevicesListViewImpl extends Composite implements DevicesListView {

	private final ClientFactory factory;
	
	private final FlexGrid flexGrid;

	public DevicesListViewImpl(ClientFactory factory) {
		this.factory = factory;

		this.factory.jaxmpp().getEventBus().addHandler(Devices.ChangedHandler.ChangedEvent.class, new Devices.ChangedHandler() {
			@Override
			public void devicesChanged(List<Device> devices) {
				updateDevices(devices);
			}
		});
		
		//AbsolutePanel panel = new AbsolutePanel();
		DockLayoutPanel panel = new DockLayoutPanel(Style.Unit.EM);
		
		TopBar topBar = new TopBar("Devices", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				try {
					factory.jaxmpp().disconnect();
				} catch (JaxmppException ex) {
					Logger.getLogger(DevicesListViewImpl.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		});
		
		panel.addNorth(topBar, 2.2);
		
		flexGrid = new FlexGrid();
		
		panel.add(new ScrollPanel(flexGrid));
		
		initWidget(panel);
	}

	protected void updateDevices(List<Device> devices) {
		flexGrid.clear();
		
		for (Device device : devices) {
			if (device instanceof TemperatureSensor) {
				Thermometer item = new Thermometer(((TemperatureSensor) device));
				flexGrid.add(item);
			} else if (device instanceof tigase.iot.framework.client.devices.LightDimmer) {
				LightsDimmer item = new LightsDimmer((tigase.iot.framework.client.devices.LightDimmer) device);
				flexGrid.add(item);
			} else if (device instanceof tigase.iot.framework.client.devices.LightSensor) {
				LightSensor item = new LightSensor((tigase.iot.framework.client.devices.LightSensor) device);
				flexGrid.add(item);
			} else if (device instanceof tigase.iot.framework.client.devices.TvSensor) {
				TvIndicator item = new TvIndicator((tigase.iot.framework.client.devices.TvSensor) device);
				flexGrid.add(item);
			} else if (device instanceof tigase.iot.framework.client.devices.MovementSensor) {
				MovementSensor item = new MovementSensor((tigase.iot.framework.client.devices.MovementSensor) device);
				flexGrid.add(item);
			}
		}
	}
	
}
