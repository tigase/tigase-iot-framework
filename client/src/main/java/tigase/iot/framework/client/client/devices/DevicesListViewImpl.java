/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.devices;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.iot.framework.client.Devices;
import tigase.iot.framework.client.Device;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.iot.framework.client.client.ClientFactory;
import tigase.iot.framework.client.client.FlexGrid;
import tigase.iot.framework.client.client.ui.MessageDialog;
import tigase.iot.framework.client.client.ui.TopBar;
import tigase.iot.framework.client.devices.TemperatureSensor;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule;

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
		
		devices.sort(new Comparator<Device>(){
			@Override
			public int compare(Device o1, Device o2) {
				return (o1.getName() != null ? o1.getName() : "").compareTo(o2.getName() != null ? o2.getName() : "");
			}			
		});

		for (Device device : devices) {
			if (device instanceof TemperatureSensor) {
				Thermometer item = new Thermometer(factory, ((TemperatureSensor) device));
				flexGrid.add(item);
			} else if (device instanceof tigase.iot.framework.client.devices.Switch) {
				Switch item = new Switch(factory, (tigase.iot.framework.client.devices.Switch) device);
				flexGrid.add(item);
			} else if (device instanceof tigase.iot.framework.client.devices.LightDimmer) {
				LightsDimmer item = new LightsDimmer(factory, (tigase.iot.framework.client.devices.LightDimmer) device);
				flexGrid.add(item);
			} else if (device instanceof tigase.iot.framework.client.devices.LightSensor) {
				LightSensor item = new LightSensor(factory, (tigase.iot.framework.client.devices.LightSensor) device);
				flexGrid.add(item);
			} else if (device instanceof tigase.iot.framework.client.devices.TvSensor) {
				TvIndicator item = new TvIndicator(factory, (tigase.iot.framework.client.devices.TvSensor) device);
				flexGrid.add(item);
			} else if (device instanceof tigase.iot.framework.client.devices.MovementSensor) {
				MovementSensor item = new MovementSensor(factory, (tigase.iot.framework.client.devices.MovementSensor) device);
				flexGrid.add(item);
			}
		}

		flexGrid.add(new AddDeviceItem());
	}

	public class AddDeviceItem extends Composite {

		private final Label icon;
		private final Label label;

		public AddDeviceItem() {
			FlowPanel item = new FlowPanel();
			item.setStylePrimaryName("flex-device-item");
			item.addStyleName("add-device");

			icon = new Label("\u2795");
			icon.setStylePrimaryName("icon");
			icon.getElement().getStyle().setFloat(Style.Float.NONE);

			item.add(icon);

			label = new Label();
			label.setStylePrimaryName("label");
			label.setText("Add device");
			item.add(label);

			initWidget(item);

			Widget w = asWidget();
			w.sinkEvents(Event.ONCLICK);
			w.addHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					clicked();
				}
			}, ClickEvent.getType());

		}

		public void clicked() {
			try {
				factory.devices().getActiveDeviceHosts(new Devices.DevicesInfoRetrieved() {
					@Override
					public void onDeviceInfoRetrieved(Map<JID, DiscoveryModule.Identity> devicesInfo) {
						if (devicesInfo.isEmpty()) {
							new MessageDialog("Error", "Could not found any IoT hosts.\nPlease check if IoT hosts are turned on.").show();
						} else {
							showContextMenu(devicesInfo);
						}
					}
				});
			} catch (JaxmppException ex) {
				Logger.getLogger(DevicesListViewImpl.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		protected void showContextMenu(Map<JID, DiscoveryModule.Identity> devicesInfo) {
			final DialogBox dialog = new DialogBox(true, true);
			dialog.setStylePrimaryName("dialog-window");

			FlowPanel panel = new FlowPanel();
			panel.setStylePrimaryName("context-menu");
			
			List<Map.Entry<JID, DiscoveryModule.Identity>> items = new ArrayList<>(devicesInfo.entrySet());
			items.sort(new Comparator<Map.Entry<JID, DiscoveryModule.Identity>>() {
				@Override
				public int compare(Map.Entry<JID, DiscoveryModule.Identity> o1, Map.Entry<JID, DiscoveryModule.Identity> o2) {
					return o1.getValue().getName().compareTo(o2.getValue().getName());
				}
			});
			
			for (Map.Entry<JID, DiscoveryModule.Identity> item : items) {
				Label itemLabel = new Label(item.getValue().getName());
				itemLabel.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						//Window.alert("select " + item.getValue().getName() + " at " + item.getKey());
						dialog.hide();
						new AddDeviceDlg(factory, item.getKey());
					}
				});
				panel.add(itemLabel);
			}

			dialog.setWidget(panel);
			dialog.center();

		}
	}

}
